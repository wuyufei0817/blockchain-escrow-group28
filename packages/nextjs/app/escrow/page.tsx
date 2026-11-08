"use client";

import { useState } from "react";
import { CreateOrderForm } from "~~/components/escrow/CreateOrderForm";
import { LifecycleActions } from "~~/components/escrow/LifecycleActions";
import { PaymentAction } from "~~/components/escrow/PaymentAction";
import { WriteProgress } from "~~/components/escrow/WriteProgress";
import { useEscrowActions } from "~~/hooks/escrow/useEscrowActions";
import { OrderList } from "~~/components/escrow/OrderList";
import { STATUS_LABELS } from "~~/utils/escrow/orderStatus";
import { formatUnits } from "viem";
import { useAccount } from "wagmi";
import { Address } from "~~/components/scaffold-eth";
import { useDeployedContractInfo, useScaffoldReadContract } from "~~/hooks/scaffold-eth";

export default function EscrowPage() {
  const [selectedOrderId, setSelectedOrderId] = useState(0n);
  const actions = useEscrowActions(setSelectedOrderId);
  const { address } = useAccount();
  const { data: deployment } = useDeployedContractInfo({ contractName: "Escrow", chainId: 60600 });
  const { data: arbiter } = useScaffoldReadContract({
    contractName: "Escrow", functionName: "arbiter", chainId: 60600,
  });
  const { data: orderCount } = useScaffoldReadContract({
    contractName: "Escrow", functionName: "orderCount", chainId: 60600,
  });
  const { data: decimals } = useScaffoldReadContract({
    contractName: "WYFToken", functionName: "decimals", chainId: 60600, watch: false,
  });
  const { data: tokenDeployment } = useDeployedContractInfo({ contractName: "WYFToken", chainId: 60600 });
  const { data: order, error, isFetching, refetch } = useScaffoldReadContract({
    contractName: "Escrow", functionName: "getOrder", args: [selectedOrderId], chainId: 60600,
    query: { enabled: orderCount !== undefined && orderCount > 0n },
  });

  const roles = [
    { label: "买家", wallet: order?.buyer },
    { label: "卖家", wallet: order?.seller },
    { label: "仲裁人", wallet: arbiter },
  ].filter(({ wallet }) => address && wallet?.toLowerCase() === address.toLowerCase())
    .map(({ label }) => label);
  const role = address ? roles.join(" / ") || "旁观者" : "未连接钱包";
  const isCourseToken = order?.token.toLowerCase() === tokenDeployment?.address.toLowerCase();

  return (
    <main className="mx-auto w-full max-w-4xl p-6 md:p-10">
      <h1 className="mb-2 text-3xl font-bold">托管交易</h1>
      <p className="mb-6">POTOS Testnet · 第28组 · 订单列表与详情</p>

      <section className="mb-6 rounded-2xl bg-base-100 p-6 shadow">
        <h2 className="mb-4 text-xl font-semibold">账户与合约</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div><p className="mb-1 font-medium">当前账户</p><Address address={address} /></div>
          <div><p className="mb-1 font-medium">Escrow 合约</p><Address address={deployment?.address} /></div>
          <div><p className="mb-1 font-medium">仲裁人</p><Address address={arbiter} /></div>
          <div><p className="mb-1 font-medium">当前订单中的角色</p><span className="badge badge-primary">{role}</span></div>
        </div>
        <p className="mb-0 mt-4">链上订单总数：{orderCount?.toString() ?? "读取中…"}</p>
      </section>

      <CreateOrderForm busy={actions.busy} onCreate={actions.createOrder} />
      <WriteProgress phase={actions.phase} notice={actions.notice} transactions={actions.transactions} />
      <OrderList selectedId={selectedOrderId} onSelect={setSelectedOrderId} decimals={decimals} token={tokenDeployment?.address} />

      <section className="rounded-2xl bg-base-100 p-6 shadow">
        <div className="mb-5 flex items-center justify-between gap-4">
          <h2 className="m-0 text-xl font-semibold">订单 #{selectedOrderId.toString()}</h2>
          <button className="btn btn-sm" disabled={isFetching || orderCount === undefined || orderCount === 0n}
            onClick={() => refetch()}>{isFetching ? "读取中…" : "刷新订单"}</button>
        </div>
        {error && <p role="alert" className="break-words text-error">读取失败：{error.message}</p>}
        {orderCount === 0n && <p>该合约还没有订单。</p>}
        {!order && !error && orderCount !== 0n && <p aria-live="polite">正在读取链上订单…</p>}
        {order && <dl className="grid gap-5 sm:grid-cols-2">
          <div><dt className="mb-1 font-medium">状态</dt>
            <dd className="badge badge-lg badge-secondary">{STATUS_LABELS[order.status] ?? `未知状态（${order.status}）`}</dd></div>
          <div><dt className="mb-1 font-medium">金额</dt><dd>
            {isCourseToken && decimals !== undefined
              ? `${formatUnits(order.amount, decimals)} 个课程代币`
              : `${order.amount.toString()}（原始单位）`}
          </dd></div>
          <div><dt className="mb-1 font-medium">买家</dt><dd><Address address={order.buyer} /></dd></div>
          <div><dt className="mb-1 font-medium">卖家</dt><dd><Address address={order.seller} /></dd></div>
          <div><dt className="mb-1 font-medium">代币合约</dt><dd><Address address={order.token} /></dd></div>
          <div><dt className="mb-1 font-medium">到期时间（北京时间）</dt><dd>
            {new Date(Number(order.deadline) * 1000).toLocaleString("zh-CN", { timeZone: "Asia/Shanghai", hour12: false })}
          </dd></div>
          <div className="sm:col-span-2"><dt className="mb-1 font-medium">描述</dt>
            <dd className="whitespace-pre-wrap break-words">{order.description}</dd></div>
        </dl>}
        {order && <PaymentAction id={selectedOrderId} buyer={order.buyer} amount={order.amount}
          token={order.token} status={order.status} courseToken={tokenDeployment?.address} escrow={deployment?.address}
          decimals={decimals} busy={actions.busy} onPay={actions.payOrder} />}
        {order && <LifecycleActions id={selectedOrderId} order={order} arbiter={arbiter}
          busy={actions.busy} onAction={actions.actOrder} />}
      </section>
    </main>
  );
}
