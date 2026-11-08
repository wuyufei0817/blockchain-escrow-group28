import { z } from "zod4";
import { getAddress, isAddress, parseUnits, zeroAddress } from "viem";
import type { Address } from "viem";

export const UINT256_MAX = (1n << 256n) - 1n;

export class OrderInputError extends Error {
  override readonly name = "OrderInputError";
}

export function createOrderSchema(buyer: Address) {
  return z.object({
    seller: z.string().trim()
      .refine(value => isAddress(value, { strict: false }), "请输入完整的卖家钱包地址。")
      .refine(value => value.toLowerCase() !== zeroAddress, "卖家地址不能为零地址。")
      .transform(value => getAddress(value)),
    amount: z.string().trim().max(100)
      .regex(/^\d+(?:\.\d{1,18})?$/, "金额须为普通正数，最多18位小数。")
      .transform(value => parseUnits(value, 18)).pipe(z.bigint().positive().max(UINT256_MAX)),
    minutes: z.string().trim().max(78).regex(/^\d+$/, "到期分钟须为正整数。")
      .transform(value => BigInt(value)).pipe(z.bigint().positive().max(UINT256_MAX / 60n)),
    description: z.string().trim().min(1, "请填写订单描述。").max(500, "描述最多500个字符。"),
  }).refine(value => value.seller.toLowerCase() !== buyer.toLowerCase(), {
    path: ["seller"], message: "卖家不能是买家本人。",
  });
}

export function orderDeadline(blockTimestamp: bigint, minutes: bigint): bigint {
  const deadline = blockTimestamp + minutes * 60n;
  if (deadline > UINT256_MAX) throw new OrderInputError("到期时间超出合约支持范围。");
  return deadline;
}
