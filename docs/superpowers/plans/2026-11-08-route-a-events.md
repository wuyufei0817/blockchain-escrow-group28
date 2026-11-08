# A路线事件时间线 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成已确认执行计划T09的事件时间线接口与共享增量索引。
**Architecture:** 在现有Spring Boot/Web3j后端新增EscrowEventIndexService。按10块闭区间读取同一实例的六种事件，排序去重，同时派生原有创建列表与每单时间线。按chain/address/deployBlock保存原子JSON缓存与checkpoint，校验checkpoint区块哈希以识别重组；查询失败不提交半成品快照。
**Tech Stack:** Java21 / Spring Boot3.2.3 / Web3j4.14.0 / Jackson / JUnit5。
**Spec:** docs/PROJECT_PLAN.md，T09；用户已明确授权进行A路线附加分项，沿用既定方案。

## Global Constraints

- 不更改原Escrow.sol、六个原接口的数据结构或组员的重入代码。
- GET /api/escrow/orders/{id}/events只读；不存在404、未加载409、参数错误400、RPC/缓存失败503。
- ID、金额、区块等整数按字符串返回。事件名保留Solidity名称，parameters保留实际事件参数；status表示事件造成的状态。
- 首次全量扫描，后续从checkpoint+1继续；最多每段10块。链、地址、部署块不同的缓存隔离。
- 失败不返回成功空数组，不自动发任何交易；原始证据及Word报告不改。
- 当前用户分支上工作；用户用GitHub Desktop提交，不自动commit/push。

## Review Focus

- RPC中段失败后的重试不能漏掉事件或错误推进checkpoint。
- 重启读缓存、合约切换、链切换均不串数据。
- 同块多日志、乱序和重复日志需要稳定排序并去重。
- OrderResolved(false)必须呈现Refunded，不要求不存在的OrderRefunded事件。
- checkpoint区块变化或head后退时重建，失败快照不污染旧缓存。

## Task 1 共享事件索引及接口

**Files:** Create backend/src/main/java/com/wetech/demo/web3j/service/EscrowEventIndexService.java; modify EscrowService.java, EscrowController.java; extend backend/src/test/java/com/wetech/demo/web3j/controller/EscrowApiTest.java; ignore runtime cache in root/backend .gitignore.

- [x] 添加时间线HTTP回归，实际观察当前404失败。
- [x] 添加六事件、10块分段、重复乱序、404/400/409、RPC错误、增量、磁盘续扫、重组及隔离测试。
- [x] 实现共享索引、持久化及只读接口，保留旧列表字段。
- [x] 全部测试及bootJar通过。

## Task 2 实际验收和交付

- [x] 临时独立端口、临时随机签名账户加载现有A实例，只读查询订单0/1时间线；验证首次、增量、重启续扫。
- [x] 独立代码复核，修复有证据的问题并重跑相关测试。
- [x] README及说明题4A-3更新为实际RPC计数；提供BONUS-TIMELINE-A截图位置与填写文字，不新增交易。

## Execution ledger

- 方案依据用户已确认的T09；不另行设计前端页面，前端由组长整合。
- 缓存默认当前工作目录下.escrow-event-index，支持web3j.event-index-dir覆盖；根目录和backend均忽略此运行数据。

- Task 1 complete：先观察新接口409断言实际404失败，随后7个新增行为测试均失败；实现后29项通过。独立复核建议补并发、扫描中重组、坏日志及部署块隔离，增加4项后33项全部通过。模拟RPC按Web3j实际请求接受address数组。基础六接口回归通过。
- Task 2 in progress：临时18080服务与只读RPC代理在扫描原A实例全部历史，代理拒绝所有写方法，等待首次扫描及重启验收结果。

- Task 2 complete：33项实际执行全绿，独立代码复核无阻断项；现有A实例只读查询7事件与历史回执逐一一致。首次1441段/611.72秒，第二个订单9段增量，重启1段增量，缓存与续扫验证成功。原8080未动，无链上写入。README/独立文档/基础版本计数说明已更新；用户原始截图待后续引导采集。
