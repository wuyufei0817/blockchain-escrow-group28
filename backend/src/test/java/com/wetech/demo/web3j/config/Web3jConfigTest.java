package com.wetech.demo.web3j.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.wetech.demo.web3j.service.SimpleStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionDecoder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Web3jConfigTest {
    private final ObjectMapper json = new ObjectMapper();
    private final List<String> calls = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> rawTransaction = new AtomicReference<>();
    private HttpServer server;
    private Credentials credentials;
    private String chainId = "0xecb8"; // 60600, independent of application settings
    private boolean chainRpcError;

    @BeforeEach
    void startRpcFixture() throws Exception {
        // A fresh throwaway test key exists only in memory; no funded account is used.
        credentials = Credentials.create(Keys.createEcKeyPair());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            JsonNode request = json.readTree(exchange.getRequestBody());
            String method = request.get("method").asText();
            calls.add(method);
            Object result = switch (method) {
                case "eth_chainId" -> chainId;
                case "eth_getTransactionCount" -> "0x0";
                case "eth_sendRawTransaction" -> {
                    rawTransaction.set(request.get("params").get(0).asText());
                    yield Hash.sha3(rawTransaction.get());
                }
                case "eth_getTransactionReceipt" -> Map.of(
                        "transactionHash", Hash.sha3(rawTransaction.get()),
                        "transactionIndex", "0x0", "blockHash", "0x" + "a".repeat(64),
                        "blockNumber", "0x1", "gasUsed", "0x5208",
                        "cumulativeGasUsed", "0x5208", "status", "0x1", "logs", List.of());
                default -> throw new IOException("Unexpected RPC method: " + method);
            };
            Map<String, Object> response = method.equals("eth_chainId") && chainRpcError
                    ? Map.of("jsonrpc", "2.0", "id", request.get("id"),
                        "error", Map.of("code", -32000, "message", "fixture unavailable"))
                    : Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", result);
            byte[] body = json.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopRpcFixture() {
        server.stop(0);
    }

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withUserConfiguration(Web3jConfig.class, SimpleStorageService.class)
                .withInitializer(context -> {
                    try {
                        context.getEnvironment().getPropertySources().addLast(
                                new ResourcePropertySource("classpath:application.properties"));
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                })
                .withPropertyValues(
                        "web3j.client-address=http://127.0.0.1:" + server.getAddress().getPort(),
                        "web3j.private-key=" + String.format("%064x", credentials.getEcKeyPair().getPrivateKey()));
    }

    @Test
    void storageWriteUsesConfiguredSignerChainAndCourseGasPrice() {
        context().run(context -> {
            assertThat(context).hasNotFailed();
            SimpleStorageService service = context.getBean(SimpleStorageService.class);
            service.loadContract("0x" + "1".repeat(40));
            assertThat(service.setValue(BigInteger.valueOf(100)).join().isStatusOK()).isTrue();
            SignedRawTransaction signed = (SignedRawTransaction) TransactionDecoder.decode(rawTransaction.get());
            assertThat(signed.getChainId()).isEqualTo(60600L);
            assertThat(signed.getFrom()).isEqualToIgnoringCase(credentials.getAddress());
            assertThat(signed.getGasPrice()).isEqualTo(BigInteger.valueOf(1_000_000));
            assertThat(signed.getGasLimit()).isEqualTo(BigInteger.valueOf(6_721_975));
            assertThat(calls).contains("eth_chainId", "eth_sendRawTransaction");
        });
    }

    @Test
    void wrongNetworkRejectsWriteBeforeBroadcast() {
        chainId = "0x89"; // a different network
        context().run(context -> {
            assertThat(context).hasNotFailed();
            SimpleStorageService service = context.getBean(SimpleStorageService.class);
            service.loadContract("0x" + "1".repeat(40));
            assertThatThrownBy(() -> service.setValue(BigInteger.ONE).join())
                    .hasRootCauseInstanceOf(IOException.class);
            assertThat(rawTransaction.get()).isNull();
            assertThat(calls).doesNotContain("eth_sendRawTransaction");
        });
    }

    @Test
    void rpcErrorRejectsWriteBeforeBroadcast() {
        chainRpcError = true;
        context().run(context -> {
            assertThat(context).hasNotFailed();
            SimpleStorageService service = context.getBean(SimpleStorageService.class);
            service.loadContract("0x" + "1".repeat(40));
            assertThatThrownBy(() -> service.setValue(BigInteger.ONE).join())
                    .hasRootCauseInstanceOf(IOException.class);
            assertThat(rawTransaction.get()).isNull();
            assertThat(calls).doesNotContain("eth_sendRawTransaction");
        });
    }

    @Test
    void blankPrivateKeyFailsStartupWithSafeConfigurationMessage() {
        context().withPropertyValues("web3j.private-key=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("WEB3J_PRIVATE_KEY");
            assertThat(calls).isEmpty();
        });
    }
}
