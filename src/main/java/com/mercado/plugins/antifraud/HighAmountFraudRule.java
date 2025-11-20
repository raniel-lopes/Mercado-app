package com.mercado.plugins.antifraud;

import com.mercado.annotations.AntiFraud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Regra de antifraude que bloqueia transações acima de um valor limite.
 * Descoberta automaticamente por reflexão através da anotação @AntiFraud.
 */
@AntiFraud(
        name = "HighAmount",
        threshold = 5000.0,
        enabled = true,
        description = "Bloqueia transações acima de R$ 5.000,00"
)
public class HighAmountFraudRule implements FraudRule {

    private static final Logger logger = LoggerFactory.getLogger(HighAmountFraudRule.class);
    private static final double THRESHOLD = 5000.0;

    @Override
    public boolean evaluate(double amount, String merchantId, String paymentMethod) {
        logger.info("Avaliando regra antifraude '{}' - Valor: R$ {}", getRuleName(), amount);

        if (amount > THRESHOLD) {
            logger.warn("⚠️ ALERTA DE FRAUDE: Transação bloqueada! Valor R$ {} excede o limite de R$ {}",
                    amount, THRESHOLD);
            logger.warn("Merchant: {} | Método: {}", merchantId, paymentMethod);
            return false; // Bloqueia
        }

        logger.info("✅ Transação aprovada pela regra '{}'", getRuleName());
        return true; // Aprova
    }

    @Override
    public String getRuleName() {
        return "HighAmount";
    }

    @Override
    public String getDescription() {
        return "Bloqueia transações acima de R$ " + THRESHOLD;
    }
}