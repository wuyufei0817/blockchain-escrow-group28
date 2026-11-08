"use client";

import type { WriteNotice, WritePhase } from "~~/hooks/escrow/useEscrowActions";
import type { TransactionRecord } from "~~/utils/escrow/paymentFlow";

const PHASE_LABELS = {
  idle: "", create: "创建订单：请在钱包确认，并等待上链。",
  approve: "授权中：请在钱包确认，授权成功后才会发起付款。",
  deposit: "付款中：请确认付款交易，并等待上链。",
  lifecycle: "订单操作中：请核对钱包交易，并等待上链。",
} as const;
const OPERATIONS = {
  createOrder: "创建订单", approve: "代币授权", deposit: "托管付款",
  confirm: "确认收货", dispute: "发起争议", resolve: "仲裁裁决",
} as const;
const STATUS = { success: "成功", reverted: "链上失败" } as const;
const NOTICE_STYLE = { success: "text-success", error: "text-error" } as const;
type Props = {
  readonly phase: WritePhase;
  readonly notice: WriteNotice | null;
  readonly transactions: readonly TransactionRecord[];
};

export function WriteProgress({ phase, notice, transactions }: Props) {
  if (phase === "idle" && !notice && transactions.length === 0) return null;
  return <section className="mb-6 rounded-2xl bg-base-100 p-6 shadow">
    <h2 className="mb-3 text-xl font-semibold">交易进度与记录</h2>
    <p aria-live="polite">{PHASE_LABELS[phase]}</p>
    {notice && <p role={notice.tone === "error" ? "alert" : "status"} className={NOTICE_STYLE[notice.tone]}>{notice.text}</p>}
    {transactions.length > 0 && <>
      <p>请保存完整交易哈希和发起账户，供报告交易表使用。刷新页面前先复制记录。</p>
      <div className="overflow-x-auto"><table className="table">
        <thead><tr><th>操作</th><th>状态</th><th>区块</th><th>交易哈希与发起账户</th></tr></thead>
        <tbody>{transactions.map(item => <tr key={item.transactionHash}>
          <td>{OPERATIONS[item.operation]}</td><td>{STATUS[item.status]}</td><td>{item.blockNumber.toString()}</td>
          <td><a className="link break-all" href={`https://scan-testnet.potos.hk/tx/${item.transactionHash}`}
            target="_blank" rel="noreferrer">{item.transactionHash}</a>
            <div className="mt-1 break-all text-sm">From: {item.from}</div></td>
        </tr>)}</tbody>
      </table></div>
    </>}
  </section>;
}
