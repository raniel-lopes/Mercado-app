# 🏪 Mercado App - Sistema de Pagamentos com FiadoPay

---

## 📖 Contexto

Aplicação console Java que consome a API FiadoPay para processamento de pagamentos em um mercado. Implementa descoberta automática de plugins via reflexão, processamento assíncrono com threads e integração completa com gateway de pagamento.

---

## ✅ Requisitos Implementados

### **Fluxos Obrigatórios**
- ✅ **Cadastro de merchant** - Registro único no FiadoPay com webhook
- ✅ **Obtenção de token** - Autenticação OAuth2 com renovação automática
- ✅ **Criação de pagamentos** - PIX e CARD com validação de plugins
- ✅ **Consulta de pagamentos** - Por ID via API
- ✅ **Devolução (Refund)** - Estorno de pagamentos aprovados
- ✅ **Reconciliação** - Manual (opção 6) e automática (job agendado)

### **Anotações + Reflexão**
- ✅ `@PaymentMethod` - Marcação de plugins de pagamento
- ✅ `@AntiFraud` - Configuração de regras antifraude
- ✅ **PluginLoader** - Descoberta automática via classpath scanning

### **Threads**
- ✅ **JobScheduler** - `ScheduledExecutorService` com pool de 2 threads
- ✅ **TokenRefreshJob** - Renovação a cada 30 minutos
- ✅ **ReconciliationJob** - Verificação a cada 30 segundos
- ✅ **RetryPolicy** - Retentativas assíncronas com exponential backoff

---

## 🏗️ Arquitetura

```
src/main/java/com/mercado/
├── annotations/              # Anotações customizadas
│   ├── PaymentMethod.java   # @PaymentMethod(type, priority)
│   └── AntiFraud.java       # @AntiFraud(name, threshold, enabled)
│
├── client/                  # Integração HTTP
│   └── FiadoPayClient.java # Cliente REST do FiadoPay
│
├── domain/                  # DTOs
│   ├── MerchantCreateRequest.java
│   ├── MerchantResponse.java
│   ├── PaymentRequest.java
│   ├── PaymentResponse.java
│   ├── TokenRequest.java
│   └── TokenResponse.java
│
├── plugins/                 # Sistema de plugins
│   ├── PluginLoader.java   # Descoberta por reflexão
│   ├── antifraud/
│   │   ├── FraudRule.java
│   │   └── LargeAmountFraudRule.java
│   └── payment/
│       ├── PaymentProcessor.java
│       ├── PixPaymentPlugin.java
│       └── CardPaymentPlugin.java
│
├── jobs/                    # Processamento assíncrono
│   ├── JobScheduler.java
│   ├── TokenRefreshJob.java
│   └── ReconciliationJob.java
│
├── utils/
│   └── RetryPolicy.java     # Retry pattern
│
└── MercadoApp.java          # Main + Menu Console
```

---

## 🎯 Decisões de Design

### **1. Anotações Customizadas**

#### **@PaymentMethod**
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PaymentMethod {
    String type();              // "PIX", "CARD", "BOLETO"
    int priority() default 10;  // Ordem de processamento
    String description() default "";
}
```

**Metadados:**
- `type`: Identificador único do método
- `priority`: Ordem de preferência (menor = maior prioridade)
- `description`: Descrição legível

**Uso:**
```java
@PaymentMethod(type = "PIX", priority = 1, description = "Pagamento instantâneo")
public class PixPaymentPlugin implements PaymentProcessor { ... }
```

#### **@AntiFraud**
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AntiFraud {
    String name();                    // Nome da regra
    double threshold() default 0.0;   // Limite de valor
    boolean enabled() default true;   // Ativa/desativa
    String description() default "";
}
```

**Uso:**
```java
@AntiFraud(
    name = "large_amount_check", 
    threshold = 10000.0, 
    enabled = true,
    description = "Bloqueia pagamentos acima de R$ 10.000"
)
public class LargeAmountFraudRule implements FraudRule { ... }
```

---

### **2. Mecanismo de Reflexão**

**PluginLoader** - Descoberta automática de plugins:

```java
public class PluginLoader {
    public void loadPlugins(String basePackage) {
        // 1. Escaneia o classpath
        List<Class<?>> classes = findAllClasses(basePackage);
        
        // 2. Filtra classes anotadas
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(PaymentMethod.class)) {
                // 3. Instancia via reflexão
                PaymentMethod annotation = clazz.getAnnotation(PaymentMethod.class);
                PaymentProcessor processor = (PaymentProcessor) 
                    clazz.getDeclaredConstructor().newInstance();
                
                // 4. Registra no mapa
                paymentProcessors.put(annotation.type(), processor);
            }
        }
    }
}
```

**Vantagens:**
- ✅ Adicionar novos métodos sem alterar código existente
- ✅ Zero configuração XML/properties
- ✅ Descoberta automática em tempo de execução

---

### **3. Sistema de Threads**

#### **JobScheduler**
```java
private final ScheduledExecutorService scheduler = 
    Executors.newScheduledThreadPool(2);

// Token Refresh - a cada 30 minutos
scheduler.scheduleAtFixedRate(
    tokenRefreshJob, 
    0, 1800, TimeUnit.SECONDS
);

// Reconciliação - a cada 30 segundos
scheduler.scheduleAtFixedRate(
    reconciliationJob, 
    10, 30, TimeUnit.SECONDS
);
```

#### **TokenRefreshJob**
- Renova token antes de expirar (3600s → renova a cada 1800s)
- Thread-safe com `volatile String currentToken`

#### **ReconciliationJob**
- Monitora pagamentos PENDING
- `ConcurrentHashMap<String, String>` para thread-safety
- Remove pagamentos finalizados automaticamente

#### **RetryPolicy**
- Retentativas com exponential backoff
- Configurável: `new RetryPolicy(maxAttempts, initialDelay)`

---

### **4. Padrões Aplicados**

| Padrão | Onde | Por quê |
|--------|------|---------|
| **Strategy** | `PaymentProcessor` | Diferentes algoritmos de pagamento |
| **Factory** | `PluginLoader` | Criação de plugins via reflexão |
| **Singleton** | `FiadoPayClient` | Único cliente HTTP |
| **Template Method** | `FraudRule` | Estrutura comum para validações |
| **Retry** | `RetryPolicy` | Resiliência em falhas de rede |
| **Observer** | Jobs agendados | Monitoramento de estado |

---

## 🚀 Como Executar

### **Pré-requisitos**
- Java 17+
- Maven 3.6+
- FiadoPay rodando em `http://localhost:8080`

### **Passo 1: Configurar JAVA_HOME (Windows)**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

### **Passo 2: Rodar FiadoPay**
```bash
cd fiadopay
.\mvnw.cmd spring-boot:run
```

### **Passo 3: Compilar e Executar Mercado-App**
```bash
cd Mercado-app
mvn clean package
java -jar target/mercado-app-1.0-SNAPSHOT.jar
```

---

## 📊 Fluxo de Uso Completo

```
1. [Opção 1] Cadastrar Merchant
   → Gera: Merchant ID, Client ID, Client Secret
   
2. [Opção 2] Obter Token
   → Autentica com Client ID/Secret
   → Token válido por 3600 segundos
   
3. [Opção 10] Iniciar Jobs Automáticos
   → TokenRefreshJob inicia (30min)
   → ReconciliationJob inicia (30s)
   
4. [Opção 3] Criar Pagamento
   → Escolher método (PIX/CARD)
   → Validação antifraude local
   → Validação de dados (plugin)
   → Envio para FiadoPay
   → Adiciona ao monitoramento
   
5. [Automático] Reconciliação
   → Job consulta status a cada 30s
   → PENDING → APPROVED/DECLINED
   → Remove do monitoramento quando finalizado
   
6. [Opção 4] Consultar Pagamento
   → Consulta por ID
   → Exibe status atualizado
   
7. [Opção 5] Refund (se APPROVED)
   → Solicita devolução
   → Status muda para REFUNDED
```

---

## ⚠️ Limites Conhecidos

### **Técnicos:**
1. **Webhook não implementado como servidor HTTP**
   - O campo `webhookUrl` é cadastrado, mas não há listener ativo
   - Reconciliação usa polling (a cada 30s) como alternativa

2. **Token não persiste entre execuções**
   - Armazenado em memória (variável `currentToken`)
   - Ao reiniciar, precisa obter novo token

3. **Sem persistência de dados**
   - Pagamentos não salvos em banco
   - Histórico perdido ao encerrar aplicação

4. **Tratamento de erros básico**
   - Exceptions genéricas com `try-catch`
   - Poderia usar exceptions customizadas

### **Funcionais:**
1. **Limite antifraude fixo** (R$ 10.000)
   - Configurável via anotação, mas não há UI para alterar

2. **Jobs não pausam individualmente**
   - Ao iniciar (opção 10), ambos os jobs iniciam
   - Shutdown encerra todos

3. **Reconciliação infinita**
   - Não há limite de tempo para pagamentos PENDING
   - Poderia adicionar timeout

---

## 🎥 Evidências

Ver pasta `docs/prints/` para screenshots completos de:

1. ✅ Menu principal
2. ✅ Plugins carregados por reflexão
3. ✅ Cadastro de merchant
4. ✅ Obtenção de token
5. ✅ Criação de pagamento PIX
6. ✅ Criação de pagamento CARD com parcelamento
7. ✅ Consulta de pagamento
8. ✅ Reconciliação automática (logs)
9. ✅ Refund
10. ✅ Status dos jobs

Ver `docs/evidencias.md` para descrição detalhada de cada screenshot.

---

## 📦 Dependências

```xml
<dependencies>
    <!-- HTTP Client (Java 11+) - Nativo -->
    
    <!-- Jackson para JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    
    <!-- SLF4J + Logback para logs -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.7</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.11</version>
    </dependency>
</dependencies>
```

---

## 🔌 Extensibilidade

### **Adicionar novo método de pagamento:**

```java
@PaymentMethod(type = "BOLETO", priority = 3, description = "Boleto bancário")
public class BoletoPaymentPlugin implements PaymentProcessor {
    @Override
    public String processPayment(double amount, String details) {
        // Lógica de geração de boleto
        return "BOLETO-" + System.currentTimeMillis();
    }

    @Override
    public boolean validate(String details) {
        // Validação de dados do boleto
        return details != null && details.length() > 0;
    }

    @Override
    public String getType() {
        return "BOLETO";
    }
}
```

**Nenhuma alteração no código existente necessária!** O PluginLoader descobre automaticamente.

### **Adicionar nova regra antifraude:**

```java
@AntiFraud(
    name = "velocity_check",
    threshold = 5.0,
    enabled = true,
    description = "Máximo 5 transações por minuto"
)
public class VelocityFraudRule implements FraudRule {
    @Override
    public boolean evaluate(double amount, String merchantId, String method) {
        // Lógica de verificação de velocidade
        return true;
    }
}
```

---

## 📄 Licença

Este projeto foi desenvolvido para fins acadêmicos.
