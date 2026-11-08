"use client";

import { formatUnits } from "viem";
import type { Address as WalletAddress } from "viem";
import { Address } from "~~/components/scaffold-eth";
import { useScaffoldReadContract } from "~~/hooks/scaffold-eth";
import { useEscrowOrders } from "~~/hooks/escrow/useEscrowOrders";
import { STATUS_LABELS } from "~~/utils/escrow/orderStatus";

type ListProps = {
  readonly onSelect: (id: bigint) => void;
  readonly selectedId: bigint;
  readonly decimals: number | undefined;
  readonly token: WalletAddress | undefined;
};
type RowProps = ListProps & { readonly id: bigint };

function OrderRow({ id, selectedId, onSelect, decimals, token }: RowProps) {
  const { data: order, error } = useScaffoldReadContract({
    contractName: "Escrow", functionName: "getOrder", args: [id], chainId: 60600,
  });
  if (error) return <tr><td>{id.toString()}</td><td colSpan={5} className="text-error">读取失败：{error.message}</td></tr>;
  if (!order) return <tr><td>{id.toString()}</td><td colSpan={5}>读取中…</td></tr>;
  const courseToken = token?.toLowerCase() === order.token.toLowerCase();
  return <tr className={selectedId === id ? "bg-base-200" : ""}>
    <td className="font-semibold">#{id.toString()}</td>
    <td><Address address={order.buyer} /></td>
    <td><Address address={order.seller} /></td>
    <td>{courseToken && decimals !== undefined ? `${formatUnits(order.amount, decimals)} 个` : `${order.amount}（原始单位）`}</td>
    <td>{STATUS_LABELS[order.status] ?? `未知状态（${order.status}）`}</td>
    <td><button className="btn btn-sm" onClick={() => onSelect(id)}
      aria-label={`查看订单${id.toString()}`}>{selectedId === id ? "当前详情" : "查看详情"}</button></td>
  </tr>;
}

export function OrderList(props: ListProps) {
  const index = useEscrowOrders();
  return <section className="mb-6 rounded-2xl bg-base-100 p-6 shadow">
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <h2 className="m-0 text-xl font-semibold">订单列表</h2>
      <button className="btn btn-sm" disabled={index.isFetching} onClick={() => index.refetch()}>
        {index.isFetching ? "同步中…" : "同步订单"}
      </button>
    </div>
    <p aria-live="polite">已从创建事件发现 {index.orders.length} 笔订单
      {index.head > 0n ? `，链上共 ${index.count.toString()} 笔` : "，正在连接测试网…"}。
    </p>
    {index.isFetching && index.head > 0n && <p className="text-sm">
      正在同步区块 {index.throughBlock.toString()} / {index.head.toString()}，已有订单可先查看。
    </p>}
    {index.error && <p role="alert" className="break-words text-error">事件同步未完成：{index.error.message} 请重试。</p>}
    {!index.isFetching && !index.error && index.head > 0n && index.orders.length === 0 && <p>该合约还没有订单。</p>}
    <div className="overflow-x-auto">
      <table className="table">
        <thead><tr><th>订单</th><th>买家</th><th>卖家</th><th>金额</th><th>状态</th><th>详情</th></tr></thead>
        <tbody>{index.orders.map(order => <OrderRow key={order.id.toString()} id={order.id} {...props} />)}</tbody>
      </table>
    </div>
  </section>;
}
