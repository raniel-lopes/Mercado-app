package com.mercado.domain;

import java.math.BigDecimal;

/**
 * DTO para criação de pagamento
 */
public class PaymentRequest {
    private String method;
    private String currency;
    private BigDecimal amount;
    private Integer installments;
    private String metadataOrderId;

    public PaymentRequest() {}

    public PaymentRequest(String method, String currency, BigDecimal amount, Integer installments, String metadataOrderId) {
        this.method = method;
        this.currency = currency;
        this.amount = amount;
        this.installments = installments;
        this.metadataOrderId = metadataOrderId;
    }

    // Getters e Setters
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public String getMetadataOrderId() {
        return metadataOrderId;
    }

    public void setMetadataOrderId(String metadataOrderId) {
        this.metadataOrderId = metadataOrderId;
    }
}