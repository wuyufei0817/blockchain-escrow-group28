import { expect } from "chai";
import { scanCreatedOrders } from "../../nextjs/utils/escrow/creationIndex";
import type { CreatedOrder, CreationSnapshot } from "../../nextjs/utils/escrow/creationIndex";

const hash = "0x1111111111111111111111111111111111111111111111111111111111111111";
const created = (id: bigint, blockNumber: bigint): CreatedOrder => ({ id, blockNumber, transactionHash: hash });

describe("Escrow creation event index", () => {
  it("discovers orders across 10-block ranges and deduplicates overlapping logs", async () => {
    // Given: two real creation events separated by more than one RPC range.
    const ranges: (readonly [bigint, bigint])[] = [];
    const snapshot = { orders: [], throughBlock: 99n };
    // When: the RPC returns a duplicated first event and a later second event.
    const result = await scanCreatedOrders({
      snapshot, head: 124n, count: 2n, signal: new AbortController().signal, onProgress: () => undefined,
      readLogs: async (from, to) => {
        ranges.push([from, to]);
        if (from === 100n) return [created(0n, 101n), created(0n, 101n)];
        if (from === 120n) return [created(1n, 123n)];
        return [];
      },
    });
    // Then: each request spans at most 10 blocks and each ID appears once.
    expect(ranges).to.deep.equal([[100n, 109n], [110n, 119n], [120n, 124n]]);
    expect(result.orders.map(order => order.id)).to.deep.equal([0n, 1n]);
    expect(result.throughBlock).to.equal(124n);
  });

  it("skips the empty tail only after all creations match the count pinned to the head", async () => {
    // Given: the event is near deployment, with a large empty block tail.
    let calls = 0;
    // When: the only creation event has been retrieved from the chain.
    const result = await scanCreatedOrders({
      snapshot: { orders: [], throughBlock: 99n }, head: 200000n, count: 1n,
      signal: new AbortController().signal, onProgress: () => undefined,
      readLogs: async () => { calls += 1; return [created(0n, 102n)]; },
    });
    // Then: no per-order enumeration or thousands of empty RPC calls are needed.
    expect(calls).to.equal(1);
    expect(result.throughBlock).to.equal(200000n);
    expect(result.orders[0]?.id).to.equal(0n);
  });

  it("keeps the last successful checkpoint when a later range fails", async () => {
    // Given: a first successful range and a second failing range.
    let saved: CreationSnapshot = { orders: [], throughBlock: 99n };
    let failure: unknown;
    // When: the RPC fails during the second range.
    try {
      await scanCreatedOrders({
        snapshot: saved, head: 130n, count: 2n, signal: new AbortController().signal,
        onProgress: snapshot => { saved = snapshot; },
        readLogs: async from => {
          if (from === 100n) return [created(0n, 102n)];
          throw new Error("RPC unavailable");
        },
      });
    } catch (error) { failure = error; }
    // Then: a retry resumes after the first range and does not lose its event.
    expect(failure).to.be.instanceOf(Error);
    expect(saved.throughBlock).to.equal(109n);
    expect(saved.orders.map(order => order.id)).to.deep.equal([0n]);
  });

  it("rejects incomplete event results instead of claiming a complete list", async () => {
    // Given: chain state says two orders exist but logs expose only one.
    let failure: unknown;
    // When: all available ranges are scanned.
    try {
      await scanCreatedOrders({
        snapshot: { orders: [], throughBlock: 99n }, head: 109n, count: 2n,
        signal: new AbortController().signal, onProgress: () => undefined,
        readLogs: async () => [created(0n, 102n)],
      });
    } catch (error) { failure = error; }
    // Then: the inconsistency is visible to the user.
    expect(failure).to.be.instanceOf(Error);
  });

  it("stops after cancellation without issuing another RPC", async () => {
    // Given: an already cancelled operation.
    const controller = new AbortController();
    controller.abort();
    let calls = 0;
    let failure: unknown;
    // When: a scan is attempted.
    try {
      await scanCreatedOrders({
        snapshot: { orders: [], throughBlock: 99n }, head: 109n, count: 1n,
        signal: controller.signal, onProgress: () => undefined,
        readLogs: async () => { calls += 1; return []; },
      });
    } catch (error) { failure = error; }
    // Then: no request or state update is performed.
    expect(calls).to.equal(0);
    expect(failure).to.be.instanceOf(Error);
  });
});
