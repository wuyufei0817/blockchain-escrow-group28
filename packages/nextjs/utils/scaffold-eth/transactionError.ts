import type { TransactionReceipt } from "viem";

/** A mined failure still has a receipt that callers may need to retain. */
export class TransactionRevertedError extends Error {
  override readonly name = "TransactionRevertedError";

  constructor(readonly receipt: TransactionReceipt) {
    super("Transaction reverted");
  }
}
