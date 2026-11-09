import { expect } from "chai";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import ts from "typescript";
import { confirmedHash, confirmedTransaction, fundOrder } from "../../nextjs/utils/escrow/paymentFlow";
import type { ChainReceipt, TransactionRecord } from "../../nextjs/utils/escrow/paymentFlow";

const original = `0x${"11".repeat(32)}` as const;
const replacement = `0x${"22".repeat(32)}` as const;
const buyer = `0x${"33".repeat(20)}` as const;
const escrow = `0x${"44".repeat(20)}` as const;

// Exercise the actual hook and its callbacks without a browser or real wallet.
function wrapper(mode: "success" | "repriced" | "cancelled" | "replaced" | "reverted" | "rejected" | "timeout") {
  const receipt: ChainReceipt = {
    transactionHash: mode === "success" || mode === "reverted" ? original : replacement,
    blockNumber: 123n, from: buyer, to: mode === "cancelled" ? buyer : escrow,
    gasUsed: 21000n, status: mode === "reverted" ? "reverted" : "success",
  };
  const notices: string[] = [];
  const mocks: Record<string, unknown> = {
    "react/jsx-runtime": { jsx() {}, jsxs() {} },
    wagmi: { useWalletClient: () => ({ data: { getChainId: async () => 60600 } }) },
    "wagmi/actions": { getPublicClient: () => ({ waitForTransactionReceipt: async (options: {
      onReplaced?: (event: { reason: string }) => void;
    }) => {
      if (mode === "timeout") throw new Error("Confirmation timed out");
      if (mode === "repriced" || mode === "cancelled" || mode === "replaced") options.onReplaced?.({ reason: mode });
      return receipt;
    } }) },
    "~~/scaffold.config": { default: { targetNetworks: [{ id: 60600 }] } },
    "~~/services/web3/wagmiConfig": { wagmiConfig: {} },
    "~~/utils/scaffold-eth": { getBlockExplorerTxLink: () => "", notification: {
      loading: () => 1, remove() {}, success: () => notices.push("success"), error() {},
    } },
    "~~/utils/scaffold-eth/contract": { getParsedErrorWithAllAbis: (error: Error) => error.message },
  };
  const filename = path.resolve(__dirname, "../../nextjs/hooks/scaffold-eth/useTransactor.tsx");
  const code = ts.transpileModule(fs.readFileSync(filename, "utf8"), { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX,
  } }).outputText;
  const exported = { exports: {} as { useTransactor: () => (
    send: () => Promise<ChainReceipt["transactionHash"]>,
    options: { onBlockConfirmation: (value: ChainReceipt) => void },
  ) => Promise<ChainReceipt["transactionHash"] | undefined> } };
  vm.runInNewContext(code, {
    exports: exported.exports, module: exported,
    require: (name: string) => name in mocks ? mocks[name]
      : name.startsWith("~~/") ? require(path.resolve(__dirname, "../../nextjs", name.slice(3)))
        : require(name),
    console: { error() {} },
  }, { filename });
  const transactor = exported.exports.useTransactor();
  const send = () => confirmedHash(onBlockConfirmation => transactor(async () => {
    if (mode === "rejected") throw new Error("User rejected request");
    return original;
  }, { onBlockConfirmation }));
  return { send, receipt, notices };
}

describe("Escrow real transaction wrapper", () => {
  for (const mode of ["rejected", "timeout"] as const) {
    it(`does not invent a receipt or report success after ${mode}`, async () => {
      const tx = wrapper(mode);
      const records: TransactionRecord[] = [];
      let failure: unknown;
      try { await confirmedTransaction("deposit", tx.send, async () => tx.receipt, item => records.push(item)); }
      catch (error) { failure = error; }
      expect(failure).to.not.equal(undefined);
      expect(records).to.deep.equal([]);
      expect(tx.notices).to.deep.equal([]);
    });
  }

  for (const mode of ["cancelled", "replaced"] as const) {
    it(`rejects a ${mode} business transaction and never sends deposit`, async () => {
      const tx = wrapper(mode);
      const records: TransactionRecord[] = [];
      let deposits = 0;
      let failure: unknown;
      try {
        await fundOrder({ approve: tx.send, deposit: async () => { deposits++; return original; },
          receipt: async () => tx.receipt, record: item => records.push(item), resume: false,
          validateWallet() {}, beforeDeposit: async () => {}, onStage() {},
        });
      } catch (error) { failure = error; }
      expect(failure).to.not.equal(undefined);
      expect(deposits).to.equal(0);
      expect(records).to.deep.equal([]);
      expect(tx.notices).to.deep.equal([]);
    });
  }

  it("records a mined revert through the actual wrapper before reporting failure", async () => {
    const tx = wrapper("reverted");
    const records: TransactionRecord[] = [];
    let failure: unknown;
    try { await confirmedTransaction("deposit", tx.send, async () => {
      throw new Error("RPC unavailable after the wrapper received the revert");
    }, item => records.push(item)); }
    catch (error) { failure = error; }
    expect(failure).to.not.equal(undefined);
    expect(records).to.have.length(1);
    expect(records[0]).to.include({ transactionHash: original, blockNumber: 123n, status: "reverted" });
    expect(tx.notices).to.deep.equal([]);
  });

  for (const mode of ["success", "repriced"] as const) {
    it(`keeps the canonical receipt for ${mode}`, async () => {
      const tx = wrapper(mode);
      expect(await tx.send()).to.equal(tx.receipt.transactionHash);
      expect(tx.notices).to.deep.equal(["success"]);
    });
  }
});
