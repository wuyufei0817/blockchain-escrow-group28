# 第28组：区块链托管交易应用

组长：武钰斐。核心开发：武钰斐、苏佳豪。截止日期：2026年11月8日；内部交付目标：11月7日晚。

**当前已核验：A路线后端已导入，六个基础接口和测试网正常收货、争议退款、错误状态仲裁流程已完成验收；33项本地测试通过。其余路线和加分项以各自代码、测试及真实交易记录为准。**

完整任务、评分证据、接口约定、测试场景和16项任务详见 [执行计划](docs/PROJECT_PLAN.md)。执行计划记载的是安排，完成状态以实际PR、测试和链上证据为准。

## 交付范围

- 必做路线B：Next.js页面实现订单列表、创建、approve后deposit、确认收货、争议、仲裁和角色提示。
- 完整路线A：Spring Boot + Web3j，六个REST接口、事件重建订单列表、服务器账户仲裁。
- 计划覆盖五项加分：完整另一条路线、重入复现与修复、订单时间线、卖家发货状态、两人一致的多签仲裁。
- README运行说明、环境变量说明、真实交易台账、全部规定截图、解释题、实际分工和AI使用说明。

评分文件07列出的五项加分名义合计21分，累计最多计20分，总评封顶100分；教程04写最多10分，提交前确认教师最终口径。两份材料的差异不改变本项目覆盖全部五项加分的目标。

## 计划分工

| 成员 | 负责范围 | 验收产出 |
|---|---|---|
| 武钰斐 | 组长、前端页面、订单交互与角色、时间线、EscrowPlus发货和双人仲裁、集成 | B路线和扩展截图/测试、报告整合、最终提交 |
| 苏佳豪 | backend全部、Java合约封装、六接口与事件查询、漏洞金库/攻击/修复及测试 | A路线与重入证据，交叉核对交易及README |
| 杨燊杰 | 少量真实卖家角色操作、截图核对与验收记录 | 本人操作记录、截图编号和实际审阅记录 |

这是待执行安排；报告中的完成分工须依据本人实际操作和提交记录填写。

## 11月1日至8日安排

| 日期 | 主要工作 | 当天出口 |
|---|---|---|
| 11月1日 | 清理导入旧工程，建立可构建代码基线；fork与接口/地址约定 | 两人环境可用、首个协作PR、角色会话预约 |
| 11月2—3日 | 武做B前端；苏做A后端，双方本地测试 | 两路线基础代码完成，事件分段与错误路径通过 |
| 11月4日 | 两路线测试网完整流程和三角色验收 | 4A/4B证据、真实哈希、baseline-complete |
| 11月5—6日 | 武做发货/多签页面合约；苏做重入与修复，完成时间线证据 | 五项加分均有独立成功与失败证据 |
| 11月7日 | 干净环境复现、报告、AI说明、台账、答辩演练 | 19:00冻结功能，21:00内部交付 |
| 11月8日 | 最后核对与按教师时刻提交 | 保存提交版本及记录，预留缓冲 |

## 苏佳豪如何加入

1. 在本仓库右上角点击 **Fork**，创建到本人账号；无需先成为主仓库协作者。
2. clone自己的fork。若此时只有规划文档，先等待/同步11月1日的代码基线，再开始开发。
3. 在自己的clone中配置上游并创建功能分支：

```bash
git remote add upstream https://github.com/wuyufei0817/blockchain-escrow-group28.git
git fetch upstream
git switch -c feat/su-backend upstream/main
```

4. 在本人分支开发，push到自己的`origin`，发PR到本仓库的`main`。武钰斐审查、验收、合并。
5. 每日开始前fetch上游，在干净工作区将`upstream/main`合入功能分支；解决冲突后再开发。PR说明修改范围、运行过的检查、截图/回执位置。

武负责前端与扩展合约，苏负责`backend/`和重入合约；共享配置、锁文件及生成ABI由武统一整合。仓库公开可读，向主仓库贡献仍通过PR。

参考：[GitHub官方fork说明](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/fork-a-repo)。

## 工程起点与运行信息

沿用已有课程工程：前端计划沿用Yarn 3.2.3、Node >=20.18.3；现有Java后端位于`backend/`，使用JDK 21、Spring Boot 3.2.3、Web3j 4.14.0、Gradle Wrapper 8.13和Solidity 0.8.20 / Paris。A后端的Solidity编译不使用npm导入，无需为此单独安装Node。

### A路线构建与测试

以下PowerShell命令从仓库根目录执行。先安装JDK 21，确认`java -version`和`javac -version`均使用21，且`JAVA_HOME`指向JDK根目录。项目自带Gradle Wrapper，无需单独安装Gradle。

```powershell
Set-Location .\backend
.\gradlew.bat generateContractWrappers test bootJar
Set-Location ..
```

首次构建需要联网下载Gradle、Solidity编译器及依赖；缓存齐全后可加`--offline`。Windows中文路径所需的编码配置已保存在`backend/gradle.properties`中。本机已验证编译、封装生成及打包成功，33项测试实际执行通过；尚不能据此替代组员在全新clone中的复现检查。

### A路线环境变量与启动

唯一必须由使用者提供的私密配置是`WEB3J_PRIVATE_KEY`，值为其本人有权使用的后端测试账户私钥。该账户会成为新部署Escrow的仲裁人。不要将私钥写进配置文件、聊天、截图或Git。

在仓库根目录的PowerShell中，以隐藏输入设置当前进程的环境变量，然后在同一终端启动：

```powershell
$beSecret = Read-Host "粘贴本人BE测试账户私钥（隐藏输入）" -AsSecureString
$env:WEB3J_PRIVATE_KEY = [System.Net.NetworkCredential]::new("BE", $beSecret).Password
$beSecret.Dispose()
Remove-Variable beSecret
java -jar .\backend\build\libs\spring-boot-web3j-1.0-SNAPSHOT.jar
```

出现`Tomcat started on port 8080`和`Started Application`后，保持终端运行；接口命令在另一个终端执行。关闭该PowerShell会话后，环境变量需要重新设置。程序启动时只记录公开签名地址，校验失败不会退回示例私钥。

配置文件`backend/src/main/resources/application.properties`中的公开参数为：

| 参数 | 本组配置 |
|---|---|
| HTTP端口 | 8080 |
| RPC | https://rpc-testnet.potos.hk |
| chain ID | 60600 |
| gas price | 1000000 wei |
| gas limit | 6721975 |

### A路线加载现有合约与查询

后端重启后需要重新加载已有合约，上下文不持久化。以下`load`仅连接现有实例并核对部署区块，不会重新部署，也不会提交链上交易：

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/escrow/load?address=0x793D08836c2e00Af0BdE963386f36e00acEe180f&fromBlock=2832142" | ConvertTo-Json
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/escrow/address" | ConvertTo-Json
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/escrow/orders/0" | ConvertTo-Json
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/escrow/orders/1" | ConvertTo-Json
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/escrow/orders" | ConvertTo-Json -Depth 6
```

验收完成后的状态是订单0 `Completed`、订单1 `Refunded`；报告4A-6的`Paid`/`Disputed`是仲裁前的真实快照。列表从部署块起按最多10个区块一段扫描订单事件，并从共享事件索引派生创建列表；首次查询较慢，后续增量续扫，缓存落盘支持重启恢复。POTOS历史`eth_getCode`返回当前代码，因此加载时改用创建交易及成功回执核对真实部署区块。

| 方法 | 路径 | 参数及用途 |
|---|---|---|
| POST | `/api/escrow/deploy` | 部署新实例，仲裁人为当前BE；会发送交易 |
| POST | `/api/escrow/load` | `address`、`fromBlock`，加载已部署实例 |
| GET | `/api/escrow/address` | 查询已加载地址、arbiter、fromBlock、chainId |
| GET | `/api/escrow/orders/{id}` | 查询单笔订单，金额、时间、ID用字符串，status为文字 |
| GET | `/api/escrow/orders` | 从创建事件重建订单列表 |
| GET | `/api/escrow/orders/{id}/events` | 按区块、交易及日志序号返回该订单全部适用事件，使用共享增量索引 |
| POST | `/api/escrow/orders/{id}/resolve` | `sellerWins=true`支付卖家，`false`退款买家；会发送交易，仅链上仲裁人可成功裁决争议单 |

仲裁成功返回交易哈希、区块、status、gasUsed；已上链回滚返回HTTP 422及失败回执；未知提交或确认结果返回503，不自动重播。错误参数400、未加载409、订单不存在404。`load`返回的交易字段为null是正常的，因为加载没有新交易。本组的两条业务流程已经结束，重复写操作不能用于重现原有交易哈希。

### A路线地址与证据

| 角色或实例 | 公开地址 |
|---|---|
| BE / A仲裁人 | `0x3391215304d3F41892d973e70D18c5642068ae51` |
| 买家 | `0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6` |
| 卖家 | `0x92b0f84D625437245c3296012A0784d10C560309` |
| A Escrow（部署块2832142） | `0x793D08836c2e00Af0BdE963386f36e00acEe180f` |
| WYF代币（18位精度） | `0x81777CEeA8CC59fa88ABcee9DC19970217Cf8637` |

任意使用者均可查询公开合约；若需要在已有实例上执行仲裁，签名账户必须与该实例的arbiter一致。新测试应由使用者部署自己的实例并记录真实地址及回执。

A路线原始截图及公开回执记录见[证据目录](docs/evidence/A/)，说明题材料见[报告草稿](docs/report-route-A-answers.md)。[A路线整体检查记录](docs/evidence/A/route-A-review-2026-11-08.md)记录检查时发现的报告错图及共享文档待办；后续修正以最新提交为准。订单时间线实现及独立加分留证步骤见[时间线说明](docs/route-A-timeline.md)；真实只读查询记录见证据目录，报告截图由使用者采集。

### A路线真实交易台账

以下八项按报告主表排列；链接中的完整哈希和区块均来自真实回执，发起账户的完整地址见上表。

| 操作 | 完整交易哈希及浏览器链接 | 区块 | status | 发起账户 |
|---|---|---|---|---|
| 部署 Escrow | [0x31cccc6d5e12d955b00e2e48ce5ec173ea9b6f420bb568a5248b9cbe63db27e6](https://scan-testnet.potos.hk/tx/0x31cccc6d5e12d955b00e2e48ce5ec173ea9b6f420bb568a5248b9cbe63db27e6) | 2832142 | 0x1 | BE |
| createOrder 订单0 | [0x944e176605765a331256582d6099b52a0303e50884f2c3b4d2e9e7d1bca18b02](https://scan-testnet.potos.hk/tx/0x944e176605765a331256582d6099b52a0303e50884f2c3b4d2e9e7d1bca18b02) | 2835064 | 0x1 | 买家 |
| approve 60枚WYF | [0x26b5a208e24b0911f2dbb01b45ebb0edd38e5bc4f77bb8d3e84a45c47bd6a406](https://scan-testnet.potos.hk/tx/0x26b5a208e24b0911f2dbb01b45ebb0edd38e5bc4f77bb8d3e84a45c47bd6a406) | 2835186 | 0x1 | 买家 |
| deposit 订单0 | [0x8f52d16a4f28f4931b46727a4915635a44db741cc38682c485399dbd0c54f57d](https://scan-testnet.potos.hk/tx/0x8f52d16a4f28f4931b46727a4915635a44db741cc38682c485399dbd0c54f57d) | 2835211 | 0x1 | 买家 |
| dispute 订单1 | [0x0882e460b0b76906dd347b7c1ffb3a4808b842cea5d7285d049668f50af55670](https://scan-testnet.potos.hk/tx/0x0882e460b0b76906dd347b7c1ffb3a4808b842cea5d7285d049668f50af55670) | 2841098 | 0x1 | 卖家 |
| resolve 订单1，退款 | [0xa161d794996043964f33938e2d813ca238f2a996c3ece95c8e67134e3899f813](https://scan-testnet.potos.hk/tx/0xa161d794996043964f33938e2d813ca238f2a996c3ece95c8e67134e3899f813) | 2842245 | 0x1 | BE |
| confirm 订单0 | [0x96cbe681312bf74aadf80a7606f3ca93e2c5fbb564452eb88b853dab9b463d73](https://scan-testnet.potos.hk/tx/0x96cbe681312bf74aadf80a7606f3ca93e2c5fbb564452eb88b853dab9b463d73) | 2843225 | 0x1 | 买家 |
| resolve 订单0，预期失败 | [0x63f9f67706ab2a51fa2a157288cde58879707e7fba77d430d083d718ec1f304e](https://scan-testnet.potos.hk/tx/0x63f9f67706ab2a51fa2a157288cde58879707e7fba77d430d083d718ec1f304e) | 2842225 | 0x0 | BE |

第二笔订单的补充记录：

| 操作 | 完整交易哈希及浏览器链接 | 区块 | status | 发起账户 |
|---|---|---|---|---|
| createOrder 订单1 | [0x191cf6168604617bc7e0f0c95ed42a2360319698f996d91e7a6d3006e0492d7c](https://scan-testnet.potos.hk/tx/0x191cf6168604617bc7e0f0c95ed42a2360319698f996d91e7a6d3006e0492d7c) | 2835118 | 0x1 | 买家 |
| deposit 订单1 | [0x4144b15dc3641391b7a051566189aa8fd971ac1c7c89b24ca45b85ea4b5f9c80](https://scan-testnet.potos.hk/tx/0x4144b15dc3641391b7a051566189aa8fd971ac1c7c89b24ca45b85ea4b5f9c80) | 2835232 | 0x1 | 买家 |

失败仲裁实际gasUsed为25889，成功退款为62917，确认收货为62549。失败回执status为0x0；它已上链但未改变业务状态。POTOS失败回执的effectiveGasPrice为0，执行gas用量与实际扣款金额须区分。

环境变量约定：A后端使用本机`WEB3J_PRIVATE_KEY`；前端连接用户钱包，前端公开变量只放链/RPC等公开配置。`.env`及真实私钥不提交，连示例工程自带的公开测试私钥也要移除。PLUS的两个仲裁钱包分别由两名成员持有，不复用A后端账户。

## 证据与完成标准

A路线已有证据位于`docs/evidence/A/`，真实交易台账见本页；其他路线及加分证据按`B/`、`C/`、`bonuses/`分别整合，总台账`docs/transactions.md`仍由组长汇总。每项写清路线、合约、真实订单ID、操作人/角色、完整哈希、区块、状态及金额，不混用不同部署实例的数据。

原始截图保持可核对。A需4A-1至4A-8和6道解释，B需4B-1至4B-6和5道解释，重入需4C-1至4C-3；五项加分各附独立说明和证据。报告末尾必须有真实的AI使用说明，记录工具、用途、实际问题及发现/修正过程。

按日更新进展；测试、截图、链上回执尚未取得时，不将对应任务标为完成。


## 重入挑战源码归档

已将本次实际使用的三份合约原样归档至[`contracts/reentrancy/`](contracts/reentrancy/)，并以Solidity 0.8.20 / Paris验证编译成功。两笔既有攻击交易已只读核验，原始截图与公开回执位于[`docs/evidence/C/`](docs/evidence/C/)。源码行为、地址、交易及验证范围见[重入挑战说明](docs/reentrancy.md)。本次未新增本地EVM回归测试，也未重新部署或执行攻击。
