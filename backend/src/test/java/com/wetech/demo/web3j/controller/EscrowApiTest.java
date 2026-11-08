package com.wetech.demo.web3j.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.wetech.demo.web3j.Application;
import com.wetech.demo.web3j.contracts.escrow.Escrow;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class EscrowApiTest {
    static final String ADDRESS="0x"+"1".repeat(40), OTHER="0x"+"2".repeat(40);
    final ObjectMapper json=new ObjectMapper();
    final List<JsonNode> calls=new CopyOnWriteArrayList<>();
    HttpServer server;
    Credentials account;
    volatile String chain="0xecb8", raw;
    volatile boolean failedReceipt, pendingReceipt, logError, noCode, incompatible, wrongHash;
    volatile boolean historicalCodeReturnsLatest, badCreationReceipt;
    final String hash="0x"+"a".repeat(64);
    final String creationHash="0x"+"b".repeat(64);

    @BeforeEach void start() throws Exception {
        account=Credentials.create(Keys.createEcKeyPair());
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            JsonNode q=json.readTree(exchange.getRequestBody()); calls.add(q);
            String method=q.path("method").asText();
            Object result=switch(method) {
                case "eth_chainId" -> chain;
                case "eth_blockNumber" -> "0x1d";
                case "eth_getCode" -> noCode || (!historicalCodeReturnsLatest && !q.path("params").get(1).asText().equals("latest") && Numeric.decodeQuantity(q.path("params").get(1).asText()).intValue()<5) ? "0x" : "0x6000";
                case "eth_getBlockByNumber" -> deploymentBlock(q.path("params").get(0).asText());
                case "eth_getTransactionCount" -> "0x0";
                case "eth_sendRawTransaction" -> {raw=q.path("params").get(0).asText();yield wrongHash ? hash : Hash.sha3(raw);}
                case "eth_getTransactionReceipt" -> q.path("params").get(0).asText().equals(creationHash) ? creationReceipt() : pendingReceipt ? null : receipt();
                case "eth_call" -> call(q.path("params").get(0));
                case "eth_getLogs" -> logs(q.path("params").get(0));
                default -> throw new IllegalStateException("Unexpected RPC method "+method);
            };
            Map<String,Object> reply=new LinkedHashMap<>();reply.put("jsonrpc","2.0");reply.put("id",q.get("id"));
            if(logError && method.equals("eth_getLogs")) reply.put("error",Map.of("code",-32000,"message","fixture log error"));
            else reply.put("result",result);
            byte[] body=json.writeValueAsBytes(reply);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);
            try(var out=exchange.getResponseBody()){out.write(body);}
        });server.start();
    }
    @AfterEach void stop(){server.stop(0);}
    Object deploymentBlock(String number){
        Map<String,Object> block=new LinkedHashMap<>();
        block.put("number",number);block.put("hash",hash);
        Map<String,Object> tx=new LinkedHashMap<>();
        tx.put("hash",creationHash);tx.put("to",null);tx.put("from",account.getAddress());
        block.put("transactions",Numeric.decodeQuantity(number).intValue()==5 ? List.of(tx) : List.of());
        return block;
    }
    Object creationReceipt(){
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("transactionHash",creationHash);r.put("blockHash",hash);
        r.put("blockNumber",badCreationReceipt?"0x6":"0x5");r.put("status","0x1");
        r.put("contractAddress",ADDRESS);r.put("to",null);r.put("logs",List.of());
        return r;
    }
    Object receipt(){
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("transactionHash",Hash.sha3(raw));r.put("transactionIndex","0x0");r.put("blockHash",hash);r.put("blockNumber","0x1d");
        r.put("from",account.getAddress());r.put("to",ADDRESS);r.put("contractAddress",OTHER);r.put("gasUsed","0x5208");r.put("cumulativeGasUsed","0x5208");r.put("status",failedReceipt?"0x0":"0x1");r.put("logs",List.of());return r;
    }
    String call(JsonNode tx){
        String data=tx.path("data").asText(tx.path("input").asText());
        if (incompatible) return "0x";
        if(data.startsWith("0x"+Hash.sha3String("arbiter()").substring(2,10))) return encode(new Address(account.getAddress()));
        if(data.startsWith("0x"+Hash.sha3String("orderCount()").substring(2,10))) return encode(new Uint256(2));
        if(data.startsWith("0x"+Hash.sha3String("resolve(uint256,bool)").substring(2,10))) return "0x08c379a0"+encode(new Utf8String("Escrow: wrong status")).substring(2);
        return encode(new Escrow.Order(OTHER,ADDRESS,OTHER,new BigInteger("30000000000000000000"),BigInteger.valueOf(2000000000),BigInteger.ONE,"course order"));
    }
    String encode(Type value){return "0x"+FunctionEncoder.encodeConstructor(List.of(value));}
    List<Map<String,Object>> logs(JsonNode filter){
        int from=Numeric.decodeQuantity(filter.path("fromBlock").asText()).intValue(),to=Numeric.decodeQuantity(filter.path("toBlock").asText()).intValue();
        List<Map<String,Object>> out=new ArrayList<>();
        for(int block:List.of(5,16,29)) if(block>=from && block<=to){
            Map<String,Object> e=new LinkedHashMap<>(); e.put("address",ADDRESS);e.put("blockNumber",Numeric.encodeQuantity(BigInteger.valueOf(block)));e.put("blockHash",hash);e.put("transactionHash","0x"+String.format("%064x",block));e.put("transactionIndex","0x0");e.put("logIndex","0x0");e.put("removed",false);
            e.put("topics",List.of(EventEncoder.encode(Escrow.ORDERCREATED_EVENT),Numeric.toHexStringWithPrefixZeroPadded(BigInteger.valueOf(block),64),"0x"+TypeEncoder.encode(new Address(OTHER)),"0x"+TypeEncoder.encode(new Address(ADDRESS))));
            e.put("data","0x"+FunctionEncoder.encodeConstructor(List.of(new Address(OTHER),new Uint256(30),new Uint256(2000000000))));out.add(e);out.add(e);
        }Collections.reverse(out);return out;
    }
    WebApplicationContextRunner context(){return new WebApplicationContextRunner().withUserConfiguration(Application.class).withPropertyValues(
            "web3j.client-address=http://127.0.0.1:"+server.getAddress().getPort(),
            "web3j.private-key="+String.format("%064x",account.getEcKeyPair().getPrivateKey()),"web3j.receipt-poll-interval-ms=1","web3j.receipt-poll-attempts=2");}
    MockMvc mvc(org.springframework.web.context.WebApplicationContext c){return MockMvcBuilders.webAppContextSetup(c).build();}
    JsonNode body(org.springframework.test.web.servlet.MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString());}
    void load(MockMvc m)throws Exception{assertThat(m.perform(post("/api/escrow/load").param("address",ADDRESS).param("fromBlock","5")).andReturn().getResponse().getStatus()).isEqualTo(200);}

    @Test void unloadedReturnsConflict(){context().run(c->{var m=mvc(c);assertThat(m.perform(get("/api/escrow/address")).andReturn().getResponse().getStatus()).isEqualTo(409);});}
    @Test void loadWorksWhenHistoricalCodeQueriesReturnLatestCode(){
        historicalCodeReturnsLatest=true;
        context().run(c->{load(mvc(c));assertThat(raw).isNull();});
    }
    @Test void historicalCodeQuirkCannotMakeWrongDeploymentBlockValid(){
        historicalCodeReturnsLatest=true;
        context().run(c->{var m=mvc(c);
            for(String block:List.of("4","6")){
                var r=m.perform(post("/api/escrow/load").param("address",ADDRESS).param("fromBlock",block)).andReturn();
                assertThat(r.getResponse().getStatus()).isEqualTo(400);
            }
            assertThat(raw).isNull();
        });
    }
    @Test void creationReceiptMustMatchTheRequestedBlock(){
        badCreationReceipt=true;
        context().run(c->{var r=mvc(c).perform(post("/api/escrow/load").param("address",ADDRESS).param("fromBlock","5")).andReturn();
            assertThat(r.getResponse().getStatus()).isEqualTo(400);assertThat(raw).isNull();
        });
    }
    @Test void loadAndReadOrderUseStringsAndStatusNames(){context().run(c->{var m=mvc(c);load(m);var r=m.perform(get("/api/escrow/orders/0")).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(200);var b=body(r);assertThat(b.path("amount").isTextual()).isTrue();assertThat(b.path("amount").asText()).isEqualTo("30000000000000000000");assertThat(b.path("status").asText()).isEqualTo("Paid");assertThat(body(m.perform(get("/api/escrow/address")).andReturn()).path("contractAddress").asText()).isEqualTo(ADDRESS);});}
    @Test void deploymentUsesBackendSignerAsArbiter(){context().run(c->{var r=mvc(c).perform(post("/api/escrow/deploy")).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(200);assertThat(body(r).path("contractAddress").asText()).isEqualTo(OTHER);var tx=(SignedRawTransaction)TransactionDecoder.decode(raw);assertThat(tx.getFrom()).isEqualToIgnoringCase(account.getAddress());assertThat(tx.getData()).endsWith(TypeEncoder.encode(new Address(account.getAddress())));assertThat(body(r).path("status").asText()).isEqualTo("0x1");});}
    @Test void listScansTenBlockRangesAndDeduplicatesEvents(){context().run(c->{var m=mvc(c);load(m);var r=m.perform(get("/api/escrow/orders")).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(200);assertThat(body(r).size()).isEqualTo(3);assertThat(body(r).get(0).path("id").asText()).isEqualTo("5");var ranges=calls.stream().filter(q->q.path("method").asText().equals("eth_getLogs")).map(q->q.path("params").get(0)).toList();assertThat(ranges).hasSize(3);assertThat(ranges.get(0).path("fromBlock").asText()).isEqualTo("0x5");assertThat(ranges.get(0).path("toBlock").asText()).isEqualTo("0xe");});}
    @Test void successfulResolutionReturnsReceipt(){context().run(c->{var m=mvc(c);load(m);var r=m.perform(post("/api/escrow/orders/0/resolve").param("sellerWins","false")).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(200);assertThat(body(r).path("transactionHash").asText()).isEqualTo(Hash.sha3(raw));assertThat(body(r).path("status").asText()).isEqualTo("0x1");});}
    @Test void revertedReceiptReturns422AndHash(){context().run(c->{var m=mvc(c);load(m);failedReceipt=true;var r=m.perform(post("/api/escrow/orders/0/resolve").param("sellerWins","false")).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(422);var b=body(r);assertThat(b.path("transactionHash").asText()).isEqualTo(Hash.sha3(raw));assertThat(b.path("status").asText()).isEqualTo("0x0");assertThat(b.path("revertReason").asText()).contains("wrong status");});}
    @Test void pendingReceiptReturns503WithHashAndNoResend(){context().run(c->{var m=mvc(c);load(m);pendingReceipt=true;var r=m.perform(post("/api/escrow/orders/0/resolve").param("sellerWins","true")).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(503);assertThat(body(r).path("transactionHash").asText()).isEqualTo(Hash.sha3(raw));assertThat(calls.stream().filter(q->q.path("method").asText().equals("eth_sendRawTransaction")).count()).isEqualTo(1);});}
    @Test void invalidInputsReturn400WithoutBroadcast(){context().run(c->{var m=mvc(c);assertThat(m.perform(post("/api/escrow/load").param("address","bad").param("fromBlock","5")).andReturn().getResponse().getStatus()).isEqualTo(400);load(m);assertThat(m.perform(post("/api/escrow/orders/-1/resolve").param("sellerWins","true")).andReturn().getResponse().getStatus()).isEqualTo(400);assertThat(m.perform(post("/api/escrow/orders/0/resolve").param("sellerWins","maybe")).andReturn().getResponse().getStatus()).isEqualTo(400);assertThat(m.perform(get("/api/escrow/orders/2")).andReturn().getResponse().getStatus()).isEqualTo(404);assertThat(raw).isNull();});}
    @Test void wrongNetworkAndEmptyCodeCannotLoad(){context().run(c->{var m=mvc(c);chain="0x1";assertThat(m.perform(post("/api/escrow/load").param("address",ADDRESS).param("fromBlock","5")).andReturn().getResponse().getStatus()).isEqualTo(503);chain="0xecb8";noCode=true;assertThat(m.perform(post("/api/escrow/load").param("address",ADDRESS).param("fromBlock","5")).andReturn().getResponse().getStatus()).isEqualTo(400);});}
    @Test void logRpcErrorDoesNotBecomeSuccessfulEmptyList(){context().run(c->{var m=mvc(c);load(m);logError=true;assertThat(m.perform(get("/api/escrow/orders")).andReturn().getResponse().getStatus()).isEqualTo(503);});}
    @Test
    void failedLoadPreservesPreviousContract() {
        context().run(c -> {
            var m = mvc(c);
            load(m);
            var result = m.perform(post("/api/escrow/load")
                    .param("address", OTHER).param("fromBlock", "6")).andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(400);
            assertThat(body(m.perform(get("/api/escrow/address")).andReturn())
                    .path("contractAddress").asText()).isEqualTo(ADDRESS);
        });
    }

    @Test
    void futureDeploymentBlockIsRejected() {
        context().run(c -> {
            var result = mvc(c).perform(post("/api/escrow/load")
                    .param("address", ADDRESS).param("fromBlock", "30")).andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(400);
            assertThat(calls.stream().noneMatch(q -> q.path("method").asText()
                    .equals("eth_sendRawTransaction"))).isTrue();
        });
    }

    @Test
    void unrelatedContractIsRejected() {
        incompatible = true;
        context().run(c -> {
            var result = mvc(c).perform(post("/api/escrow/load")
                    .param("address", ADDRESS).param("fromBlock", "5")).andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(400);
        });
    }

    @Test
    void mismatchedRpcHashReturnsLocalHash() {
        context().run(c -> {
            var m = mvc(c);
            load(m);
            wrongHash = true;
            var result = m.perform(post("/api/escrow/orders/0/resolve")
                    .param("sellerWins", "true")).andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(503);
            assertThat(body(result).path("transactionHash").asText()).isEqualTo(Hash.sha3(raw));
            assertThat(calls.stream().filter(q -> q.path("method").asText()
                    .equals("eth_sendRawTransaction")).count()).isEqualTo(1);
        });
    }

}
