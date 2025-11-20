package com.mercado.plugins.payment;

/**
 * Interface que define o contrato para plugins de métodos de pagamento.
 * Todas as implementações devem ser anotadas com @PaymentMethod.
 */
public interface PaymentProcessor {

    /**
     * Processa um pagamento
     * @param amount Valor do pagamento
     * @param details Detalhes adicionais (ex: número do cartão, chave PIX)
     * @return ID da transação ou código de confirmação
     */
    String processPayment(double amount, String details);

    /**
     * Valida se os dados do pagamento são válidos
     * @param details Detalhes do pagamento
     * @return true se válido
     */
    boolean validate(String details);

    /**
     * Retorna o tipo do método de pagamento
     */
    String getType();
}