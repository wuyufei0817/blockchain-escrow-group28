import type { Hash } from "viem";

export type CreatedOrder = {
  readonly id: bigint;
  readonly blockNumber: bigint;
  readonly transactionHash: Hash;
};

export type CreationSnapshot = {
  readonly orders: readonly CreatedOrder[];
  readonly throughBlock: bigint;
};

export type ScanOptions = {
  readonly snapshot: CreationSnapshot;
  readonly head: bigint;
  readonly count: bigint;
  readonly signal: AbortSignal;
  readonly readLogs: (from: bigint, to: bigint) => Promise<readonly CreatedOrder[]>;
  readonly onProgress: (snapshot: CreationSnapshot) => void;
};

export class EventIndexError extends Error {
  override readonly name = "EventIndexError";
}

export async function scanCreatedOrders(options: ScanOptions): Promise<CreationSnapshot> {
  const { head, count, signal, readLogs, onProgress } = options;
  signal.throwIfAborted();
  const orders = new Map(options.snapshot.orders.map(order => [order.id, order]));
  let throughBlock = options.snapshot.throughBlock;
  const snapshot = (): CreationSnapshot => ({
    orders: [...orders.values()].sort((left, right) => left.id < right.id ? -1 : left.id > right.id ? 1 : 0),
    throughBlock,
  });

  while (BigInt(orders.size) < count && throughBlock < head) {
    signal.throwIfAborted();
    const from = throughBlock + 1n;
    const to = from + 9n < head ? from + 9n : head;
    const logs = await readLogs(from, to);
    signal.throwIfAborted();
    for (const order of logs) {
      if (order.id < 0n || order.id >= count || order.blockNumber < from || order.blockNumber > to) {
        throw new EventIndexError("事件与所查询区块或订单数量不一致，请重新同步。");
      }
      orders.set(order.id, order);
    }
    throughBlock = to;
    onProgress(snapshot());
  }

  if (BigInt(orders.size) !== count) {
    throw new EventIndexError(`订单事件不完整：发现${orders.size}笔，链上数量为${count}笔。`);
  }
  // Escrow increments orderCount exactly once for each OrderCreated emission.
  // Both the count and log upper bound refer to this same pinned head.
  throughBlock = head;
  const complete = snapshot();
  onProgress(complete);
  return complete;
}
