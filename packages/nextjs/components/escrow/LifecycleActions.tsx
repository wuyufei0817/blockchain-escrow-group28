"use client";

import type { Address } from "viem";
import { useAccount } from "wagmi";
import { LIFECYCLE_LABELS, lifecycleActions } from "~~/utils/escrow/orderPermissions";
import type { LifecycleAction, OrderParties } from "~~/utils/escrow/orderPermissions";

type Props = {
  readonly id: bigint;
  readonly order: OrderParties;
  readonly arbiter: Address | undefined;
  readonly busy: boolean;
  readonly onAction: (id: bigint, action: LifecycleAction) => Promise<void>;
};

export function LifecycleActions({ id, order, arbiter, busy, onAction }: Props) {
  const { address, chainId } = useAccount();
  const available = lifecycleActions(order, arbiter, address);
  if (available.length === 0) return null;
  return <div className="mt-6 border-t border-base-300 pt-5">
    <h3 className="mb-2 font-semibold">订单 #{id.toString()} 的操作</h3>
    <p>确认收货会将代币付给卖家；争议订单只能由仲裁人裁决。请核对订单后在钱包确认。</p>
    {chainId !== 60600 && <p className="text-warning">请切换到 POTOS Testnet 后操作。</p>}
    <div className="flex flex-wrap gap-3">
      {available.map(action => <button key={action} className="btn btn-primary"
        disabled={busy || chainId !== 60600} onClick={() => { void onAction(id, action); }}>
        {busy ? "交易处理中…" : LIFECYCLE_LABELS[action]}
      </button>)}
    </div>
  </div>;
}
