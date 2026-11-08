package com.wetech.demo.web3j.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class NonceSerializationTest {
    @Test
    void sharedManagerSerializesNonceSelectionAndBroadcast() throws Exception {
        var json = new ObjectMapper();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var rpcThreads = Executors.newCachedThreadPool();
        var callers = Executors.newFixedThreadPool(2);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var requests = new AtomicInteger();
        var accepted = new AtomicInteger();
        List<BigInteger> nonces = new CopyOnWriteArrayList<>();
        server.setExecutor(rpcThreads);
        server.createContext("/", exchange -> {
            var q = json.readTree(exchange.getRequestBody());
            Object result;
            switch (q.path("method").asText()) {
                case "eth_chainId" -> result = "0xecb8";
                case "eth_getTransactionCount" -> {
                    int nonce = accepted.get();
                    if (requests.getAndIncrement() == 0) {
                        entered.countDown();
                        try { release.await(5, TimeUnit.SECONDS); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                    result = "0x" + Integer.toHexString(nonce);
                }
                case "eth_sendRawTransaction" -> {
                    String raw = q.path("params").get(0).asText();
                    nonces.add(TransactionDecoder.decode(raw).getNonce());
                    accepted.incrementAndGet();
                    result = Hash.sha3(raw);
                }
                default -> throw new IllegalStateException("Unexpected method");
            }
            byte[] body = json.writeValueAsBytes(Map.of("jsonrpc", "2.0", "id", q.get("id"), "result", result));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        var client = Web3j.build(new HttpService("http://127.0.0.1:" + server.getAddress().getPort()));
        try {
            var manager = new ChainCheckedTransactionManager(client, Credentials.create(Keys.createEcKeyPair()), 60600);
            Callable<?> send = () -> manager.sendTransaction(BigInteger.valueOf(1000000), BigInteger.valueOf(6721975),
                    "0x" + "1".repeat(40), "0x", BigInteger.ZERO, false);
            var first = callers.submit(send);
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            var second = callers.submit(send);
            try {
                assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            } finally { release.countDown(); }
            first.get(3, TimeUnit.SECONDS);
            second.get(3, TimeUnit.SECONDS);
            assertThat(nonces).containsExactly(BigInteger.ZERO, BigInteger.ONE);
        } finally {
            release.countDown();
            client.shutdown();
            server.stop(0);
            rpcThreads.shutdownNow();
            callers.shutdownNow();
        }
    }
}
