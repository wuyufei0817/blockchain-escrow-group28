package com.wetech.demo.web3j.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetech.demo.web3j.contracts.escrow.Escrow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Public event data only. A snapshot is published after a complete, successful scan. */
@Service
public class EscrowEventIndexService {
    public record EventView(String id, String eventName, String status, Map<String, String> parameters,
                            String contractAddress, String blockNumber, String transactionIndex,
                            String logIndex, String transactionHash, String blockHash) {}
    public record Snapshot(int version, String chainId, String address, String fromBlock,
                           String lastBlock, String lastBlockHash, List<EventView> events) {}
    private record Key(String chainId, String address, String fromBlock) {}
    private static final class Entry { Snapshot snapshot; }
    private static final List<Event> EVENTS = List.of(Escrow.ORDERCREATED_EVENT, Escrow.ORDERPAID_EVENT,
            Escrow.ORDERCOMPLETED_EVENT, Escrow.ORDERREFUNDED_EVENT, Escrow.ORDERDISPUTED_EVENT, Escrow.ORDERRESOLVED_EVENT);
    private static final Map<String, String> NAMES;
    static {
        Map<String, String> names = new HashMap<>();
        for (Event event : EVENTS) names.put(EventEncoder.encode(event).toLowerCase(Locale.ROOT), event.getName());
        NAMES = Map.copyOf(names);
    }
    private final Web3j web3j;
    private final ObjectMapper mapper;
    private final Path directory;
    private final Map<Key, Entry> entries = new ConcurrentHashMap<>();

    public EscrowEventIndexService(Web3j web3j, ObjectMapper mapper,
                                  @Value("${web3j.event-index-dir:.escrow-event-index}") String directory) {
        this.web3j = web3j;
        this.mapper = mapper;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public List<EventView> events(BigInteger chainId, String address, BigInteger fromBlock, BigInteger head) throws IOException {
        Key key = new Key(chainId.toString(), address.toLowerCase(Locale.ROOT), fromBlock.toString());
        if (chainId.signum() <= 0 || !key.address.matches("0x[0-9a-f]{40}") || fromBlock.signum() < 0 || head.compareTo(fromBlock) < 0) {
            throw new IOException("Invalid event index context");
        }
        Entry entry = entries.computeIfAbsent(key, ignored -> new Entry());
        synchronized (entry) {
            Snapshot previous = entry.snapshot == null ? read(key) : entry.snapshot;
            String headHash = blockHash(head);
            BigInteger checkpoint = previous == null ? fromBlock.subtract(BigInteger.ONE) : new BigInteger(previous.lastBlock);
            boolean reusable = previous != null && checkpoint.compareTo(head) <= 0
                    && previous.lastBlockHash.equalsIgnoreCase(checkpoint.equals(head) ? headHash : blockHash(checkpoint));
            if (!reusable) { previous = null; checkpoint = fromBlock.subtract(BigInteger.ONE); }
            if (checkpoint.equals(head)) { entry.snapshot = previous; return previous.events; }

            Map<String, EventView> unique = new LinkedHashMap<>();
            if (previous != null) for (EventView event : previous.events) unique.put(identity(event), event);
            for (BigInteger start = checkpoint.add(BigInteger.ONE); start.compareTo(head) <= 0; start = start.add(BigInteger.TEN)) {
                BigInteger end = start.add(BigInteger.valueOf(9)).min(head);
                EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(start), DefaultBlockParameter.valueOf(end), key.address);
                filter.addOptionalTopics(NAMES.keySet().toArray(String[]::new));
                EthLog response = checked(web3j.ethGetLogs(filter).send());
                if (response.getLogs() == null) throw new IOException("Missing event logs");
                for (EthLog.LogResult<?> result : response.getLogs()) {
                    if (!(result.get() instanceof Log log)) throw new IOException("Invalid event log");
                    if (!key.address.equalsIgnoreCase(log.getAddress()) || log.getBlockNumber() == null
                            || log.getBlockNumber().compareTo(start) < 0 || log.getBlockNumber().compareTo(end) > 0) {
                        throw new IOException("Event log does not match requested range/address");
                    }
                    if (log.isRemoved()) continue;
                    EventView event = decode(log);
                    if (event == null) continue;
                    EventView duplicate = unique.putIfAbsent(identity(event), event);
                    if (duplicate != null && !duplicate.equals(event)) throw new IOException("Conflicting duplicate event");
                }
            }
            if (!headHash.equalsIgnoreCase(blockHash(head))) throw new IOException("Chain changed during event scan");
            List<EventView> sorted = unique.values().stream().sorted(Comparator
                    .comparing((EventView e) -> new BigInteger(e.blockNumber))
                    .thenComparing(e -> new BigInteger(e.transactionIndex))
                    .thenComparing(e -> new BigInteger(e.logIndex))).toList();
            Snapshot next = new Snapshot(1, key.chainId, key.address, key.fromBlock, head.toString(), headHash, sorted);
            save(key, next);
            entry.snapshot = next;
            return next.events;
        }
    }

    private EventView decode(Log log) throws IOException {
        if (log.getTopics() == null || log.getTopics().isEmpty()) throw new IOException("Missing event topics");
        String name = NAMES.get(log.getTopics().get(0).toLowerCase(Locale.ROOT));
        if (name == null) return null;
        if (log.getTopics().size() != (name.equals("OrderCreated") ? 4 : 2)) throw new IOException("Invalid indexed event fields");
        if (log.getTransactionIndex() == null || log.getLogIndex() == null || !hash(log.getTransactionHash()) || !hash(log.getBlockHash())) {
            throw new IOException("Missing event identity");
        }
        BigInteger id;
        String status;
        Map<String, String> values = new LinkedHashMap<>();
        try {
            switch (name) {
                case "OrderCreated" -> {
                    var e = Escrow.getOrderCreatedEventFromLog(log); id = e.id; status = "Created";
                    values.put("buyer", e.buyer); values.put("seller", e.seller); values.put("token", e.token);
                    values.put("amount", e.amount.toString()); values.put("deadline", e.deadline.toString());
                }
                case "OrderPaid" -> { id = Escrow.getOrderPaidEventFromLog(log).id; status = "Paid"; }
                case "OrderCompleted" -> { id = Escrow.getOrderCompletedEventFromLog(log).id; status = "Completed"; }
                case "OrderRefunded" -> { id = Escrow.getOrderRefundedEventFromLog(log).id; status = "Refunded"; }
                case "OrderDisputed" -> {
                    var e = Escrow.getOrderDisputedEventFromLog(log); id = e.id; status = "Disputed"; values.put("by", e.by);
                }
                case "OrderResolved" -> {
                    var e = Escrow.getOrderResolvedEventFromLog(log); id = e.id;
                    status = e.sellerWins ? "Completed" : "Refunded"; values.put("sellerWins", e.sellerWins.toString());
                }
                default -> throw new IOException("Unsupported event");
            }
            if (id.signum() < 0 || log.getTransactionIndex().signum() < 0 || log.getLogIndex().signum() < 0) throw new IOException("Invalid event integer");
            return new EventView(id.toString(), name, status, Collections.unmodifiableMap(values),
                    log.getAddress().toLowerCase(Locale.ROOT), log.getBlockNumber().toString(), log.getTransactionIndex().toString(),
                    log.getLogIndex().toString(), log.getTransactionHash().toLowerCase(Locale.ROOT), log.getBlockHash().toLowerCase(Locale.ROOT));
        } catch (RuntimeException exception) { throw new IOException("Cannot decode event", exception); }
    }

    private String blockHash(BigInteger number) throws IOException {
        var block = checked(web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number), false).send()).getBlock();
        if (block == null || !number.equals(block.getNumber()) || !hash(block.getHash())) throw new IOException("Missing checkpoint block");
        return block.getHash().toLowerCase(Locale.ROOT);
    }

    private Path file(Key key) { return directory.resolve(key.chainId + "-" + key.address + "-" + key.fromBlock + ".json"); }
    private Snapshot read(Key key) throws IOException {
        if (!Files.exists(file(key))) return null;
        try {
            Snapshot saved = mapper.readValue(file(key).toFile(), Snapshot.class);
            if (saved.version != 1 || !key.chainId.equals(saved.chainId) || !key.address.equals(saved.address)
                    || !key.fromBlock.equals(saved.fromBlock) || !hash(saved.lastBlockHash) || saved.events == null
                    || new BigInteger(saved.lastBlock).compareTo(new BigInteger(key.fromBlock)) < 0) throw new IOException("Invalid event checkpoint");
            for (EventView event : saved.events) {
                if (event == null || !key.address.equals(event.contractAddress) || !NAMES.containsValue(event.eventName)
                        || event.parameters == null || event.status == null || new BigInteger(event.id).signum() < 0
                        || new BigInteger(event.blockNumber).compareTo(new BigInteger(key.fromBlock)) < 0
                        || new BigInteger(event.blockNumber).compareTo(new BigInteger(saved.lastBlock)) > 0
                        || new BigInteger(event.transactionIndex).signum() < 0 || new BigInteger(event.logIndex).signum() < 0
                        || !hash(event.transactionHash) || !hash(event.blockHash)) throw new IOException("Invalid cached event");
            }
            return new Snapshot(saved.version, saved.chainId, saved.address, saved.fromBlock, saved.lastBlock, saved.lastBlockHash, List.copyOf(saved.events));
        } catch (RuntimeException exception) { throw new IOException("Invalid event cache", exception); }
    }

    private void save(Key key, Snapshot value) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "events-", ".tmp");
        try {
            mapper.writeValue(temporary.toFile(), value);
            try { Files.move(temporary, file(key), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file(key), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    private String identity(EventView event) { return event.transactionHash + ":" + event.logIndex; }
    private boolean hash(String value) { return value != null && value.matches("0x[0-9a-fA-F]{64}"); }
    private <T extends Response<?>> T checked(T response) throws IOException {
        if (response == null || response.hasError() || response.getResult() == null) throw new IOException("Event RPC error");
        return response;
    }
}
