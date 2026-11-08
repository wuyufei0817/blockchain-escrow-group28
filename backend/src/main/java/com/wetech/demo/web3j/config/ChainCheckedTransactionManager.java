package com.wetech.demo.web3j.config;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthChainId;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;

import java.io.IOException;
import java.math.BigInteger;

/** Uses EIP-155 signing and checks the RPC network before signing each write. */
public class ChainCheckedTransactionManager extends RawTransactionManager {
    private final Web3j client;
    private final BigInteger expectedChainId;

    public ChainCheckedTransactionManager(Web3j client, Credentials credentials, long chainId) {
        super(client, credentials, chainId);
        if (chainId <= 0) {
            throw new IllegalArgumentException("web3j.chain-id must be positive");
        }
        this.client = client;
        this.expectedChainId = BigInteger.valueOf(chainId);
    }

    // The lock covers nonce selection through broadcast for every service using this bean.
    @Override
    public synchronized EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit,
            String to, String data, BigInteger value, boolean constructor) throws IOException {
        return super.sendTransaction(gasPrice, gasLimit, to, data, value, constructor);
    }

    @Override
    public synchronized EthSendTransaction sendEIP1559Transaction(long chainId, BigInteger priorityFee,
            BigInteger maxFee, BigInteger gasLimit, String to, String data, BigInteger value,
            boolean constructor) throws IOException {
        if (!expectedChainId.equals(BigInteger.valueOf(chainId))) {
            throw new IOException("Signing chain ID does not match configured chain ID");
        }
        return super.sendEIP1559Transaction(chainId, priorityFee, maxFee, gasLimit, to, data, value, constructor);
    }

    @Override
    public EthSendTransaction signAndSend(RawTransaction transaction) throws IOException {
        EthChainId response = client.ethChainId().send();
        if (response == null || response.hasError() || response.getResult() == null) {
            throw new IOException("Cannot verify RPC chain ID; transaction was not sent");
        }
        BigInteger actualChainId;
        try {
            actualChainId = response.getChainId();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid RPC chain ID; transaction was not sent");
        }
        if (!expectedChainId.equals(actualChainId)) {
            throw new IOException("RPC chain ID mismatch: expected " + expectedChainId
                    + ", got " + actualChainId + "; transaction was not sent");
        }
        return super.signAndSend(transaction);
    }
}
