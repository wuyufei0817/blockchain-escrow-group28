package com.wetech.demo.web3j.service;

import com.wetech.demo.web3j.contracts.escrow.Escrow;
import com.wetech.demo.web3j.exception.EscrowApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class EscrowService {
    public record ReceiptView(String transactionHash, String blockNumber, String status, String gasUsed) {}
    public record ContractView(String contractAddress, String arbiter, String fromBlock, String chainId,
                               String transactionHash, String blockNumber, String status, String gasUsed) {}
    public record OrderView(String id, String buyer, String seller, String token, String amount,
                            String deadline, String status, String description) {}
    public record CreatedOrderView(String id, String buyer, String seller, String amount,
                                   String blockNumber, String transactionHash) {}
    private record Context(Escrow contract, String address, String arbiter, BigInteger fromBlock) {}
    private static final String[] STATES = {"Created", "Paid", "Completed", "Refunded", "Disputed"};
    private static final BigInteger MAX_UINT = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);
    private final Web3j web3j;
    private final TransactionManager transactions;
    private final ContractGasProvider gas;
    private final BigInteger chainId;
    private final long pollInterval;
    private final int pollAttempts;
    private final ReentrantLock writes = new ReentrantLock();
    private volatile Context current;

    public EscrowService(Web3j web3j, TransactionManager transactions, ContractGasProvider gas,
                         @Value("${web3j.chain-id:60600}") long chainId,
                         @Value("${web3j.receipt-poll-interval-ms:1000}") long pollInterval,
                         @Value("${web3j.receipt-poll-attempts:40}") int pollAttempts) {
        if (pollInterval < 0 || pollAttempts < 1) throw new IllegalArgumentException("Invalid receipt polling settings");
        this.web3j = web3j;
        this.transactions = transactions;
        this.gas = gas;
        this.chainId = BigInteger.valueOf(chainId);
        this.pollInterval = pollInterval;
        this.pollAttempts = pollAttempts;
    }

    public ContractView deploy() {
        return locked(() -> {
            checkChain();
            String constructor = FunctionEncoder.encodeConstructor(List.of(new Address(transactions.getFromAddress())));
            TransactionReceipt receipt = send(null, Escrow.BINARY + constructor, true);
            String address = checkedAddress(receipt.getContractAddress());
            Context next = new Context(Escrow.load(address, web3j, transactions, gas), address,
                    transactions.getFromAddress(), receipt.getBlockNumber());
            current = next;
            return view(next, receipt);
        });
    }

    public ContractView load(String inputAddress, String inputBlock) {
        String address = checkedAddress(inputAddress);
        BigInteger fromBlock = unsigned(inputBlock, "fromBlock");
        return locked(() -> {
            checkChain();
            BigInteger head = checked(web3j.ethBlockNumber().send()).getBlockNumber();
            if (fromBlock.compareTo(head) > 0) throw bad("fromBlock is beyond the current block");
            if (emptyCode(code(address, DefaultBlockParameterName.LATEST))) throw bad("No contract code at this address");
            verifyDeploymentBlock(address, fromBlock);
            Escrow contract = Escrow.load(address, web3j, transactions, gas);
            String arbiter;
            try {
                arbiter = checkedAddress(contract.arbiter().send());
                if (contract.orderCount().send().signum() < 0) throw bad("Invalid order count");
            } catch (IllegalArgumentException | org.web3j.tx.exceptions.ContractCallException exception) {
                throw bad("Address does not expose the expected Escrow interface");
            }
            Context next = new Context(contract, address, arbiter, fromBlock);
            current = next; // Publish only after all checks; failed loads preserve the previous instance.
            return view(next, null);
        });
    }

    public ContractView address() {
        Context context = required();
        return rpc(() -> { checkChain(); return view(context, null); });
    }

    public OrderView order(String inputId) {
        BigInteger id = unsigned(inputId, "id");
        Context context = required();
        return rpc(() -> {
            checkChain();
            if (id.compareTo(context.contract.orderCount().send()) >= 0) {
                throw new EscrowApiException(404, "ORDER_NOT_FOUND", "Order does not exist");
            }
            Escrow.Order order = context.contract.getOrder(id).send();
            int state = order.status.intValueExact();
            if (state < 0 || state >= STATES.length) throw new IOException("Unexpected Escrow state");
            return new OrderView(id.toString(), order.buyer, order.seller, order.token,
                    order.amount.toString(), order.deadline.toString(), STATES[state], order.description);
        });
    }

    public List<CreatedOrderView> orders() {
        Context context = required();
        return rpc(() -> {
            checkChain();
            BigInteger head = checked(web3j.ethBlockNumber().send()).getBlockNumber();
            List<Log> logs = new ArrayList<>();
            for (BigInteger start = context.fromBlock; start.compareTo(head) <= 0; start = start.add(BigInteger.TEN)) {
                BigInteger end = start.add(BigInteger.valueOf(9)).min(head);
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(start), DefaultBlockParameter.valueOf(end), context.address);
                filter.addSingleTopic(EventEncoder.encode(Escrow.ORDERCREATED_EVENT));
                EthLog response = checked(web3j.ethGetLogs(filter).send());
                if (response.getLogs() == null) throw new IOException("Missing log result");
                for (EthLog.LogResult<?> result : response.getLogs()) {
                    if (!(result.get() instanceof Log log)) throw new IOException("Invalid log result");
                    if (!context.address.equalsIgnoreCase(log.getAddress())) throw new IOException("Log address mismatch");
                    if (!log.isRemoved()) logs.add(log);
                }
            }
            logs.sort(Comparator.comparing(Log::getBlockNumber).thenComparing(Log::getTransactionIndex).thenComparing(Log::getLogIndex));
            Map<String, CreatedOrderView> unique = new LinkedHashMap<>();
            for (Log log : logs) {
                Escrow.OrderCreatedEventResponse event = Escrow.getOrderCreatedEventFromLog(log);
                unique.putIfAbsent(log.getTransactionHash() + ":" + log.getLogIndex(),
                        new CreatedOrderView(event.id.toString(), event.buyer, event.seller, event.amount.toString(),
                                log.getBlockNumber().toString(), log.getTransactionHash()));
            }
            return List.copyOf(unique.values());
        });
    }

    public ReceiptView resolve(String inputId, String inputDecision) {
        BigInteger id = unsigned(inputId, "id");
        if (!"true".equals(inputDecision) && !"false".equals(inputDecision)) throw bad("sellerWins must be true or false");
        boolean sellerWins = Boolean.parseBoolean(inputDecision);
        return locked(() -> {
            Context context = required();
            checkChain();
            // Fixed gas deliberately permits a mined failing transaction. Do not substitute eth_estimateGas.
            String data = context.contract.resolve(id, sellerWins).encodeFunctionCall();
            return receiptView(send(context.address, data, false));
        });
    }

    private TransactionReceipt send(String address, String data, boolean creation) throws Exception {
        EthSendTransaction sent;
        try {
            sent = transactions.sendTransaction(gas.getGasPrice(),
                    gas.getGasLimit(), address, data, BigInteger.ZERO, creation);
        } catch (org.web3j.tx.exceptions.TxHashMismatchException exception) {
            throw new EscrowApiException(503, "SUBMISSION_UNKNOWN",
                    "RPC returned an unexpected hash; submission must be checked before resending",
                    Map.of("transactionHash", exception.getTxHashLocal()));
        } catch (IOException exception) {
            throw new EscrowApiException(503, "SUBMISSION_UNKNOWN", "Transaction submission could not be confirmed; do not resend automatically");
        }
        String hash = sent.getTransactionHash();
        if (sent.hasError() || hash == null || !hash.matches("0x[0-9a-fA-F]{64}")) {
            throw new EscrowApiException(503, "SUBMISSION_UNKNOWN", "RPC did not confirm transaction acceptance", hash == null ? Map.of() : Map.of("transactionHash", hash));
        }
        for (int attempt = 0; attempt < pollAttempts; attempt++) {
            try {
                var response = checked(web3j.ethGetTransactionReceipt(hash).send());
                if (response.getTransactionReceipt().isPresent()) {
                    TransactionReceipt receipt = response.getTransactionReceipt().get();
                    if (!hash.equalsIgnoreCase(receipt.getTransactionHash()) || receipt.getBlockNumberRaw() == null || receipt.getStatus() == null) {
                        throw new IOException("Incomplete receipt");
                    }
                    if (!receipt.isStatusOK()) {
                        Map<String, String> details = receiptDetails(receipt);
                        details.put("revertReason", revertReason(receipt, address, data));
                        throw new EscrowApiException(422, "TRANSACTION_REVERTED", "Transaction was mined and reverted", details);
                    }
                    return receipt;
                }
                if (attempt + 1 < pollAttempts) Thread.sleep(pollInterval);
            } catch (EscrowApiException exception) { throw exception; }
            catch (Exception exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                throw unknown(hash);
            }
        }
        throw unknown(hash);
    }

    private String revertReason(TransactionReceipt receipt, String address, String data) {
        if (receipt.getRevertReason() != null && !receipt.getRevertReason().isBlank()) return receipt.getRevertReason();
        if (address != null) {
            try {
                EthCall call = web3j.ethCall(Transaction.createEthCallTransaction(transactions.getFromAddress(), address, data),
                        DefaultBlockParameter.valueOf(receipt.getBlockNumber())).send();
                if (call.getRevertReason() != null) return call.getRevertReason();
                String value = call.getValue();
                if (value != null && value.startsWith("0x08c379a0")) {
                    return FunctionReturnDecoder.decode(value.substring(10), new Function("Error", List.of(), List.of(new TypeReference<Utf8String>() {})).getOutputParameters()).get(0).getValue().toString();
                }
            } catch (Exception ignored) { /* Receipt status remains authoritative even if replay is unavailable. */ }
        }
        return "Reverted; the RPC did not provide a reason";
    }

    private void checkChain() throws IOException {
        EthChainId response = checked(web3j.ethChainId().send());
        if (!chainId.equals(response.getChainId())) throw new IOException("RPC chain ID mismatch");
    }
    private void verifyDeploymentBlock(String address, BigInteger fromBlock) throws IOException {
        // POTOS currently returns latest code even for historical eth_getCode queries.
        // Prove the creation block with a top-level creation transaction and its receipt.
        EthBlock.Block block = checked(web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(fromBlock), true).send()).getBlock();
        if (block == null || !fromBlock.equals(block.getNumber()) || block.getHash() == null
                || block.getTransactions() == null) throw new IOException("Missing deployment block");
        for (EthBlock.TransactionResult<?> result : block.getTransactions()) {
            if (!(result.get() instanceof EthBlock.TransactionObject transaction)) {
                throw new IOException("Missing full block transactions");
            }
            if (transaction.getTo() != null && !transaction.getTo().isEmpty()) continue;
            if (transaction.getHash() == null) throw new IOException("Missing creation transaction hash");
            TransactionReceipt receipt = checked(web3j.ethGetTransactionReceipt(transaction.getHash()).send())
                    .getTransactionReceipt().orElseThrow(() -> new IOException("Missing creation receipt"));
            if (!address.equalsIgnoreCase(receipt.getContractAddress())) continue;
            if (!receipt.isStatusOK() || !fromBlock.equals(receipt.getBlockNumber())
                    || !block.getHash().equalsIgnoreCase(receipt.getBlockHash())
                    || !transaction.getHash().equalsIgnoreCase(receipt.getTransactionHash())
                    || (receipt.getTo() != null && !receipt.getTo().isEmpty())) {
                throw bad("Creation receipt does not match the deployment block");
            }
            return;
        }
        throw bad("fromBlock must contain the contract deployment transaction");
    }

    private String code(String address, DefaultBlockParameter block) throws IOException { return checked(web3j.ethGetCode(address, block).send()).getCode(); }
    private boolean emptyCode(String value) { return value == null || value.matches("0x0*"); }
    private Context required() {
        Context context = current;
        if (context == null) throw new EscrowApiException(409, "CONTRACT_NOT_LOADED", "Deploy or load an Escrow contract first");
        return context;
    }
    private String checkedAddress(String value) {
        if (value == null || !value.matches("0x[0-9a-fA-F]{40}") || value.equalsIgnoreCase("0x" + "0".repeat(40))) throw bad("Invalid contract address");
        return value.toLowerCase(Locale.ROOT);
    }
    private BigInteger unsigned(String value, String name) {
        if (value == null || !value.matches("[0-9]{1,78}")) throw bad(name + " must be an unsigned decimal integer");
        BigInteger number = new BigInteger(value);
        if (number.compareTo(MAX_UINT) > 0) throw bad(name + " exceeds uint256");
        return number;
    }
    private <T extends Response<?>> T checked(T response) throws IOException {
        if (response == null || response.hasError()) throw new IOException("RPC error");
        return response;
    }
    private <T> T rpc(Callable<T> operation) {
        try { return operation.call(); }
        catch (EscrowApiException exception) { throw exception; }
        catch (Exception exception) { throw new EscrowApiException(503, "RPC_UNAVAILABLE", "RPC unavailable, invalid response, or wrong network"); }
    }
    private <T> T locked(Callable<T> operation) {
        writes.lock();
        try { return rpc(operation); } finally { writes.unlock(); }
    }
    private ContractView view(Context c, TransactionReceipt r) {
        return new ContractView(c.address, c.arbiter, c.fromBlock.toString(), chainId.toString(),
                r == null ? null : r.getTransactionHash(), r == null ? null : r.getBlockNumber().toString(),
                r == null ? null : r.getStatus(), r == null ? null : r.getGasUsed().toString());
    }
    private ReceiptView receiptView(TransactionReceipt r) {
        return new ReceiptView(r.getTransactionHash(), r.getBlockNumber().toString(), r.getStatus(), r.getGasUsed().toString());
    }
    private Map<String, String> receiptDetails(TransactionReceipt r) {
        ReceiptView v = receiptView(r);
        Map<String, String> details = new LinkedHashMap<>();
        details.put("transactionHash", v.transactionHash); details.put("blockNumber", v.blockNumber);
        details.put("status", v.status); details.put("gasUsed", v.gasUsed);
        return details;
    }
    private EscrowApiException bad(String message) { return new EscrowApiException(400, "INVALID_PARAMETER", message); }
    private EscrowApiException unknown(String hash) {
        return new EscrowApiException(503, "RECEIPT_UNKNOWN", "Receipt is not confirmed; do not resend automatically", Map.of("transactionHash", hash));
    }
}
