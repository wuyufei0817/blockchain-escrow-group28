import type { Address, Hash } from "viem";
import { TransactionRevertedError } from "../scaffold-eth/transactionError";

export type Operation = "createOrder" | "approve" | "deposit" | "confirm" | "dispute" | "resolve";
export type ChainReceipt = {
  readonly transactionHash: Hash;
  readonly blockNumber: bigint;
  readonly from: Address;
  readonly to: Address | null;
  readonly gasUsed: bigint;
  readonly status: "success" | "reverted";
};
export type TransactionRecord = ChainReceipt & { readonly operation: Operation };
export type PaymentOptions = {
  readonly approve: () => Promise<Hash | undefined>;
  readonly deposit: () => Promise<Hash | undefined>;
  readonly receipt: (hash: Hash) => Promise<ChainReceipt>;
  readonly record: (record: TransactionRecord) => void;
  readonly validateWallet: () => void;
  readonly beforeDeposit: () => Promise<void>;
  readonly onStage: (stage: "approve" | "deposit") => void;
  readonly resume: boolean;
};

export class WalletActionError extends Error {
  override readonly name = "WalletActionError";
}

export async function confirmedTransaction<T extends ChainReceipt>(
  operation: Operation,
  send: () => Promise<Hash | undefined>,
  receipt: (hash: Hash) => Promise<T>,
  record: (record: TransactionRecord) => void,
): Promise<T> {
  let hash: Hash | undefined;
  try {
    hash = await send();
  } catch (error) {
    if (error instanceof TransactionRevertedError) {
      // The wallet wrapper already received this failure. Retain it even if
      // a second RPC receipt lookup would be unavailable.
      record({ operation, ...error.receipt });
      throw new WalletActionError("链上交易失败，请保留交易哈希核对。");
    }
    throw error;
  }
  if (!hash) throw new WalletActionError("交易未发送，请检查钱包连接与网络。");
  const result = await receipt(hash);
  record({ operation, ...result });
  if (result.status !== "success") throw new WalletActionError("链上交易失败，请保留交易哈希核对。");
  return result;
}

export async function fundOrder(options: PaymentOptions): Promise<void> {
  options.validateWallet();
  if (!options.resume) {
    options.onStage("approve");
    await confirmedTransaction("approve", options.approve, options.receipt, options.record);
  }
  options.validateWallet();
  await options.beforeDeposit();
  options.validateWallet();
  options.onStage("deposit");
  await confirmedTransaction("deposit", options.deposit, options.receipt, options.record);
}

export async function confirmedHash(
  submit: (onConfirmed: (receipt: Pick<ChainReceipt, "transactionHash">) => void) => Promise<Hash | undefined>,
): Promise<Hash | undefined> {
  let minedHash: Hash | undefined;
  const originalHash = await submit(receipt => { minedHash = receipt.transactionHash; });
  return minedHash ?? originalHash;
}
