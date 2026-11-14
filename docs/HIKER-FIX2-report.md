# HIKER-FIX2 运行时七项修复报告

- 日期：2026-11-14
- 依据：`~/workspace/projects/xbox-hiker/notes/sim-report.md`（主判定 3/51 通播放、29 条失败为我方 bug；fixed 对照证明修好后 +7 通播放、24 条 home 恢复）
- 改动范围：仅 `app/src/main/assets/hiker/hiker_engine.js`、`app/src/main/assets/hiker/hiker_ui.js`，自测扩充 `docs/hiker-js-selftest.js`。未动 Java/Manifest/包名版本，未提交（主代理统一提交）。
- 验证：`node --check` 全部 assets JS 通过；selftest **39 passed / 0 failed**（原 26 条不回归 + 新增 13 条）；另用真实规则数据离线冒烟（见文末）。

## BUG-1 fyclass/fypage 模板零处理（主因，25 条）

- 改法（`hiker_ui.js`）：新增 `classUrls()/firstClassUrl()/substTpl(url, page)`（:39–58）：`fyclass`→当前分类 class_url（默认首个，class_url 支持 `&` 分隔字符串与数组两种形态）、`fypage`→页码，替换用函数式 replacer 防 `$` 转义；无占位 URL 原样返回，老规则零影响。`UI.substTpl/UI.classUrls` 已导出供自测。
- 落点：
  - `openSite`（:252）：进站 `STATE.curClass = firstClassUrl()`，`HK.setPage` 的 url 先替换（页码 1）。
  - `runPage`（:283）：执行前把 `vars.MY_URL` 按 `vars.MY_PAGE` 替换出 execVars 再跑；分页上下文 `baseVars` 改存**未替换**的原始 vars（:311）。
  - `loadNextPage`（:330）：翻页时以新页码对原始模板重新替换，fyclass 保持当前分类。
  - `navigate`（:838）：目标 URL 形如 `base##<分类>##<页码>` 且分类段为具体值时更新 `STATE.curClass`（点分类条目即此路径），随后统一走替换、页码归 1。
  - `refreshCurrent` 的 setPage 同步替换。
- engine 侧核查：全 engine grep `fyclass|fypage` 原本 0 处，engine 内没有从规则模板拼页面 URL 的其它路径（hkFetch 的 `hiker://page/` 是模块源码通道，不经模板），故落点只在 UI 导航层，与任务书一致。

## BUG-2 encodeLazy 带参不加括号

- 改法（`hiker_engine.js:1492` `encodeLazy`）：带参时返回 `'(' + src + ')(' + args + ')'`，箭头函数与普通函数统一按「调用」语义编码；无参时保持原样（仍是表达式本体）。

## BUG-3 $().rule(fn) 裸函数从不被调用

- 改法（`hiker_engine.js:1756` `BARE_FN_RE`、:1759 `runInline`）：runInline 表达式求值后，若结果是函数**且**源码形态是裸函数（`function…(...)`、箭头 `(...)=>`/`x=>`、可带前导括号/async），立即以当前 `input` 为参数调用，用其返回值与 setResult 作结果；调用抛错直接报失败，不再落进语句形态兜底掩盖错误。普通表达式（如 `1+2`）有自测保证不被误调用。
- UI 侧无需改：handleItem 的 @rule/@lazyRule 路径本就走 runInline，HoHoJ 卡片 `$('...#noHistory#').rule(() => { $.require('hiker://page/HohojCore').detailParse(); })` 现在能真正执行函数体。

## BUG-4 条目 extra 不传 MY_PARAMS

- 改法（`hiker_ui.js:838` `navigate(url, extra)`）：第二参数为被点条目 `it.extra`，与目标 URL 的查询参数合并（extra 优先）后注入 `vars.MY_PARAMS`；调用点已接：handleItem 的 lazy 返回跳转、站内 `hiker://` 跳转、普通 http 详情跳转、select 回调跳转。
- engine 配套（`hiker_engine.js:1006` `getParam`）：未显式传 URL 时先查 `PAGE.params`（MY_PARAMS），查不到再回落解析页面 URL 查询串——影探4K 详情规则用 `getParam('id')` 读 `extra.id=vod_id` 的路径就此打通。

## BUG-5 动态模块 require 解析缺失

- 定位证据（本地规则包实测）：HoHoJ 预处理（page `HohojLocalPreprocessing`）把核心源码写入 `hiker://files/rules/HoHoJ/HoHoJ.js` 并产出卡片，卡片回调以 `$.require('hiker://page/HohojCore')` 引用；pages[] 里只有 HohojLocal* 五个模块，没有 HohojCore。
- 改法（`hiker_engine.js:1585` `requireModule` + :1561 `findStoredModule`）：
  1. 保留 `origName`（未剥前缀的原始 name），回退判断改用 origName——原实现用已剥 `hiker://page/` 前缀的 key 判断 `hiker://` 开头，永远不成立（任务书指出的回退判断 bug）。
  2. 回退链：(a) origName 是 http(s) URL → 远程加载；是 `hiker://files/…` 或 `.js` 结尾 → fileRead 加载（内容非空才 runModule，不再把空内容缓存成 null 堵死后续重试）；(b) `findStoredModule`：在 STORE.files 里按 basename 归一化（小写去非字母数字）比对——全等或互为前缀（前缀匹配要求主名 ≥3 字符防误命中，多命中取主名最长），`HohojCore ↔ HoHoJ.js` 即走此路；(c) 构造 `hiker://files/rules/<规则名>/<name>.js` 再试 fileRead（覆盖只落桥盘、内存表没有的情况）。
  3. 全部落空才报「模块不存在」。

## BUG-6 缺失全局与环境对象

- config（聚阅）：`hiker_engine.js:334` STORE 新增 `config` 并纳入 persistSave/persistLoad（localStorage `hk_config_v1`）；:1267 `sc.config = STORE.config`（本体引用，读无值即 undefined 不报错），:1268 `sc.initConfig(obj)` 按官方语义合并写入并立即落盘——聚阅 preRule 的 `initConfig({聚阅: srcHome})` 与 find_rule 的 `if (config.聚阅) { require(config.聚阅); yiji(); }` 就此闭环。
- sleep（DrpyHiker）：先定位再补——sim 错误原文 `TypeError: Cannot read properties of undefined (reading 'sleep')`；查 DrpyHiker data.zip，home 入口 `subpage/main.js`（find_rule 以 `eval(request('hiker://files/data/DrpyHiker/subpage/main.js'))` 执行）第 67 行倒计时循环调的是 **`java.lang.Thread.sleep(1000)`**，而 JAVA_SHIM.lang 没有 Thread，`java.lang.Thread` 为 undefined、读 `.sleep` 即此错。针对性补（`hiker_engine.js:1295`）：`JAVA_SHIM.lang.Thread.sleep(ms)` 同步忙等桩，单次上限 5s 防长睡冻死 UI 线程；`Packages.java.lang.Thread` 因共用同一 JAVA_SHIM 自动同步可用。没有乱撒全局 sleep。

## BUG-7 lazy/模块执行作用域不共享同页定义

- 改法（`hiker_engine.js`）：「收割 + 注入」等价官方的同规则共享全局作用域：
  - :335 STORE 新增 `shared`（按规则名隔离的共享袋，仅会话内有效、不落盘）。
  - :1673 `declNames` 静态扫源码顶层声明名（`function name(`、每条 `var/let/const` 语句首名）；:1682 `capCall` 生成收尾片段，在 `run`（页面规则）、`runModule`（require 模块）、`runInline` 语句形态的 with 块末尾把这些名字的值经 `__cap` 回调（:1690 `makeCap`）存入共享袋（undefined 不覆盖）。
  - `makeScope`（:1254）在建作用域时把共享袋注入——只填作用域里还没有的名字，内置 API 与 extraVars 优先。
- 效果：中国X站 lazy 可见 detail 同页 `jGet`；CMS模板远程 cms.js 的顶层 `version` 等定义可跨 require 共享（其 `initConfig` 调用则由 BUG-6 的真全局接住）。收割的是闭包本体，其捕获的原作用域对象仍被引用，`MY_URL` 等 live getter 读的是同一 PAGE，行为与官方一致。

## 自测结果

`node docs/hiker-js-selftest.js`：**39 passed, 0 failed**。新增 13 条：
- BUG-1 ×3：substTpl 默认首分类+第 1 页、当前分类+翻页、无占位 URL 原样。
- BUG-2 ×2：lazyRule 箭头带参生成 `((arg)=>{...})("X")`、且可被 runInline 执行出 `got:X`。
- BUG-3 ×3：裸箭头、裸 function 均被调用；普通表达式不误调用。
- BUG-4 ×1：MY_PARAMS 注入后 `getParam('id')` 与 `MY_PARAMS.id` 双通道读到。
- BUG-5 ×1：落盘 `HoHoJT.js` 后 `$.require('hiker://page/HoHoJTCore')` 别名解析成功。
- BUG-6 ×2：initConfig/config 往返与无值 undefined；`typeof java.lang.Thread.sleep === 'function'`。
- BUG-7 ×1：页面 run 定义的 `jGet`/`var` 在后续 runInline 可见。

真实规则离线冒烟（Node 同链加载，网络桩返空）：
- 九色（41 站合集）find_rule 以替换后 URL `hiker://empty##v-latest##1` 执行 ok（结果 0 条系桩网络返空，非运行时错误）；其后 runInline 调 `jGet(...)` 不再 ReferenceError。
- HoHoJ preRule 执行 ok 且核心已落盘 STORE.files；`$.require('hiker://page/HohojCore')` 解析成功，`typeof exports.detailParse === 'function'`——BUG-3+BUG-5 链路在真规则上闭环。

## 仍存疑处

1. **BUG-1 的 UI 全链路**（openSite→分类点击→翻页）只做了 substTpl 纯函数自测与代码级核对，未在真 WebView/模拟器里点验；sim 的 harness 是复刻 UI 语义的，主代理回归 sim 时需把其 UI 复刻同步到新语义（替换发生在 runPage/openSite/navigate/loadNextPage 四处）才能复现通过率提升。
2. **BUG-7 收割局限**：只收割每条 var/let/const 语句的首个声明名（逗号连排的后续名漏收）；页面代码顶层 `return` 提前返回时收割片段执行不到；同名定义后写覆盖先写（与官方全局后定义覆盖一致）。已知语料的 jGet/version 均为独立声明，不受影响。
3. **BUG-5 前缀匹配是启发式**：`HohojCore↔HoHoJ` 靠「请求名以文件主名开头」命中；若某规则 files 目录下有多个主名互为前缀的文件，取最长主名，但仍可能与作者意图不符——属无 pages 声明时的兜底，命中后模块代码本身会自校验（如 HoHoJ 检查 baseParse）。
4. **DrpyHiker 更深层缺口不在本轮清单**：其代码还有 `$.require('./subpage/ConfigManager.js')` 这类相对路径 require 与 `$.require('updateRecords')` 这类裸名 require，现回退链对相对路径仍无能为力（fileRead 相对路径读不到包内 data 文件）；Thread.sleep 忙等在真机 WebView 主线程上会短暂卡 UI（倒计时 1s×N 次），与官方 Rhino 后台线程语义有别。两者建议列入下一轮。
5. **BUG-6 config 为全规则共享一个对象**（键即规则标题，如 `聚阅`），与语料用法一致；若未来出现两条规则用同一裸键（如都写 `config.url`）会互相覆盖，届时再按规则名分仓。
