"use client";

import { useState } from "react";
import { useAccount } from "wagmi";

type Props = {
  readonly busy: boolean;
  readonly onCreate: (input: unknown) => Promise<void>;
};

export function CreateOrderForm({ busy, onCreate }: Props) {
  const { address, chainId } = useAccount();
  const [seller, setSeller] = useState("0x92b0f84D625437245c3296012A0784d10C560309");
  const [amount, setAmount] = useState("30");
  const [minutes, setMinutes] = useState("30");
  const [description, setDescription] = useState("页面订单");
  return <section className="mb-6 rounded-2xl bg-base-100 p-6 shadow">
    <h2 className="mb-4 text-xl font-semibold">创建订单</h2>
    <form className="grid gap-4 sm:grid-cols-2" onSubmit={event => {
      event.preventDefault();
      void onCreate({ seller, amount, minutes, description });
    }}>
      <div className="sm:col-span-2">
        <label className="mb-1 block font-medium" htmlFor="order-seller">卖家钱包地址</label>
        <input id="order-seller" className="input input-bordered w-full" required value={seller}
          placeholder="0x…" maxLength={42} disabled={busy} onChange={event => setSeller(event.target.value)} />
      </div>
      <div>
        <label className="mb-1 block font-medium" htmlFor="order-amount">金额（课程代币个数）</label>
        <input id="order-amount" className="input input-bordered w-full" inputMode="decimal" required
          pattern="[0-9]+([.][0-9]{1,18})?" value={amount} disabled={busy} onChange={event => setAmount(event.target.value)} />
      </div>
      <div>
        <label className="mb-1 block font-medium" htmlFor="order-minutes">多少分钟后到期</label>
        <input id="order-minutes" className="input input-bordered w-full" inputMode="numeric" required
          pattern="[0-9]+" value={minutes} disabled={busy} onChange={event => setMinutes(event.target.value)} />
      </div>
      <div className="sm:col-span-2">
        <label className="mb-1 block font-medium" htmlFor="order-description">订单描述</label>
        <textarea id="order-description" className="textarea textarea-bordered w-full" required maxLength={500}
          value={description} disabled={busy} onChange={event => setDescription(event.target.value)} />
      </div>
      <div className="sm:col-span-2">
        {!address && <p>请连接买家钱包后创建订单。</p>}
        {address && chainId !== 60600 && <p className="text-warning">请将钱包切换到 POTOS Testnet。</p>}
        <button type="submit" className="btn btn-primary" disabled={busy || !address || chainId !== 60600}>
          {busy ? "交易处理中…" : "创建订单"}
        </button>
      </div>
    </form>
  </section>;
}
