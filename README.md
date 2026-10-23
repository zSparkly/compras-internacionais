# Sistema de Compras Internacionais

Segundo projeto de Programação Backend II — exercício da aula de 17/10/2026.
API REST em Java e Spring Boot para registrar compras em USD, consultar a cotação
de venda na AwesomeAPI e calcular os valores em BRL, respeitando o limite mensal
de R$ 10.000,00 por cliente. O tributo de 10% é fictício e exclusivamente didático.

Este projeto é independente da API de eventos do primeiro exercício.

## Tecnologias e organização

- Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Cloud OpenFeign.
- MySQL 8 e Flyway para criação do esquema e inserção dos clientes de exemplo.
- Bean Validation nos DTOs de entrada e `@RestControllerAdvice` para os erros.
- `BigDecimal` nos cálculos e na persistência; nenhuma conta monetária usa `double`.
- Duas entidades: **Cliente** e **Compra**, com relacionamento bidirecional 1:N e carregamento LAZY.

```text
src/main/java/com/aula/pos/compras/
  client/       Cliente OpenFeign e DTO da AwesomeAPI
  config/       Relógio e fuso horário
  controller/   Contratos HTTP; recebem DTOs com @Valid
  dto/          Entradas, respostas e erros padronizados
  entity/       Cliente e Compra
  exception/    Exceções de negócio e handler global
  repository/   Persistência, soma mensal e bloqueio de cliente
  service/      Consulta/validação da cotação e regras de negócio
```

## 1. Preparar e executar

Instale o **JDK 21** e tenha um servidor **MySQL 8** disponível. O Maven Wrapper
está incluído, portanto não é necessário instalar Maven separadamente.

No PowerShell, dentro da pasta do projeto:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/compras-internacionais?createDatabaseIfNotExist=true'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'SUA_SENHA_LOCAL'
.\mvnw.cmd spring-boot:run
```

Se o banco estiver na porta 3307, altere essa porta em `DB_URL`. Use a senha do
seu servidor; se ela estiver vazia, use `$env:DB_PASSWORD = ''`.
Se necessário, configure `JAVA_HOME` para a pasta do JDK 21.

No Linux/macOS, configure as mesmas variáveis usando `export` e execute
`sh mvnw spring-boot:run`.

A aplicação inicia em **http://localhost:8081**. A porta pode ser alterada com
`SERVER_PORT`. Não existe página visual: este exercício implementa uma API REST.

### Banco de dados

O usuário configurado precisa ter permissão para criar a base e suas tabelas.
O Flyway aplica automaticamente:

1. `V1__criar_clientes_e_compras.sql`: tabelas `tb_clientes` e `tb_compras`, chave estrangeira e índice mensal.
2. `V2__inserir_clientes_de_exemplo.sql`: dois clientes fictícios para as requisições.

| Cliente | UUID | E-mail |
| --- | --- | --- |
| Ana Exemplo | `11111111-1111-1111-1111-111111111111` | ana@example.com |
| Bruno Exemplo | `22222222-2222-2222-2222-222222222222` | bruno@example.com |

As entidades validam nome obrigatório e e-mail obrigatório/válido. Os clientes são
previamente inseridos por migration: não há CRUD de clientes, conforme o enunciado.
Não é preciso importar um dump nem enviar o servidor de banco junto do código.

### Integração externa

O cliente `AwesomeApiClient` usa OpenFeign para realizar:

```text
GET https://economia.awesomeapi.com.br/json/last/USD-BRL
```

O DTO interpreta o objeto raiz `USDBRL`, lê `ask` como String e converte para
`BigDecimal`. **O campo `bid` não é utilizado.**
`create_date` é validado com o formato estrito `uuuu-MM-dd HH:mm:ss`.
Cada simulação e cada registro consultam a última cotação disponível; não existe
cotação fixa nem fallback com valor inventado.

Configurações opcionais por variável de ambiente:

| Variável | Padrão | Finalidade |
| --- | --- | --- |
| `AWESOMEAPI_URL` | `https://economia.awesomeapi.com.br` | URL-base do provedor |
| `AWESOMEAPI_CONNECT_TIMEOUT` | `3000` | Timeout de conexão, em milissegundos |
| `AWESOMEAPI_READ_TIMEOUT` | `5000` | Timeout de leitura, em milissegundos |
| `APP_ZONE_ID` | `America/Sao_Paulo` | Fuso usado para data de compra e mês-calendário |
| `SERVER_PORT` | `8081` | Porta da API |

Documentação do provedor: https://docs.awesomeapi.com.br/api-de-moedas.
O endpoint público pode sofrer cache e limitação de acesso do próprio provedor.
Em caso de falha, a aplicação devolve erro e não registra a compra.

## 2. Endpoints e exemplos

| Método | Rota | Resultado |
| --- | --- | --- |
| POST | `/compras/simular` | 200: consulta a cotação e calcula, sem persistir nem consumir limite |
| POST | `/compras` | 201: consulta cotação, calcula, verifica limite e persiste |
| GET | `/clientes/{id}/compras` | 200: histórico por data de compra decrescente |
| GET | `/clientes/{id}/limite` | 200: total utilizado e saldo do mês atual |

### Simular

```http
POST http://localhost:8081/compras/simular
Content-Type: application/json

{
  "valorUSD": 200.00
}
```

A simulação não exige cliente porque não registra a compra nem verifica o limite
de uma pessoa específica. Ela permite conferir os valores antes do registro.

### Registrar

```http
POST http://localhost:8081/compras
Content-Type: application/json

{
  "clienteId": "11111111-1111-1111-1111-111111111111",
  "valorUSD": 200.00
}
```

O valor deve ser positivo, com até 12 dígitos inteiros e 2 casas decimais.
Não envie cotação nem data de cotação: campos extras são rejeitados com 400,
inclusive `ask`, `cotacao`, `cotacaoUtilizada` e `dataCotacao`.

Para consultar os dados:

```http
GET http://localhost:8081/clientes/11111111-1111-1111-1111-111111111111/compras
GET http://localhost:8081/clientes/11111111-1111-1111-1111-111111111111/limite
```

`requests.http` contém essas requisições e exemplos inválidos. Também há uma
coleção importável em `docs/postman/Compras-Internacionais.postman_collection.json`.

## 3. Regras de negócio — conferência do enunciado

| Regra | Implementação |
| --- | --- |
| RN01 | Usa exclusivamente `USDBRL.ask` recebido pelo OpenFeign; não aceita cotação do cliente |
| RN02 | `valorConvertido = valorUSD × cotacaoUtilizada` |
| RN03 | Abaixo de US$ 100: 5% sobre convertido; a partir de US$ 100, inclusive: 2% |
| RN04 | Tributo didático de 10% sobre convertido |
| RN05 | `valorFinalBRL = valorConvertido + taxaServico + tributoSimulado` |
| RN06 | Soma dos valores finais do cliente no mês-calendário mais nova compra ≤ R$ 10.000,00 |
| RN07 | Histórico lê os valores persistidos, sem consultar nem recalcular pela cotação atual |
| RN08 | Falhas de validação, cotação ou limite não persistem a compra |

### Arredondamento e datas

Todos os valores em USD/BRL são representados com 2 casas decimais. A conversão é
arredondada primeiro, depois taxa e tributo são calculados sobre esse valor
convertido e arredondados individualmente. O total é a soma desses três valores.
A política é **RoundingMode.HALF_UP** em todas as etapas monetárias.
A cotação mantém sua precisão original, dentro de `DECIMAL(30,15)`, sem redução a
duas casas. Cotações fora da capacidade de armazenamento são rejeitadas, sem truncar.

O enunciado lista `dataCotacao` como `LocalDate`, mas também pede registrar o
horário de `create_date`. Por isso a compra guarda ambos: `dataCotacao` (data) e
`dataHoraCotacao` (data/hora original do provedor). `dataCompra` é o instante local
do registro, no fuso configurado.

O mês é delimitado pelo primeiro dia às 00:00, inclusive, e pelo primeiro dia do
mês seguinte, exclusivo. Não se usa janela móvel de 30 dias.

### Concorrência e imutabilidade

O registro é transacional. A linha do cliente é bloqueada com
`PESSIMISTIC_WRITE`; a soma e a inserção ocorrem na mesma transação, com isolamento
`READ_COMMITTED`. Assim, duas requisições concorrentes do mesmo cliente não
conseguem aprovar compras com base no mesmo saldo desatualizado.
O valor exatamente igual a R$ 10.000,00 é permitido.

Não há endpoint de edição/exclusão de compra. Seus campos são mapeados como
`updatable = false` e não possuem setters. A consulta retorna DTOs, sem expor
entidades ou serializar recursivamente o relacionamento.

### Exemplo ilustrativo do PDF

Se o provedor retornar **ask = 5,40**, uma compra de **US$ 200,00** gera:

| Parcela | Valor |
| --- | --- |
| Convertido | R$ 1.080,00 |
| Taxa de serviço (2%) | R$ 21,60 |
| Tributo didático (10%) | R$ 108,00 |
| Total | **R$ 1.209,60** |

Esse valor de cotação aparece apenas nos testes controlados; a execução normal
sempre consulta a AwesomeAPI.

## 4. Erros padronizados

O `GlobalExceptionHandler` centraliza `@ExceptionHandler` em um
`@RestControllerAdvice`. Exemplo de resposta:

```json
{
  "timestamp": "2026-10-23T12:00:00",
  "status": 400,
  "codigo": "VALIDACAO",
  "mensagem": "Dados de entrada inválidos",
  "detalhes": [
    { "campo": "valorUSD", "mensagem": "Valor em USD deve ser maior que zero" }
  ]
}
```

| HTTP | Código | Situação |
| --- | --- | --- |
| 400 | `VALIDACAO` | Campos obrigatórios, valor zero/negativo ou fora da precisão permitida |
| 400 | `JSON_INVALIDO` | JSON inválido, tipo incorreto ou campo extra proibido |
| 400 | `PARAMETRO_INVALIDO` | UUID inválido na rota |
| 404 | `CLIENTE_NAO_ENCONTRADO` | Cliente não cadastrado |
| 422 | `COTACAO_INVALIDA` | USDBRL/ask/create_date ausentes ou inválidos, cotação não positiva ou JSON externo inválido |
| 422 | `LIMITE_MENSAL_EXCEDIDO` | Nova compra ultrapassaria o limite |
| 503 | `COTACAO_INDISPONIVEL` | Erro HTTP ou falha de conexão do provedor |
| 504 | `COTACAO_TIMEOUT` | Timeout na consulta externa |
| 500 | `ERRO_INTERNO` | Mensagem genérica para erro inesperado |

As respostas não expõem stack trace, corpo de erro externo ou informações internas.

## 5. Testes

### Sem banco ou internet

```powershell
.\mvnw.cmd test
```

Executa cálculos, validação da resposta externa, constraints e regras do serviço
com dependências simuladas. Não precisa subir uma aplicação separada.

### HTTP + OpenFeign + MySQL real

Configure uma **base exclusiva de teste**, diferente da utilizada pela aplicação:

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3307/compras_internacionais_test?createDatabaseIfNotExist=true'
$env:TEST_DB_USERNAME = 'root'
$env:TEST_DB_PASSWORD = 'SUA_SENHA_LOCAL'
.\mvnw.cmd -Pintegracao verify
```

O perfil executa os testes unitários e de integração. Os testes de integração
usam HTTP real até os controllers, OpenFeign real para um servidor HTTP local
controlado e MySQL real para persistência. Apenas o provedor de câmbio é
controlado: isso permite reproduzir cotação 5,40, JSON inválido, indisponibilidade
e timeout sem depender da cotação ou disponibilidade do dia.

**A base de teste tem as compras apagadas antes/depois de cada cenário. Nunca
aponte `TEST_DB_URL` para uma base de uso real.** Os clientes de exemplo são mantidos.

O relógio é fixado somente nos testes de integração para reproduzir os limites de
mês. Em execução normal, a aplicação usa o relógio atual do fuso configurado.

### Roteiro de correção

- Exemplo de US$ 200 e ask 5,40; faixa de US$ 99,99, US$ 100,00 e US$ 100,01.
- Casas decimais, arredondamento e cálculo para valor pequeno.
- Simulação sem persistência ou consumo de limite.
- POST com persistência e armazenamento da cotação e da data/hora originais.
- Nova compra consulta nova cotação; histórico não recalcula valores.
- Cliente inexistente nos três endpoints que recebem cliente.
- Ausência de campos, valores inválidos e rejeição de cotação enviada no payload.
- Falhas externas, respostas incompletas, cotação não positiva, data inválida e timeout.
- Limite por cliente e mês-calendário, incluindo virada de mês.
- Aprovação exatamente no limite e rejeição da próxima compra, sem gravação parcial.
- Duas compras simultâneas: uma aprovada e outra rejeitada quando a soma ultrapassaria o limite.
- Erros com data/hora, status, código, mensagem e detalhes; sem stack trace.

Resultado verificado em **23/10/2026**: **68 testes aprovados**, sendo 36 unitários
e 32 de integração, sem falhas, erros ou testes ignorados, em MySQL 8.4.11.
Também foi realizada uma compra com a AwesomeAPI real, pela aplicação empacotada.
Os relatórios e as respostas reais estão em [docs/testes/VALIDACAO.md](docs/testes/VALIDACAO.md).

## 6. Entrega

O PDF informa prazo até **31/10/2026**, individualmente ou em dupla, e pede código
no GitHub com README de execução, banco e integração externa.
O e-mail de entrega é `southiagorm@gmail.com`, com o assunto exato:
**Segundo Exercício Pós Graduação BackEnd II**.
No corpo devem constar o nome completo de cada integrante e o link do repositório.
