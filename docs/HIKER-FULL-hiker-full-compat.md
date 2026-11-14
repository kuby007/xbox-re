# HIKER-FULL 海阔全量兼容（方案A）

Recovery anchor:
- Objective: 在不改包名(com.fongmi.android.oktv)/app名/版本(5.6.0/560)的前提下，按 notes 两份审计修 P0x13+P1x10、补齐5种输入来源、修横屏、按钮单行横滑、UI与点播一致，最终 Node 桩模拟 47 规则+4 口令规则到播放地址，并构建 mobile+leanback APK。
- Completion sentence: 海阔运行时加载链完整、规则可翻页/可搜索/可进详情/可解析出播放地址、五种输入可导入、手机不切横屏。
- Scope (allowed paths): app/src/main/assets/hiker/**, app/src/main/java/com/fongmi/android/tv/hiker/**, app/src/main/AndroidManifest.xml, app/src/mobile/AndroidManifest.xml, app/src/leanback/AndroidManifest.xml, docs/HIKER-FULL-*.md。本文件外零改动；不动 applicationId/version/签名配置/首页代码。
- Baseline: git commit 02d1c86 + tag baseline-original（zip 快照无 .codex，task_guard 不可用，以 git commit/tag 为还原点）。
- Bridge contract (新增，两侧共用):
  - HikerApi.call("fetchFull", [url, optionsJson]) -> {"status":n,"headers":{},"body":""}
  - HikerApi.call("unzipRule", [base64zip]) -> {"ok":true,"rules":[ruleJsonText...],"stored":[names]}
  - HikerApi.call("setCookie", [url, cookieStr]) / getCookie 已有，须与 HikerHttp 共用同一 Cookie 存储（新 HikerCookies 类，落盘 filesDir/hiker/cookies.txt）
  - HikerBridge.readClipboard() -> String; HikerBridge.writeClipboard(String)
  - 播放: JS 调 bridge.play(url,title,pic) + report("play", JSON{url,title,pic,headers,flag}); Java 侧 onBridgeEvent 解析后走宿主 PUSH 最丰富格式
- Estimates (2026-11-14 20:37 CST 起): JS 运行时+UI 约 50 分钟, Java 约 30 分钟, Node 桩模拟约 30 分钟, Gradle 双 flavor 构建约 40 分钟, 打包+报告约 20 分钟。预计 23:30 前交付候选。
- Verification: node --check 全部 JS; Node 桩跑 47+4 规则; gradle assembleMobileArm64_v8aRelease + assembleLeanbackArm64_v8aRelease (fastRelease) 后 aapt2 dump badging 核包名/版本。
- Status: 实施中（JS/Java 并行）。Next action: 两侧完成后合并冒烟。
