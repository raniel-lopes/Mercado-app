package com.mercado.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercado.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Cliente HTTP para integração com a API do FiadoPay.
 * Realiza todas as operações: cadastro, autenticação, pagamentos, consultas e refund.
 */
public class FiadoPayClient {

    private static final Logger logger = LoggerFactory.getLogger(FiadoPayClient.class);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Construtor do cliente FiadoPay
     * @param baseUrl URL base da API (ex: http://localhost:8080)
     */
    public FiadoPayClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 1. Cadastra um novo merchant no FiadoPay
     */
    public MerchantResponse registerMerchant(String name, String webhookUrl) throws Exception {
        logger.info("Cadastrando merchant: {}", name);

        MerchantCreateRequest request = new MerchantCreateRequest(name, webhookUrl);
        String jsonBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/fiadopay/admin/merchants"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            MerchantResponse merchant = objectMapper.readValue(response.body(), MerchantResponse.class);
            logger.info("Merchant cadastrado com sucesso! ID: {}", merchant.getId());
            logger.info("Client ID: {}", merchant.getClientId());
            logger.info("Client Secret: {}", merchant.getClientSecret());
            return merchant;
        } else if (response.statusCode() == 409) {
            logger.error("Merchant ja existe com esse nome!");
            throw new Exception("Merchant ja existe com esse nome!");
        } else {
            logger.error("Erro ao cadastrar merchant. Status: {}", response.statusCode());
            throw new Exception("Erro ao cadastrar merchant: " + response.body());
        }
    }

    /**
     * 2. Obtém token de autenticação
     */
    public TokenResponse getToken(String clientId, String clientSecret) throws Exception {
        logger.info("Obtendo token para client_id: {}", clientId);

        TokenRequest request = new TokenRequest(clientId, clientSecret);
        String jsonBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/fiadopay/auth/token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            TokenResponse token = objectMapper.readValue(response.body(), TokenResponse.class);
            logger.info("Token obtido com sucesso!");
            logger.info("Access Token: {}", token.getAccess_token());
            logger.info("Expira em: {} segundos", token.getExpires_in());
            return token;
        } else {
            logger.error("Erro ao obter token. Status: {}", response.statusCode());
            throw new Exception("Falha na autenticacao: " + response.body());
        }
    }

    /**
     * 3. Cria um novo pagamento
     */
    public PaymentResponse createPayment(String token, PaymentRequest paymentRequest) throws Exception {
        return createPayment(token, paymentRequest, null);
    }

    /**
     * 3. Cria um novo pagamento com idempotência
     */
    public PaymentResponse createPayment(String token, PaymentRequest paymentRequest, String idempotencyKey) throws Exception {
        logger.info("Criando pagamento - Metodo: {} | Valor: R$ {}",
                paymentRequest.getMethod(), paymentRequest.getAmount());

        String jsonBody = objectMapper.writeValueAsString(paymentRequest);

        // Gera idempotency key se não fornecida
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/fiadopay/gateway/payments"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        HttpRequest httpRequest = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            PaymentResponse payment = objectMapper.readValue(response.body(), PaymentResponse.class);
            logger.info("Pagamento criado! ID: {} | Status: {}", payment.getId(), payment.getStatus());
            if (payment.getInterestRate() != null && payment.getInterestRate() > 0) {
                logger.info("Valor original: R$ {} | Total com juros: R$ {}",
                        payment.getAmount(), payment.getTotal());
            }
            return payment;
        } else {
            logger.error("Erro ao criar pagamento. Status: {}", response.statusCode());
            throw new Exception("Erro ao criar pagamento: " + response.body());
        }
    }

    /**
     * 4. Consulta um pagamento pelo ID
     */
    public PaymentResponse getPayment(String paymentId) throws Exception {
        logger.info("Consultando pagamento: {}", paymentId);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/fiadopay/gateway/payments/" + paymentId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            PaymentResponse payment = objectMapper.readValue(response.body(), PaymentResponse.class);
            logger.info("Pagamento encontrado! Status: {}", payment.getStatus());
            return payment;
        } else if (response.statusCode() == 404) {
            logger.error("Pagamento nao encontrado!");
            throw new Exception("Pagamento nao encontrado!");
        } else {
            logger.error("Erro ao consultar pagamento. Status: {}", response.statusCode());
            throw new Exception("Erro ao consultar pagamento: " + response.body());
        }
    }

    /**
     * 5. Realiza refund (devolução) de um pagamento
     */
    public String refundPayment(String token, String paymentId) throws Exception {
        logger.info("Solicitando refund do pagamento: {}", paymentId);

        String jsonBody = "{\"paymentId\":\"" + paymentId + "\"}";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/fiadopay/gateway/refunds"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.info("Refund solicitado com sucesso!");
            return response.body();
        } else if (response.statusCode() == 404) {
            logger.error("Pagamento nao encontrado!");
            throw new Exception("Pagamento nao encontrado para refund!");
        } else if (response.statusCode() == 403) {
            logger.error("Voce nao tem permissao para fazer refund deste pagamento!");
            throw new Exception("Sem permissao para refund!");
        } else {
            logger.error("Erro ao fazer refund. Status: {}", response.statusCode());
            throw new Exception("Erro ao fazer refund: " + response.body());
        }
    }

    /**
     * 6. Health check da API
     */
    public boolean isHealthy() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/fiadopay/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            logger.error("FiadoPay indisponivel: {}", e.getMessage());
            return false;
        }
    }
}