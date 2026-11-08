"use client";

import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { BaseError, decodeEventLog, parseAbiItem, toEventSelector } from "viem";
import type { Address } from "viem";
import { usePublicClient } from "wagmi";
import { getAccount } from "wagmi/actions";
import { useDeployedContractInfo, useScaffoldWriteContract } from "~~/hooks/scaffold-eth";
import { wagmiConfig } from "~~/services/web3/wagmiConfig";
import { lifecycleActions, LIFECYCLE_LABELS, LIFECYCLE_OPERATIONS } from "~~/utils/escrow/orderPermissions";
import type { LifecycleAction } from "~~/utils/escrow/orderPermissions";
import { createOrderSchema, orderDeadline } from "~~/utils/escrow/orderForm";
import { confirmedHash, confirmedTransaction, fundOrder, WalletActionError } from "~~/utils/escrow/paymentFlow";
import type { TransactionRecord } from "~~/utils/escrow/paymentFlow";

export type WritePhase = "idle" | "create" | "approve" | "deposit" | "lifecycle";
export type WriteNotice = { readonly tone: "success" | "error"; readonly text: string };
const CREATED = parseAbiItem("event OrderCreated(uint256 indexed id, address indexed buyer, address indexed seller, address token, uint256 amount, uint256 deadline)");

function requireWallet(expected?: Address): Address {
  const connection = getAccount(wagmiConfig);
  if (!connection.address || connection.status !== "connected") throw new WalletActionError("请先连接钱包。");
  if (connection.chainId !== 60600) throw new WalletActionError("请切换钱包到 POTOS Testnet。");
  if (expected && connection.address.toLowerCase() !== expected.toLowerCase()) {
    throw new WalletActionError("操作期间钱包账户已切换，请检查账户后重新操作。");
  }
  return connection.address;
}

export function useEscrowActions(onCreated: (id: bigint) => void) {
  const client = usePublicClient({ chainId: 60600 });
  const queryClient = useQueryClient();
  const { data: escrow } = useDeployedContractInfo({ contractName: "Escrow", chainId: 60600 });
  const { data: token } = useDeployedContractInfo({ contractName: "WYFToken", chainId: 60600 });
  const { writeContractAsync: writeEscrow } = useScaffoldWriteContract({ contractName: "Escrow", chainId: 60600 });
  const { writeContractAsync: writeToken } = useScaffoldWriteContract({ contractName: "WYFToken", chainId: 60600 });
  const lock = useRef(false);
  const [phase, setPhase] = useState<WritePhase>("idle");
  const [notice, setNotice] = useState<WriteNotice | null>(null);
  const [transactions, setTransactions] = useState<readonly TransactionRecord[]>([]);
  const record = (item: TransactionRecord) => setTransactions(previous =>
    [...previous.filter(existing => existing.transactionHash !== item.transactionHash), item]);

  const run = async (initialPhase: WritePhase, action: () => Promise<string>) => {
    if (lock.current) return;
    lock.current = true;
    setPhase(initialPhase);
    setNotice(null);
    try {
      const message = await action();
      setNotice({ tone: "success", text: message });
      await queryClient.invalidateQueries();
    } catch (error) {
      const text = error instanceof BaseError ? error.shortMessage
        : error instanceof Error ? error.message : "操作未完成，请核对钱包与链上状态。";
      setNotice({ tone: "error", text });
    } finally {
      lock.current = false;
      setPhase("idle");
    }
  };

  const createOrder = async (input: unknown) => run("create", async () => {
    const buyer = requireWallet();
    if (!client || !escrow || !token) throw new WalletActionError("合约配置尚未加载。");
    const parsed = createOrderSchema(buyer).safeParse(input);
    if (!parsed.success) throw new WalletActionError(parsed.error.issues.map(issue => issue.message).join(" "));
    const form = parsed.data;
    const [block, decimals] = await Promise.all([
      client.getBlock({ blockTag: "latest" }),
      client.readContract({ address: token.address, abi: token.abi, functionName: "decimals" }),
    ]);
    if (decimals !== 18) throw new WalletActionError("课程代币精度不是18，请核对代币配置。");
    const deadline = orderDeadline(block.timestamp, form.minutes);
    requireWallet(buyer);
    const receipt = await confirmedTransaction("createOrder", () => confirmedHash(onBlockConfirmation =>
      writeEscrow({ functionName: "createOrder", args: [form.seller, token.address, form.amount, deadline, form.description], account: buyer },
        { onBlockConfirmation })), hash => client.getTransactionReceipt({ hash }), record);
    const creationLog = receipt.logs.find(log =>
      log.address.toLowerCase() === escrow.address.toLowerCase() && log.topics[0] === toEventSelector(CREATED));
    if (!creationLog) throw new WalletActionError("交易已确认，但尚未解析到创建事件；请保留哈希核对，不要重复提交。");
    const event = decodeEventLog({ abi: [CREATED], data: creationLog.data, topics: creationLog.topics, strict: true });
    onCreated(event.args.id);
    return `创建成功，订单 #${event.args.id.toString()}。请保存下方交易哈希。`;
  });

  const payOrder = async (id: bigint, resume = false) => run(resume ? "deposit" : "approve", async () => {
    const buyer = requireWallet();
    if (!client || !escrow || !token) throw new WalletActionError("合约配置尚未加载。");
    const readOrder = () => client.readContract({ address: escrow.address, abi: escrow.abi, functionName: "getOrder", args: [id] });
    const order = await readOrder();
    if (order.buyer.toLowerCase() !== buyer.toLowerCase() || order.status !== 0) {
      throw new WalletActionError("只有买家能为待付款订单付款，请刷新订单状态。");
    }
    if (order.token.toLowerCase() !== token.address.toLowerCase()) throw new WalletActionError("订单代币与课程代币不一致。");
    const checkFunding = async (checkAllowance: boolean) => {
      const [current, block, balance, allowance] = await Promise.all([
        readOrder(), client.getBlock({ blockTag: "latest" }),
        client.readContract({ address: token.address, abi: token.abi, functionName: "balanceOf", args: [buyer] }),
        client.readContract({ address: token.address, abi: token.abi, functionName: "allowance", args: [buyer, escrow.address] }),
      ]);
      if (current.status !== 0 || current.buyer.toLowerCase() !== buyer.toLowerCase()) throw new WalletActionError("订单状态或买家已变化，请刷新。");
      if (block.timestamp >= current.deadline) throw new WalletActionError("订单已到期，请创建新订单。");
      if (balance < current.amount) throw new WalletActionError("课程代币余额不足，请补足后再付款。");
      if (checkAllowance && allowance < current.amount) throw new WalletActionError("授权额度不足，请选择“授权并付款”。");
    };
    await checkFunding(resume);
    await fundOrder({
      resume, record, onStage: setPhase, validateWallet: () => { requireWallet(buyer); },
      beforeDeposit: () => checkFunding(true), receipt: hash => client.getTransactionReceipt({ hash }),
      approve: () => confirmedHash(onBlockConfirmation => writeToken({ functionName: "approve", args: [escrow.address, order.amount], account: buyer }, { onBlockConfirmation })),
      deposit: () => confirmedHash(onBlockConfirmation => writeEscrow({ functionName: "deposit", args: [id], account: buyer }, { onBlockConfirmation })),
    });
    return `订单 #${id.toString()} 付款成功。已确认的授权和付款交易分别列在下方。`;
  });

  const actOrder = async (id: bigint, action: LifecycleAction) => run("lifecycle", async () => {
    const actor = requireWallet();
    if (!client || !escrow) throw new WalletActionError("合约配置尚未加载。");
    const [order, arbiter] = await Promise.all([
      client.readContract({ address: escrow.address, abi: escrow.abi, functionName: "getOrder", args: [id] }),
      client.readContract({ address: escrow.address, abi: escrow.abi, functionName: "arbiter" }),
    ]);
    if (!lifecycleActions(order, arbiter, actor).includes(action)) {
      throw new WalletActionError("当前账户或订单状态不允许此操作，请刷新订单后核对。");
    }
    const submit = () => confirmedHash(onBlockConfirmation => {
      requireWallet(actor);
      const options = { onBlockConfirmation };
      switch (action) {
        case "confirm": return writeEscrow({ functionName: "confirm", args: [id], account: actor }, options);
        case "dispute": return writeEscrow({ functionName: "dispute", args: [id], account: actor }, options);
        case "resolveSeller": return writeEscrow({ functionName: "resolve", args: [id, true], account: actor }, options);
        case "resolveBuyer": return writeEscrow({ functionName: "resolve", args: [id, false], account: actor }, options);
        default: {
          const unsupported: never = action;
          throw new WalletActionError(`不支持的操作：${unsupported}`);
        }
      }
    });
    await confirmedTransaction(LIFECYCLE_OPERATIONS[action], submit,
      hash => client.getTransactionReceipt({ hash }), record);
    return `订单 #${id.toString()} 的“${LIFECYCLE_LABELS[action]}”交易已确认，请保存下方哈希。`;
  });

  return { createOrder, payOrder, actOrder, phase, notice, transactions, busy: phase !== "idle" };
}
