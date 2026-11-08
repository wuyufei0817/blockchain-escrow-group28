import type { Address } from "viem";

export type LifecycleAction = "confirm" | "dispute" | "resolveSeller" | "resolveBuyer";
export type OrderParties = {
  readonly buyer: Address;
  readonly seller: Address;
  readonly status: number;
};

export function lifecycleActions(
  order: OrderParties,
  arbiter: Address | undefined,
  account: Address | undefined,
): readonly LifecycleAction[] {
  if (!account) return [];
  const sameAccount = (wallet: Address | undefined) => wallet?.toLowerCase() === account.toLowerCase();
  switch (order.status) {
    case 1:
      if (sameAccount(order.buyer)) return ["confirm", "dispute"];
      return sameAccount(order.seller) ? ["dispute"] : [];
    case 4:
      return sameAccount(arbiter) ? ["resolveSeller", "resolveBuyer"] : [];
    default:
      // Unknown numeric values from an ABI read grant no permissions.
      return [];
  }
}

export const LIFECYCLE_LABELS = {
  confirm: "确认收货", dispute: "发起争议", resolveSeller: "判给卖家", resolveBuyer: "退给买家",
} as const;

export const LIFECYCLE_OPERATIONS = {
  confirm: "confirm", dispute: "dispute", resolveSeller: "resolve", resolveBuyer: "resolve",
} as const;
