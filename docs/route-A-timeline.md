# A路线订单事件时间线

## 功能与接口

新增只读接口`GET /api/escrow/orders/{id}/events`。从已加载Escrow的真实部署块开始，按最多10块的闭区间读取OrderCreated、OrderPaid、OrderCompleted、OrderRefunded、OrderDisputed、OrderResolved六种事件。结果按blockNumber、transactionIndex、logIndex排序，以transactionHash与logIndex去重，按订单ID筛选。

每项包含id、eventName、status、parameters、contractAddress、blockNumber、transactionIndex、logIndex、transactionHash、blockHash。整数用字符串表示；parameters中的sellerWins也用字符串true/false。status为该事件造成的状态，不是另做一次当前状态查询。

原`GET /api/escrow/orders`保持原有六个字段，从同一索引的OrderCreated事件派生。`OrderResolved(false)`表示仲裁退款，状态为Refunded；它不意味着该交易还应有OrderRefunded事件。直接超期退款才发OrderRefunded。

## 缓存与错误

缓存保存在启动工作目录下`.escrow-event-index/`，可用`--web3j.event-index-dir=目录`覆盖；根目录和backend均已忽略默认缓存。缓存只含公开事件。键包含chainId、合约地址和部署块，切换实例不会复用其他实例的数据。

首次查询扫描完整历史，后续从checkpoint+1扫描新增区块；重启后从JSON恢复。每次检查checkpoint的规范区块哈希，发生重组或链头回退时重建；扫描前后复查head哈希。请求的所有分段成功且原子写文件完成后，才发布新快照。中段RPC失败、日志地址/区间/身份字段错误或缓存异常返回503，不返回成功空数组，也不提交半成品checkpoint。同键并发请求共用锁，只进行一次必要扫描。

未加载409、参数非法400、订单不存在404。所有请求只读，无需再次创建、支付、争议或仲裁。

## 构建与运行

从仓库根目录构建：

```powershell
.\backend\gradlew.bat -p .\backend test bootJar
```

在已经设置本人BE私钥环境变量的原终端停止旧进程，使用同一命令启动新JAR；保持窗口运行。另一个终端加载原合约，这一步不用截图：

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/escrow/load?address=0x793D08836c2e00Af0BdE963386f36e00acEe180f&fromBlock=2832142" | ConvertTo-Json
```

## 报告截图位置

在报告4.5加分项中，与重入挑战分开，新建“订单事件时间线（A路线）”小节。不替换4A-1至4A-8，也不要求重截基础Service代码；基础截图对应此前验收提交fc3c58b，新增实现由本小节和新版本源码说明。

第一张图：执行后立即截命令及所有三行事件，放在新小节第一张。为了在一张图内呈现全部事件，使用原响应的字段投影显示，不编辑或拼接原图：

```powershell
$events0 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/escrow/orders/0/events"
$events0 | Format-Table eventName,status,blockNumber,transactionHash -AutoSize -Wrap
```

图注：BONUS-TIMELINE-A（1）正常订单0的事件时间线，依次为OrderCreated、OrderPaid、OrderCompleted。

第二张图：执行后立即截命令及所有四行事件，放在同一小节第二张：

```powershell
$events1 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/escrow/orders/1/events"
$events1 | Format-Table eventName,status,blockNumber,transactionHash -AutoSize -Wrap
```

图注：BONUS-TIMELINE-A（2）争议订单1的事件时间线，依次为OrderCreated、OrderPaid、OrderDisputed、OrderResolved，最终退款。

若需要完整JSON用于核对参数，可执行`$events1 | ConvertTo-Json -Depth 8`，不额外要求截图。

## RPC数量说明

基础验收截图版本的列表开销为2+ceil(H/10)，该实现当时没有缓存。新增索引版本需要检查checkpoint哈希与扫描前后head哈希，因此不能沿用旧计数描述新版本。

设H为从部署块到本次链头的区块数、D为成功checkpoint之后的新增区块数。链头稳定、无错误和重组时，新列表首次约4+ceil(H/10)次RPC；链头不变的缓存命中约3次；正常增量约5+ceil(D/10)次。时间线接口多一次orderCount存在性校验，分别约5+ceil(H/10)、4、6+ceil(D/10)。解析、排序和去重在本地完成，不产生RPC；恢复本地文件也不产生链上请求。

这些计数只描述一次HTTP请求的正常路径，不含load、另查详情、RPC故障或重组重建。事件法仍不保证在所有区块跨度下都比逐单查询更少；增量缓存使后续请求不必重复扫描整段历史。

## 验证记录

本地测试涵盖六种事件、同块与跨分段排序、重复日志、并发共享扫描、错误重试不推进checkpoint、落盘重启续扫、链/地址/部署块隔离、重组和坏日志处理。真实订单的只读查询结果与扫描计数在`docs/evidence/A/timeline/`记录。人工报告截图由用户在本机运行新接口后采集。

实际验收已完成：订单0为3个事件，订单1为4个事件，全部7笔哈希与已有回执一一对应。首次查询扫描1441段，耗时611.72秒；之后订单1仅补扫9段，重启仅补扫1段。详见[实测记录](evidence/A/backend-timeline-check.md)。33项本地测试通过。此结果不代替用户采集报告原始截图。
