package com.mercado.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para marcar classes que implementam regras de antifraude.
 * Estas classes serão descobertas automaticamente por reflexão.
 *
 * Exemplo de uso:
 * @AntiFraud(name = "HighAmount", threshold = 10000.0, enabled = true)
 * public class HighAmountFraudRule implements FraudRule { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AntiFraud {

    /**
     * Nome da regra de antifraude
     */
    String name();

    /**
     * Valor limite para ativar a regra
     */
    double threshold() default 0.0;

    /**
     * Se a regra está ativa
     */
    boolean enabled() default true;

    /**
     * Descrição da regra
     */
    String description() default "";
}