package com.mercado.jobs;

import com.mercado.client.FiadoPayClient;
import com.mercado.domain.TokenResponse;
import com.mercado.utils.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job agendado que renova o token de autenticação automaticamente.
 * Executa periodicamente para garantir que o token nunca expire.
 */
public class TokenRefreshJob implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshJob.class);

    private final FiadoPayClient client;
    private final String clientId;
    private final String clientSecret;
    private final RetryPolicy retryPolicy;
    private String currentToken;

    public TokenRefreshJob(FiadoPayClient client, String clientId, String clientSecret) {
        this.client = client;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.retryPolicy = new RetryPolicy(3, 1000); // 3 tentativas, 1s inicial
    }

    @Override
    public void run() {
        logger.info("[TokenRefreshJob] Iniciando renovacao de token...");

        try {
            TokenResponse token = retryPolicy.execute(
                    () -> {
                        try {
                            return client.getToken(clientId, clientSecret);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    "Refresh Token"
            );

            currentToken = token.getAccess_token();
            logger.info("[TokenRefreshJob] Token renovado com sucesso!");
            logger.info("Novo token: {}", currentToken);
            logger.info("Proxima renovacao em: {} segundos", token.getExpires_in() - 60);

        } catch (Exception e) {
            logger.error("[TokenRefreshJob] Falha ao renovar token!", e);
        }
    }

    public String getCurrentToken() {
        return currentToken;
    }
}