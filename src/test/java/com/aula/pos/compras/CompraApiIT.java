package com.aula.pos.compras;

import com.aula.pos.compras.entity.Compra;
import com.aula.pos.compras.repository.*;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP + real OpenFeign + real MySQL. Only the upstream HTTP server is controlled. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CompraApiIT.RelogioTeste.class)
class CompraApiIT {
    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRUNO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 23, 12, 0);
    private static final AtomicReference<String> respostaExterna = new AtomicReference<>();
    private static final AtomicInteger statusExterno = new AtomicInteger(200);
    private static final AtomicInteger atraso = new AtomicInteger();
    private static final AtomicInteger consultasExternas = new AtomicInteger();
    private static final AtomicReference<String> rotaExterna = new AtomicReference<>();
    private static final HttpServer externo = iniciarServidor();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort int port;
    @Autowired CompraRepository compras;
    @Autowired ClienteRepository clientes;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @TestConfiguration
    static class RelogioTeste {
        @Bean @Primary
        Clock relogioControlado() {
            return Clock.fixed(AGORA.atZone(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("awesomeapi.url", () -> "http://127.0.0.1:" + externo.getAddress().getPort());
        registry.add("spring.cloud.openfeign.client.config.awesomeApi.readTimeout", () -> 500);
        registry.add("spring.datasource.url", () -> System.getenv().getOrDefault("TEST_DB_URL",
                "jdbc:mysql://127.0.0.1:3307/compras_internacionais_test?createDatabaseIfNotExist=true"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
    }

    private static HttpServer iniciarServidor() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool(Thread.ofPlatform().daemon().factory()));
            server.createContext("/", exchange -> {
                consultasExternas.incrementAndGet();
                rotaExterna.set(exchange.getRequestURI().getPath());
                String texto = respostaExterna.get();
                int status = statusExterno.get();
                try {
                    if (atraso.get() > 0) Thread.sleep(atraso.get());
                    byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException ignored) {
                    // Expected when the application times out before the controlled server replies.
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void preparar() {
        compras.deleteAllInBatch();
        statusExterno.set(200);
        atraso.set(0);
        consultasExternas.set(0);
        respostaExterna.set(cotacao("5.4000"));
    }

    @AfterEach
    void limpar() {
        compras.deleteAllInBatch();
    }

    @AfterAll
    static void encerrarServidor() { externo.stop(0); }

    private static String cotacao(String ask) {
        // bid intentionally differs from ask to verify the application uses the sale rate.
        return """
                {"USDBRL":{"code":"USD","codein":"BRL","bid":"1.00","ask":"%s",
                "create_date":"2026-10-16 17:00:00","timestamp":"1792166400"}}
                """.formatted(ask);
    }

    private HttpResponse<String> chamar(String metodo, String rota, String corpo) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rota))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> registrar(String valor) throws Exception {
        return chamar("POST", "/compras", "{\"clienteId\":\"" + ANA + "\",\"valorUSD\":" + valor + "}");
    }

    private JsonNode json(HttpResponse<String> response) { return JSON.readTree(response.body()); }

    private void erro(HttpResponse<String> response, int status, String codigo) {
        assertEquals(status, response.statusCode(), response.body());
        var body = json(response);
        assertEquals(status, body.path("status").asInt());
        assertEquals(codigo, body.path("codigo").asText());
        assertFalse(body.path("timestamp").asText().isBlank());
        assertFalse(body.path("mensagem").asText().isBlank());
        assertTrue(body.path("detalhes").isArray());
        assertFalse(body.has("trace"));
        assertFalse(body.has("exception"));
    }

    @Test
    void simulaExemploDoPdfComAskSemPersistir() throws Exception {
        var response = chamar("POST", "/compras/simular", "{\"valorUSD\":200.00}");
        assertEquals(200, response.statusCode(), response.body());
        var body = json(response);
        assertEquals(0, body.path("cotacaoUtilizada").decimalValue().compareTo(new BigDecimal("5.4")));
        assertEquals(0, new BigDecimal("1080.00").compareTo(body.path("valorConvertido").decimalValue()));
        assertEquals(0, new BigDecimal("21.60").compareTo(body.path("taxaServico").decimalValue()));
        assertEquals(0, new BigDecimal("108.00").compareTo(body.path("tributoSimulado").decimalValue()));
        assertEquals(0, new BigDecimal("1209.60").compareTo(body.path("valorFinalBRL").decimalValue()));
        assertEquals("2026-10-16", body.path("dataCotacao").asText());
        assertEquals("2026-10-16T17:00:00", body.path("dataHoraCotacao").asText());
        assertEquals("/json/last/USD-BRL", rotaExterna.get());
        assertEquals(0, compras.count());
    }

    @Test
    void registraPreservaHistoricoEConsultaLimiteSemNovaCotacao() throws Exception {
        var response = registrar("200.00");
        assertEquals(201, response.statusCode(), response.body());
        UUID compraId = UUID.fromString(json(response).path("id").asText());
        var entity = compras.findById(compraId).orElseThrow();
        assertEquals(0, entity.getCotacaoUtilizada().compareTo(new BigDecimal("5.4")));
        assertEquals(new BigDecimal("1209.60"), entity.getValorFinalBRL());
        assertEquals(AGORA, entity.getDataCompra());
        assertEquals(LocalDateTime.of(2026, 10, 16, 17, 0), entity.getDataHoraCotacao());
        respostaExterna.set(cotacao("9.99"));
        var historico = chamar("GET", "/clientes/" + ANA + "/compras", null);
        assertEquals(200, historico.statusCode());
        assertEquals(compraId.toString(), json(historico).get(0).path("id").asText());
        assertEquals(0, new BigDecimal("1209.60").compareTo(json(historico).get(0).path("valorFinalBRL").decimalValue()));
        var limite = chamar("GET", "/clientes/" + ANA + "/limite", null);
        assertEquals(0, new BigDecimal("1209.60").compareTo(json(limite).path("totalUtilizado").decimalValue()));
        assertEquals(0, new BigDecimal("8790.40").compareTo(json(limite).path("saldoDisponivel").decimalValue()));
        assertEquals(1, consultasExternas.get());
    }

    @Test
    void cadaCompraConsultaNovaCotacao() throws Exception {
        assertEquals(201, registrar("10").statusCode());
        respostaExterna.set(cotacao("6.0"));
        var segunda = registrar("10");
        assertEquals(201, segunda.statusCode());
        assertEquals(0, json(segunda).path("cotacaoUtilizada").decimalValue().compareTo(new BigDecimal("6")));
        assertEquals(2, consultasExternas.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"valorUSD\":0}", "{\"valorUSD\":-1}",
            "{\"valorUSD\":0.001}", "{\"clienteId\":\"invalido\",\"valorUSD\":10}",
            "{\"valorUSD\":\"abc\"}"})
    void rejeitaEntradaInvalidaSemConsultarOuGravar(String body) throws Exception {
        var response = chamar("POST", "/compras", body);
        assertEquals(400, response.statusCode(), response.body());
        assertFalse(json(response).has("trace"));
        assertEquals(0, compras.count());
        assertEquals(0, consultasExternas.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cotacaoUtilizada", "cotacao", "dataCotacao", "ask"})
    void naoAceitaCotacaoOuDataDoCliente(String campo) throws Exception {
        String payload = "{\"clienteId\":\"" + ANA + "\",\"valorUSD\":200,\"" + campo + "\":\"5.4\"}";
        erro(chamar("POST", "/compras", payload), 400, "JSON_INVALIDO");
        erro(chamar("POST", "/compras/simular", "{\"valorUSD\":200,\"" + campo + "\":\"5.4\"}"), 400, "JSON_INVALIDO");
        assertEquals(0, consultasExternas.get());
        assertEquals(0, compras.count());
    }

    @Test
    void retornaDetalhesDosCamposInvalidos() throws Exception {
        var response = chamar("POST", "/compras", "{}");
        erro(response, 400, "VALIDACAO");
        assertEquals(2, json(response).path("detalhes").size());
    }

    @Test
    void clienteInexistenteRetorna404NosTresEndpoints() throws Exception {
        UUID id = UUID.randomUUID();
        erro(chamar("POST", "/compras", "{\"clienteId\":\"" + id + "\",\"valorUSD\":10}"), 404, "CLIENTE_NAO_ENCONTRADO");
        erro(chamar("GET", "/clientes/" + id + "/compras", null), 404, "CLIENTE_NAO_ENCONTRADO");
        erro(chamar("GET", "/clientes/" + id + "/limite", null), 404, "CLIENTE_NAO_ENCONTRADO");
        assertEquals(0, consultasExternas.get());
        assertEquals(0, compras.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"USDBRL\":null}", "{\"USDBRL\":{}}",
            "{\"USDBRL\":{\"ask\":\"5.4\"}}", "{\"USDBRL\":{\"create_date\":\"2026-10-16 17:00:00\"}}",
            "{\"USDBRL\":{\"ask\":\"0\",\"create_date\":\"2026-10-16 17:00:00\"}}",
            "{\"USDBRL\":{\"ask\":\"abc\",\"create_date\":\"2026-10-16 17:00:00\"}}",
            "{\"USDBRL\":{\"ask\":\"5.4\",\"create_date\":\"invalida\"}}", "nao e json"})
    void respostaExternaInvalidaRetorna422SemPersistir(String body) throws Exception {
        respostaExterna.set(body);
        erro(registrar("200"), 422, "COTACAO_INVALIDA");
        assertEquals(0, compras.count());
    }

    @Test
    void indisponibilidadeExternaRetorna503SemDetalhesInternos() throws Exception {
        statusExterno.set(503);
        respostaExterna.set("{\"segredoInterno\":\"nao_expor\"}");
        var response = registrar("200");
        erro(response, 503, "COTACAO_INDISPONIVEL");
        assertFalse(response.body().contains("nao_expor"));
        assertEquals(0, compras.count());
    }

    @Test
    void timeoutExternoRetorna504SemPersistir() throws Exception {
        atraso.set(1200);
        erro(registrar("200"), 504, "COTACAO_TIMEOUT");
        assertEquals(0, compras.count());
    }

    @Test
    void simularNaoConsomeLimiteMesmoSeClienteJaEstiverNoLimite() throws Exception {
        inserir(ANA, "10000.00", AGORA.minusDays(1));
        var response = chamar("POST", "/compras/simular", "{\"valorUSD\":200}");
        assertEquals(200, response.statusCode());
        assertEquals(1, compras.count());
        var limite = chamar("GET", "/clientes/" + ANA + "/limite", null);
        assertEquals(0, new BigDecimal("0.00").compareTo(json(limite).path("saldoDisponivel").decimalValue()));
    }

    @Test
    void limiteUsaMesCalendarioEIsolaCliente() throws Exception {
        inserir(ANA, "9000.00", LocalDateTime.of(2026, 9, 30, 23, 59, 59));
        inserir(ANA, "100.00", LocalDateTime.of(2026, 10, 1, 0, 0));
        inserir(ANA, "200.00", LocalDateTime.of(2026, 10, 31, 23, 59, 59));
        inserir(ANA, "9000.00", LocalDateTime.of(2026, 11, 1, 0, 0));
        inserir(BRUNO, "5000.00", AGORA);
        var response = chamar("GET", "/clientes/" + ANA + "/limite", null);
        assertEquals("2026-10", json(response).path("mes").asText());
        assertEquals(0, new BigDecimal("300.00").compareTo(json(response).path("totalUtilizado").decimalValue()));
        assertEquals(0, new BigDecimal("9700.00").compareTo(json(response).path("saldoDisponivel").decimalValue()));
    }

    @Test
    void aceitaTotalExatamenteNoLimiteRejeitaCompraSeguinte() throws Exception {
        inserir(ANA, "8790.40", AGORA.minusDays(1));
        assertEquals(201, registrar("200").statusCode());
        erro(registrar("0.01"), 422, "LIMITE_MENSAL_EXCEDIDO");
        assertEquals(2, compras.count());
        var response = chamar("GET", "/clientes/" + ANA + "/limite", null);
        assertEquals(0, new BigDecimal("10000.00").compareTo(json(response).path("totalUtilizado").decimalValue()));
    }

    @Test
    void duasComprasConcorrentesNaoUltrapassamLimite() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Integer> tarefa = () -> { inicio.await(); return registrar("1000").statusCode(); };
            Future<Integer> primeira = executor.submit(tarefa);
            Future<Integer> segunda = executor.submit(tarefa);
            inicio.countDown();
            List<Integer> status = new ArrayList<>(List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)));
            Collections.sort(status);
            assertEquals(List.of(201, 422), status);
            assertEquals(1, compras.count());
            var limite = chamar("GET", "/clientes/" + ANA + "/limite", null);
            assertEquals(0, new BigDecimal("6048.00").compareTo(json(limite).path("totalUtilizado").decimalValue()));
        }
    }

    @Test
    void historicoVazioELimiteInicialSaoValidos() throws Exception {
        assertEquals(0, json(chamar("GET", "/clientes/" + ANA + "/compras", null)).size());
        var limite = chamar("GET", "/clientes/" + ANA + "/limite", null);
        assertEquals(0, new BigDecimal("0.00").compareTo(json(limite).path("totalUtilizado").decimalValue()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(json(limite).path("saldoDisponivel").decimalValue()));
        assertEquals(0, consultasExternas.get());
    }

    @Test
    void rotasEUuidInvalidosSaoPadronizados() throws Exception {
        erro(chamar("GET", "/clientes/nao-e-uuid/limite", null), 400, "PARAMETRO_INVALIDO");
        erro(chamar("GET", "/rota-inexistente", null), 404, "ROTA_NAO_ENCONTRADA");
        erro(chamar("DELETE", "/compras", null), 405, "METODO_NAO_PERMITIDO");
    }

    private void inserir(UUID clienteId, String total, LocalDateTime data) {
        var valor = new BigDecimal(total);
        var cliente = clientes.findById(clienteId).orElseThrow();
        compras.saveAndFlush(new Compra(cliente, BigDecimal.ONE, BigDecimal.ONE, valor,
                new BigDecimal("0.00"), new BigDecimal("0.00"), valor, data.minusDays(1), data));
    }
}
