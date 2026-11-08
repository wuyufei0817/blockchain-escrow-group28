# 路线 B 验收材料

以下 PNG 直接取自本组报告中已保存的原始截图，未修改内容、拼接或补造钱包回执。正常订单实际编号为 4，争议订单实际编号为 5。

| 任务书编号 | 文件 | 说明及操作成员 |
|---|---|---|
| 4B-1 | `4B-1-page-01.png`～`4B-1-page-03.png` | 武钰斐整理，连续覆盖 escrow/page.tsx；业务实现拆分到组件、Hook 和辅助文件，路径见主 README |
| 4B-2 | `4B-2-created-list.png`、`4B-2-order5-created.png`、`4B-2-approve-wallet.png`、`4B-2-deposit-wallet.png` | 武钰斐，买家；展示订单 #5 Created 及 30 WYF 授权、付款弹窗 |
| 4B-3 | `4B-3-buyer-account.png`、`4B-3-buyer-actions.png`、`4B-3-seller-account.png`、`4B-3-seller-actions.png` | 武钰斐为买家，杨燊杰为卖家；同一 Paid 订单的按钮对比 |
| 4B-4 | `4B-4-disputed-list.png`、`4B-4-arbiter-account.png`、`4B-4-arbiter-actions.png` | 杨燊杰卖家发起争议后，苏佳豪仲裁人账户显示两个裁决按钮 |
| 4B-5 | `4B-5-resolve-receipt.png`、`4B-5-order5-refunded.png`、`4B-5-confirm-receipt.png`、`4B-5-confirm-explorer.png`、`4B-5-final-orders.png` | 苏佳豪退给买家，随后武钰斐确认正常订单；最终列表为 #4 Completed、#5 Refunded |
| 4B-6 | `4B-6-resolve-details.png`、`4B-6-transfer-log.png`、`4B-6-resolve-log.png` | 苏佳豪为 resolve 的 From；退款 30 WYF；Escrow 日志的订单 ID 为 5 |

4B-5 的中间截图保留了 #4 尚为 Paid 的时点，最后一张才是两笔订单均结束后的状态。交易记录中的完整哈希与浏览器链接见 [交易表](../../transactions.md)。

三名成员使用各自钱包参与验收；卖家和仲裁人在武钰斐电脑前由本人操作，不将同一台电脑操作描述成三个独立部署环境。
