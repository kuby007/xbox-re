package com.fongmi.android.tv.hiker;

import android.webkit.JavascriptInterface;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 独立 WebView 的「同步字符串」Java 桥（addJavascriptInterface 注入，桥名 HikerBridge）。
 *
 * 与首页桥的本质区别（兼容性分析 A 组结论）：
 *   - 首页 HomeWebBridge.invoke() -> Task.execute 线程池 -> fongmiNative.resolve 回调，异步 Promise；
 *   - 本类每个 @JavascriptInterface 方法都在 JavaBridge 线程**同步返回一个 String**。
 *     这正是 v390 jar 的 JsReqBridge / FM_SHELL.call、以及海阔原版 fy_bridge_app.fetch 的语义。
 *   - 海阔规则形如 eval.call(null, fetch(url))、pdfh(fetch(url), ...)，
 *     拿到 Promise 会静默变成 "[object Object]" 或抛错（阶段二、四已实测）。
 *
 * 零耦合设计：不 import 任何 com.fongmi.android.tv.* 业务类。
 * 播放走宿主已有的本地 HTTP 通道 /action?do=push（Action.onPush -> ServerEvent.push ->
 * HomeActivity.onServerEvent(PUSH) -> VideoActivity.push），端口按 Server.start() 的 9978~9998 探测。
 * 因此本类可用 ecj + android-all.jar 单独真实编译，不需要整个工程。
 */
public class HikerBridge {

    private final HikerActivity activity;
    private final Map<String, String> cache = new HashMap<>();
    private volatile int port = -1;

    public HikerBridge(HikerActivity activity) {
        this.activity = activity;
        /* 把下载 / 图片 / m3u8 缓存落到 App 私有目录，供 HikerHost.readFile 读回。
         * 用 try/catch 包裹：单元测试里 activity 可能为 null，且不影响桥的同步语义。 */
        try {
            HikerApi.setRoot(new java.io.File(activity.getFilesDir(), "hiker").getAbsolutePath());
        } catch (Throwable ignored) {
        }
    }

    /* ------------------------------------------------------------------ 同步网络 */

    /** 海阔 fetch / request：同步返回正文字符串。options 为 JSON 字符串（method/headers/body/timeout）。 */
    @JavascriptInterface
    public String req(String url, String options) {
        if (url == null || url.length() == 0) return "";
        Opt o = Opt.parse(options);
        Map<String, String> headers = o.headers;
        if ("POST".equalsIgnoreCase(o.method)) return HikerHttp.post(url, o.body, headers, o.timeout);
        return HikerHttp.get(url, headers, o.timeout);
    }

    /** 历史别名：不同版本规则里 fetch / request / ajax 都可能出现。 */
    @JavascriptInterface
    public String request(String url, String options) {
        return req(url, options);
    }

    @JavascriptInterface
    public String fetch(String url, String options) {
        return req(url, options);
    }

    /** 兼容 fy_bridge_app.fetch(url, obj) 的签名（obj 为 JSON 字符串）。 */
    @JavascriptInterface
    public String fetchSync(String url, String options) {
        return req(url, options);
    }

    /* ------------------------------------------------------------------ 结果 / 事件回传 */

    /** setResult 回传：页面也可直接用，Java 侧转发生命周期事件。 */
    @JavascriptInterface
    public void report(String type, String payload) {
        if (activity != null) activity.onBridgeEvent(type, payload);
    }

    @JavascriptInterface
    public void log(String message) {
        report(HikerContract.EV_LOG, message);
    }

    /* ------------------------------------------------------------------ 播放出口 */

    /**
     * 桥接 App 播放器：走本地 /action?do=push，最终由宿主 VideoActivity 播放。
     * 不直接调用 VideoActivity.start，避免与首页/播放器代码耦合。
     */
    @JavascriptInterface
    public boolean play(final String url, String title, String pic) {
        if (url == null || url.length() == 0) return false;
        if (isDuplicatePush(url)) return true;   /* 与 report("play") 的二次投递合流，见 pushRich */
        return pushUrl(url);
    }

    /**
     * 富信息播放投递（HikerActivity.onBridgeEvent 解析 report("play", JSON) 后调用）。
     *
     * 宿主 PUSH 链的格式上限（实测，见 docs/HIKER-FULL-java-report.md）：
     *   ServerEvent.push(String) -> HomeActivity.onServerEvent -> VideoActivity.push(text)
     *   -> SiteApi push_agent：Result.url=id、parse=0、header 不设置。
     * 即宿主只接受「单个 URL 字符串」：title / pic / headers / flag 在这条链上没有任何
     * 透传字段（ServerEvent 的 name 字段只供 SETTING 事件使用，PUSH 工厂方法不设置它）。
     * 因此本方法把 title/pic/headers/flag 记入日志留痕，最终仍只投递 url 本身；
     * 需要 headers 的直链在宿主侧会缺头，这是宿主链的限制，不是本层吞掉。
     */
    public boolean pushRich(final String url, String title, String pic,
                            Map<String, String> headers, String flag) {
        if (url == null || url.length() == 0) return false;
        android.util.Log.d("HikerBridge", "pushRich url=" + url
                + " title=" + title + " pic=" + pic + " flag=" + flag
                + " headers=" + (headers == null ? "" : headers.keySet().toString()));
        if (isDuplicatePush(url)) return true;
        return pushUrl(url);
    }

    /** 实际投递：首选宿主事件，失败兜底本地 HTTP 推送（与原 play 一致）。 */
    private boolean pushUrl(final String url) {
        /* 首选：直接投递宿主的播放事件（Main 源集 → 两个 flavor 的 HomeActivity 都订阅了 PUSH）。
         * 这条路不依赖本地 HTTP 服务是否在跑 —— 服务只在部分入口里 start()，
         * 之前用 HTTP 推送会出现「点了没反应」。 */
        try {
            com.fongmi.android.tv.App.post(() -> com.fongmi.android.tv.event.ServerEvent.push(url));
            report(HikerContract.EV_PLAY, url);
            return true;
        } catch (Throwable ignored) {
        }
        /* 兜底：老路径（本地 HTTP 推送） */
        try {
            String target = "http://127.0.0.1:" + detectPort() + HikerContract.PUSH_PATH
                    + URLEncoder.encode(url, "UTF-8");
            String r = HikerHttp.get(target, null);
            return r != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /* JS 契约是 bridge.play(...) 与 report("play", JSON) 双路投递同一个 URL，
     * 不去重会让 VideoActivity 被同一地址连续 start 两次（闪退/重播）。
     * 5 秒内同 URL 的第二次投递视为重复，直接报成功不重复推送。 */
    private static volatile String sLastPushUrl = "";
    private static volatile long sLastPushAt = 0;

    private static synchronized boolean isDuplicatePush(String url) {
        long now = System.currentTimeMillis();
        if (url.equals(sLastPushUrl) && now - sLastPushAt < 5000) return true;
        sLastPushUrl = url;
        sLastPushAt = now;
        return false;
    }

    /* ------------------------------------------------------------------ 剪贴板 */

    /**
     * 读剪贴板纯文本（口令自动获取/导入用）。ClipboardManager 必须在主线程访问，
     * 而 @JavascriptInterface 运行在 JavaBridge 线程 —— 用同步等待切到主线程取，
     * 超时/无 activity/无内容一律返回 ""，不抛异常。
     */
    @JavascriptInterface
    public String readClipboard() {
        if (activity == null) return "";
        try {
            final String[] out = {""};
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            activity.runOnUiThread(() -> {
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (cm != null && cm.hasPrimaryClip()) {
                        android.content.ClipData clip = cm.getPrimaryClip();
                        if (clip != null && clip.getItemCount() > 0) {
                            CharSequence t = clip.getItemAt(0).coerceToText(activity);
                            if (t != null) out[0] = t.toString();
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    latch.countDown();
                }
            });
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS);
            return out[0];
        } catch (Throwable t) {
            return "";
        }
    }

    /** 写剪贴板（copy:// 动作的原生兜底；WebView 内 navigator.clipboard 常不可用）。 */
    @JavascriptInterface
    public void writeClipboard(final String text) {
        if (activity == null || text == null) return;
        try {
            activity.runOnUiThread(() -> {
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("hiker", text));
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** 页面里的 toast 直接弹原生的（比 HTML toast 更显眼，也不会被 WebView 层挡住）。 */
    @JavascriptInterface
    public void toast(final String message) {
        if (activity == null || message == null) return;
        try {
            activity.runOnUiThread(() ->
                    android.widget.Toast.makeText(activity.getApplicationContext(), message,
                            android.widget.Toast.LENGTH_SHORT).show());
        } catch (Throwable ignored) {
        }
    }

    /** 探测宿主本地服务端口（Server.start 从 9978 起试到 9998）。 */
    private int detectPort() {
        if (port > 0) return port;
        for (int p = HikerEntry.PORT_MIN; p <= HikerEntry.PORT_MAX; p++) {
            if (alive(p)) {
                port = p;
                return p;
            }
        }
        return HikerEntry.PORT_MIN;
    }

    private boolean alive(int p) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL("http://127.0.0.1:" + p + "/").openConnection();
            c.setConnectTimeout(300);
            c.setReadTimeout(300);
            c.getResponseCode();   // 任何 HTTP 响应（含 404）都说明端口在监听
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /* ------------------------------------------------------------------ 兼容 fy_bridge_app / JsT4 的周边 API */

    @JavascriptInterface
    public String getInternalJs() {
        return "";
    }

    @JavascriptInterface
    public String getVar(String key) {
        String v = cache.get(key);
        return v == null ? "" : v;
    }

    @JavascriptInterface
    public void putVar(String key, String value) {
        cache.put(key, value == null ? "" : value);
    }

    @JavascriptInterface
    public String getCache(String key) {
        return getVar(key);
    }

    @JavascriptInterface
    public void setCache(String key, String value) {
        putVar(key, value);
    }

    /** 能力表：页面 HikerCompat.probe() 可用它做二次确认。 */
    @JavascriptInterface
    public String capabilities() {
        return "{\"bridge\":\"HikerBridge\",\"sync\":true,\"return\":\"string\","
                + "\"play\":\"local-push\",\"port\":" + detectPort() + ",\"es5\":true}";
    }

    /* ------------------------------------------------------------------ 阶段八：bridge 档 API 补齐 */

    /**
     * 34 个 bridge 档 API 的同步主通道（HikerApi 提供真语义）。
     *
     * 为什么不逐个暴露 @JavascriptInterface 方法：addJavascriptInterface 在 JS 传入
     * 数组/对象时会隐式 String 化（常见结果 "[object Object]"），且方法参数个数必须与
     * JS 调用严格一致，否则 WebView 直接抛错。统一走 call(name, argsJson) 后，
     * JS 侧用 JSON.stringify(args) 显式序列化，Java 侧用 HikerJson 还原，参数不丢不串。
     * 所有方法同步返回 String，与海阔规则 eval.call(null, fetch(...)) 的硬约束一致。
     */
    @JavascriptInterface
    public String call(String name, String argsJson) {
        return HikerApi.call(name, argsJson);
    }

    /** 桥能力自省：让页面能区分「符号可见」与「有语义」，避免假阳性。 */
    @JavascriptInterface
    public String apiCapabilities() {
        return HikerApi.capabilities();
    }

    /** 运行期留痕（降级 / 错误 / 缓存统计），供真机探针排查。 */
    @JavascriptInterface
    public String apiReport() {
        return HikerApi.report();
    }

    /* 具名方法：按官方高频签名提供，兼容「直接按方法名调用」的历史形态。
     * 三参以上、对象/数组参数一律走 call(name, argsJson)，避免 WebView 参数个数不匹配。 */
    @JavascriptInterface
    public String md5(String text) { return HikerApi.callVarargs("md5", text); }

    @JavascriptInterface
    public String aesEncode(String text, String key) { return HikerApi.callVarargs("aesEncode", text, key); }

    @JavascriptInterface
    public String aesDecode(String text, String key) { return HikerApi.callVarargs("aesDecode", text, key); }

    @JavascriptInterface
    public String getCookie(String url) { return HikerApi.callVarargs("getCookie", url); }

    @JavascriptInterface
    public String fetchCookie(String url) { return HikerApi.callVarargs("fetchCookie", url); }

    @JavascriptInterface
    public String fetchCache(String url) { return HikerApi.callVarargs("fetchCache", url); }

    @JavascriptInterface
    public String postRequest(String url, String body) { return HikerApi.callVarargs("postRequest", url, body); }

    @JavascriptInterface
    public String downloadFile(String url, String path) { return HikerApi.callVarargs("downloadFile", url, path); }

    @JavascriptInterface
    public String cacheM3u8(String url) { return HikerApi.callVarargs("cacheM3u8", url); }

    @JavascriptInterface
    public String clearM3u8Ad(String m3u8) { return HikerApi.callVarargs("clearM3u8Ad", m3u8); }

    @JavascriptInterface
    public String createQRCode(String text) { return HikerApi.callVarargs("createQRCode", text); }

    @JavascriptInterface
    public String getIP() { return HikerApi.callVarargs("getIP"); }

    /** hiker://files/ 与 hiker_scheme.js 的 file:// 分发会调用；阶段七的适配器已预留该调用点。 */
    @JavascriptInterface
    public String readFile(String path) { return HikerApi.callVarargs("readFile", path); }

    /* ------------------------------------------------------------------ options 解析（不依赖 org.json，保证可独立编译） */

    private static final class Opt {
        String method = "GET";
        String body = "";
        int timeout = 0;   /* 秒；0 = HikerHttp 默认 */
        Map<String, String> headers = new HashMap<>();

        /** 极简 JSON 解析，只取 method / body / headers / timeout，避免引入 gson 依赖。 */
        static Opt parse(String json) {
            Opt o = new Opt();
            if (json == null || json.length() == 0) return o;
            String m = str(json, "method");
            if (m != null && m.length() > 0) o.method = m;
            String b = str(json, "body");
            if (b != null) o.body = b;
            java.util.regex.Matcher tm = java.util.regex.Pattern
                    .compile("\"timeout\"\\s*:\\s*([0-9]+)").matcher(json);
            if (tm.find()) {
                try { o.timeout = Integer.parseInt(tm.group(1)); } catch (Throwable ignored) {}
            }
            String h = obj(json, "headers");
            if (h != null) {
                java.util.regex.Matcher mt = java.util.regex.Pattern
                        .compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"").matcher(h);
                while (mt.find()) o.headers.put(mt.group(1), mt.group(2));
            }
            return o;
        }

        private static String str(String s, String key) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"" + key + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(s);
            if (!m.find()) return null;
            return m.group(1).replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
        }

        private static String obj(String s, String key) {
            int i = s.indexOf("\"" + key + "\"");
            if (i < 0) return null;
            int st = s.indexOf('{', i);
            if (st < 0) return null;
            int depth = 0;
            for (int j = st; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return s.substring(st + 1, j);
                }
            }
            return null;
        }
    }
}
