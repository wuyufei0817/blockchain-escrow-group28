# 第28组大作业报告 A 路线文字草稿

本稿汇集苏佳豪负责部分的已核实数据与可填写答案，供后续汇入分组报告。按项目计划，小组必做路线为 B，完整 A 路线作为加分项；此处保留讲义的 4A 编号，方便组长对应。尚未完成的交易、截图和贡献比例分别标明待补，不将本地模拟测试当成真实链上验收。

## 已核实的关键数据

| 项目 | 实际值 |
|---|---|
| 组号 | 第28组 |
| 组长 | 武钰斐 |
| 组员 | 苏佳豪、杨燊杰 |
| 小组主仓库 | https://github.com/wuyufei0817/blockchain-escrow-group28 |
| 苏佳豪 Fork | https://github.com/GoForIt-keepgoing/blockchain-escrow-group28 |
| 后端分支 | feat/su-backend |
| 后端专用账户 BE | 0x3391215304d3F41892d973e70D18c5642068ae51 |
| RPC | https://rpc-testnet.potos.hk |
| chain ID | 60600 |
| gas price | 1000000 wei |
| gas limit | 6721975 |
| SimpleStorage 地址 | 0x3685e0FAbF6e3664C52a74771C3100E427405b47 |
| A 路线 Escrow 地址与部署块 | 0x793D08836c2e00Af0BdE963386f36e00acEe180f；2832142 |
| 小组代币地址 | 0x81777CEeA8CC59fa88ABcee9DC19970217Cf8637；符号 WYF202492451119；精度 18 |
| 班级、专业、各人学号及贡献比例 | 待按本人信息和小组实际完成记录核定 |

## A 路线角色与已执行交易

| 角色 | 姓名及公开地址 |
|---|---|
| 买家 | 武钰斐：0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| 卖家 | 杨燊杰：0x92b0f84D625437245c3296012A0784d10C560309 |
| 后端仲裁人 | 苏佳豪本人配置的专用 BE：0x3391215304d3F41892d973e70D18c5642068ae51 |

以上买卖家地址从实验三报告与组长原 Escrow 的订单 0 交叉核对；旧订单 buyer、seller、token 与此对应，组长 token owner 和原 Escrow 创建账户均为上述买家地址。本轮买家五笔交易的链上 From 均为武钰斐地址，已与她提供的材料交叉核对；卖家争议交易的实际记录见下节，后端仲裁的真实成功与失败回执及最终确认收货记录见后续章节。

| 操作 | 实际交易哈希 | 区块号 | status | gasUsed | 发起账户 |
|---|---|---|---|---|---|
| 部署 A_BASE Escrow | 0x31cccc6d5e12d955b00e2e48ce5ec173ea9b6f420bb568a5248b9cbe63db27e6 | 2832142 | 0x1 | 103312 | 0x3391215304d3F41892d973e70D18c5642068ae51 |

部署交易 to 为 null，创建的新地址为 0x793D08836c2e00Af0BdE963386f36e00acEe180f。链上运行时代码与本组 Solidity 0.8.20 / Paris 编译产物完全一致，构造参数确认为 BE 地址。交易浏览器：https://scan-testnet.potos.hk/tx/0x31cccc6d5e12d955b00e2e48ce5ec173ea9b6f420bb568a5248b9cbe63db27e6 。

此前买家 70 枚 WYF 是早期余额快照，不代表本轮验收后的余额。收到买家材料后，于 2026-11-07T23:32:34.467506+08:00 对五笔交易进行独立只读核验：回执均成功，From 均为本组买家；create 的参数和 OrderCreated 事件确认订单号为 0、1；approve 的 spender 为本次 A 路线 Escrow；两笔 deposit 均有买家向 Escrow 转入 30 枚 WYF 的 Transfer 及对应 OrderPaid 事件。

| 操作 | 实际交易哈希 | 区块号 | status | gasUsed |
|---|---|---|---|---|
| 创建正常订单 0 | 0x944e176605765a331256582d6099b52a0303e50884f2c3b4d2e9e7d1bca18b02 | 2835064 | 0x1 | 80548 |
| 创建争议路线订单 1 | 0x191cf6168604617bc7e0f0c95ed42a2360319698f996d91e7a6d3006e0492d7c | 2835118 | 0x1 | 80548 |
| 授权本次 Escrow 使用 60 枚 WYF | 0x26b5a208e24b0911f2dbb01b45ebb0edd38e5bc4f77bb8d3e84a45c47bd6a406 | 2835186 | 0x1 | 28423 |
| 支付订单 0：30 枚 WYF | 0x8f52d16a4f28f4931b46727a4915635a44db741cc38682c485399dbd0c54f57d | 2835211 | 0x1 | 70217 |
| 支付订单 1：30 枚 WYF | 0x4144b15dc3641391b7a051566189aa8fd971ac1c7c89b24ca45b85ea4b5f9c80 | 2835232 | 0x1 | 70217 |

核验快照：订单 0（A-NORMAL）和订单 1（A-DISPUTE）均为 Paid（1），每笔金额 30000000000000000000，即 30 枚 WYF；共同 deadline 为 1794070422，即北京时间 2026-11-08 00:53:42。链上 buyer、seller、token 与上述角色表一致。此时买家余额 0、卖家余额 30、Escrow 余额 60 枚 WYF，买家给本 Escrow 的剩余 allowance 为 0，符合两次支付已用完 60 枚授权的情况。

买家原始说明和 17 张原图已按原字节归档于 docs/evidence/A/buyer-materials/；公共链上原始交易、回执和核验快照见该目录的 chain-verification.json。准备阶段另外增发 50 枚的说明和参数图保留为辅助材料，但缺少交易哈希，尚未独立核验，不计入以上五笔业务交易。该买家材料核验时点尚未完成卖家 dispute、后端 resolve 或正常单 confirm；之后的卖家争议记录见下节，不能将早期 Paid 快照当作最新状态。


## 卖家发起争议的实际记录

收到用户提供的成功回执及 getOrder(1) 原图后，独立只读核验确认：本次 dispute(1) 的链上 From 为卖家地址 0x92b0f84D625437245c3296012A0784d10C560309，To 为本次 A 路线 Escrow。调用参数为订单 1，回执 status 为 0x1，并包含 OrderDisputed(1, seller) 事件。实际点击和签名操作人的身份不由链上地址推断。

| 操作 | 实际交易哈希 | 区块号 | status | gasUsed |
|---|---|---|---|---|
| 卖家地址发起 dispute(1) | 0x0882e460b0b76906dd347b7c1ffb3a4808b842cea5d7285d049668f50af55670 | 2841098 | 0x1 | 36389 |

2026-11-08T11:15:00.977342+08:00 的独立链上快照确认：订单 0 为 Paid（1），订单 1 为 Disputed（4），两笔金额均为 30 枚 WYF。dispute 只改变争议状态，此次未发生托管代币转出。两张原始截图已归档为 4A-5-seller-dispute-receipt.png、4A-5-seller-order1-disputed.png；原始交易、回执及核验记录见 docs/evidence/A/seller-dispute-verification.json。4A-5 的部署、买家创建及支付、卖家发起争议原始素材现已收齐，最终报告选图排版仍待整理。该时点后端三项GET与仲裁尚待执行；之后已完成，实际截图与回执见后文，不应将此历史快照当作当前待办。

## 解释 4A-1

截图 4A-1 中，本组将私钥配置为 `${WEB3J_PRIVATE_KEY}`，而没有把明文私钥写入文件；同一图中链 ID 为 60600，gas price 为 1000000 wei，gas limit 为 6721975。本组另建的后端账户为 0x3391215304d3F41892d973e70D18c5642068ae51，私钥由本人在本机通过隐藏输入注入运行进程。

讲义示例原本使用的 Hardhat 默认私钥是公开测试数据。把它用于测试网也不能获得私密性，其他人知道这个私钥后同样能以该账户签名，转走账户的测试币，或行使合约授予该地址的权限。因此，示例账户不能作为实际仲裁账户使用。

仲裁人私钥由服务器持有时，后端能在收到 HTTP 请求后直接签名，责任集中在服务的身份认证、操作授权、密钥保护、限流和审计上。任何能访问未受保护接口的人，都可能间接触发服务器动用签名权。私钥放在个人 MetaMask 中时，通常由持有人在钱包中逐笔确认签名，服务端不直接持有该私钥，但持有人仍需保护钱包和核对交易内容。两种方式的链上权限最终都由实际签名地址决定。

## 解释 4A-2

截图 4A-2 的 `Escrow.java` 中，Solidity 的 `Order memory` 返回值被映射为静态内部类 `Escrow.Order`，它继承 Web3j 的 `DynamicStruct`。类中共有七个字段：buyer、seller、token 对应 Java 的 String；amount、deadline、status 对应 BigInteger；description 对应 String。status 在 Solidity 中是枚举，经 ABI 编码为 uint8，生成类用 BigInteger 接收，后端再将 0 至 4 映射为 Created、Paid、Completed、Refunded、Disputed。

`ORDERCREATED_EVENT` 描述了 OrderCreated 事件的名称、各参数类型及 indexed 标记。事件参数为 id、buyer、seller、token、amount、deadline，其中前三项 indexed。查询日志时，`EventEncoder.encode` 根据事件名和参数类型计算签名主题 topic0，配合合约地址筛选 OrderCreated 日志；indexed 标记决定参数保存在 topics 还是 data 中，不参与事件签名的类型字符串。

截图中的 `getOrderCreatedEventFromLog` 先用该事件定义解码日志，再从三个 indexed 值中取得 id、buyer、seller，从非 indexed 值中取得 token、amount、deadline，组成 `OrderCreatedEventResponse`。因此，事件常量同时为日志过滤和参数解码提供依据。

## 解释 4A-3

本段对应基础验收截图版本（提交fc3c58b）。后续事件时间线加分版本已增加共享索引和持久化缓存，其RPC计数及说明见docs/route-A-timeline.md；原截图的历史版本说明保留，不能把两种实现的计数混用。

截图 4A-4 中，本组订单列表方法使用 OrderCreated 事件作为订单创建记录，并按区块号、交易序号、日志序号排序，再以交易哈希和日志序号去重。本组没有在列表中逐笔读取全部订单详情，因为列表只需要 id、buyer、seller、amount、blockNumber、transactionHash 等创建信息；当前状态和 description 仍通过单笔订单详情查询取得。

如果使用逐单方案，先读取一次 orderCount，再对 N 个订单分别调用 getOrder，则通常需要 1 + N 次 RPC；如果也加入本组相同的链 ID 校验，则为 2 + N 次。题设有 1 万笔订单时，两种计数分别是 10001 次和 10002 次，这不是本组实际已有订单数量。

本组当前实现先做一次 eth_chainId 校验、一次 eth_blockNumber 查询，再从部署块到当前块分段调用 eth_getLogs。截图中 start 每次增加 BigInteger.TEN，end 取 start + 9，因此每段最多覆盖 10 个区块。设区块跨度 H = 当前块号 - 部署块号 + 1，则本组这一版列表约需 2 + ceil(H / 10) 次 RPC；事件解析、排序和去重在本地完成，不增加 RPC。假设 H 为 1000，则约为 102 次；若 H 为 100000，则约为 10002 次，因此不能断言事件方式在任何跨度下都比逐单查询更少。

目前此列表版本每次都会重新扫描指定区间，尚未完成增量缓存。若另查 M 个详情，本组现有详情接口每次还会执行链校验、orderCount 和 getOrder，约再增加 3M 次 RPC。后续做缓存与事件时间线时，应依据最终实现和真实部署区间更新上述计数。

## 解释 4A-4

本组通过后端请求 resolve(0, true)，实际返回 HTTP 422，code 为 TRANSACTION_REVERTED，message 为 Transaction was mined and reverted。后端在读取到失败的链上回执后，抛出本组 EscrowApiException，并由异常处理器将回执信息映射为 JSON 响应；不是因 HTTP 无法连接而失败。

实际交易哈希为 0x63f9f67706ab2a51fa2a157288cde58879707e7fba77d430d083d718ec1f304e，所在区块 2842225，回执 status 为 0x0，gasUsed 为 25889，From 为 BE 账户 0x3391215304d3F41892d973e70D18c5642068ae51，To 为本组 A 路线 Escrow。交易已被打包上链，但合约执行回滚，没有产生事件日志。独立查询确认订单 0 仍为 Paid，订单 1 仍为 Disputed。结合本组合约 resolve 的 inStatus(id, Status.Disputed) 校验可判断正常单 0 的状态不符合仲裁要求。本次响应中的 revertReason 实际为空字符串，不能将源码中的错误文案写成接口已成功解码返回的内容。

失败交易仍有执行 gas 消耗，回滚不会把已执行的 gas 用量清零。此处 gasUsed 为 25889；交易设置的 gasPrice 为 1000000 wei，但 POTOS 回执的 effectiveGasPrice 实际为 0，因此按该回执字段计算的执行手续费为 0，不能把 gasUsed 乘以设置 gasPrice 的数值直接写成已实际扣除的 POT。链上发送方及通常的费用承担方是 BE，调用 HTTP 接口的人不会因此自动支付链上手续费。

如果有人反复调用开放的 resolve 接口，即使订单状态不合法，也可能让服务器反复签名广播失败交易，消耗 RPC、签名、回执轮询和 gas 配额；在收取有效手续费的网络还会消耗 BE 的原生币余额。因此需要对接口进行身份认证、业务授权、限流、并发及费用控制和审计，不能仅依赖合约最终回滚。

## 解释 4A-5

本组实际退款仲裁交易为 0xa161d794996043964f33938e2d813ca238f2a996c3ece95c8e67134e3899f813，区块号 2842245，status 为 0x1，gasUsed 为 62917。交易 From 是后端 BE 账户 0x3391215304d3F41892d973e70D18c5642068ae51，To 是 Escrow 合约 0x793D08836c2e00Af0BdE963386f36e00acEe180f。输入参数确认调用 resolve(1,false)，由仲裁人判买家胜诉。

同一交易的 WYF 代币 Transfer 日志确认：30000000000000000000 个最小单位，即 30 枚 WYF，从 Escrow 转给买家 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6。它不是从 BE 的代币余额转出，交易外层 From 与代币日志中的转出方含义不同。OrderResolved 事件确认 id=1、sellerWins=false；独立 getOrder 查询确认订单 1 为 Refunded（3），订单 0 仍为 Paid（1）。

本组 Controller 仅转交 HTTP 参数；EscrowService.java 第 153 行通过生成封装的 encodeFunctionCall 组装 resolve 调用数据，第 161 行调用 transactions.sendTransaction 发起签名发送。该对象是 ChainCheckedTransactionManager，它持有由 Web3jConfig 注入的 BE Credentials 和链 ID 60600；ChainCheckedTransactionManager.java 第 60 行在校验链 ID 后调用 super.signAndSend(transaction)，实际签名与广播委托给 Web3j 依赖的 RawTransactionManager。因此签名权限来自服务端配置的 BE 私钥，HTTP 调用者地址不会成为链上发送人。上述行号对应本次修复加载校验后的源文件。

## 解释 4A-6

本组当前本地接口位于 /api/escrow，控制器没有认证配置。HTTP 调用者并不因此成为链上的 msg.sender；实际交易仍由后端配置的 BE 账户签名。能访问该接口的人若直接请求 resolve，可能让服务器付出 gas 并动用其仲裁权。

真实系统至少需要两层保护。第一层是身份认证与业务授权，例如登录令牌或受控服务凭据，并核对当前用户是否有权处理这个争议订单，不能只相信调用者传入的地址。第二层是限流和费用控制，配合审计记录、异常告警与敏感裁决的人工复核，限制恶意请求消耗服务器的测试币或正式资金。密钥还应由受控的密钥管理或签名设施保护，而非写入仓库。

如果仲裁私钥泄露，攻击者可以直接绕过 HTTP 层，以仲裁地址向合约提交交易，但不能因此任意提走所有状态下的全部托管代币。本组确认的 Escrow.sol 中，resolve 同时检查 onlyArbiter 和 Disputed 状态；资金只按订单金额转给该订单固定的 buyer 或 seller，合约没有让仲裁人指定任意收款地址的统一提现函数。因此，攻击者可恶意裁决符合条件的争议订单；是否能实际收到这些币还取决于其能否控制相应收款地址，不能把这描述成任意提现权限。

## 已完成的 SimpleStorage 交易补充记录

以下是示例验收记录，不可混填到模板的八条 Escrow 业务交易中。

| 操作 | 实际交易哈希 | 区块号 | status | gasUsed |
|---|---|---|---|---|
| 部署 SimpleStorage | 0x28dce258f630b094a83cfe3970012da767ebd2e260cd994d3d13ededdb990360 | 2831833 | 0x1 | 4699 |
| set(100) | 0x49f336782dda690729d7a024d3b70b95031a5f0e1df56763a0a710d90d7aacf4 | 2831850 | 0x1 | 27267 |

两笔交易的实际 From 均为上述 BE 地址。get() 是只读调用，没有独立的上链交易哈希；用户终端与独立 eth_call 均读回 100。截图 4A-3 中的 set 响应为 status 0x1、blockNumber 2831850、gasUsed 27267，浏览器显示同一交易成功。

## 问题与解决及 AI 使用说明草稿

我们使用 Codex 辅助整理旧示例、移除示例私钥、配置链 ID、生成合约封装、实现后端接口、编写本地测试，以及核对公开链上回执。私钥由本人通过本机 PowerShell 隐藏输入配置，没有发送到聊天或写入仓库。真实部署和 set(100) 均由本人在本机终端发起，Codex 对公共 RPC 的调用用于只读核验。

实际遇到的问题包括：安装 JDK 21 后终端仍使用旧的 JDK 17，之后修正 JAVA_HOME、Path 并刷新运行环境；Gradle 在 Windows 中文路径下的测试进程参数编码不匹配，通过兼容编码设置并保持 Java 源码 UTF-8 解决；新增代码最初使用了与 Web3j 4.14 不匹配的 gas provider 调用方式，编译报错后核对本机库 API 并修正。重新加载真实合约时，还遇到 POTOS 节点对部署前历史区块也返回当前合约代码的问题，使原先的部署块校验误报 400；之后改用指定区块中的创建交易及成功回执验证部署块，增加回归测试并用现有合约只读核验加载成功。代码复核还发现两个示例共享签名账户时的 nonce 并发问题，以及 RPC 返回不一致哈希时丢失本地哈希的问题；用本地测试复现后修复。加入时间线加分接口后，当前完整本地测试为 33 项通过，但这些模拟 RPC 测试不能代替真实链上业务验收。

Codex 先前遗漏了部分截图提醒，并过早把 4A-3 素材标齐。核对讲义后确认 4A-3 还需最初的 deploy 响应，4A-4 要提供完整代码材料。我们保留已有原始截图，按讲义补齐，避免把尚未收集的材料写为已完成。

## 待补项目

- 4A-3：deploy、set、get 三条完整终端响应及对应 set 交易浏览器素材已收齐，最终报告选图与排版待完成。
- 4A-4：已只读检查用户保存的报告第10–14页。Service共10张原图，覆盖1–295行；Controller共2张原图，覆盖全部代码及6个路由。行号连续，包含最新加载校验修复，排版未见缺段或越界；无需继续补截。完整原图和检查记录已归档，Word原件未改动。
- A 路线 Escrow 部署地址、部署块及真实部署回执已填写；4A-5 后端部署响应图已收集，买家两次 create、一次 approve、两次 deposit 的交易与原始材料已核验归档，卖家 dispute(1) 及两张原始截图也已核验归档；4A-6 三张 GET 原始截图已收集核对；4A-7 的真实失败回执已核验，解释 4A-4 已按实填写；成功退款仲裁回执与链上 Transfer/OrderResolved 事件已核验，解释 4A-5 已按实填写；4A-7 的 Refunded 查询原图也已收集核对，三张原始材料现已齐全；4A-8交易详情与Transfer日志两张原图也已收集核对，显示BE为外层交易发送人、Escrow向买家退回30枚WYF；最终报告由用户自行放图和填写。
- 小组必做 B 路线和其余加分项材料由实际完成人提供并核对。
- 最终实际分工、班级专业学号与贡献比例，由组长按真实记录汇总后填写。


## 4A-8 浏览器原图核对

交易详情图显示实际退款交易成功，区块2842245、gasUsed62917，From为BE、调用目标为本次Escrow，Tokens transferred为Escrow向买家转30枚WYF。Transfer日志图显示from为Escrow、to为买家、value为30000000000000000000。原图均已按原字节归档。这里只确认素材收集，Word报告仍由用户自行插图和填写；该截图收集时正常订单0的买家confirm尚未执行；后续确认收货交易见下节。


## 正常订单0确认收货的实际记录

买家手动调用confirm(0)，交易哈希为 0x96cbe681312bf74aadf80a7606f3ca93e2c5fbb564452eb88b853dab9b463d73，区块号2843225，status为0x1，gasUsed为62549。链上From为买家0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6，调用目标为本次A路线Escrow。Transfer事件确认30枚WYF从Escrow转给卖家0x92b0f84D625437245c3296012A0784d10C560309，OrderCompleted事件确认订单号为0。

2026-11-08T15:31:32.745828+08:00 的只读核验快照：订单0为Completed（2），订单1为Refunded（3）；买家WYF余额30、卖家余额60、Escrow余额0。卖家此前已有30枚，本次新增收到30枚，不能把余额60枚全部写成本次订单付款。

确认收货成功回执原图已收集。按与用户确认的原要求，补充验收只放成功回执；订单0最终状态以文字和已核验链上记录确认即可，不再要求新增GET或Remix状态截图。用户后续提供的完整Remix查询图显示status为2，可保留为备用材料，不要求插入。Word报告由用户自行插图和填写。A路线两条业务流程已完成链上核验，完整Service/Controller材料、最终报告排版与小组其余任务仍按实际情况完成，不能据此声称整个大作业完成。


## 报告4.3交易记录表

| 操作 | 交易哈希 | 区块浏览器链接 | 发起账户 |
|---|---|---|---|
| 部署 Escrow | 0x31cccc6d5e12d955b00e2e48ce5ec173ea9b6f420bb568a5248b9cbe63db27e6 | https://scan-testnet.potos.hk/tx/0x31cccc6d5e12d955b00e2e48ce5ec173ea9b6f420bb568a5248b9cbe63db27e6 | 0x3391215304d3F41892d973e70D18c5642068ae51 |
| createOrder 订单 0 | 0x944e176605765a331256582d6099b52a0303e50884f2c3b4d2e9e7d1bca18b02 | https://scan-testnet.potos.hk/tx/0x944e176605765a331256582d6099b52a0303e50884f2c3b4d2e9e7d1bca18b02 | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| approve | 0x26b5a208e24b0911f2dbb01b45ebb0edd38e5bc4f77bb8d3e84a45c47bd6a406 | https://scan-testnet.potos.hk/tx/0x26b5a208e24b0911f2dbb01b45ebb0edd38e5bc4f77bb8d3e84a45c47bd6a406 | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| deposit 订单 0 | 0x8f52d16a4f28f4931b46727a4915635a44db741cc38682c485399dbd0c54f57d | https://scan-testnet.potos.hk/tx/0x8f52d16a4f28f4931b46727a4915635a44db741cc38682c485399dbd0c54f57d | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| dispute 订单 1 | 0x0882e460b0b76906dd347b7c1ffb3a4808b842cea5d7285d049668f50af55670 | https://scan-testnet.potos.hk/tx/0x0882e460b0b76906dd347b7c1ffb3a4808b842cea5d7285d049668f50af55670 | 0x92b0f84D625437245c3296012A0784d10C560309 |
| resolve 订单 1 | 0xa161d794996043964f33938e2d813ca238f2a996c3ece95c8e67134e3899f813 | https://scan-testnet.potos.hk/tx/0xa161d794996043964f33938e2d813ca238f2a996c3ece95c8e67134e3899f813 | 0x3391215304d3F41892d973e70D18c5642068ae51 |
| confirm 订单 0 | 0x96cbe681312bf74aadf80a7606f3ca93e2c5fbb564452eb88b853dab9b463d73 | https://scan-testnet.potos.hk/tx/0x96cbe681312bf74aadf80a7606f3ca93e2c5fbb564452eb88b853dab9b463d73 | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| resolve 失败（路线 A） | 0x63f9f67706ab2a51fa2a157288cde58879707e7fba77d430d083d718ec1f304e | https://scan-testnet.potos.hk/tx/0x63f9f67706ab2a51fa2a157288cde58879707e7fba77d430d083d718ec1f304e | 0x3391215304d3F41892d973e70D18c5642068ae51 |

补充交易（上述八行之外，保留第二笔订单的创建与支付）：

| 操作 | 交易哈希 | 区块浏览器链接 | 发起账户 |
|---|---|---|---|
| createOrder 订单 1 | 0x191cf6168604617bc7e0f0c95ed42a2360319698f996d91e7a6d3006e0492d7c | https://scan-testnet.potos.hk/tx/0x191cf6168604617bc7e0f0c95ed42a2360319698f996d91e7a6d3006e0492d7c | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| deposit 订单 1 | 0x4144b15dc3641391b7a051566189aa8fd971ac1c7c89b24ca45b85ea4b5f9c80 | https://scan-testnet.potos.hk/tx/0x4144b15dc3641391b7a051566189aa8fd971ac1c7c89b24ca45b85ea4b5f9c80 | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
