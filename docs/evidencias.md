# Evidências de Execução

Este documento apresenta prints de tela da aplicação em funcionamento, demonstrando todos os requisitos implementados.

## 1. Inicialização do Sistema

### 1.1. Menu Principal
![Menu Principal](prints/03-menu-principal.png)

**Descrição:** Menu interativo console com todas as funcionalidades disponíveis.

### 1.2. Plugins Carregados por Reflexão
![Plugins Carregados](prints/02-plugins-carregados.png)

**Descrição:** Sistema de plugins descobrindo automaticamente:
- `PixPaymentPlugin` (anotado com `@PaymentMethod(type="PIX")`)
- `CardPaymentPlugin` (anotado com `@PaymentMethod(type="CARD")`)
- `LargeAmountFraudRule` (anotado com `@AntiFraud`)

---

## 2. Fluxo de Autenticação

### 2.1. Cadastro de Merchant
![Cadastro](prints/01-cadastro-merchant.png)

**Descrição:** Cadastro do merchant no FiadoPay com URL do webhook.

### 2.2. Credenciais Geradas
![Credenciais](prints/04-credenciais-merchant.png)

**Descrição:** Client ID e Secret gerados automaticamente pelo FiadoPay.

### 2.3. Obtenção de Token
![Token](prints/05-obter-token.png)

**Descrição:** Token OAuth2 obtido com sucesso, válido por 3600 segundos.

---

## 3. Sistema de Threads

### 3.1. Jobs Automáticos Iniciados
![Jobs](prints/06-iniciar-jobs.png)

**Descrição:** 
- **TokenRefreshJob:** Renovação automática do token a cada 30 minutos
- **ReconciliationJob:** Verificação de status a cada 30 segundos

### 3.2. Status dos Jobs
![Status Jobs](prints/12-status-jobs.png)

**Descrição:** Monitoramento em tempo real dos jobs agendados usando `ScheduledExecutorService`.

---

## 4. Criação de Pagamentos

### 4.1. Pagamento PIX
![PIX](prints/07-criar-pagamento-pix.png)

**Descrição:** 
1. Validação antifraude (valor < R$ 10.000)
2. Validação do plugin PIX (chave não vazia)
3. Processamento local (simulação)
4. Envio para FiadoPay
5. Adição ao ReconciliationJob

### 4.2. Pagamento com Cartão (Parcelado)
![CARD](prints/08-criar-pagamento-card.png)

**Descrição:** 
- Pagamento em 3x com cálculo de juros (5%)
- Total atualizado: R$ 315,00

---

## 5. Consulta e Reconciliação

### 5.1. Consulta de Pagamento
![Consulta](prints/09-consultar-pagamento.png)

**Descrição:** Status atualizado de PENDING → APPROVED.

### 5.2. Reconciliação Automática (Logs)
![Reconciliação](prints/10-reconciliacao-logs.png)

**Descrição:** Job executando a cada 30 segundos:
- Detecta mudança de status
- Remove pagamento finalizado do monitoramento
- Thread-safe com `ConcurrentHashMap`

---

## 6. Refund (Devolução)

### 6.1. Solicitar Refund
![Refund](prints/11-refund.png)

**Descrição:** Estorno de pagamento aprovado, alterando status para REFUNDED.

---

## Resumo das Evidências

| Funcionalidade | Print | Status |
|----------------|-------|--------|
| Menu Principal | 03 | ✅ |
| Plugins por Reflexão | 02 | ✅ |
| Cadastro Merchant | 01, 04 | ✅ |
| Obtenção Token | 05 | ✅ |
| Jobs Agendados | 06, 12 | ✅ |
| Pagamento PIX | 07 | ✅ |
| Pagamento CARD | 08 | ✅ |
| Consulta | 09 | ✅ |
| Reconciliação | 10 | ✅ |
| Refund | 11 | ✅ |

**Total:** 12 evidências cobrindo 100% dos requisitos.