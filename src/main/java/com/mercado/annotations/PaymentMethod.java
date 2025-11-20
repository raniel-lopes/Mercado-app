package com.mercado.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotações para marcar classes que implementam métodos de pagamento.
 * Estas classes serão descobertas automaticamente por reflexão.

 * Exemplo de uso:
 * @PaymentMethod(type = "PIX", priority = 1)
 * public class PixPayment implements PaymentProcessor {...}
 */

@Retention(RetentionPolicy.RUNTIME) //Disponível em tempo de execução
@Target(ElementType.TYPE)
public @interface PaymentMethod {

    /**
     * Tipo do método de pagamento (ex: "PIX", "CARD", "BOLETO")
     */
    String type();

    /**
     * Prioridade de processamento (menor = maior prioridade)
     */
    int priority() default 10;

    /**
     * Descrição do método de pagamento
     */
    String description() default "";


}
