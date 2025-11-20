package com.mercado.domain;

/**
 * DTO para cadastro de mercado no FiadoPay
 */
public class MerchantCreateRequest {
    private String name;
    private String webhookUrl;

    public MerchantCreateRequest() {}

    public MerchantCreateRequest(String name, String webhookUrl) {
        this.name = name;
        this.webhookUrl = webhookUrl;
    }

    // Getters e Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}