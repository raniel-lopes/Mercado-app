package com.mercado.plugins.payment;

import com.mercado.annotations.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin para processar pagamentos com cartão de crédito/débito.
 * Descoberto automaticamente por reflexão através da anotação @PaymentMethod.
 */
@PaymentMethod(type = "CARD", priority = 2, description = "Pagamento com cartão de crédito/débito")
public class CardPaymentPlugin implements PaymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CardPaymentPlugin.class);

    @Override
    public String processPayment(double amount, String details) {
        logger.info("Processando pagamento com CARTÃO - Valor: R$ {}", amount);
        logger.info("Dados do cartão: {}", maskCardNumber(details));

        // Simulação de processamento
        String transactionId = "CARD-" + System.currentTimeMillis();

        logger.info("Pagamento com cartão processado com sucesso! ID: {}", transactionId);
        return transactionId;
    }

    @Override
    public boolean validate(String details) {
        // Validação simples: número do cartão deve ter pelo menos 13 dígitos
        if (details == null || details.replaceAll("\\D", "").length() < 13) {
            logger.warn("Número de cartão inválido");
            return false;
        }

        logger.info("Número de cartão válido");
        return true;
    }

    @Override
    public String getType() {
        return "CARD";
    }

    /**
     * Mascara o número do cartão para exibição segura
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String cleaned = cardNumber.replaceAll("\\D", "");
        return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
    }
}