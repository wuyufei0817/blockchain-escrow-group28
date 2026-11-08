package com.wetech.demo.web3j.contracts.simplestorage;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.14.0.
 */
@SuppressWarnings("rawtypes")
public class SimpleStorage extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5060e68061001f6000396000f3fe6080604052348015600f57600080fd5b506004361060325760003560e01c806360fe47b11460375780636d4ce63c146048575b600080fd5b604660423660046098565b605d565b005b60005460405190815260200160405180910390f35b60008190556040518181527f30abf1e3f71dc652b1df75682ae16af5c006ad42cd23d00001e8e106d8af4e1d9060200160405180910390a150565b60006020828403121560a957600080fd5b503591905056fea2646970667358221220e1761d96cd68fa39fbb29013ac3f87a245c8af29fb20cba6b7f5542c34a2ad2964736f6c63430008140033";

    private static String librariesLinkedBinary;

    public static final String FUNC_GET = "get";

    public static final String FUNC_SET = "set";

    public static final Event DATACHANGED_EVENT = new Event("DataChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected SimpleStorage(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SimpleStorage(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SimpleStorage(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SimpleStorage(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<DataChangedEventResponse> getDataChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DATACHANGED_EVENT, transactionReceipt);
        ArrayList<DataChangedEventResponse> responses = new ArrayList<DataChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DataChangedEventResponse typedResponse = new DataChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DataChangedEventResponse getDataChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DATACHANGED_EVENT, log);
        DataChangedEventResponse typedResponse = new DataChangedEventResponse();
        typedResponse.log = log;
        typedResponse.newValue = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<DataChangedEventResponse> dataChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDataChangedEventFromLog(log));
    }

    public Flowable<DataChangedEventResponse> dataChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DATACHANGED_EVENT));
        return dataChangedEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> get() {
        final Function function = new Function(FUNC_GET, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> set(BigInteger x) {
        final Function function = new Function(
                FUNC_SET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(x)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static SimpleStorage load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleStorage(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SimpleStorage load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleStorage(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SimpleStorage load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new SimpleStorage(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SimpleStorage load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SimpleStorage(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SimpleStorage> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleStorage.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<SimpleStorage> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SimpleStorage.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    public static RemoteCall<SimpleStorage> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleStorage.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<SimpleStorage> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SimpleStorage.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class DataChangedEventResponse extends BaseEventResponse {
        public BigInteger newValue;
    }
}
