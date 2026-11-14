# HIKER-FULL Java 侧实施报告

范围：`app/src/main/java/com/fongmi/android/tv/hiker/**` + 三份 AndroidManifest。
未动：JS、build.gradle、包名/版本/签名、git。基线 02d1c86。

## 改动文件

| 文件 | 变更 |
|---|---|
| `HikerCookies.java` | **新建**：统一 Cookie 存储单例 |
| `HikerHttp.java` | Cookie 注入/回收、可配置超时、新增 `Result`/`request()` |
| `HikerApi.java` | Cookie 改走 HikerCookies；新增 `fetchFull`/`unzipRule`/`setCookie` 分发；新增 `rootPath()` |
| `HikerBridge.java` | 新增 `readClipboard()`/`writeClipboard()`；`play()` 抽出 `pushUrl()` + 新增 `pushRich()` + 5 秒去重；`req` 透传 timeout |
| `HikerActivity.java` | `onBridgeEvent` 解析 play JSON 并投递；运行时方向兜底改为 leanback→LANDSCAPE/否则 FULL_USER；WebChromeClient 新增 `onShowFileChooser` + `onActivityResult` |
| `app/src/main/AndroidManifest.xml` | HikerActivity `sensorLandscape` → `unspecified` |
| `app/src/mobile/AndroidManifest.xml` | 完整节点重声明 HikerActivity：`fullUser` |
| `app/src/leanback/AndroidManifest.xml` | 完整节点重声明 HikerActivity：`sensorLandscape` |

## 逐项状态

1. **Cookie 体系 ✅** — `HikerCookies`：静态单例、内存 `host -> LinkedHashMap`、落盘
   `filesDir/hiker/cookies.txt`（一行 `host\tname\tvalue`，值转义）、全方法 synchronized、惰性加载。
   `HikerHttp.request` 请求前按 URL host 注入 jar 的 Cookie（调用方显式 Cookie 头优先，不覆盖），
   响应后从响应头收 Set-Cookie（同名多值以 `\n` 合并，禁止逗号拼——expires 含逗号）存回 jar。
   `HikerApi.getCookie/putCookie` 改为委托 HikerCookies，原孤立 `COOKIES` 表删除；
   `fetchCookie/fetchCache/batchFetch/postRequest/downloadFile/cacheM3u8` 原有「取 getCookie 拼头」
   的语义不变，只是数据源换成与 HikerHttp 互通的同一 jar。`setCookie` 新增分发 `[url, cookieStr]`，
   存后返回该 host 的 Cookie 串。根目录共用：`HikerApi.rootPath()`（包内）供 HikerCookies 落盘，
   ROOT 仍由 HikerBridge 构造时 `setRoot(filesDir/hiker)` 注入。
2. **超时可配置 ✅** — `HikerHttp.get/post(url, ..., timeoutSeconds)` 重载 + `request(..., timeoutSeconds)`，
   <=0 时默认 15s（原写死值）。`HikerBridge.Opt` 解析 options 的 `timeout` 数值字段并透传。
3. **fetchFull ✅** — `HikerApi.call("fetchFull", [url, optionsJson])`，options 支持
   `{method, headers, body, timeout}`（options 也兼容 JSON 字符串形态）。返回经 HikerJson 拼装：
   `{"status":n,"headers":{...},"body":"..."}`，headers 为响应头原样（含合并 Set-Cookie），转义由
   HikerJson.quote 保证。同步；失败返回 `{"status":0,"headers":{},"body":""}` 不抛（冒烟实测见下）。
4. **unzipRule ✅** — `HikerApi.call("unzipRule", [base64zip])`：ZipInputStream 解包；
   rule.json（根或一层子目录，depth<=1）文本入 `rules`；require.json/libs.zip/data.zip/data2.zip
   按包内相对路径落盘（`writeBytesKeepPath`，canonical 路径校验防 Zip Slip，逃逸条目丢弃不存），
   文件名入 `stored`，同时以相对路径与拍平名双注册进 FILES 供 readFile 读回。
   单文件上限 16MB、总量 64MB，超限返回 `{"ok":false,"error":"zip too large"}`；
   坏包/无 rule.json 返回 `{"ok":false,"error":...}`。libs.zip/data.zip 内层展开由 JS 侧对落盘
   文件再调 unzipRule 递归完成（本层不递归解内层 zip，与契约「stored 落盘」一致）。
5. **剪贴板 ✅** — `HikerBridge.readClipboard()`：runOnUiThread + CountDownLatch(2s) 同步读
   ClipboardManager 纯文本，无 activity/无内容/超时返回 `""`；`writeClipboard(String)` 主线程写。
6. **播放出口 ✅（带宿主限制，见下节）** — `HikerActivity.onBridgeEvent` 对 type 为
   `"play"` 或 `"hiker.play"`(EV_PLAY) 且 payload 以 `{` 开头时用 HikerJson 解析
   url/title/pic/headers/flag，经 `HikerBridge.pushRich` 投递；纯字符串 payload 保持旧日志行为。
   JS 契约的双路投递（bridge.play + report）在 HikerBridge 内以「同 URL 5 秒窗口」去重，
   避免 VideoActivity 被同一地址连续 start 两次。
7. **横屏 ✅** — 见改动表。HikerActivity 运行时补丁保留但改为双向：leanback 特性→
   SENSOR_LANDSCAPE，否则 FULL_USER（最终效果与原补丁一致，Manifest 不再让手机先横屏再回正）。
   mobile/leanback 原本均无 HikerActivity 节点，故为新增完整节点重声明（属性与 main 一致，
   仅 orientation 按 flavor 取值），无重复声明；两文件原本就只有 android 命名空间，
   未用 tools 属性，不需要引入 tools 命名空间。三份 Manifest 均过 XML 解析。
8. **文件选择兜底 ✅** — WebChromeClient.onShowFileChooser：单选、`ACTION_GET_CONTENT` + `*/*`
   （params 声明了单一具体 accept type 时采用之），结果在 onActivityResult 经
   filePathCallback 回传；重复打开时先取消旧 callback，onDestroy 时兜底回传 null。

## PUSH 消费链结论（任务 5 的调查结果）

链路（两 flavor 一致）：
`ServerEvent.push(String)`（`app/src/main/java/com/fongmi/android/tv/event/ServerEvent.java`，
record 只有 `type/text/name`，且 `push()` 工厂不设置 name——name 只供 SETTING 事件）
→ `HomeActivity.onServerEvent`（mobile:270 / leanback:554）
→ `VideoActivity.push(activity, text)`（mobile:283 / leanback:271）
→ `Sniffer.getUrl(text)` → `start(activity, SiteApi.PUSH, url, url)`（name 与 id 都等于 URL 本身）
→ `SiteApi.playerContent` 的 push_agent 分支：`Result.setUrl(id); setParse(0);`，**不设置 header**。

**结论：宿主 PUSH 能接受的最丰富格式就是单个 URL 字符串。** title / pic / headers / flag
没有任何透传字段；带 headers 的直链经此链播放会缺头。这是宿主链（ServerEvent/VideoActivity/
SiteApi，均不在本任务允许改动范围）的上限，本层已做到：解析全部字段、日志留痕、投递 url。
后续若要真透传 headers，需要另行立项改宿主（如给 ServerEvent 增加 PUSH 载荷或让 push_agent
分支从 URL 尾标记解析 headers），不在本次范围。

## 编译验证（javac + android.jar，语法级）

方式：hiker 包全部 9 个 java（`*.java` 通配，含新建 HikerCookies）+
`com.fongmi.android.tv.App` / `com.fongmi.android.tv.event.ServerEvent` 两桩
（桩签名与真实类逐一对齐：`App.post(Runnable)` 见 App.java:70、
`ServerEvent.push(String)` 见 ServerEvent.java:11），classpath =
`~/workspace/tools/android-sdk/platforms/android-37/android.jar`，`javac --release 8`
（与工程 Java 8 语法口径一致）。真实 App/ServerEvent 本体依赖 EventBus 等全工程类，
无法单独编译，故用桩——这是任务预授权的方式，桩只出现在 /tmp 验证目录，未入仓库。

命令输出原文：

```
javac 21.0.13.0.3
warning: [options] source value 8 is obsolete and will be removed in a future release
warning: [options] target value 8 is obsolete and will be removed in a future release
warning: [options] To suppress warnings about obsolete options, use -Xlint:-options.
Note: HikerActivity.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
3 warnings
JAVAC_EXIT:0
```

产物含 HikerCookies.class、HikerHttp$Result.class 等全部类。仅警告（release 8 过时提示、
HikerActivity 既有 deprecated API），无错误。

## JVM 运行时冒烟（纯 JDK 部分，同一 JVM 实测）

```
UNZIP={"ok":true,"rules":["{\"title\":\"A\"}","{\"title\":\"B\"}"],"stored":["require.json","libs.zip","data.zip"]}
EVIL_EXISTS=false ROOT EVIL=false
BAD={"ok":false,"error":"no rule.json in zip"}
SLIP={"ok":true,"rules":["{}"],"stored":[]}
SLIP_FILE_OUTSIDE=false
SET=sid=abc; theme=dark
GET=sid=abc; theme=dark
GET_OTHER_HOST=
COOKIES_FILE=true
FETCH_DOWN={"status":0,"headers":{},"body":""}
REPORT={"events":[],"cookies":1,"cache":0,"files":3,"root":"/tmp/hiker-verify/root"}
```

- unzipRule：根+一层子目录两份 rule.json 均收集；require.json/libs.zip/data.zip 落盘入 stored；
  `../evil.json`（非目标类型）忽略；`../../libs.zip` 被 Zip Slip 防护丢弃（stored 不含、
  盘外无文件）；坏包返回 ok:false。
- setCookie/getCookie：往返一致、按 host 隔离、cookies.txt 已落盘。
- fetchFull 失败路径：不可达地址返回契约规定的 `status:0` 空 body，不抛。

**未能实测的部分（如实记录）**：fetchFull/HikerHttp 的真实 HTTP 往返（含请求 Cookie 注入、
响应 Set-Cookie 回存）在本沙箱无法从 JVM 验证——沙箱对 JVM 直连 TCP 有拦截（原始 Socket
读到的就是拦截提示文本，curl 走出口代理可用、Java 代理隧道同样失败，与 sdkmanager 既往
失败同因）。HikerHttp 的传输层与改动前完全同构（HttpURLConnection 直连，原有 fetch 一直
走此路），Cookie 注入/回收为请求前后的确定性代码路径，存储侧已由 HikerCookies 冒烟覆盖；
真机/模拟器上仍需复测一次带 Cookie 的真实站点请求。

## 遗留问题

1. 播放 headers/title/pic/flag 无法经宿主 PUSH 透传（宿主链上限，见上节），需宿主侧另行立项。
2. `HikerApi.readFile` 仍按拍平名（safeName）读文件：unzipRule 落盘的子目录文件已双注册
   （相对路径 + 拍平名）可被读回，但同名不同目录的文件会互相覆盖拍平名映射——与既有
   readFile/writeFile 语义一致，未在本轮改动其契约，JS 侧若按相对路径读需注意此点。
3. 去重窗口（5 秒）内用户手动重播同一 URL 会被合并为一次投递并返回成功；如真机手感
   有问题可缩短窗口或仅对 report 路径去重，需实测后定。
4. HikerHttp 的 POST 现显式 `setInstanceFollowRedirects(true)`（与 HttpURLConnection
   默认值相同），POST 重定向的实际跟随行为由 JDK 实现决定，与改动前默认一致。
5. 本报告只做语法级编译 + JVM 冒烟；整包 Gradle 构建与真机验证不在本子任务范围。
