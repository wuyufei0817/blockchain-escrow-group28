# 重入挑战源码与验收记录

三份源码来自苏佳豪本次提供的实验文件，原样复制至 `contracts/reentrancy/`；原文件未改动，复制后逐文件比对SHA-256。此目录独立于A后端的Solidity源目录，不会为金库合约额外生成Java封装，也没有改动组长的Hardhat工作区配置。

## 文件与行为

| 文件 | 作用 |
|---|---|
| [VulnerableVault.sol](../contracts/reentrancy/VulnerableVault.sol) | deposit登记余额；withdraw先外部call、后清零，回调期间旧余额仍可再次提款 |
| [FixedVault.sol](../contracts/reentrancy/FixedVault.sol) | withdraw先清零再外部call，重入时余额检查失败；采用CEI顺序，没有额外重入锁 |
| [Attacker.sol](../contracts/reentrancy/Attacker.sol) | 构造时固定vault和owner；owner以0.001 POT调用attack，先deposit再withdraw；receive中在绑定金库足额时再次withdraw；withdrawLoot将已取得余额转回owner |

本次实际源码没有固定回调次数上限。已执行实验只在本组金库进行：0.002 POT受害者资金加0.001 POT攻击本金，按每次0.001 POT提取；停止条件是金库余额不足。源码与最初计划中“固定上限”的描述不同，归档保留实际执行版本，不将修改后的代码冒充已部署版本。

## 编译检查

已使用本机Solidity `0.8.20+commit.a1b79de6`、EVM `paris`对三份源文件生成ABI和bin，编译成功，并核对了ABI中的全部函数。检查未部署合约、未发送攻击交易，也未新增本地EVM自动化测试。编译器版本是本次归档检查使用的版本，不代表已独立确认Remix历史部署的全部编译设置。

复现编译时，将 `solc` 替换为本机对应可执行文件路径，从仓库根目录运行，输出目录放在临时位置：

```text
solc --bin --abi --evm-version paris --output-dir <临时输出目录> contracts/reentrancy/VulnerableVault.sol contracts/reentrancy/FixedVault.sol contracts/reentrancy/Attacker.sol
```

集成到组长的Hardhat工程时，需要由共享配置负责人将实际源目录纳入编译，并补充本地EVM回归测试；本次源码归档不等于已经完成这些测试。

## 两轮测试网记录

网络：POTOS Testnet，chain ID 60600。以下数据来自本轮只读查询，未重新执行攻击。

| 项目 | 漏洞版 | 修复版 |
|---|---|---|
| 金库 | `0xE9fE119aFf9B9F4738cDDF93a2aB2171f8615123` | `0xc120509D8a34Fc3658253f95AfA1D52a300396b8` |
| 对应Attacker | `0x98d531Dc0374DB43a30E00Bb22aD673fA5C30a21` | `0x780F34434831eBf9Cb9E566206415528d3b216af` |
| 攻击owner / From | `0x8c1f39320b2Dabf58eCeaD5225931674d558d3eD` | 同左 |
| attack附带原生币 | 0.001 POT | 0.001 POT |

| 操作 | 完整交易哈希及浏览器链接 | 区块 | status | gasUsed |
|---|---|---|---|---|
| 漏洞版攻击 | [0xee324fddf87ffb0f1e09aba22dee15ceacc1db1f135c779da9fa2841d6a91272](https://scan-testnet.potos.hk/tx/0xee324fddf87ffb0f1e09aba22dee15ceacc1db1f135c779da9fa2841d6a91272) | 2845336 | 0x1 | 116282 |
| 修复版攻击 | [0xe7e73b521adbecc7e97c7c3f6f77972baf2574572becb774cbd15780ed1277aa](https://scan-testnet.potos.hk/tx/0xe7e73b521adbecc7e97c7c3f6f77972baf2574572becb774cbd15780ed1277aa) | 2845571 | 0x0 | 74369 |

已核对两笔交易的attack调用数据、0.001 POT金额、回执成功/失败状态，以及各Attacker的vault绑定和owner。完整公共回执见[chain-verification.json](evidence/C/chain-verification.json)。尚未在本次检查中核验源码与历史部署字节码的一致性，也未补查各部署和受害者deposit的完整交易哈希。

报告记录的攻击后快照为：漏洞金库0、攻击合约0.003 POT；修复金库0.002 POT、对应攻击合约0。归档检查时（2026-11-08T21:36:58.812519+08:00），漏洞金库和其Attacker均为0，修复金库仍为0.002 POT、其Attacker为0。当前余额不能代替报告历史截图；漏洞Attacker后续资金操作尚未核验，不能据当前为0就改写攻击后曾取得0.003 POT的截图记录。

## 原始截图与来源

从用户报告中提取并原字节归档8张截图：4C-1三张、4C-2一张、4C-3四张，见[证据目录](evidence/C/)。没有编辑或拼接图片，也没有修改Word报告。

- [源码SHA-256及编译检查范围](evidence/C/source-manifest.json)
- [原图SHA-256及报告来源](evidence/C/report-image-manifest.json)
- [两笔攻击的公开链上回执](evidence/C/chain-verification.json)

应区分本金与gas：修复版attack整体回滚，本次0.001 POT的deposit也回滚，金库保留原受害者0.002 POT。部署、普通存取、成功攻击、修复拒绝等完整本地测试仍需按实际实现补齐，不能把源码成功编译写成本地攻击测试已经通过。
