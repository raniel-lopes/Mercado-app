package com.mercado.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Sistema de retentativas com exponential backoff.
 * Tenta executar uma operação múltiplas vezes em caso de falha.
 */
public class RetryPolicy {

    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);

    private final int maxAttempts;
    private final long initialDelayMs;

    public RetryPolicy(int maxAttempts, long initialDelayMs) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
    }

    /**
     * Executa uma operação com retry automático
     *
     * @param operation Operação a ser executada
     * @param operationName Nome da operação (para logs)
     * @return Resultado da operação
     */
    public <T> T execute(Supplier<T> operation, String operationName) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("Tentativa {}/{} - {}", attempt, maxAttempts, operationName);
                T result = operation.get();

                if (attempt > 1) {
                    logger.info("Operacao bem-sucedida apos {} tentativa(s)!", attempt);
                }

                return result;

            } catch (Exception e) {
                lastException = e;
                logger.warn("Tentativa {}/{} falhou: {}", attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    long delay = calculateDelay(attempt);
                    logger.info("Aguardando {}ms antes da proxima tentativa...", delay);

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrompido", ie);
                    }
                }
            }
        }

        logger.error("Todas as {} tentativas falharam para: {}", maxAttempts, operationName);
        throw new RuntimeException("Falha apos " + maxAttempts + " tentativas", lastException);
    }

    /**
     * Calcula o delay com exponential backoff
     * Delay = initialDelay * 2^(attempt-1)
     */
    private long calculateDelay(int attempt) {
        return initialDelayMs * (long) Math.pow(2, attempt - 1);
    }

    /**
     * Executa uma operação void com retry
     */
    public void executeVoid(Runnable operation, String operationName) {
        execute(() -> {
            operation.run();
            return null;
        }, operationName);
    }
}