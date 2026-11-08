# 重入攻击挑战

## 合约与原理

| 源码 | 行为 |
|---|---|
| [VulnerableVault.sol](../contracts/reentrancy/VulnerableVault.sol) | withdraw先向调用方转账、后清零余额；回调可在余额清零前再次提款 |
| [FixedVault.sol](../contracts/reentrancy/FixedVault.sol) | withdraw先清零余额、后转账，遵循Checks-Effects-Interactions顺序，重入时余额检查失败 |
| [Attacker.sol](../contracts/reentrancy/Attacker.sol) | attack先存入0.001 POT再提款，receive回调在金库足额时继续提款；仅owner可发起攻击或提取所得 |

## 复现方法

使用Solidity 0.8.20、EVM Paris，在Remix编译三份合约，连接POTOS Testnet（chain ID 60600），使用自己的测试账户操作：

1. 部署VulnerableVault，由另一账户deposit 0.002 POT。
2. 部署Attacker，构造参数为漏洞金库地址。
3. 向attack附带0.001 POT，交易成功后查看金库与攻击合约余额。
4. 部署FixedVault和一个绑定修复金库的新Attacker，向修复金库存入0.002 POT。
5. 再向新Attacker的attack附带0.001 POT，交易整体回滚；金库保留原0.002 POT，攻击合约未取得资金。

漏洞版实验攻击后金库为0、攻击合约为0.003 POT，其中0.001 POT为攻击本金、0.002 POT为额外取得的资金。修复版交易回滚也会回滚本次deposit。余额截图收录于课程报告；后续提取会改变当前余额，应按对应交易及历史截图核对。

三份合约也可用本机solc编译，输出目录需自行创建：

```text
solc --bin --abi --evm-version paris --output-dir <输出目录> contracts/reentrancy/VulnerableVault.sol contracts/reentrancy/FixedVault.sol contracts/reentrancy/Attacker.sol
```

## 测试网实例

| 项目 | 漏洞版 | 修复版 |
|---|---|---|
| 金库 | `0xE9fE119aFf9B9F4738cDDF93a2aB2171f8615123` | `0xc120509D8a34Fc3658253f95AfA1D52a300396b8` |
| Attacker | `0x98d531Dc0374DB43a30E00Bb22aD673fA5C30a21` | `0x780F34434831eBf9Cb9E566206415528d3b216af` |
| owner | `0x8c1f39320b2Dabf58eCeaD5225931674d558d3eD` | 同左 |

| 操作 | 交易哈希 | 区块 | status | gasUsed |
|---|---|---|---|---|
| 漏洞版攻击 | [0xee324fddf87ffb0f1e09aba22dee15ceacc1db1f135c779da9fa2841d6a91272](https://scan-testnet.potos.hk/tx/0xee324fddf87ffb0f1e09aba22dee15ceacc1db1f135c779da9fa2841d6a91272) | 2845336 | 0x1 | 116282 |
| 修复版攻击 | [0xe7e73b521adbecc7e97c7c3f6f77972baf2574572becb774cbd15780ed1277aa](https://scan-testnet.potos.hk/tx/0xe7e73b521adbecc7e97c7c3f6f77972baf2574572becb774cbd15780ed1277aa) | 2845571 | 0x0 | 74369 |

以上为测试网实验记录；本目录未包含本地EVM自动化攻击测试。
