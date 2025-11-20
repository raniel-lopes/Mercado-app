package com.mercado.domain;

/**
 * DTO para requisição de token
 */
public class TokenRequest {
    private String client_id;
    private String client_secret;

    public TokenRequest() {}

    public TokenRequest(String client_id, String client_secret) {
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    // Getters e Setters
    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }
}