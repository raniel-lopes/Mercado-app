package com.mercado.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gerenciador de jobs agendados com threads.
 * Controla a execução periódica de tarefas em background.
 */
public class JobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final TokenRefreshJob tokenRefreshJob;
    private final ReconciliationJob reconciliationJob;

    public JobScheduler(TokenRefreshJob tokenRefreshJob, ReconciliationJob reconciliationJob) {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.tokenRefreshJob = tokenRefreshJob;
        this.reconciliationJob = reconciliationJob;
    }

    /**
     * Inicia todos os jobs agendados
     */
    public void start() {
        logger.info("Iniciando JobScheduler...");

        // Job 1: Token Refresh - executa a cada 30 minutos (1800 segundos)
        // Token expira em 3600s, renovamos com 30min de antecedência
        scheduler.scheduleAtFixedRate(
                tokenRefreshJob,
                0,              // Delay inicial (executa imediatamente)
                1800,           // Período (30 minutos)
                TimeUnit.SECONDS
        );
        logger.info("TokenRefreshJob agendado: executa a cada 30 minutos");

        // Job 2: Reconciliation - executa a cada 30 segundos
        scheduler.scheduleAtFixedRate(
                reconciliationJob,
                10,             // Delay inicial de 10s
                30,             // Período (30 segundos)
                TimeUnit.SECONDS
        );
        logger.info("ReconciliationJob agendado: executa a cada 30 segundos");

        logger.info("JobScheduler iniciado com sucesso!");
    }

    /**
     * Para todos os jobs agendados
     */
    public void shutdown() {
        logger.info("Encerrando JobScheduler...");
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            logger.info("JobScheduler encerrado com sucesso!");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public ReconciliationJob getReconciliationJob() {
        return reconciliationJob;
    }

    public TokenRefreshJob getTokenRefreshJob() {
        return tokenRefreshJob;
    }
}