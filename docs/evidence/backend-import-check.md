# Backend starter import check

Date: 2026-11-02 (Asia/Shanghai). Branch: feat/su-backend.

- Imported the local Ubuntu spring-boot-web3j working files into backend/.
  Source HEAD: a36f03e5c26a9df77dd6acc17c4b1a4eef6616af.
- Excluded old .git, .gradle, build output, logs and README.
- Removed the hardcoded private key and Java fallback before writing into this repository.
- The only key configuration is ${WEB3J_PRIVATE_KEY}; gas price is 21000 wei.
- Checked imported text for 0x-prefixed 64-digit private-key-shaped literals: none found.
  This is a scoped import check, not a complete repository-history secret audit.
- Checked Git ignore rules for build/cache/.env/local properties/log output.

## Build result

Windows JDK: Temurin 21.0.13. Gradle Wrapper: 8.13.

```powershell
.\gradlew.bat compileJava test bootJar -x generateContractWrappers -x generateTestContractWrappers -x compileSolidity -x compileTestSolidity --offline --no-daemon --console=plain
```

Result: BUILD SUCCESSFUL in 23s; 4 actionable tasks executed.
compileJava, processResources, resolveMainClassName and bootJar completed.
Test tasks reported NO-SOURCE: this starter contains no automated Java tests yet.

The initial online Gradle download timed out. Cached Gradle and dependencies
were reused from Ubuntu via a temporary directory outside the repository.
A restricted-process run could not connect to its local Gradle daemon; the
normal local-process build succeeded. An earlier offline attempt triggered
a missing Windows Node download from test-wrapper generation; the final
command explicitly excludes both main and test Solidity generation.

## Missing-key check

Ran the packaged jar with WEB3J_PRIVATE_KEY absent and web application type none.
It exited nonzero with Could not resolve placeholder WEB3J_PRIVATE_KEY.
No private key was supplied and no transaction was submitted.

## Scope and next step

This verifies Java starter import, compilation, packaging and the mandatory
key placeholder. It does not validate Solidity compilation, the imported
wrapper bytecode, RPC access, chain ID, deployment, or Escrow functionality.
Solidity 0.8.20 / paris and explicit chain ID still need configuration
before chain acceptance. T07 is not complete. No commit or push was made.

## Backend configuration checkpoint - 2026-11-02

Scope agreed with the user: configure the backend first. The local Escrow
source has not been confirmed as the group leader's original; no Escrow
or token source/wrapper was imported in this checkpoint. No README added.

- Added explicit chain ID 60600 and an EIP-155 transaction manager.
- SimpleStorage deploy/load now use that transaction manager.
- Before signing/broadcasting a write, query eth_chainId and refuse
  a mismatched network, missing result or JSON-RPC error.
- Validate key format/range without echoing the key; close Web3j on shutdown.
- Corrected gas price to the handout's recommended 1000000 wei (04, page 3),
  superseding the import's 21000-wei setting, which was the network baseline
  mentioned in the preceding page rather than the recommended configuration.
- Added Spring Boot test dependencies (JUnit 5 / Mockito).

Test-first record: 4 behavior tests failed against the old configuration:
unsigned-chain signing, broadcasts despite wrong network/RPC failure, and
an unhelpful blank-key error. After the configuration changes all 4 pass.
Tests exercise the real Spring configuration, generated SimpleStorage
wrapper and signed requests against a local HTTP JSON-RPC fixture. Keys
are generated only in memory for each test. No real account is used.

Final command from backend (GRADLE_USER_HOME points to the previously
prepared external cache):

```powershell
.\gradlew.bat test bootJar -x generateContractWrappers -x generateTestContractWrappers -x compileSolidity -x compileTestSolidity --offline --no-daemon --console=plain --rerun-tasks
```

Result: BUILD SUCCESSFUL in 20s, 6 tasks executed; XML reports 4 tests,
0 failures and 0 errors. The packaged application was also run without
WEB3J_PRIVATE_KEY and correctly refused startup on the missing placeholder.
Source checks found no 0x-prefixed 64-digit key-shaped literals.

Windows test-worker issue: Gradle's UTF-8 argument file did not match
the native Windows encoding when paths contained Chinese characters.
Matched the daemon encoding to the native encoding using file.encoding=COMPAT
in backend/gradle.properties; JavaCompile remains explicitly UTF-8.
Sources: https://github.com/gradle/gradle/issues/30304 and
https://openjdk.org/jeps/400 . No system encoding setting was changed.

Still unverified: actual RPC/network reachability, account funding, Solidity
compilation, group contract consistency, and all testnet operations. T07
remains incomplete until these later acceptance steps are performed.
Changes are uncommitted; no PR, deployment or testnet transaction was made.

## Group Escrow wrapper checkpoint - 2026-11-07

The user confirmed the course-directory Escrow.sol is the group's source.
Imported it unchanged into backend/src/main/resources/contracts/Escrow.sol.
SHA256: ce57a9a75278dd65e9a5481a4dc2392d6ee4d68eab4b5bf384157be8b63c24ab.
This supersedes the earlier assumption about the personal experiment copy:
the ABI is compatible, but source text/revert messages and resolve
implementation differ. The user-confirmed group source is authoritative.

Pinned Solidity 0.8.20 / EVM paris and disabled npm package resolution
because the contracts have no npm imports. Framework versions unchanged.

Full command from backend, using the prepared external GRADLE_USER_HOME:

```powershell
.\gradlew.bat generateContractWrappers test bootJar --no-daemon --console=plain
```

Result: BUILD SUCCESSFUL in 1m 14s; 8 actionable tasks, 7 executed and
1 up-to-date. Solidity compilation and main wrapper generation ran.
XML: 4 tests, 0 failures, 0 errors. No Solidity-generation skip flags.

Generated Escrow.java, IERC20.java and refreshed SimpleStorage.java.
Verified compiler metadata 0.8.20+commit.a1b79de6, EVM paris;
Escrow.java BINARY exactly equals Escrow.bin; ABI matches metadata;
getOrder returns buyer/seller/token/amount/deadline/status/description.
Order, OrderCreatedEventResponse and getOrderCreatedEventFromLog exist.

Generator notices about non-view functions returning values are expected:
write methods return transaction receipts. New order IDs must be read
from OrderCreated receipt events rather than assumed.
Build output and compiler/cache are not tracked in Git.

No deployment, public-network transaction or PR was performed. The
original source outside the repository was not modified. T07 still
requires backend-account setup and real SimpleStorage acceptance.
Escrow REST endpoints and the event index remain later tasks.

## Escrow REST checkpoint - 2026-11-07

Implemented the six T08 endpoint routes:

- POST /api/escrow/deploy
- POST /api/escrow/load?address=...&fromBlock=...
- GET /api/escrow/address
- GET /api/escrow/orders/{id}
- GET /api/escrow/orders
- POST /api/escrow/orders/{id}/resolve?sellerWins=true|false

Deploy encodes the backend signer as arbiter and returns the confirmed
receipt fields. Load validates chain, address, bytecode, deployment-block
bounds/history and expected read methods before atomically replacing
the current immutable context; failed loads preserve the old instance.
Detail IDs/amounts/times use decimal strings and states use names.
The list scans OrderCreated logs in inclusive ranges of at most 10 blocks,
checks RPC errors, sorts and deduplicates. Persistent caching and the
full order-event timeline remain T09 work.

Write paths use fixed gas and do not estimate away a deliberate failing
resolve. Mined status-0 receipts return 422 with hash/block/status/gas and
best-effort receipt/replay reason. Pending or unknown receipts return
503 with the accepted hash and do not trigger another broadcast. RPC
hash mismatches retain the local signed-transaction hash. Malformed
parameters return 400; unloaded state 409; nonexistent order 404.

Load/deploy/resolve are serialized at the service boundary. The shared
transaction manager also serializes nonce selection through broadcast
for both Escrow and existing SimpleStorage writes, using pending nonces.
This covers one backend process; it is not a multi-server nonce service.

Test-first record: 10 initial API tests returned 404 before implementation;
they passed after the routes/service were added. Three further load
boundary tests cover preserving the old instance, future blocks and
incompatible ABI. An independent read-only review found a shared-manager
nonce race and discarded local hashes on RPC hash mismatch. Both were
reproduced with failing tests and fixed.

Final full command, with the previously prepared GRADLE_USER_HOME:

```powershell
.\gradlew.bat generateContractWrappers test bootJar --offline --no-daemon --console=plain
```

Result: BUILD SUCCESSFUL in 23s; 8 tasks, 5 executed and 3 up-to-date.
XML totals: 19 tests, 0 failures, 0 errors (14 Escrow API, 4 configuration,
1 shared-nonce concurrency). Main/test text scan found no 0x-prefixed
64-digit private-key-shaped literals. No README was created.

Tests use actual Spring MVC routing, Web3j ABI encoding/decoding and
locally signed requests against loopback HTTP JSON-RPC fixtures. The
fixtures supply synthetic receipts and logs; they do not execute an EVM
or provide formal on-chain acceptance evidence. No fixture hash should
be copied into the group's real transaction ledger.

T08 public-network acceptance remains pending: real funded backend account,
confirmed successful arbitration and an actually mined status-0 arbitration
receipt. No public-network transaction, commit, push or PR was made.

## Backend account preparation - 2026-11-07

The user created/selected the MetaMask account named 28-group backend BE
and supplied public address 0x3391215304d3F41892d973e70D18c5642068ae51.
Read-only RPC snapshot at https://rpc-testnet.potos.hk:
chain ID 0xecb8 (60600), native balance 0 wei, pending nonce 0.
The account needs testnet POT for gas before deployment. No private key
was requested/received and no transaction was sent in this check.
This is a dated snapshot, not a permanent statement of account balance.

## BE test-fund receipt - 2026-11-07

Read-only RPC confirmed funding transaction:
0xbceb77000304b313b7c244699e788a9e3adf8ccfece536624104e23e02dfc99c
From: 0x8c1f39320b2Dabf58eCeaD5225931674d558d3eD
To (BE): 0x3391215304d3F41892d973e70D18c5642068ae51
Value: 500000000000000 wei = 0.0005 test POT.
Block: 2831739; receipt status: 0x1; gasUsed: 21000.
Balance at query: 500000000000000 wei. This is wallet funding, not
an Escrow/SimpleStorage acceptance transaction. The hash was verified
from the block/RPC rather than relying on screenshot transcription.

Added a startup log of the public backend signer address so the user can
check BE identity after entering its private key locally. No key has been
received or configured by Codex. Actual service startup remains pending
the user's local hidden-input configuration.

## SimpleStorage deployment - 2026-11-07

The user started the packaged service locally with a hidden-input
WEB3J_PRIVATE_KEY. Startup output confirmed signer
0x3391215304d3f41892d973e70d18c5642068ae51 and port 8080.
The user then manually called POST /api/storage/deploy.
Returned contract: 0x3685e0FAbF6e3664C52a74771C3100E427405b47.

Read-only explorer lookup and RPC receipt/code checks verified:
transaction 0x28dce258f630b094a83cfe3970012da767ebd2e260cd994d3d13ededdb990360
from: 0x3391215304d3F41892d973e70D18c5642068ae51
to: null (contract creation)
contractAddress: 0x3685e0FAbF6e3664C52a74771C3100E427405b47
blockNumber: 2831833
status: 0x1
gasUsed: 4699 (the exact POTOS receipt value)
Code exists at this address; BE confirmed nonce was 1 at query.
Explorer: https://scan-testnet.potos.hk/tx/0x28dce258f630b094a83cfe3970012da767ebd2e260cd994d3d13ededdb990360

Deployment was submitted by the user's backend call. Codex performed
only read-only verification. set(100)/get() acceptance is still pending.

## SimpleStorage set/get acceptance and screenshot tracking - 2026-11-07

The user manually called POST /api/storage/value/set?value=100 and
GET /api/storage/value/get. Terminal output showed value as the string
100 for 0x3685e0FAbF6e3664C52a74771C3100E427405b47.
Read-only RPC independently verified set transaction
0x49f336782dda690729d7a024d3b70b95031a5f0e1df56763a0a710d90d7aacf4
from BE 0x3391215304d3F41892d973e70D18c5642068ae51 to that contract,
block 2831850, receipt status 0x1, gasUsed 27267. DataChanged event
data is 0x64 and a direct get() eth_call also returned 0x64 (100).

The original user terminal screenshot was saved unchanged at
docs/evidence/A/4A-3-terminal-set100.png. It is the terminal component
of 4A-3, not yet the complete terminal + explorer screenshot.

The user requested reminders before every required screenshot step.
Prior reminders were missed: 4A-1 properties, 4A-2 generated Java wrapper,
and 4A-4 service/controller code should be captured from the current
project. 4A-3 still needs the corresponding transaction explorer view.
4A-5 through 4A-8 belong to subsequent Escrow acceptance. Do not mark
these screenshots complete merely because the code exists.

## 4A-3 explorer screenshot received - 2026-11-07

Saved the user's original explorer screenshot unchanged as
docs/evidence/A/4A-3-browser-set100.png. It clearly shows the previously
verified set(100) hash, Success/set, block 2831850, BE sender, the
SimpleStorage recipient, gasUsed 27267 and gas limit 6721975.
Together with 4A-3-terminal-set100.png, the terminal and explorer source
materials for 4A-3 are collected. Final report placement is still pending.
No screenshots have been fabricated or combined. Next capture: 4A-1.

## 4A-1 screenshot received - 2026-11-07

Saved the original configuration screenshot unchanged at
docs/evidence/A/4A-1-properties.png. Visible fields: server port 8080,
POTOS RPC, ${WEB3J_PRIVATE_KEY} placeholder, gas price 1000000 wei,
gas limit 6721975 and chain ID 60600. No plaintext private key is shown.
4A-1 source screenshot is collected; report placement remains pending.
Next: collect 4A-2 generation output and generated wrapper code.

## 4A-2 generation success screenshot received - 2026-11-07

Saved original screenshot as docs/evidence/A/4A-2-build-success.png.
It shows BUILD SUCCESSFUL in 14s and 3 tasks executed. The generation
command and generated file list are outside the captured frame.
The file list and generated wrapper code still need source screenshots;
4A-2 is not marked complete yet.

## 4A-2 generated file-list screenshot received - 2026-11-07

Saved original screenshot unchanged as
docs/evidence/A/4A-2-generated-files.png. It includes the actual
Get-ChildItem command and paths for Escrow.java, IERC20.java and
SimpleStorage.java. Generated wrapper code screenshots remain pending.

## 4A-2 wrapper screenshots received - 2026-11-07

Saved three original screenshots unchanged under docs/evidence/A/:
4A-2-binary.png shows the BINARY declaration and beginning of bytecode;
4A-2-order.png shows Order extends DynamicStruct and all seven fields;
4A-2-event-decoder.png shows getOrderCreatedEventFromLog, the typed
response and decoding of indexed and non-indexed parameters.
Order and event-decoder images are usable. The BINARY screenshot has
word wrapping enabled and omits FUNC_ constants; collect a supplemental
constants view with wrapping disabled before completing 4A-2 materials.

## 4A-2 function-constant screenshot received - 2026-11-07

Saved original screenshot unchanged as
docs/evidence/A/4A-2-function-constants.png. It shows all nine FUNC_
constants and the beginning of the event declarations. Together with
the previously collected build success, file list, BINARY, Order and
event decoder screenshots, the source materials for 4A-2 are collected.
Final report selection/layout remains pending. Next: 4A-4 controller
routes and service event-list query code.

## 4A-4 controller and query screenshots received - 2026-11-07

Saved original images unchanged at docs/evidence/A/4A-4-controller.png
and 4A-4-service-log-query.png. Controller shows /api/escrow and all six
routes. Service image shows orders(), 10-block ranges, OrderCreated
topic filtering, ethGetLogs, response validation and removed-log handling.
The bottom of orders() is outside the frame; collect a supplemental view
of sorting, deduplication and getOrderCreatedEventFromLog before marking
4A-4 source materials collected.

## Service decoder screenshot and report-text checkpoint - 2026-11-07

Saved the original screenshot as docs/evidence/A/4A-4-service-log-decoder.png.
It completes the captured orders() method (sort, dedup, event decoding).
Rechecked the handout: 4A-4 asks for full Service/Controller code, so
selected key-code images alone are not marked as complete full-code
materials. Full source/report placement remains pending.

Created docs/report-route-A-answers.md with verified key data, actual
SimpleStorage receipts, explanations 4A-1/2/3/6, issue/AI-use draft, and
explicit pending fields for actual arbitration-dependent answers.
RPC counts were corrected for this implementation: orders() includes
chain ID + head queries, hence 2 + ceil(H/10), with no cache yet.

A previous 4A-3 collection-complete note was premature: the terminal
image contains set/get only. The handout asks for deploy/set/get responses;
the original deploy response screenshot must still be captured from
terminal history. No new deployment is needed merely to capture it.

## Complete 4A-3 terminal screenshot received - 2026-11-07

Saved the original image unchanged as
docs/evidence/A/4A-3-terminal-complete.png. It shows all three actual
HTTP commands and responses: deploy returns the confirmed SimpleStorage
address, set(100) returns status 0x1/block 2831850/gasUsed 27267 and the
verified hash, get returns the string 100 for the same address.
Together with 4A-3-browser-set100.png, source materials for 4A-3 are now
collected. The earlier missing-deploy-response item is resolved.
No repeat deployment or transaction was needed to obtain this screenshot.
Next live operation: user submits A-route Escrow deploy and captures
its POST response and GET /api/escrow/address response under 4A-5.

## A_BASE deployment and historical role lookup - 2026-11-07

User manually deployed A_BASE at 0x793D08836c2e00Af0BdE963386f36e00acEe180f. Deployment tx 0x31cccc6d5e12d955b00e2e48ce5ec173ea9b6f420bb568a5248b9cbe63db27e6, block 2832142,
status 0x1, gasUsed 103312, from BE 0x3391215304d3F41892d973e70D18c5642068ae51, to null.
Read-only RPC confirmed receipt, runtime exact match to group compiler artifact,
and BE as constructor arbiter. Original screenshot saved unchanged as
docs/evidence/A/4A-5-backend-deploy.png. GET /address null transaction fields
are expected: address query itself creates no transaction. The Remix half
of 4A-5 has not yet been performed/captured.

The user asked to find buy/sell addresses from experiment 3. Examined
24-network-engineering Su Jiahao final experiment-3 report and the older
partial draft. The older draft has incomplete/swapped name-role labels;
used the final report plus chain data instead. Final report associates
Yang Shenjie with 0x92b0f84D625437245c3296012A0784d10C560309, and Wu Yufei's original Escrow with
0xbdB2Dfcd8A9Cd903848a1B9d95416b85178f05Af. That original contract's
creation account and WYF token owner are both 0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6. Its actual order 0
returns buyer=0x898Ed6F95ef783aD4292D5fF2351f2DC7936ADF6, seller=0x92b0f84D625437245c3296012A0784d10C560309, token=0x81777CEeA8CC59fa88ABcee9DC19970217Cf8637, amount=30 tokens.
Thus the role mapping matches the accepted project plan.

Token symbol verified as WYF202492451119, decimals=18; buyer balance
70 tokens at query. Both participants have native test POT. Role holders
still need to perform their own current-wallet transactions; no member
interaction was claimed as already completed or sent by Codex.


## Buyer five-transaction materials verified - 2026-11-07

Archived the original peer Markdown note and 17 PNG files unchanged under docs/evidence/A/buyer-materials/. All 18 copied files were SHA-256 compared with the source. Originals remain untouched.

Read-only verification snapshot: 2026-11-07T23:32:34.467506+08:00, chain 60600, head 2835361. Raw public transactions and receipts are retained in chain-verification.json.

create A-NORMAL: 0x944e176605765a331256582d6099b52a0303e50884f2c3b4d2e9e7d1bca18b02, block 2835064, status 0x1, gasUsed 80548.
create A-DISPUTE: 0x191cf6168604617bc7e0f0c95ed42a2360319698f996d91e7a6d3006e0492d7c, block 2835118, status 0x1, gasUsed 80548.
approve 60 WYF: 0x26b5a208e24b0911f2dbb01b45ebb0edd38e5bc4f77bb8d3e84a45c47bd6a406, block 2835186, status 0x1, gasUsed 28423.
deposit 0: 0x8f52d16a4f28f4931b46727a4915635a44db741cc38682c485399dbd0c54f57d, block 2835211, status 0x1, gasUsed 70217.
deposit 1: 0x4144b15dc3641391b7a051566189aa8fd971ac1c7c89b24ca45b85ea4b5f9c80, block 2835232, status 0x1, gasUsed 70217.

Validated transaction senders/targets/calldata and OrderCreated, Approval, Transfer, OrderPaid logs. IDs 0 and 1 are both Paid with 30 WYF each, correct buyer/seller/token, deadline 1794070422. Escrow holds 60 WYF; remaining buyer allowance is zero. Remix decoded logs=[] does not negate verified raw events.

Updated report-route-A-answers.md with the five real transaction records and dated state/balance snapshot. Preparation mint50 lacks a hash and remains unverified auxiliary evidence. Seller dispute(1), 4A-6/7/8 and arbitration-dependent written answers remain pending. Normal order 0 must remain Paid for the planned invalid-state arbitration check before buyer confirmation. No chain transaction was signed or sent by Codex.


## Seller dispute(1) verified - 2026-11-08

User supplied hash 0x0882e460b0b76906dd347b7c1ffb3a4808b842cea5d7285d049668f50af55670. Independent read-only RPC matched the sole dispute(1) from seller to A Escrow in block 2841098: status 0x1, gasUsed 36389; verified OrderDisputed(1, seller) event. Current state snapshot 2026-11-08T11:15:00.977342+08:00: order 0 Paid, order 1 Disputed, each 30 WYF. No chain write was sent by Codex.

Copied both original screenshot files unchanged and hash-compared source/destination under docs/evidence/A/: 4A-5-seller-dispute-receipt.png and 4A-5-seller-order1-disputed.png. Full public RPC evidence retained in seller-dispute-verification.json. Updated report draft with real dispute record; backend GET screenshots, failed-state arbitration and refund remain pending. Local GET /api/escrow/address returned connection refused (8080 backend not running); user needs to restart service and reload existing A contract, not redeploy.


## Reload HTTP 400 fixed and verified - 2026-11-08

The running service returned INVALID_PARAMETER / fromBlock must be the contract deployment block for the verified A contract and deployment block 2832142. Independent RPC checks confirmed its original successful creation receipt still reports 2832142, while eth_getCode(latest), eth_getCode(2832142), eth_getCode(2832141), and eth_getCode(0) all return the identical 9496-character code string (SHA256 0e72b7daf380726f179b50634c15c63548745d24a3119683d59f818e85f56a09). This endpoint therefore cannot establish deployment by the first historical non-empty code.

Added a local regression reproducing HTTP 400 before the fix, then changed load verification to inspect full transactions in the requested block and require a matching top-level creation receipt: successful status, contract address, transaction hash, block number and block hash, with null creation destination. Latest code and Escrow interface checks remain. Unsupported factory-created contracts are not silently accepted. Invalid loads preserve the previous context.

Full offline test bootJar build succeeded: 22 tests, zero failures/errors (17 API, 4 config, 1 nonce). New cases cover historical code returning latest, wrong earlier/later blocks, and mismatched creation-receipt block. A fresh temporary process on 18080 with an ephemeral random signer made only read calls: correct load returned 200, order 0 Paid, order 1 Disputed; wrong block 2832141 returned 400; correct context preserved. Public results saved in A/load-fix-live-verification.json. Temporary process terminated; user's 8080 service and secret were not read or stopped. User must restart their existing process to load the rebuilt JAR. No new chain transaction or report screenshot was created by this verification.


## 4A-6 captured and real failed resolve verified - 2026-11-08

Archived the three original GET screenshots and the failed-resolve HTTP screenshot unchanged, SHA256 compared. Orders list has 0 and 1, detail 0 Paid/A-NORMAL, detail 1 Disputed/A-DISPUTE. No Word report edited.

User manually submitted resolve(0,true). Verified 0x63f9f67706ab2a51fa2a157288cde58879707e7fba77d430d083d718ec1f304e, block 2842225, status 0x0, gasUsed 25889, BE sender, A Escrow target, empty logs, and unchanged order states. Full public receipt/transaction saved in A/failed-resolve-verification.json. HTTP status 422/TRANSACTION_REVERTED; actual revertReason empty, not a decoded wrong-status string. gasPrice 1000000 wei, receipt effectiveGasPrice 0; report must distinguish consumed execution gas from an actual charged fee. Completed draft explanation 4A-4 using those exact facts. Success refund and explanation 4A-5 remain pending. Codex sent no chain transaction.


## Successful resolve(1,false) verified - 2026-11-08

User manually submitted refund arbitration. Verified 0xa161d794996043964f33938e2d813ca238f2a996c3ece95c8e67134e3899f813, block 2842245, status 0x1, gasUsed 62917, From BE and To A Escrow. Verified input resolve(1,false), WYF Transfer from Escrow to buyer for 30*10^18 units, and OrderResolved(1,false). Latest orders: 0 Paid, 1 Refunded. Full public receipt/transaction stored in A/successful-refund-verification.json. User original success screenshot copied unchanged and SHA256 compared as 4A-7-successful-refund.png. Completed draft explanation 4A-5 with actual source delegation lines. Refunded HTTP screenshot and browser From/Transfer screenshot remain pending. No Word report edited and no chain write sent by Codex.


## Refunded HTTP result screenshot received - 2026-11-08

Original screenshot copied unchanged and SHA256 compared as docs/evidence/A/4A-7-order1-refunded.png. Shows GET orders/1, id 1, amount 30*10^18, status Refunded, description A-DISPUTE, correct buyer/seller/token. Three 4A-7 source screenshots now collected; 4A-8 browser materials still pending. User places images in Word; no Word edits made.


## 4A-8 browser screenshots received - 2026-11-08

Archived both originals unchanged and SHA256 compared: 4A-8-refund-browser-details.png and 4A-8-refund-transfer-log.png. Details confirms the previously verified successful refund transaction, BE From, Escrow target, block 2842245, gasUsed 62917 and 30 WYF token transfer. Logs decodes Transfer with Escrow sender, buyer recipient, 30*10^18 units. 4A-8 source materials collected; explanations 4A-4/5/6 available in report draft and to be given for user insertion. No Word report changed. Normal order 0 buyer confirm still pending.


## Buyer confirm(0) and final business states verified - 2026-11-08

User provided 0x96cbe681312bf74aadf80a7606f3ca93e2c5fbb564452eb88b853dab9b463d73. Verified successful confirm(0) from buyer to A Escrow, block 2843225, gasUsed62549; Transfer30 WYF from Escrow to seller and OrderCompleted(0). Snapshot 2026-11-08T15:31:32.745828+08:00: order0 Completed, order1 Refunded; buyer30/seller60/Escrow0 WYF. Seller had30 before this payment. Full public records retained in A/buyer-confirm-verification.json.

Both original images copied unchanged and SHA256 compared: A-supplement-confirm0-receipt.png and A-supplement-order0-query-cropped.png. Receipt is suitable supplemental evidence. The query image is cropped before status; do not claim it visually proves Completed. User can capture a local backend GET /orders/0 instead of asking buyer to repeat any write. No Word edits and no chain write by Codex.


## Complete 4A-4 report screenshots reviewed - 2026-11-08

Read-only audit of report saved at 17:03:37, SHA256 dff195f239e357a98f73a9025cf2032008736c816c344e54ecf9bdbec7fa291d. Extracted 12 original embedded PNGs and visually inspected each: EscrowService 10 images cover lines1-295 continuously, EscrowController 2 images cover entire file and all six routes. Latest verifyDeploymentBlock code present. Exported read-only report snapshot with native Word (packaged renderer unavailable: LibreOffice missing), rendered and visually inspected report pages10-14; no missing segment, clipping or overflow found. Scope is 4A-4, not a full report QA. User report SHA256 unchanged. Copies of embedded originals and range audit retained under docs/evidence/A/4A4-full-source/. No new screenshot request needed.
