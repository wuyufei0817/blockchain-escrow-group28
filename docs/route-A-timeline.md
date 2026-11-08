# 订单事件时间线

## 查询接口

`GET /api/escrow/orders/{id}/events`返回已加载Escrow中指定订单的历史事件。调用前先通过`POST /api/escrow/load`指定合约地址与部署区块，启动及加载方法见[README](../README.md)。

接口读取OrderCreated、OrderPaid、OrderCompleted、OrderRefunded、OrderDisputed、OrderResolved六种事件，按区块号、交易序号、日志序号排序，以交易哈希和日志序号去重，再按订单ID筛选。每项包含id、eventName、status、parameters、contractAddress、blockNumber、transactionIndex、logIndex、transactionHash、blockHash；整数以字符串表示。

status表示事件产生的状态。`OrderResolved(false)`对应仲裁退款，显示为`Refunded`；直接超期退款使用`OrderRefunded`事件。

```powershell
$events = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/escrow/orders/0/events"
$events | Format-Table eventName,status,blockNumber,transactionHash -AutoSize -Wrap
```

本组订单0有创建、付款、完成三个事件，订单1有创建、付款、争议、仲裁退款四个事件。验收截图收录于课程报告，交易哈希见[README交易台账](../README.md#A路线真实交易台账)。

## 分段扫描与缓存

从部署块开始，每次查询最多10个区块。订单列表与时间线共用事件索引，列表由OrderCreated事件派生。首次扫描全部历史，后续仅扫描新增区块；首次查询耗时取决于部署至当前的区块跨度。

缓存默认位于启动工作目录的`.escrow-event-index/`，可通过`--web3j.event-index-dir=目录`修改。缓存按chain ID、合约地址和部署块隔离，持久化进度以支持重启续扫；缓存目录不提交Git。

查询会核对检查点和链头的区块哈希，链重组或链头回退时重建。所有分段成功且文件原子写入完成后才发布新快照。RPC失败或日志校验失败返回503，不推进进度；同一索引的并发请求共用锁。未加载合约返回409，非法参数返回400，不存在的订单返回404。

## RPC数量

设H为部署块至链头的区块数，D为成功检查点之后的新增区块数。在链头稳定、无错误和重组时：

| 请求 | 首次扫描 | 链头不变 | 增量扫描 |
|---|---|---|---|
| 订单列表 | 约4+ceil(H/10) | 约3 | 约5+ceil(D/10) |
| 订单时间线 | 约5+ceil(H/10) | 约4 | 约6+ceil(D/10) |

时间线额外调用orderCount校验订单存在性；排序、去重和读取本地缓存不产生RPC。上述计数不包含load、单独查询详情和错误重试。

测试用例位于`backend/src/test/java/com/wetech/demo/web3j/controller/EscrowApiTest.java`，覆盖事件解析、排序去重、共享扫描、重启恢复、错误不推进进度及链重组处理。
