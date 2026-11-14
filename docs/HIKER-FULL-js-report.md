# HIKER-FULL · JS 运行时实施报告（方案 A 全量）

- 范围：仅 `app/src/main/assets/hiker/**`（hiker_engine.js、hiker_ui.js、index.html 三个文件）+ 本报告 + `docs/hiker-js-selftest.js`。未改 Java / Manifest / 包名版本 / git。
- 桥契约以 `docs/HIKER-FULL-hiker-full-compat.md` 为准；编号对照 `notes/audit-api-surface.md`。
- 仓库 AGENTS.md 要求 task_guard + 提交打 tag：本快照无 `.codex`（guard 不可用），且本子任务明确「勿动 git」，故未执行提交/打 tag，留父任务统一收口。签名 v2-only 等构建规则属构建阶段，未触及。

## 改动文件

| 文件 | 改动 |
|---|---|
| `assets/hiker/index.html` | 加载链改为 bridge_adapter → runtime_shim → api_surface → scheme → compat → engine → import → ui（带注释说明 jst4/hiker_app 不加载的原因）；新增 CSS（.hscroll 单行横滑、.spacer 三档、.media-row、.marquee、方图、选择弹层、more-btn）；导入弹层加密码框 + 选择文件按钮 + 隐藏 file input；新增选择弹层 markup |
| `assets/hiker/hiker_engine.js` | 见下逐项 |
| `assets/hiker/hiker_ui.js` | 见下逐项 |
| `docs/hiker-js-selftest.js` | 新增离线自测（不进 assets） |

## 逐项状态

### 1. 加载链 [P0-2]
- ✅ 五个契约/适配文件在 engine 之前按依赖序加载；shim/compat **只加载不 install**（其 install 会用桩覆盖真实现）；api_surface 由 engine 在 makeScope 里「缺失才填」（delegate 优先，engine 真实现不被覆盖），全局层由 `HK.installGlobals()` 在 UI boot 时安装，之后 engine 再断言自己的 CryptoJS/getCryptoJS/joinUrl/getResCode，最后 scheme 包装全局 fetch。
- ✅ `hiker_jst4.js` 不进 WebView（QuickJS 专用，会同步化 setTimeout）；`hiker_app.js` 不加载——它自带另一套站点模型（key/api/ext/categories），与 hiker_ui 的 STATE 模型双头并存会状态分叉且无调用方。两者原因已写进 index.html 注释。
- ✅ boot 不破坏：自测 boot() 通过。

### 2. P0
- **P0-3 CryptoJS/getCryptoJS ✅**：engine 内置兼容子集——WordArray、enc.Hex/Utf8/Latin1/Base64、MD5（复用 engine）、SHA1/SHA256/HmacMD5/HmacSHA1/HmacSHA256 为纯 JS 真实现（已知向量自测全过），AES(CBC/ECB+Pkcs7) 走桥 `aesEncode/aesDecode`。作用域直给 `CryptoJS` 对象；`getCryptoJS()` 按官方语义返回源码串 `'var CryptoJS = HikerEngine.cryptoObject;'`，`eval(getCryptoJS())` 路径自测通过。边界：AES 密钥/IV 以 Latin1 字节串经 Java String 传桥，ASCII 密钥精确，非 ASCII 字节密钥受 Java String 语义限制（已知边界，报告明示）。
- **P0-4 Cookie ⚠️（JS 侧完成，依赖 Java fetchFull）**：作用域 `getCookie/setCookie` 走桥契约（HikerCookies 落盘、与 HikerHttp 共用）；带 withHeaders 的 fetch 走 fetchFull，响应头/状态码按官方 wrapFetch 形返回——cookie 的自动带/存发生在 Java 的 fetchFull/HikerHttp 链路里。**普通 fetch（无 withHeaders）仍走既有 req 路径**，其 cookie 是否与 HikerCookies 共用取决于 Java 侧 HikerHttp 的实现（并行 Java 任务范围），JS 侧无法单独保证。
- **P0-5 fetch 选项 ✅**：`fetch/request/post` 统一走 fetchImpl；withHeaders/onlyHeaders/withStatusCode 返回 JSON 串 `{body,headers,url,statusCode,error}`；onlyHeaders 时 body=null；fetchFull 不可用时降级为空头包装 + hk.log 留痕（不静默）。`fetchPC/postPC` 新增并强制 PC UA；timeout 经 options 透传桥（req/fetchFull 的 options 原样下发）。
- **P0-6 动作 scheme ✅**：UI `dispatchAction()` 在点击分发与 navigate 两处先行处理 toast://（解码弹提示）、copy://（剪贴板+桥 writeClipboard）、video://（HK.play）、web://、x5webview://、webview://（openWeb）、download://（桥 downloadFile）、magnet:/thunder:/ed2k:（交播放）、none://、#、javascript:、tel:，不再当详情 URL。
- **P0-7 播放出口 ✅**：`HK.play` 解析地址尾 `;{K@V&&…}` 为 headers、`#标记#` 为 flag，调 `bridge.play(url,title,pic)` 并 `report('play', {url,title,pic,headers,flag})`；playVideos 透传每条的 headers/flag。
- **P0-8 $().select ✅**：`$(选项, 默认序, 标题).select(回调)` 生成 `__hkSelectExec("id")` 链接并登记到 `window.__HK_SELECTS__`；UI 点击时弹**页面内选择弹层**（新增 markup），选中后以选中值执行回调并支持 setResult 换页；无弹层环境退回 prompt 真选择；引擎内直接执行该链接时走 `sc.__hkSelectExec` 的 prompt 路径。**绝不静默选第一项**。`showSelectOptions` 同步版改为 prompt 编号真选择，支持数组与对象式入参。
- **P0-9 MY_PARAMS/host/params/input ✅**：全部改为与 PAGE 绑定的 getter/setter（MY_URL 带查询串时自动解析 params）；run/runInline 先把 extraVars 同步进 PAGE。自测：MY_PARAMS.p=1、host=a.b.com、input=关键词 均真实。
- **P0-10 分页 ✅**：UI runPage 新增 `opts.pager`（home/search/detail 三处接入）；STATE.pager 记录 {code, baseVars, page}；列表渲染≥4 条时底部出「加载下一页」，点击以 MY_PAGE+1 重跑同一规则、条目**追加**到当前页（栈内合并 + 保持滚动位置）；到底（空结果）自动移除按钮并提示。注：规则代码内的 `##fypage/##fyclass` 模板替换发生在规则自身的 URL 拼接里（MY_PAGE 已是真实递增值）；宿主侧无模板引擎可改，这点与审计口径一致（审计的缺失是 MY_PAGE 恒 1，已修）。
- **P0-12 getResCode/setResCode ✅**：engine 新增 HK.resCode 存储 + 作用域/全局双份；@rule= 点击链路先 `fetch(目标页)` 并 setResCode 再跑规则（UI handleItem）。
- **P0-13 全局 d ✅**：作用域 `d` 与 OUT.d 绑定的 getter/setter；run/runInline 收尾把 d 结果集并入 result（无 setResult 时生效），setPreResult 同理。自测通过。
- **$().input/search 的 input ✅**：doSearch 传真实关键词（原恒传 ''）；handleItem 的 lazy/rule 分支 input 改传当前页 URL；input 弹框分支原本已传真实输入值，保持。

### 3. P1
- **P1-1 col_type 全映射 ✅**：渲染器按 60 常量建表——海报/图标网格按类型分列数（1/2/3/4/5 列，pic_3_square 方图）；横滑组（scroll_button/flex_button/text_4/text_5）；单件全覆盖（blank 三档 spacer、header/footer、text_icon、movie_1_left_pic/icon_1_left_pic 左图行、avatar 圆形左图行、movie_3_marquee 跑马灯、icon_1_search 搜索行、long_text 纯文本、video 播放行、x5_webview_single 退化为打开网页行）；**未知类型降级**：带图→左图行、无图→行按钮，绝不空白。分组循环按「同列数」断组，避免旧 POSTER 混组问题。
- **P1-3 持久化 ✅**：vars/myVars 改落 localStorage（hk_vars_v1/hk_myvars_v1，put/clear 即存、启动回载）；getItem/setItem 从页面条目注册表剥离，改走按规则名隔离的持久 KV（hk_kv_v1），getPublicItem/setPublicItem 用独立 pub@ 前缀。自测验证落盘。
- **P1-4 事件消费 ✅**：UI 改用 `HK.__emitHook` 消费（engine 的 emit 已改钩子式；旧 UI 覆盖 HK.emit 的写法在新 engine 下会递归，已拆除）。updateItem 原地改 + 重渲染（原有，保留并补分页按钮参数）；addItem（hk.addItem，载荷 ['after'|'before', id, item]）真插入栈顶数组并重渲染；deleteItem 真删并重渲染；hk.open（openPage/openHikerPage/openDetailPage）接 navigate/openWeb；hk.degrade 落诊断日志。
- **P1-5 confirm 对象式 ✅**：`confirm({title/content, confirm, cancel})`——字符串=按钮文案走原生确认返回布尔，函数=回调按结果触发；字符串入参保持原行为。setPageParams 真存 PAGE.params（原恒 true 空转）；setPageReverse/isPageReversed 真状态；getHome 返回当前规则概要、getColTypes 返回 60 常量真清单（原恒 '[]'）。
- **P1-7 单点缺失 ✅**：joinUrl（engine 实现 + 作用域 + 全局）、_pd/_pdfa/_pdfh/_pdfl 别名、setLastChapterRule（+nset 别名）、setPreResult、getPastes 家族——getPastes/parsePaste 等 engine 原本由 api_surface 填缺路径提供，本轮经 makeScope 的 AS 填缺正式接通（此前 api_surface 是死代码，等于没有）。
- **P1-9 fileExist ✅**：优先桥 `fileExist`（Java 按文件系统判存，返回 "1"/"0" 已兼容），不再以「内容非空」误判。
- **P2-2 toast 双定义 ✅**：合并为唯一定义（原生桥 Toast + hk.toast 事件双通道）。

### 4. 输入来源（JS 侧）✅
- 导入弹层新增密码框，`doImportText` 透传 `{password}` 给 `importText`（import.js 本就支持 opts.password，此前 UI 恒传 {}）。
- 「选择文件」按钮 + 隐藏 file input（accept .json,.zip,.hkzip,.txt）：json/txt 读文本走文本导入；zip/hkzip 读 ArrayBuffer→base64→`bridge.call('unzipRule')`，返回 rules 逐条 `IMP.buildSites` 入库，stored 文件名拼进成功提示。
- 剪贴板自动识别：boot + visibilitychange(回前台) + focus 时查——桥 `readClipboard()` 优先，无桥 `navigator.clipboard.readText()` 兜底；正则命中云口令（`云\d+[a-z]+/…`）/分享口令（海阔视界、小程序￥home_rule）且 hash 未处理过（localStorage hk_clip_seen_v1）时，自动打开导入弹层**预填**，等用户点导入，不自动入库。

### 5. UI 单行横滑 ✅
- 分类 chips 的来源类型（scroll_button/flex_button/text_4/text_5）由渲染器统一收进 `.hscroll{display:flex;flex-wrap:nowrap;overflow-x:auto}` 容器，一行滚动；详情页线路/选集按钮在规则侧正是这四类 col_type，同路生效。选择弹层选项用 .chip.on 表示默认项。样式只复用现有变量与 .btn/.chip/.card 类，无新配色。

### 6. require/libs ✅（JS 侧对齐，含一条 Java 侧遗留）
- engine require 现有链保持：pages[] → http 直取 → fileRead（桥 readFile，Java 侧 safeName 拍平 + writeBytesKeepPath 兼容读回，unzipRule 落盘的 require.json/libs.zip/data.zip 可被读回）；fetchCache 的本地 libs 缓存路径不变。
- **遗留（Java 侧）**：unzipRule 把 libs.zip/data.zip **按原样整包落盘、未解压内层**，JS 无解压能力，包内单个 .js 文件无法被 require 按名读到；需 Java 侧在 unzipRule 时顺带解压 libs.zip/data.zip 内层条目（或新增桥方法）。require.json 本身仅落盘，宿主未做「按 require.json 预下载」——规则运行时经 fetchCache 按需取数可覆盖多数场景。

## 验收记录

- `node --check`：hiker_engine.js ✅、hiker_ui.js ✅（hiker_import.js 未改动，一并校验 ✅）。
- 离线自测 `node docs/hiker-js-selftest.js`：**26 passed, 0 failed**（退出码 0）。原文：

```
LOAD  hiker_bridge_adapter.js
LOAD  hiker_runtime_shim.js
LOAD  hiker_api_surface.js
LOAD  hiker_scheme.js
LOAD  hiker_compat.js
LOAD  hiker_engine.js
LOAD  hiker_import.js
LOAD  hiker_ui.js
PASS  HikerEngine 存在
PASS  HikerImport 存在
PASS  HikerUI 存在
PASS  HikerUI.boot() 不抛错
PASS  全局 CryptoJS 已安装
PASS  全局 getCryptoJS 是函数
PASS  全局 joinUrl 是函数
PASS  全局 getResCode 是函数
PASS  CryptoJS.MD5('abc')
PASS  CryptoJS.SHA1('abc')
PASS  CryptoJS.SHA256('abc')
PASS  CryptoJS.HmacSHA1(fox,'key')
PASS  CryptoJS.AES.encrypt/decrypt 存在
PASS  CryptoJS.enc.Base64 往返
PASS  作用域: CryptoJS(object)/getCryptoJS/fetch/fetchPC/joinUrl/$().select 已定义
PASS  作用域: eval(getCryptoJS()) 后 CryptoJS 可用
PASS  作用域: setResCode/getResCode 往返
PASS  全局 d 已注入: 只 d.push 也能成为结果
PASS  MY_PARAMS/host/input/MY_PAGE 真实值
PASS  $().select 生成登记链接（不静默选第一项）
PASS  fetch withHeaders 走 fetchFull 包装语义
PASS  putMyVar/getMyVar 往返
PASS  变量已落 localStorage
PASS  joinUrl 相对路径补全
PASS  _pd/_pdfa/_pdfh/_pdfl 别名已定义
PASS  getColTypes 含 flex_button
---
selftest: 26 passed, 0 failed
```

## 未完成项 / 遗留（如实）

1. **P0-4 普通 fetch 的 cookie 共用**：取决于 Java 侧 HikerHttp 是否与 HikerCookies 共用存储（并行 Java 任务范围）；JS 侧桥通道已接通。
2. **libs.zip/data.zip 内层未解压**（Java 侧 unzipRule 待办，见第 6 节）。
3. **AES 非 ASCII 字节密钥**：经 Java String 传桥有语义损失，ASCII 密钥不受影响。
4. **pdfh/pdfa 取值行为未在 Node 验**：选择器走原生 DOMParser，Node 桩为空实现，自测只验 _pd 家族存在性；须在真 WebView/桩模拟阶段验。
5. **分页的 hasNext 判定**：引擎 runTarget 不存在于本分支 UI 模型（UI 用 HK.run），无官方 hasNext 信号，现以「本页条目数 ≥4」启发式出下一页按钮，空结果自动收起；个别每页条数少的规则可能多点一次才知道到底。
6. **x5_webview_single** 退化为「打开网页」行（WebView 壳内不嵌第二 WebView），非空白但非官方内嵌形态。
7. 本轮未做真机/WebView 实跑；DOM 渲染与桥的真机行为待父任务的构建+桩模拟阶段验证。
