package com.mercado.plugins.payment;

import com.mercado.annotations.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin para processar pagamentos via PIX.
 * Descoberto automaticamente por reflexão através da anotação @PaymentMethod.
 */
@PaymentMethod(type = "PIX", priority = 1, description = "Pagamento instantâneo via PIX")
public class PixPaymentPlugin implements PaymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PixPaymentPlugin.class);

    @Override
    public String processPayment(double amount, String details) {
        logger.info("Processando pagamento PIX - Valor: R$ {}", amount);
        logger.info("Chave PIX: {}", details);

        // Simulação de processamento
        String transactionId = "PIX-" + System.currentTimeMillis();

        logger.info("Pagamento PIX processado com sucesso! ID: {}", transactionId);
        return transactionId;
    }

    @Override
    public boolean validate(String details) {
        // Validação simples: chave PIX não pode ser vazia
        if (details == null || details.trim().isEmpty()) {
            logger.warn("Chave PIX inválida ou vazia");
            return false;
        }

        logger.info("Chave PIX válida");
        return true;
    }

    @Override
    public String getType() {
        return "PIX";
    }
}