package com.wetech.demo.web3j.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;

@Slf4j
@Configuration
public class Web3jConfig {

    @Value("${web3j.client-address:http://localhost:8545}")
    private String clientAddress;

    @Value("${web3j.private-key}")
    private String privateKey;

    @Value("${web3j.gas-price:1000000}")
    private String gasPrice;

    @Value("${web3j.gas-limit:6721975}")
    private String gasLimit;

    @Value("${web3j.chain-id:60600}")
    private long chainId;

    @Bean(destroyMethod = "shutdown")
    public Web3j web3j() {
        log.info("Initializing Web3j client");
        return Web3j.build(new HttpService(clientAddress));
    }

    @Bean
    public Credentials credentials() {
        if (privateKey == null || !privateKey.matches("(?i)(0x)?[0-9a-f]{64}")) {
            throw new IllegalStateException("Set WEB3J_PRIVATE_KEY to a valid 32-byte private key");
        }
        String normalized = privateKey.replaceFirst("(?i)^0x", "");
        BigInteger scalar = new BigInteger(normalized, 16);
        if (scalar.signum() <= 0 || scalar.compareTo(org.web3j.crypto.Sign.CURVE_PARAMS.getN()) >= 0) {
            throw new IllegalStateException("Set WEB3J_PRIVATE_KEY to a valid 32-byte private key");
        }
        Credentials credentials = Credentials.create(normalized);
        log.info("Backend signer address: {}", credentials.getAddress());
        return credentials;
    }

    @Bean
    public TransactionManager transactionManager(Web3j web3j, Credentials credentials) {
        return new ChainCheckedTransactionManager(web3j, credentials, chainId);
    }

    @Bean
    public ContractGasProvider contractGasProvider() {
        return new StaticGasProvider(
                new BigInteger(gasPrice),
                new BigInteger(gasLimit)
        );
    }
}
