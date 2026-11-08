"use client";

import { useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { parseAbiItem } from "viem";
import { usePublicClient } from "wagmi";
import { useDeployedContractInfo } from "~~/hooks/scaffold-eth";
import { EventIndexError, scanCreatedOrders } from "~~/utils/escrow/creationIndex";
import type { CreationSnapshot } from "~~/utils/escrow/creationIndex";

const DEPLOYMENT_BLOCK = 2673049n;
const INITIAL_SNAPSHOT: CreationSnapshot = { orders: [], throughBlock: DEPLOYMENT_BLOCK - 1n };
const CREATED_EVENT = parseAbiItem(
  "event OrderCreated(uint256 indexed id, address indexed buyer, address indexed seller, address token, uint256 amount, uint256 deadline)",
);
type Progress = CreationSnapshot & { readonly head: bigint; readonly count: bigint };

export function useEscrowOrders() {
  const client = usePublicClient({ chainId: 60600 });
  const { data: deployment } = useDeployedContractInfo({ contractName: "Escrow", chainId: 60600 });
  const checkpoint = useRef<CreationSnapshot>(INITIAL_SNAPSHOT);
  const [progress, setProgress] = useState<Progress>({ ...INITIAL_SNAPSHOT, head: 0n, count: 0n });

  const query = useQuery({
    queryKey: ["escrow-created-index", 60600, deployment?.address, DEPLOYMENT_BLOCK.toString()],
    enabled: client !== undefined && deployment !== undefined,
    refetchInterval: 30000,
    retry: 1,
    queryFn: async ({ signal }) => {
      if (!client || !deployment) throw new EventIndexError("合约配置尚未加载。");
      const head = await client.getBlockNumber({ cacheTime: 0 });
      const count = await client.readContract({
        address: deployment.address, abi: deployment.abi, functionName: "orderCount", blockNumber: head,
      });
      signal.throwIfAborted();
      if (checkpoint.current.throughBlock > head || BigInt(checkpoint.current.orders.length) > count) {
        checkpoint.current = INITIAL_SNAPSHOT;
      }
      try {
        return await scanCreatedOrders({
          snapshot: checkpoint.current, head, count, signal,
          readLogs: async (fromBlock, toBlock) => {
            const logs = await client.getLogs({
              address: deployment.address, event: CREATED_EVENT, fromBlock, toBlock, strict: true,
            });
            return logs.map(log => {
              if (log.blockNumber === null || log.transactionHash === null) {
                throw new EventIndexError("RPC返回了未确认的创建事件。");
              }
              return { id: log.args.id, blockNumber: log.blockNumber, transactionHash: log.transactionHash };
            });
          },
          onProgress: snapshot => {
            signal.throwIfAborted();
            checkpoint.current = snapshot;
            setProgress({ ...snapshot, head, count });
          },
        });
      } catch (error) {
        if (error instanceof EventIndexError) {
          checkpoint.current = { ...checkpoint.current, throughBlock: DEPLOYMENT_BLOCK - 1n };
        }
        throw error;
      }
    },
  });

  return { ...progress, isFetching: query.isFetching, error: query.error, refetch: query.refetch };
}
