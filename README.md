# 第28组：区块链托管交易应用

本项目包含两条托管交易应用路线：B路线使用Scaffold-ETH 2、Next.js与MetaMask提供订单页面，A路线使用Spring Boot与Web3j提供后端接口。双方在POTOS Testnet分别完成正常收货和争议退款验收；A路线另提供订单事件时间线及增量缓存，重入挑战包含漏洞、攻击与修复合约。

组长：武钰斐，202492451119，计科1班。组员：苏佳豪、杨燊杰。

## 目录

| 路径 | 内容 |
|---|---|
| `packages/nextjs/` | B路线页面、钱包交互及角色操作 |
| `packages/hardhat/` | B路线合约、部署记录和自动化测试 |
| `backend/` | A路线后端、Java合约封装及自动化测试 |
| `contracts/reentrancy/` | 漏洞金库、修复金库和攻击合约 |
| `docs/transactions.md` | B路线交易记录；A路线交易台账见本文对应小节 |
| `docs/route-A-timeline.md` | A路线订单事件接口与缓存机制 |
| `docs/reentrancy.md` | 重入挑战原理、复现方法及交易记录 |

A、B路线使用不同的Escrow实例和仲裁账户，请按各节的地址与订单ID核对。B页面通过钱包直接调用链上合约，无需先启动A后端。

## 路线 B：前端与合约

### 环境与启动

使用 Node.js 20（至少 20.18.3）、Corepack 和仓库内固定的 Yarn 3.2.3。以下命令在 Linux、WSL 或 macOS 终端运行；Windows 用户建议在 WSL 中将仓库克隆到 Linux 文件系统。

```bash
git clone https://github.com/wuyufei0817/blockchain-escrow-group28.git
cd blockchain-escrow-group28
corepack enable
yarn install --immutable
yarn start
```

打开 <http://localhost:3000/escrow>，连接 MetaMask，切换到 POTOS Testnet。若 3000 端口已占用，以终端显示的端口为准。

本页面读取实验三已部署的合约，不需要先启动本地链，也不需要运行 deploy。不要为了查看现有订单重新部署或覆盖 `packages/nextjs/contracts/deployedContracts.ts`。

| 网络配置 | 值 |
|---|---|
| 网络 | POTOS Testnet |
| Chain ID | 60600 |
| RPC | https://rpc-testnet.potos.hk |
| 原生币 | POT |
| 浏览器 | https://scan-testnet.potos.hk |

### 环境变量

B 页面使用 MetaMask 扩展连接时无需配置私钥环境变量。签名在成员自己的钱包中完成。可选配置写在 `packages/nextjs/.env.local`，参考同目录 `.env.example`：

| 变量 | 用途 |
|---|---|
| `NEXT_PUBLIC_WALLET_CONNECT_PROJECT_ID` | 自有 WalletConnect 项目 ID；使用该连接方式时配置 |
| `NEXT_PUBLIC_ALCHEMY_API_KEY` | Scaffold-ETH 通用网络服务配置；POTOS 使用上表 RPC |

只有另行部署合约时才需要在 `packages/hardhat/.env` 中设置 `DEPLOYER_PRIVATE_KEY`。该文件不提交。`ALCHEMY_API_KEY` 用于可选分叉网络，`ETHERSCAN_V2_API_KEY` 用于相应浏览器验证，本组现有 B 页面验收不需要二者。

Hardhat 默认为本地测试网络，配置中没有示例部署私钥；未提供部署私钥时，外部网络账户列表为空。账号管理脚本解密后的 `__RUNTIME_DEPLOYER_PRIVATE_KEY` 仅用于进程运行时。

### 合约与账户

| 角色或合约 | 地址 |
|---|---|
| B Escrow（实验三原合约） | `0xbdB2Dfcd8A9Cd903848a1B9d95416b85178f05Af` |
| WYFToken（18 位精度） | `0x81777CEeA8CC59fa88ABcee9DC19970217Cf8637` |
| 买家：武钰斐 | `0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6` |
| 卖家：杨燊杰 | `0x92b0f84D625437245c3296012A0784d10C560309` |
| 仲裁人：苏佳豪 | `0x8c1f39320b2Dabf58eCeaD5225931674d558d3eD` |

Escrow 部署区块为 2673049。本轮正常订单实际 ID 为 4，争议订单实际 ID 为 5，金额均为 30 WYF；它们分别对应任务书的正常、争议场景。历史订单 0—3 继续保留。

### 路线 B 功能与代码

| 功能 | 主要文件（相对 `packages/nextjs/`） |
|---|---|
| 页面、账户与角色、订单详情 | `app/escrow/page.tsx` |
| 从 OrderCreated 发现订单，每段最多查 10 个区块 | `hooks/escrow/useEscrowOrders.ts`、`utils/escrow/creationIndex.ts` |
| 列表与文字状态 | `components/escrow/OrderList.tsx`、`utils/escrow/orderStatus.ts` |
| 创建表单、18 位金额、分钟转换 | `components/escrow/CreateOrderForm.tsx`、`utils/escrow/orderForm.ts` |
| 一键授权及付款、确认后发送下一笔 | `components/escrow/PaymentAction.tsx`、`utils/escrow/paymentFlow.ts` |
| 确认、争议、仲裁及角色权限 | `components/escrow/LifecycleActions.tsx`、`utils/escrow/orderPermissions.ts` |
| 钱包写操作及交易回执 | `hooks/escrow/useEscrowActions.ts`、`components/escrow/WriteProgress.tsx` |

`approve` 只设置额度；授权交易确认成功后才发送 `deposit`。Paid 状态下，买家可确认或争议，卖家只能争议；Disputed 状态下，只有合约记录的仲裁人显示两个裁决按钮。前端显示规则不能代替合约中的账户和状态检查。

首次打开页面需要从部署区块分段同步事件。该过程受公共 RPC 响应速度影响，已有订单可先查看。保持页面打开可保留当前同步进度；刷新会重新建立内存索引。`Failed to fetch`、RPC 超时或钱包无法连接时，先检查网络并查看是否已有交易哈希，恢复后核对订单状态，避免重复提交。

### 验收结果与原始截图

三名成员使用各自角色钱包通过本组页面操作，相关截图见 [路线 B 验收材料](docs/evidence/B/README.md)。正常订单 #4 的最终状态为 Completed；争议订单 #5 由卖家发起争议，仲裁人裁定退给买家，最终状态为 Refunded。

金额的原始值为 `30000000000000000000`，按 18 位精度换算为 30 WYF。本组创建时设置 120 分钟的到期窗口，报告保留实际时间，不替换成教程示例的 30 分钟。

### 交易记录

下表只记录路线 B。授权交易的目标是 WYFToken，授权支出者为 B Escrow；其他业务交易的目标为 B Escrow。订单编号均为实际链上编号。

| 操作 | 交易哈希及浏览器链接 | 发起账户 |
|---|---|---|
| B 部署 Escrow | [0x7cb22a0da3735a5caed06db50f7c048703ca5e8468baca998b09d9e52a3288dc](https://scan-testnet.potos.hk/tx/0x7cb22a0da3735a5caed06db50f7c048703ca5e8468baca998b09d9e52a3288dc) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| B createOrder 正常订单 #4 | [0x4d77be393ee3c121af13e2c33b6047beff36090fcd14fcd36bd9957544df8062](https://scan-testnet.potos.hk/tx/0x4d77be393ee3c121af13e2c33b6047beff36090fcd14fcd36bd9957544df8062) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| B approve 正常订单付款授权 30 WYF | [0x77c85dc2f5a7ec69fc0f105ffe58dc965266929ff3d5787f431909750cd77e3e](https://scan-testnet.potos.hk/tx/0x77c85dc2f5a7ec69fc0f105ffe58dc965266929ff3d5787f431909750cd77e3e) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| B deposit 正常订单 #4 | [0x61747c4a6dc1d368d06ed12fe2ba9634737f93a70faa0f20e66e42b86a2afc70](https://scan-testnet.potos.hk/tx/0x61747c4a6dc1d368d06ed12fe2ba9634737f93a70faa0f20e66e42b86a2afc70) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| B createOrder 争议订单 #5 | [0x91b39590b1c53d7b240f0e8f3ce673f98fde4bc4220345ad1bbafa33b34f715c](https://scan-testnet.potos.hk/tx/0x91b39590b1c53d7b240f0e8f3ce673f98fde4bc4220345ad1bbafa33b34f715c) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| B approve 争议订单付款授权 30 WYF | [0x6ca4306610cd22507f692fa4073724349e797539a1acee8f68a76083c63a89f5](https://scan-testnet.potos.hk/tx/0x6ca4306610cd22507f692fa4073724349e797539a1acee8f68a76083c63a89f5) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| B deposit 争议订单 #5 | [0x97d804cc7bf9ba74b46ba9ed81a040b06e5bf7da28e20f8b51f3e72614dc35b2](https://scan-testnet.potos.hk/tx/0x97d804cc7bf9ba74b46ba9ed81a040b06e5bf7da28e20f8b51f3e72614dc35b2) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |
| B dispute 争议订单 #5 | [0xfee09bfcfeaa191820e10583b880113fb78d4a8ff969d32e6fad77b11fd61dc7](https://scan-testnet.potos.hk/tx/0xfee09bfcfeaa191820e10583b880113fb78d4a8ff969d32e6fad77b11fd61dc7) | 0x92b0f84D625437245c3296012A0784d10C560309 |
| B resolve 争议订单 #5 退给买家 | [0x5694b3f8026bb4ebc081b8384ec6258bc4bb00a36b661715a678eececae3b5aa](https://scan-testnet.potos.hk/tx/0x5694b3f8026bb4ebc081b8384ec6258bc4bb00a36b661715a678eececae3b5aa) | 0x8c1f39320b2Dabf58eCeaD5225931674d558d3eD |
| B confirm 正常订单 #4 | [0xdf4e747ab8f1296e003b22408d3c71329dcf9606165d2d18deffbaf424212e31](https://scan-testnet.potos.hk/tx/0xdf4e747ab8f1296e003b22408d3c71329dcf9606165d2d18deffbaf424212e31) | 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6 |

### 检查命令

```bash
yarn compile
yarn test
yarn next:check-types
yarn hardhat:check-types
```

测试包括合约托管流程及权限、10 区块日志分页、金额与到期时间校验、授权确认顺序、拒绝或失败交易、按角色显示操作等。检查结果记录在 [验证记录](docs/VERIFICATION.md)，本地测试结果不代替测试网交易证据。

### 实际分工

| 成员 | 工作与对应文件/材料 |
|---|---|
| 武钰斐 | 项目统筹、B 前端实现与联调、买家验收、截图及报告整合；负责上述 `app/escrow`、`components/escrow`、`hooks/escrow`、`utils/escrow` 等文件 |
| 苏佳豪 | A 后端、订单事件时间线及重入挑战合约；B 中使用本人原仲裁钱包完成裁决及相关验收 |
| 杨燊杰 | B 卖家钱包操作、发起争议、配合角色权限对比及验收截图 |

开发与排错过程中使用了 AI 辅助，报告的“AI 使用说明”记录工具用途及纠错过程。实际钱包交易、成员分工和截图均以本组实验记录为准。

### 来源与许可

前端工程基于 [Scaffold-ETH 2](https://github.com/scaffold-eth/scaffold-eth-2)，保留原项目的 [许可文件](LICENCE)。本组在其基础上实现课程托管业务页面、业务测试和 POTOS 网络配置。

## 路线 A：Spring Boot与Web3j后端

使用JDK 21、Spring Boot 3.2.3、Web3j 4.14.0、Gradle Wrapper 8.13、Solidity 0.8.20 / Paris。后端合约没有npm导入，无需安装Node。

### A路线构建与测试

以下PowerShell命令从仓库根目录执行。先安装JDK 21，确认`java -version`和`javac -version`均使用21，且`JAVA_HOME`指向JDK根目录。项目自带Gradle Wrapper，无需单独安装Gradle。

```powershell
Set-Location .\backend
.\gradlew.bat generateContractWrappers test bootJar
Set-Location ..
```

首次构建需要联网下载Gradle、Solidity编译器及依赖；缓存齐全后可加`--offline`。Windows中文路径所需的编码配置已保存在`backend/gradle.properties`中。测试位于`backend/src/test/`，共33项，覆盖接口、事件索引、配置校验和nonce串行化。

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

事件索引机制见[订单时间线说明](docs/route-A-timeline.md)，验收截图收录于课程报告。

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

## 重入攻击挑战

源码位于[contracts/reentrancy/](contracts/reentrancy/)，包含漏洞版`VulnerableVault.sol`、采用CEI修复的`FixedVault.sol`和`Attacker.sol`。实验方法、测试网地址和成功／失败交易见[重入挑战说明](docs/reentrancy.md)，验收截图收录于课程报告。
