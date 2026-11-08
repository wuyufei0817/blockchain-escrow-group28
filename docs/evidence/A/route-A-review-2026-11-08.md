# A路线整体检查记录 2026年11月8日

检查对象：用户保存的报告（20:43:46版本）、当前backend源码、公开链上记录、根README。报告原文件保持不变；本轮没有签名或发送链上交易。B路线和重入挑战不在本轮评审范围，不能从本轮结果推断它们完成与否。

## 已通过

- 六个基础REST接口已实现；独立代码复核未发现阻断性问题。事件列表按10块闭区间扫描OrderCreated，不逐单getOrder冒充列表。固定gas使真实状态错误仲裁可上链失败，422与503区分，未知结果不自动重播；共用签名账户nonce受同步保护。
- 实际重新运行test --rerun-tasks成功，22项测试零失败（17 API、4配置、1nonce）；test bootJar构建也成功。测试使用本地模拟RPC，另有真实链上验收。
- 独立重新查询12笔公开回执（10笔A Escrow相关、2笔SimpleStorage），hash、区块、status、gasUsed、From与已有记录一致。订单0当前Completed，订单1Refunded。链上Escrow运行时代码与本地重新编译产物完全一致，backend中的Escrow.sol与小组确认原件字节一致。
- 4A-1配置占位符、4A-2封装、4A-3 deploy/set/get及浏览器、4A-4完整Service/Controller、4A-6三GET、4A-7真实失败/成功/Refunded、4A-8浏览器From及Transfer均有对应材料。
- 4A-4 Service 1–295行、Controller全部代码完整。新报告第10–14页与上次逐页检查结果像素一致。其余A及共用表格页已视觉检查。
- 六道A解释已填写，与实际代码和回执一致。4A-4解释正确区分gasUsed25889与effectiveGasPrice0；不将空revertReason写成已解码文案。退款来源Escrow、外层From为BE的区分正确。
- 交易主表8行hash、链接与From均正确。当前后端main源码未发现形如64位十六进制私钥的明文字面量；这不是完整Git历史秘密审计。

## 必须收尾

1. 报告4A-5（4）approve图放错：第16页第二张图片与上方创建订单1回执是同一个嵌入PNG（同一rId/字节），其哈希为0x191cf616...e0492d7c，属于createOrder(1)。真正approve60的交易哈希应为0x26b5a208e24b0911f2dbb01b45ebb0edd38e5bc4f77bb8d3e84a45c47bd6a406。替换为docs/evidence/A/buyer-materials/evidence/4A-5-3-approve60-success-receipt.png，保留图注4A-5（4），不需要重发交易或重新截图。图注（4）目前被拆到第17页，替换后应与对应图同页。
2. 报告第3页4.2关键数据仍全空：仓库URL、A后端BE、A Escrow、代币地址须填写。两路线合并时应分别标A/B实例，不混用。
3. 根README仍称业务代码未导入且缺真实安装/启动/测试/load恢复步骤，应由共享README负责人整合后端已验收的运行说明。相关段落在README第5、62行；用户曾明确不需要backend/README，不应据此另建一个替代根README。
4. 共用报告信息：封面仍第X组/姓名为空、4.1分工及选型未填、4.6问题解决空白、AI使用说明仍是模板提示语。它们是最终报告收尾事项，不是要重新执行A业务。成员实际操作和贡献按事实填写；不修改评分人及教师评分区。

## 建议与边界

- 主表已满足8行要求；为完整记录两单，可再附创建订单1和支付订单1两笔补充交易，不要求新增截图。
- 确认收货回执已在报告中，Completed查询图也在；不再要求新增状态截图。可加“正常订单0确认收货（补充验收）”文字说明，避免与退款日志混淆，但不是新增验收操作。
- 事件时间线GET /orders/{id}/events、全部事件增量索引属于尚未完成的独立加分项，不能把基础OrderCreated列表当作该加分完成证据。
- report-route-A-answers.md第33、64行仍有旧时点“待仲裁”文字，应明确为历史快照或清理后再提交，避免与后文成功验收记录冲突。

报告SHA256：47bcf2c48a189695fd7984efcca088ccc63f7a7dc6a37d2dcceef50c242b538b
