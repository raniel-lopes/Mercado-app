package com.mercado;

import com.mercado.client.FiadoPayClient;
import com.mercado.domain.*;
import com.mercado.jobs.JobScheduler;
import com.mercado.jobs.ReconciliationJob;
import com.mercado.jobs.TokenRefreshJob;
import com.mercado.plugins.PluginLoader;
import com.mercado.plugins.antifraud.FraudRule;
import com.mercado.plugins.payment.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Scanner;

public class MercadoApp {

    private static final Logger logger = LoggerFactory.getLogger(MercadoApp.class);
    private static PluginLoader pluginLoader;
    private static FiadoPayClient fiadoPayClient;
    private static JobScheduler jobScheduler;

    // Armazena as credenciais do merchant
    private static MerchantResponse currentMerchant;
    private static TokenRefreshJob tokenRefreshJob;

    public static void main(String[] args) {
        logger.info("=== Mercado App - Sistema de Pagamentos ===");

        // Inicializa o cliente FiadoPay
        fiadoPayClient = new FiadoPayClient("http://localhost:8080");

        // Verifica se o FiadoPay está rodando
        System.out.println("\n[INFO] Verificando conexao com FiadoPay...");
        if (fiadoPayClient.isHealthy()) {
            System.out.println("[OK] FiadoPay esta online!");
        } else {
            System.out.println("[ERRO] FiadoPay nao esta rodando!");
            System.out.println("       Por favor, inicie o FiadoPay antes de continuar.");
            return;
        }

        // Inicializa o sistema de plugins
        pluginLoader = new PluginLoader();
        pluginLoader.loadPlugins("com.mercado.plugins");

        // Exibe plugins carregados
        System.out.println("\n[PLUGINS] Plugins Carregados:");
        pluginLoader.getAllPaymentProcessors().forEach((type, processor) -> {
            System.out.println("          - " + type);
        });

        System.out.println("\n[ANTIFRAUDE] Regras Ativas:");
        pluginLoader.getFraudRules().forEach(rule -> {
            System.out.println("             - " + rule.getRuleName() + ": " + rule.getDescription());
        });

        Scanner scanner = new Scanner(System.in);

        // Adiciona shutdown hook para encerrar jobs gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (jobScheduler != null) {
                logger.info("Encerrando jobs agendados...");
                jobScheduler.shutdown();
            }
        }));

        while (true) {
            System.out.println("\n=== Menu Principal ===");
            System.out.println("1. Cadastrar Merchant");
            System.out.println("2. Obter Token");
            System.out.println("3. Criar Pagamento");
            System.out.println("4. Consultar Pagamento");
            System.out.println("5. Devolucao (Refund)");
            System.out.println("6. Reconciliacao Manual");
            System.out.println("7. Testar Plugins");
            System.out.println("8. Ver Credenciais Atuais");
            System.out.println("9. Status dos Jobs");
            System.out.println("10. Iniciar/Parar Jobs Automaticos");
            System.out.println("0. Sair");
            System.out.print("\nDigite sua opcao: ");

            int opcao = scanner.nextInt();
            scanner.nextLine(); // Limpa o buffer

            try {
                switch (opcao) {
                    case 1:
                        cadastrarMerchant(scanner);
                        break;
                    case 2:
                        obterToken(scanner);
                        break;
                    case 3:
                        criarPagamento(scanner);
                        break;
                    case 4:
                        consultarPagamento(scanner);
                        break;
                    case 5:
                        realizarRefund(scanner);
                        break;
                    case 6:
                        reconciliacao(scanner);
                        break;
                    case 7:
                        testarPlugins(scanner);
                        break;
                    case 8:
                        verCredenciais();
                        break;
                    case 9:
                        verStatusJobs();
                        break;
                    case 10:
                        gerenciarJobs(scanner);
                        break;
                    case 0:
                        System.out.println("Encerrando aplicacao...");
                        if (jobScheduler != null) {
                            jobScheduler.shutdown();
                        }
                        scanner.close();
                        return;
                    default:
                        System.out.println("Opcao invalida. Tente novamente.");
                }
            } catch (Exception e) {
                logger.error("Erro ao executar operacao", e);
                System.out.println("[ERRO] " + e.getMessage());
            }
        }
    }

    /**
     * 1. Cadastrar Merchant no FiadoPay
     */
    private static void cadastrarMerchant(Scanner scanner) {
        System.out.println("\n=== Cadastro de Merchant ===");
        System.out.print("Nome do Merchant: ");
        String name = scanner.nextLine();

        System.out.print("URL do Webhook (ex: http://localhost:8081/webhooks): ");
        String webhookUrl = scanner.nextLine();

        // Valida se o webhook não está vazio
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            System.out.println("[AVISO] URL do Webhook eh obrigatoria! Usando padrao...");
            webhookUrl = "http://localhost:8081/webhooks";
        }

        try {
            currentMerchant = fiadoPayClient.registerMerchant(name, webhookUrl);

            System.out.println("\n[SUCESSO] Merchant cadastrado com sucesso!");
            System.out.println("=========================================");
            System.out.println("GUARDE ESTAS CREDENCIAIS:");
            System.out.println("   Merchant ID: " + currentMerchant.getId());
            System.out.println("   Client ID: " + currentMerchant.getClientId());
            System.out.println("   Client Secret: " + currentMerchant.getClientSecret());
            System.out.println("=========================================");
            System.out.println("\n[DICA] Use a opcao 2 para obter o token de autenticacao!");

        } catch (Exception e) {
            System.out.println("[ERRO] " + e.getMessage());
        }
    }

    /**
     * 2. Obter Token de Autenticação
     */
    private static void obterToken(Scanner scanner) {
    System.out.println("\n=== Obter Token ===");

    String clientId, clientSecret;

    if (currentMerchant != null) {
        System.out.println("Usar credenciais do merchant atual? (S/N)");
        String resposta = scanner.nextLine();

        if (resposta.equalsIgnoreCase("S")) {
            clientId = currentMerchant.getClientId();
            clientSecret = currentMerchant.getClientSecret();
        } else {
            System.out.print("Client ID: ");
            clientId = scanner.nextLine();
            System.out.print("Client Secret: ");
            clientSecret = scanner.nextLine();
        }
    } else {
        System.out.print("Client ID: ");
        clientId = scanner.nextLine();
        System.out.print("Client Secret: ");
        clientSecret = scanner.nextLine();
    }

    try {
        TokenResponse token = fiadoPayClient.getToken(clientId, clientSecret);

        // Cria o TokenRefreshJob se não existir
        if (tokenRefreshJob == null) {
            tokenRefreshJob = new TokenRefreshJob(fiadoPayClient, clientId, clientSecret);
            System.out.println("\n[DICA] Voce pode iniciar os jobs automaticos na opcao 10!");
        }
        
        
        tokenRefreshJob.setCurrentToken(token.getAccess_token());

        System.out.println("\n[SUCESSO] Token obtido com sucesso!");
        System.out.println("          Token: " + token.getAccess_token());
        System.out.println("          Expira em: " + token.getExpires_in() + " segundos");

    } catch (Exception e) {
        System.out.println("[ERRO] " + e.getMessage());
    }
}

    /**
     * 3. Criar Pagamento (com plugins + antifraude + FiadoPay)
     */
    private static void criarPagamento(Scanner scanner) {
        System.out.println("\n=== Criar Pagamento ===");

        String currentToken = getCurrentToken();
        if (currentToken == null) {
            System.out.println("[AVISO] Voce precisa obter um token primeiro! (Opcao 2)");
            return;
        }

        System.out.print("Valor: R$ ");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        System.out.println("\nMetodos disponiveis:");
        pluginLoader.getAllPaymentProcessors().keySet().forEach(type -> {
            System.out.println("  - " + type);
        });

        System.out.print("Escolha o metodo: ");
        String method = scanner.nextLine().toUpperCase();

        PaymentProcessor processor = pluginLoader.getPaymentProcessor(method);
        if (processor == null) {
            System.out.println("[ERRO] Metodo de pagamento nao encontrado!");
            return;
        }

        // 1. Validar com regras antifraude localmente
        String merchantId = currentMerchant != null ? currentMerchant.getId().toString() : "UNKNOWN";
        for (FraudRule rule : pluginLoader.getFraudRules()) {
            if (!rule.evaluate(amount, merchantId, method)) {
                System.out.println("[BLOQUEADO] Pagamento bloqueado: " + rule.getDescription());
                return;
            }
        }

        System.out.print("Detalhes do pagamento (chave PIX, cartao, etc): ");
        String details = scanner.nextLine();

        // 2. Validar dados do pagamento com o plugin
        if (!processor.validate(details)) {
            System.out.println("[ERRO] Dados de pagamento invalidos!");
            return;
        }

        // 3. Processar localmente (simulação)
        String localTxId = processor.processPayment(amount, details);

        // 4. Parcelamento (se for cartão)
        Integer installments = 1;
        if (method.equals("CARD")) {
            System.out.print("Numero de parcelas (1-12): ");
            installments = scanner.nextInt();
            scanner.nextLine();
        }

        // 5. Enviar para o FiadoPay
        try {
            PaymentRequest paymentRequest = new PaymentRequest(
                    method,
                    "BRL",
                    BigDecimal.valueOf(amount),
                    installments,
                    localTxId
            );

            PaymentResponse payment = fiadoPayClient.createPayment(
                    currentToken,
                    paymentRequest
            );

            // Adiciona ao job de reconciliação se estiver PENDING
            if (payment.getStatus().equals("PENDING") && jobScheduler != null) {
                jobScheduler.getReconciliationJob().addPendingPayment(payment.getId());
            }

            System.out.println("\n[SUCESSO] Pagamento criado com sucesso!");
            System.out.println("=========================================");
            System.out.println("   ID: " + payment.getId());
            System.out.println("   Status: " + payment.getStatus());
            System.out.println("   Metodo: " + payment.getMethod());
            System.out.println("   Valor: R$ " + payment.getAmount());
            if (payment.getInterestRate() != null && payment.getInterestRate() > 0) {
                System.out.println("   Parcelas: " + payment.getInstallments() + "x");
                System.out.println("   Taxa de juros: " + payment.getInterestRate() + "%");
                System.out.println("   Total com juros: R$ " + payment.getTotal());
            }
            System.out.println("=========================================");

            if (payment.getStatus().equals("PENDING")) {
                System.out.println("\n[INFO] Pagamento sera monitorado automaticamente pelo ReconciliationJob!");
            }

        } catch (Exception e) {
            System.out.println("[ERRO] Erro ao criar pagamento: " + e.getMessage());
        }
    }

    /**
     * 4. Consultar Pagamento
     */
    private static void consultarPagamento(Scanner scanner) {
        System.out.println("\n=== Consultar Pagamento ===");
        System.out.print("ID do Pagamento: ");
        String paymentId = scanner.nextLine();

        try {
            PaymentResponse payment = fiadoPayClient.getPayment(paymentId);

            System.out.println("\n[DETALHES] Detalhes do Pagamento:");
            System.out.println("=========================================");
            System.out.println("   ID: " + payment.getId());
            System.out.println("   Status: " + payment.getStatus());
            System.out.println("   Metodo: " + payment.getMethod());
            System.out.println("   Valor: R$ " + payment.getAmount());
            if (payment.getInstallments() > 1) {
                System.out.println("   Parcelas: " + payment.getInstallments() + "x");
                System.out.println("   Total: R$ " + payment.getTotal());
            }
            System.out.println("=========================================");

        } catch (Exception e) {
            System.out.println("[ERRO] " + e.getMessage());
        }
    }

    /**
     * 5. Realizar Refund (Devolução)
     */
    private static void realizarRefund(Scanner scanner) {
        System.out.println("\n=== Devolucao (Refund) ===");

        String currentToken = getCurrentToken();
        if (currentToken == null) {
            System.out.println("[AVISO] Voce precisa obter um token primeiro! (Opcao 2)");
            return;
        }

        System.out.print("ID do Pagamento: ");
        String paymentId = scanner.nextLine();

        System.out.print("Confirma a devolucao do pagamento " + paymentId + "? (S/N): ");
        String confirmacao = scanner.nextLine();

        if (!confirmacao.equalsIgnoreCase("S")) {
            System.out.println("[CANCELADO] Operacao cancelada.");
            return;
        }

        try {
            String result = fiadoPayClient.refundPayment(currentToken, paymentId);
            System.out.println("\n[SUCESSO] Refund solicitado com sucesso!");
            System.out.println("          Resposta: " + result);

        } catch (Exception e) {
            System.out.println("[ERRO] " + e.getMessage());
        }
    }

    /**
     * 6. Reconciliação Manual (consulta múltiplos pagamentos)
     */
    private static void reconciliacao(Scanner scanner) {
        System.out.println("\n=== Reconciliacao de Pagamentos ===");
        System.out.println("Digite os IDs dos pagamentos separados por virgula:");
        System.out.print("IDs: ");
        String input = scanner.nextLine();

        String[] paymentIds = input.split(",");

        System.out.println("\n[RECONCILIACAO] Reconciliando " + paymentIds.length + " pagamento(s)...\n");

        for (String paymentId : paymentIds) {
            paymentId = paymentId.trim();
            try {
                PaymentResponse payment = fiadoPayClient.getPayment(paymentId);
                System.out.println("[OK] " + paymentId + " -> " + payment.getStatus() + " (R$ " + payment.getAmount() + ")");
            } catch (Exception e) {
                System.out.println("[ERRO] " + paymentId + " -> ERRO: " + e.getMessage());
            }
        }
    }

    /**
     * 7. Testar Plugins Locais
     */
    private static void testarPlugins(Scanner scanner) {
        System.out.println("\n[TESTE] Teste de Plugins");
        System.out.println("Testando PIX com R$ 100,00...");

        PaymentProcessor pixProcessor = pluginLoader.getPaymentProcessor("PIX");
        if (pixProcessor != null) {
            String txId = pixProcessor.processPayment(100.0, "chave@pix.com");
            System.out.println("Resultado: " + txId);
        }
    }

    /**
     * 8. Ver Credenciais Atuais
     */
    private static void verCredenciais() {
        System.out.println("\n=== Credenciais Atuais ===");
        if (currentMerchant != null) {
            System.out.println("Merchant ID: " + currentMerchant.getId());
            System.out.println("Client ID: " + currentMerchant.getClientId());
            System.out.println("Client Secret: " + currentMerchant.getClientSecret());
        } else {
            System.out.println("[AVISO] Nenhum merchant cadastrado.");
        }

        String currentToken = getCurrentToken();
        if (currentToken != null) {
            System.out.println("Token: " + currentToken);
        } else {
            System.out.println("[AVISO] Nenhum token obtido.");
        }
    }

    /**
     * 9. Ver Status dos Jobs
     */
    private static void verStatusJobs() {
        System.out.println("\n=== Status dos Jobs Automaticos ===");

        if (jobScheduler == null) {
            System.out.println("[AVISO] Jobs automaticos nao estao rodando");
            System.out.println("[DICA] Use a opcao 10 para iniciar!");
            return;
        }

        System.out.println("[STATUS] Jobs automaticos ATIVOS\n");

        System.out.println("[JOB] TokenRefreshJob:");
        if (tokenRefreshJob != null) {
            String token = tokenRefreshJob.getCurrentToken();
            if (token != null) {
                System.out.println("      Token atual: " + token);
                System.out.println("      Status: ATIVO");
            } else {
                System.out.println("      Status: AGUARDANDO PRIMEIRA EXECUCAO");
            }
        }

        System.out.println("\n[JOB] ReconciliationJob:");
        ReconciliationJob reconJob = jobScheduler.getReconciliationJob();
        System.out.println("      Pagamentos monitorados: " + reconJob.getPendingCount());
        System.out.println("      Intervalo: 30 segundos");
        System.out.println("      Status: ATIVO");
    }

    /**
     * 10. Gerenciar Jobs Automáticos
     */
    private static void gerenciarJobs(Scanner scanner) {
        System.out.println("\n=== Gerenciar Jobs Automaticos ===");

        if (jobScheduler == null) {
            System.out.println("Jobs nao estao rodando. Deseja iniciar? (S/N)");
            String resposta = scanner.nextLine();

            if (resposta.equalsIgnoreCase("S")) {
                if (tokenRefreshJob == null) {
                    System.out.println("[AVISO] Voce precisa obter um token primeiro! (Opcao 2)");
                    return;
                }

                ReconciliationJob reconJob = new ReconciliationJob(fiadoPayClient);
                jobScheduler = new JobScheduler(tokenRefreshJob, reconJob);
                jobScheduler.start();

                System.out.println("\n[SUCESSO] Jobs automaticos iniciados com sucesso!");
                System.out.println("           TokenRefreshJob: executa a cada 30 minutos");
                System.out.println("           ReconciliationJob: executa a cada 30 segundos");
            }
        } else {
            System.out.println("Jobs estao rodando. Deseja parar? (S/N)");
            String resposta = scanner.nextLine();

            if (resposta.equalsIgnoreCase("S")) {
                jobScheduler.shutdown();
                jobScheduler = null;
                System.out.println("[SUCESSO] Jobs automaticos encerrados!");
            }
        }
    }

    /**
     * Retorna o token atual (do job ou null)
     */
    private static String getCurrentToken() {
        if (tokenRefreshJob != null && tokenRefreshJob.getCurrentToken() != null) {
            return tokenRefreshJob.getCurrentToken();
        }
        return null;
    }
}