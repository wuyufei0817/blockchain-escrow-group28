# 第28组：区块链托管交易应用

本项目基于 Scaffold-ETH 2，为实验三的 ERC-20 托管合约提供订单页面。买家通过钱包创建订单并授权、付款；买卖双方可发起争议；仲裁人使用本人钱包裁决。页面按当前账户及订单状态显示可执行操作。

组长：武钰斐，202492451119，计科1班。组员：苏佳豪、杨燊杰。

当前提交包含已完成的路线 B、合约源码、自动化测试及本组验收记录。路线 A 由苏佳豪后续通过 PR 提交，合并后补充后端启动说明；本版本不包含 Spring Boot 后端。

## 环境与启动

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

## 环境变量

B 页面使用 MetaMask 扩展连接时无需配置私钥环境变量。签名在成员自己的钱包中完成。可选配置写在 `packages/nextjs/.env.local`，参考同目录 `.env.example`：

| 变量 | 用途 |
|---|---|
| `NEXT_PUBLIC_WALLET_CONNECT_PROJECT_ID` | 自有 WalletConnect 项目 ID；使用该连接方式时配置 |
| `NEXT_PUBLIC_ALCHEMY_API_KEY` | Scaffold-ETH 通用网络服务配置；POTOS 使用上表 RPC |

只有另行部署合约时才需要在 `packages/hardhat/.env` 中设置 `DEPLOYER_PRIVATE_KEY`。该文件不提交。`ALCHEMY_API_KEY` 用于可选分叉网络，`ETHERSCAN_V2_API_KEY` 用于相应浏览器验证，本组现有 B 页面验收不需要二者。

Hardhat 默认为本地测试网络，配置中没有示例部署私钥；未提供部署私钥时，外部网络账户列表为空。账号管理脚本解密后的 `__RUNTIME_DEPLOYER_PRIVATE_KEY` 仅用于进程运行时。

## 合约与账户

| 角色或合约 | 地址 |
|---|---|
| B Escrow（实验三原合约） | `0xbdB2Dfcd8A9Cd903848a1B9d95416b85178f05Af` |
| WYFToken（18 位精度） | `0x81777CEeA8CC59fa88ABcee9DC19970217Cf8637` |
| 买家：武钰斐 | `0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6` |
| 卖家：杨燊杰 | `0x92b0f84D625437245c3296012A0784d10C560309` |
| 仲裁人：苏佳豪 | `0x8c1f39320b2Dabf58eCeaD5225931674d558d3eD` |

Escrow 部署区块为 2673049。本轮正常订单实际 ID 为 4，争议订单实际 ID 为 5，金额均为 30 WYF；它们分别对应任务书的正常、争议场景。历史订单 0—3 继续保留。

## 路线 B 功能与代码

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

## 验收结果与原始截图

三名成员使用各自角色钱包通过本组页面操作，相关截图见 [路线 B 验收材料](docs/evidence/B/README.md)。正常订单 #4 的最终状态为 Completed；争议订单 #5 由卖家发起争议，仲裁人裁定退给买家，最终状态为 Refunded。

金额的原始值为 `30000000000000000000`，按 18 位精度换算为 30 WYF。本组创建时设置 120 分钟的到期窗口，报告保留实际时间，不替换成教程示例的 30 分钟。

## 交易记录

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

## 检查命令

```bash
yarn compile
yarn test
yarn next:check-types
yarn hardhat:check-types
```

测试包括合约托管流程及权限、10 区块日志分页、金额与到期时间校验、授权确认顺序、拒绝或失败交易、按角色显示操作等。检查结果记录在 [验证记录](docs/VERIFICATION.md)，本地测试结果不代替测试网交易证据。

## 实际分工

| 成员 | 工作与对应文件/材料 |
|---|---|
| 武钰斐 | 项目统筹、B 前端实现与联调、买家验收、截图及报告整合；负责上述 `app/escrow`、`components/escrow`、`hooks/escrow`、`utils/escrow` 等文件 |
| 苏佳豪 | A 后端开发，后续通过 PR 提交；B 中使用本人原仲裁钱包完成裁决及相关验收 |
| 杨燊杰 | B 卖家钱包操作、发起争议、配合角色权限对比及验收截图 |

开发与排错过程中使用了 AI 辅助，报告的“AI 使用说明”记录工具用途及纠错过程。实际钱包交易、成员分工和截图均以本组实验记录为准。

## 来源与许可

前端工程基于 [Scaffold-ETH 2](https://github.com/scaffold-eth/scaffold-eth-2)，保留原项目的 [许可文件](LICENCE)。本组在其基础上实现课程托管业务页面、业务测试和 POTOS 网络配置。
