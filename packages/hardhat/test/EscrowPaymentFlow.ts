import { expect } from "chai";
import { confirmedHash, confirmedTransaction, fundOrder } from "../../nextjs/utils/escrow/paymentFlow";
import type { ChainReceipt, PaymentOptions, TransactionRecord } from "../../nextjs/utils/escrow/paymentFlow";

const approvalHash = "0x1111111111111111111111111111111111111111111111111111111111111111";
const depositHash = "0x2222222222222222222222222222222222222222222222222222222222222222";
const buyer = "0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6";
const escrow = "0xbdB2Dfcd8A9Cd903848a1B9d95416b85178f05Af";
const receipt = (transactionHash: ChainReceipt["transactionHash"]): ChainReceipt => ({
  transactionHash, blockNumber: 123n, from: buyer, to: escrow, gasUsed: 21000n, status: "success",
});
const baseOptions = (): PaymentOptions => ({
  approve: async () => approvalHash, deposit: async () => depositHash,
  receipt: async hash => receipt(hash), record: () => undefined,
  validateWallet: () => undefined, beforeDeposit: async () => undefined, onStage: () => undefined, resume: false,
});
async function observedFailure(action: () => Promise<unknown>): Promise<unknown> {
  try { await action(); return undefined; } catch (error) { return error; }
}

describe("Escrow payment sequencing", () => {
  it("waits for approval confirmation before opening the deposit transaction", async () => {
    // Given: a deferred approval receipt.
    let release = (_receipt: ChainReceipt): void => undefined;
    const approvalGate = new Promise<ChainReceipt>(resolve => { release = resolve; });
    const recorded: TransactionRecord[] = [];
    let deposits = 0;
    // When: funding starts but approval is still unconfirmed.
    const pending = fundOrder({ ...baseOptions(), record: item => recorded.push(item),
      receipt: async hash => hash === approvalHash ? approvalGate : receipt(hash),
      deposit: async () => { deposits += 1; return depositHash; },
    });
    await new Promise<void>(resolve => setImmediate(resolve));
    expect(deposits).to.equal(0);
    release(receipt(approvalHash));
    await pending;
    // Then: both confirmed transactions are recorded in order, once.
    expect(deposits).to.equal(1);
    expect(recorded.map(item => item.operation)).to.deep.equal(["approve", "deposit"]);
  });

  for (const scenario of ["rejected", "undefined", "reverted", "wallet-changed", "allowance-failed"] as const) {
    it(`does not send deposit after ${scenario}`, async () => {
      // Given: an interrupted or invalid approval-to-deposit transition.
      let deposits = 0;
      let walletChecks = 0;
      const options: PaymentOptions = { ...baseOptions(),
        approve: async () => {
          if (scenario === "rejected") throw new Error("User rejected");
          return scenario === "undefined" ? undefined : approvalHash;
        },
        receipt: async hash => scenario === "reverted" ? { ...receipt(hash), status: "reverted" } : receipt(hash),
        validateWallet: () => { walletChecks += 1; if (scenario === "wallet-changed" && walletChecks > 1) throw new Error("Account changed"); },
        beforeDeposit: async () => { if (scenario === "allowance-failed") throw new Error("Allowance insufficient"); },
        deposit: async () => { deposits += 1; return depositHash; },
      };
      // When: funding is attempted.
      const failure = await observedFailure(() => fundOrder(options));
      // Then: the failure is surfaced and no deposit is broadcast.
      expect(failure).to.be.instanceOf(Error);
      expect(deposits).to.equal(0);
    });
  }

  it("can explicitly resume a previously approved payment", async () => {
    // Given: the user explicitly chooses to resume, and fresh chain checks pass.
    let approvals = 0;
    let deposits = 0;
    // When: resumed funding is requested.
    await fundOrder({ ...baseOptions(), resume: true,
      approve: async () => { approvals += 1; return approvalHash; },
      deposit: async () => { deposits += 1; return depositHash; },
    });
    // Then: only the missing deposit is sent.
    expect(approvals).to.equal(0);
    expect(deposits).to.equal(1);
  });

  it("keeps a successful approval record if the user rejects deposit", async () => {
    // Given: the approval succeeds and deposit is rejected.
    const records: TransactionRecord[] = [];
    // When: the user cancels the second wallet request.
    const failure = await observedFailure(() => fundOrder({ ...baseOptions(), record: item => records.push(item),
      deposit: async () => { throw new Error("User rejected deposit"); },
    }));
    // Then: the page can show the completed approval without claiming a payment.
    expect(failure).to.be.instanceOf(Error);
    expect(records.map(item => item.operation)).to.deep.equal(["approve"]);
  });

  it("records reverted receipts as failures", async () => {
    // Given: a transaction really mined with a reverted status.
    const records: TransactionRecord[] = [];
    // When: its receipt is handled.
    const failure = await observedFailure(() => confirmedTransaction("deposit", async () => depositHash,
      async hash => ({ ...receipt(hash), status: "reverted" }), item => records.push(item)));
    // Then: its real hash/status is kept and success is not reported.
    expect(failure).to.be.instanceOf(Error);
    expect(records[0]?.status).to.equal("reverted");
  });

  it("uses the confirmed replacement hash when a transaction is sped up", async () => {
    // Given: the wrapper returns an original hash but confirms its replacement.
    // When: the canonical mined hash is captured.
    const result = await confirmedHash(async onConfirmed => {
      onConfirmed({ transactionHash: depositHash });
      return approvalHash;
    });
    // Then: follow-up receipt checks use the hash that actually mined.
    expect(result).to.equal(depositHash);
  });
});
