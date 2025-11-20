package com.mercado.plugins.antifraud;

/**
 * Interface que define o contrato para regras de antifraude.
 * Todas as implementações devem ser anotadas com @AntiFraud.
 */
public interface FraudRule {

    /**
     * Avalia se uma transação é suspeita
     * @param amount Valor da transação
     * @param merchantId ID do merchant
     * @param paymentMethod Método de pagamento usado
     * @return true se a transação for aprovada, false se suspeita
     */
    boolean evaluate(double amount, String merchantId, String paymentMethod);

    /**
     * Retorna o nome da regra
     */
    String getRuleName();

    /**
     * Retorna a descrição da regra
     */
    String getDescription();
}