# A路线时间线加分项验证

33项本地测试实际执行通过：28项API及索引、4项配置、1项nonce。新增接口先观察404回归失败；随后7项功能测试红转绿，补充4项复核边界测试。独立代码复核未发现阻断性问题。

只读实际验证时间：2026-11-08T22:17:34.015889+08:00。临时18080端口使用随机测试签名账户，RPC代理仅允许读方法，未读取用户私钥，未停止8080服务，未提交任何链上交易。临时进程已自动关闭。

首次全量扫描1441段，每段最多10个区块，耗时611.72秒。扫描期间链持续增长，第二个订单查询仅新增9段；同一索引派生的订单列表返回2笔；重启后恢复缓存，仅新增1段。初次慢不代表后续会重复全量。

## 正常订单0

| eventName | status | blockNumber | transactionHash |
|---|---|---|---|
| OrderCreated | Created | 2835064 | 0x944e176605765a331256582d6099b52a0303e50884f2c3b4d2e9e7d1bca18b02 |
| OrderPaid | Paid | 2835211 | 0x8f52d16a4f28f4931b46727a4915635a44db741cc38682c485399dbd0c54f57d |
| OrderCompleted | Completed | 2843225 | 0x96cbe681312bf74aadf80a7606f3ca93e2c5fbb564452eb88b853dab9b463d73 |

## 争议订单1

| eventName | status | blockNumber | transactionHash |
|---|---|---|---|
| OrderCreated | Created | 2835118 | 0x191cf6168604617bc7e0f0c95ed42a2360319698f996d91e7a6d3006e0492d7c |
| OrderPaid | Paid | 2835232 | 0x4144b15dc3641391b7a051566189aa8fd971ac1c7c89b24ca45b85ea4b5f9c80 |
| OrderDisputed | Disputed | 2841098 | 0x0882e460b0b76906dd347b7c1ffb3a4808b842cea5d7285d049668f50af55670 |
| OrderResolved | Refunded | 2842245 | 0xa161d794996043964f33938e2d813ca238f2a996c3ece95c8e67134e3899f813 |

全部7个事件哈希与先前独立核验的创建/支付/争议/仲裁/确认收货回执逐一一致，金额30*10^18、token、deadline、卖家争议地址一致。按区块/交易/日志序号排序，无重复。OrderResolved(false)状态为Refunded，不伪造OrderRefunded事件。

下一步：用户重启原8080后端、load同一地址，从新接口采集BONUS-TIMELINE-A两张原始截图。报告4.5单列此加分小节，既有4A截图保留为基础验收版本；RPC计数按docs/route-A-timeline.md说明。Word报告未改动，未自动commit/push。
