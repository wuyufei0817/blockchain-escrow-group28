"use client";

import { formatUnits } from "viem";
import type { Address } from "viem";
import { useAccount } from "wagmi";
import { useScaffoldReadContract } from "~~/hooks/scaffold-eth";

type Props = {
  readonly id: bigint;
  readonly buyer: Address;
  readonly amount: bigint;
  readonly token: Address;
  readonly status: number;
  readonly courseToken: Address | undefined;
  readonly escrow: Address | undefined;
  readonly decimals: number | undefined;
  readonly busy: boolean;
  readonly onPay: (id: bigint, resume?: boolean) => Promise<void>;
};

export function PaymentAction(props: Props) {
  const { address, chainId } = useAccount();
  const { data: balance } = useScaffoldReadContract({
    contractName: "WYFToken", functionName: "balanceOf", args: [props.buyer], chainId: 60600,
  });
  const { data: allowance } = useScaffoldReadContract({
    contractName: "WYFToken", functionName: "allowance", args: [props.buyer, props.escrow], chainId: 60600,
  });
  if (!address || address.toLowerCase() !== props.buyer.toLowerCase() || props.status !== 0) return null;
  const matchingToken = props.token.toLowerCase() === props.courseToken?.toLowerCase();
  const fundsKnown = balance !== undefined && allowance !== undefined && props.decimals !== undefined;
  const insufficient = balance !== undefined && balance < props.amount;
  const disabled = props.busy || chainId !== 60600 || !matchingToken || !fundsKnown || insufficient;
  return <div className="mt-6 border-t border-base-300 pt-5">
    <h3 className="mb-2 font-semibold">为订单 #{props.id.toString()} 付款</h3>
    <p>先授权课程代币给本 Escrow，确认后再付款；每一步都需要在钱包中确认。</p>
    <p>买家代币余额：{balance !== undefined && props.decimals !== undefined
      ? `${formatUnits(balance, props.decimals)} 个` : "读取中…"}</p>
    {insufficient && <p className="text-error">代币余额不足，请补足后再付款。</p>}
    {!matchingToken && <p className="text-error">订单代币与课程代币不一致。</p>}
    {chainId !== 60600 && <p className="text-warning">请切换到 POTOS Testnet。</p>}
    <div className="flex flex-wrap gap-3">
      <button className="btn btn-primary" disabled={disabled} onClick={() => props.onPay(props.id, false)}>
        {props.busy ? "交易处理中…" : "授权并付款"}
      </button>
      {allowance !== undefined && allowance >= props.amount && <button className="btn" disabled={disabled}
        onClick={() => props.onPay(props.id, true)}>继续付款（已授权）</button>}
    </div>
  </div>;
}
