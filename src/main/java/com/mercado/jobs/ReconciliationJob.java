package com.mercado.jobs;

import com.mercado.client.FiadoPayClient;
import com.mercado.domain.PaymentResponse;
import com.mercado.utils.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Job agendado que realiza reconciliação automática de pagamentos.
 * Consulta periodicamente pagamentos PENDING para atualizar status.
 */
public class ReconciliationJob implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationJob.class);

    private final FiadoPayClient client;
    private final RetryPolicy retryPolicy;
    private final ConcurrentHashMap<String, String> pendingPayments;

    public ReconciliationJob(FiadoPayClient client) {
        this.client = client;
        this.retryPolicy = new RetryPolicy(2, 500); // 2 tentativas, 500ms inicial
        this.pendingPayments = new ConcurrentHashMap<>();
    }

    /**
     * Adiciona um pagamento para ser monitorado
     */
    public void addPendingPayment(String paymentId) {
        pendingPayments.put(paymentId, "PENDING");
        logger.info("Pagamento {} adicionado para reconciliacao automatica", paymentId);
    }

    @Override
    public void run() {
        if (pendingPayments.isEmpty()) {
            logger.debug("[ReconciliationJob] Nenhum pagamento pendente para reconciliar");
            return;
        }

        logger.info("[ReconciliationJob] Iniciando reconciliacao de {} pagamento(s)...",
                pendingPayments.size());

        List<String> toRemove = new ArrayList<>();

        for (String paymentId : pendingPayments.keySet()) {
            try {
                PaymentResponse payment = retryPolicy.execute(
                        () -> {
                            try {
                                return client.getPayment(paymentId);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        "Consultar Pagamento " + paymentId
                );

                String oldStatus = pendingPayments.get(paymentId);
                String newStatus = payment.getStatus();

                if (!oldStatus.equals(newStatus)) {
                    logger.info("[ReconciliationJob] Status alterado: {} -> {} ({})",
                            paymentId, oldStatus, newStatus);

                    // Se não está mais PENDING, remove do monitoramento
                    if (!newStatus.equals("PENDING")) {
                        toRemove.add(paymentId);
                        logger.info("Pagamento {} finalizado: {}", paymentId, newStatus);
                    } else {
                        pendingPayments.put(paymentId, newStatus);
                    }
                }

            } catch (Exception e) {
                logger.error("[ReconciliationJob] Erro ao reconciliar {}: {}",
                        paymentId, e.getMessage());
            }
        }

        // Remove pagamentos finalizados
        toRemove.forEach(id -> {
            pendingPayments.remove(id);
            logger.info("Pagamento {} removido do monitoramento", id);
        });

        logger.info("[ReconciliationJob] Reconciliacao concluida. {} pagamento(s) ainda pendente(s)",
                pendingPayments.size());
    }

    public int getPendingCount() {
        return pendingPayments.size();
    }
}