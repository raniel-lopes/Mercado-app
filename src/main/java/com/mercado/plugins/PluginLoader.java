package com.mercado.plugins;

import com.mercado.annotations.AntiFraud;
import com.mercado.annotations.PaymentMethod;
import com.mercado.plugins.antifraud.FraudRule;
import com.mercado.plugins.payment.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Sistema de descoberta de plugins usando reflexão.
 * Escaneia o classpath e carrega automaticamente todas as classes
 * anotadas com @PaymentMethod e @AntiFraud.
 */
public class PluginLoader {

    private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

    private final Map<String, PaymentProcessor> paymentProcessors = new HashMap<>();
    private final List<FraudRule> fraudRules = new ArrayList<>();

    /**
     * Carrega todos os plugins do pacote especificado
     */
    public void loadPlugins(String basePackage) {
        logger.info("🔍 Iniciando descoberta de plugins no pacote: {}", basePackage);

        try {
            List<Class<?>> classes = findAllClasses(basePackage);

            for (Class<?> clazz : classes) {
                loadPaymentProcessor(clazz);
                loadFraudRule(clazz);
            }

            logger.info("✅ Plugins carregados com sucesso!");
            logger.info("   - {} métodos de pagamento", paymentProcessors.size());
            logger.info("   - {} regras de antifraude", fraudRules.size());

        } catch (Exception e) {
            logger.error("❌ Erro ao carregar plugins", e);
        }
    }

    /**
     * Carrega um plugin de método de pagamento se tiver a anotação @PaymentMethod
     */
    private void loadPaymentProcessor(Class<?> clazz) {
        if (clazz.isAnnotationPresent(PaymentMethod.class)) {
            try {
                PaymentMethod annotation = clazz.getAnnotation(PaymentMethod.class);
                PaymentProcessor processor = (PaymentProcessor) clazz.getDeclaredConstructor().newInstance();

                paymentProcessors.put(annotation.type(), processor);

                logger.info("✓ Plugin de pagamento carregado: {} (prioridade: {})",
                        annotation.type(), annotation.priority());

            } catch (Exception e) {
                logger.error("Erro ao carregar PaymentProcessor: {}", clazz.getName(), e);
            }
        }
    }

    /**
     * Carrega uma regra de antifraude se tiver a anotação @AntiFraud
     */
    private void loadFraudRule(Class<?> clazz) {
        if (clazz.isAnnotationPresent(AntiFraud.class)) {
            try {
                AntiFraud annotation = clazz.getAnnotation(AntiFraud.class);

                if (annotation.enabled()) {
                    FraudRule rule = (FraudRule) clazz.getDeclaredConstructor().newInstance();
                    fraudRules.add(rule);

                    logger.info("✓ Regra antifraude carregada: {} (threshold: R$ {})",
                            annotation.name(), annotation.threshold());
                } else {
                    logger.info("⊘ Regra antifraude desabilitada: {}", annotation.name());
                }

            } catch (Exception e) {
                logger.error("Erro ao carregar FraudRule: {}", clazz.getName(), e);
            }
        }
    }

    /**
     * Encontra todas as classes em um pacote (usa reflexão)
     */
    private List<Class<?>> findAllClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());

            if (directory.exists()) {
                findClassesInDirectory(directory, packageName, classes);
            }
        }

        return classes;
    }

    /**
     * Busca recursivamente classes em um diretório
     */
    private void findClassesInDirectory(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    logger.warn("Classe não encontrada: {}", file.getName());
                }
            }
        }
    }

    // === Getters ===

    /**
     * Retorna um processador de pagamento pelo tipo
     */
    public PaymentProcessor getPaymentProcessor(String type) {
        return paymentProcessors.get(type);
    }

    /**
     * Retorna todos os processadores de pagamento disponíveis
     */
    public Map<String, PaymentProcessor> getAllPaymentProcessors() {
        return Collections.unmodifiableMap(paymentProcessors);
    }

    /**
     * Retorna todas as regras de antifraude ativas
     */
    public List<FraudRule> getFraudRules() {
        return Collections.unmodifiableList(fraudRules);
    }
}