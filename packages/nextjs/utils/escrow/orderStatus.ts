export const STATUS_LABELS: Readonly<Record<number, string>> = {
  0: "Created（待付款）",
  1: "Paid（已付款）",
  2: "Completed（已完成）",
  3: "Refunded（已退款）",
  4: "Disputed（争议中）",
} as const;
