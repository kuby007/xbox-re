# 海阔运行时第三轮修复报告（FIX3：BUG-8 / BUG-9 / BUG-7 残留 / BUG-5 残留）

- 日期：2026-11-14
- 范围：只改 `app/src/main/assets/hiker/` 的 JS（`hiker_ui.js`、`hiker_engine.js`），自测改 `docs/hiker-js-selftest.js`。未动 Java/Manifest/包名版本，未动 git。
- 证据基线：`~/workspace/projects/xbox-hiker/notes/sim-report2.md`（第二轮复跑，真实 UI 驱动）+ `sim/results2.json` 的 19 条 BUG-8 探针值。

## 总览

| 项 | 结论 | 验证 |
|---|---|---|
| BUG-8 lazy 返回播放地址被当页面打开（19 条） | ✅ 已修 | 自测 5 项分支断言 + 真实规则探针 2/2 命中（黑桃、新黄果由 fail 转 pass） |
| BUG-9 聚阅 lazy 作用域缺 MY_INDEX | ✅ 已修 | 自测引擎层 2 项 + UI 层下标注入 1 项 |
| BUG-7 残留 CMS 跨模块顶层定义不共享 | ✅ 已修（同次执行可见性缺口） | 自测「require 后同次执行内读模块顶层 var」1 项 |
| BUG-5 残留 DrpyHiker 裸名 require 返回 null | ✅ 解析链已补（见残留说明：该规则整链另有拦截点） | 自测探盘路径 1 项（严格路径桩） |

- 自测：`node docs/hiker-js-selftest.js` → **49 passed, 0 failed**（FIX2 的 41 项无回归 + FIX3 新增 8 项）。
- 语法：assets/hiker 下全部 10 个 JS `node --check` 通过。

## BUG-8（主项）：lazy 返回 http(s) 播放地址串走播放出口

**改法**（`hiker_ui.js`）：
- 新增 `playLazyString(s, it)`（L756–778）：lazy 结果字符串的播放分发器。
- `handleItem` 的 lazy/@rule 分支（L844）：拿到返回串 `back` 后、调用 `navigate()` 之前先过 `playLazyString`，命中即 `HK.play(...)` 返回，不再当页面打开。
- `execSelectCallback`（L732–733）：select 回调返回串同路分发（与 lazy 同语义）。

**判别规则**（命中才走播放，缺一不可）：
1. trim 后以 `http://` 或 `https://` 开头；
2. 串内不含 `@lazyRule` / `@rule=`，且 `HK.parseUrl` 拆不出 lazy/rule——带规则尾巴的 http 串是「页面地址+待执行规则」的页面型跳转，仍走 navigate；
3. `hiker://` 串不匹配 http 前缀，自然走原有 scheme/navigate 路径；
4. 命中后走既有 `HK.play`（内部 `parsePlayTarget` 照常拆 `#标记#` flag 与 `;{K@V}` 请求头，并经 bridge.play + report('play') 全 JSON 出口汇报，与第一轮播放出口契约一致，未另起通道）。

**与 19 条探针的对表**：探针值绝大多数以 `/index.m3u8`、`.m3u8`、`.mp4` 结尾；两条无扩展名（Porn吃瓜网的长 hex 路径、Javtiful 的 `/media/video/…720p?signature=…`）——纯扩展名判别会漏掉这 2 条，故按任务口径采用「http(s) 开头 + 非规则跳转串」判别，不做扩展名白名单。

**可能误伤面（如实列明）**：
- detail 阶段 lazy 返回 http **页面**地址、期望 UI 再拿详情规则解析的规则（如个别 CMS 网页型二级解析），返回串会被送进播放器而非详情规则。本语料 36 条复跑范围内未见此形态（CMS模板的选集 lazy 返回的是最终播放地址）；官方海阔对 lazy 返回 http 串的语义本身即「条目真实地址」（由播放器/嗅探处理），与本次判别一致。
- lazy 返回 `hiker://files/…` 本地缓存路径（中国X站精选、鉴黄师的双重前缀形态）不在本项范围：不匹配 http 前缀，仍走 navigate，维持第二轮的存疑定性，需另立「本地文件播放通道 + 路径去重」议题。

## BUG-9：MY_INDEX 注入 lazy 作用域

**改法**：
- engine（`hiker_engine.js`）：`PAGE` 加 `index` 字段（L372）；`makeScope` 的 liveProp 区与 MY_PAGE 同层加 `MY_INDEX`（L1173–1177，绑定 PAGE.index，未知默认 0）；`applyVarsToPage` 认 `MY_INDEX`（L1762）。
- UI（`hiker_ui.js`）：新增 `itemIndex(it)`（L745–754）——在页面栈里按对象 identity 自顶向下找条目下标，找不到回 0；`handleItem` 的 lazy 分支（L836）、input 回调分支（L809）、`execSelectCallback`（L722）三处 lazy 执行入口统一注入 `MY_INDEX: itemIndex(it)`。
- 聚阅的实际触发点正是 select 回调路径（其首个 lazy 为 `__hkSelectExec("s1")`，第二轮探针实证），已覆盖。

## BUG-7 残留（CMS模板）：跨 require 模块顶层定义的「同次执行可见性」

**定位（实抓远程文件核对，非推测）**：
- CMS模板 find_rule `require('https://dr.playdreamer.cn/js/cms.js')` 生成「♻检测升级」条目，其 lazy 内先 `require(资源网预处理.js)`（走 initConfig 写 config.模板），再 `require(config['模板'])` 即 zyw.js——zyw.js 第 1 行 `var version={...}` 定义版本对象，lazy **在同一次执行内、require 返回后立刻**读 `version.requireId`。
- FIX2 的「收割+注入」只在作用域**构建时**注入；该 lazy 的作用域在 require 之前已建好，模块执行末尾收割进 `STORE.shared` 也赶不上同次读取——这是收割时机/注入点的时序缺口，与报告二轮定性一致。

**补强（不推翻收割+注入机制）**（`hiker_engine.js` L1721–1749）：
- `makeCap` 收割时除写入 `STORE.shared[ruleName]` 外，新增 `mirrorSharedToGlobal`：把名字同步镜像到全局对象。`with(作用域)` 查不到的名字会动态落到全局对象，require 返回后的同次读取即可见；后续执行的注入（作用域内同名）仍优先于全局，原有语义不变。
- 护栏：只写「全局上原本没有」或「已由本机制放置过（SHARED_PLACED 登记）」的名字，绝不覆盖 window 既有属性（fetch/document/name 等——这类同名定义同次读取仍会落空，属已知取舍）；`__scope/__cap` 排除。

**自测**：页模块 `var version={ver:'9.9'}` 被 require 后，同次执行内 `version.ver` 读到 '9.9'（修复前为 ReferenceError）。

## BUG-5 残留（DrpyHiker）：裸名 require 解析

**改法**（`hiker_engine.js`）：
- `makeDollar` 的 `$.require/$.requireMem` 把本模块 key 作为 `fromKey` 传入 `requireModule(name, param, fromKey)`（L1554–1559）。
- requireModule 回退链（L1636 起）在 `findStoredModule`（内存表）之后，把原来单一的 `rules/<规则名>/<name>.js` 猜测扩成**裸名探盘序列**（逐个 `fileRead`，命中即 `runModule`）：① 调用方模块所在目录（fromKey 为 hiker://files/ 路径时）+ `<name>.js`/`<name>`；② `hiker://files/data/<规则名>/subpage/<name>.js` 与无后缀形（DrpyHiker 实测布局：`data/DrpyHiker/subpage/updateRecords.js`，与调用方 main.js/settings.js 同目录）；③ `data/<规则名>/…`；④ `rules/<规则名>/<name>.js`（原有猜测保留）；⑤ `hiker://files/<name>.js`、`<name>`。
- 依据：Java 侧 `HikerApi.readFile` 按 safeName（basename）拍平读回、包内 data.zip 按相对路径落盘，探盘路径与落盘布局对齐；探盘只发生在 pages/内存都未命中的回退段，命中后走模块缓存，无重复 IO 顾虑。

**自测**：以严格路径桩（桥 readFile 只认精确路径、无模糊 basename 兜底）验证 `$.require("updateRecords")` 探中 `data/DrpyT/subpage/updateRecords.js` 并拿到 exports。

**残留说明（重要，勿误读为已全通）**：复核第二轮原始数据后确认，DrpyHiker 当前整链的**直接拦截点其实不是裸名 require**：其详情链里 `$.require("hiker://files/data/DrpyHiker/libs/hikerPop.js")` 的模块执行先报 `com is not defined` 返回 null，随后 `.updateRecordsBottom(...)` 读 null 抛 TypeError——表达式求值顺序下 `$.require("updateRecords")` 在该链中根本未被到达（二轮探针的报错截断为 'updateRe…' 实为 'updateRecordsBottom'）。`com` 是 Rhino 的 Java 包根（hikerPop.js 头部 `const XPopup = com.lxj.xpopup.XPopup;`、`com.example.hikerview.…` 等原生类），属 Java 互操作层缺口，不在本轮 JS 4 项范围内。本次补的是裸名解析链本身（已独立验证可达），DrpyHiker 要整链跑通还需另议 `com.*` 原生类桩/桥接方案。

## 离线探针（sim/run2.js 的 --worker 模式，自备 spec、结果落 /tmp，未改 sim 目录）

驱动真实 HikerUI 全链（打开站点→点条目→详情→点选集），网络为只读请求、未下载任何媒体内容：

| 规则 | 第二轮 | 本轮 | 播放地址（形态） |
|---|---|---|---|
| 黑桃短剧 | fail（探针 m3u8 被 navigate） | ✅ **pass**（home=30/detail=5，playVia=bridge.play） | `https://play.djqiepian.app/vod/…/ep-000001-…/index.m3u8` |
| 新黄果短剧 | fail（口径纠正后被 BUG-8 挡住） | ✅ **pass**（home=3/detail=7，playVia=bridge.play） | `https://aimove.click/api/media/…/index.m3u8` |
| 影探4K（口令） | fail（C 类存疑） | fail（home=68/detail=68，选集未出播放地址） | — |

影探4K 说明：追加 probe 模式复测，其详情链首个 lazy 条目返回值仍为 `hiker://empty`（valueForm=not-http），与第二轮探针一致——**BUG-8 的前置条件（选集 lazy 返回 http 播放串）在这条规则上未成立**，它不是 19 条 BUG-8 规则之一；其选集结构需按详情 68 条逐项深挖才能定性（第二轮已定为存疑 C 类），不在本轮 4 项的证据范围内，不冒充已修。

## 改动文件清单

- `app/src/main/assets/hiker/hiker_ui.js`：`itemIndex`（L745）、`playLazyString`（L767）；handleItem lazy 分支（L836/L844）、input 分支（L809）、execSelectCallback（L722/L732）接线。
- `app/src/main/assets/hiker/hiker_engine.js`：MY_INDEX 三处（L372/L1177/L1762）；requireModule 裸名探盘 + fromKey（L1554–1559、L1592、L1636 起）；makeCap 全局镜像（L1721–1749）。
- `docs/hiker-js-selftest.js`：FIX3 用例段（L262 起，共新增 8 项断言），收尾改为等异步 UI 用例完成后汇总退出。
