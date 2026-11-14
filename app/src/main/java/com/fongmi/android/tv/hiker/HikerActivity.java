package com.fongmi.android.tv.hiker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * 海阔独立运行时容器 —— 与首页完全隔离。
 *
 * 「不要与首页混用」的落地：
 *   - 独立 Activity，不复用 HomeActivity.webOverlay；
 *   - 独立 WebView 实例，不复用 HomeWebController / CustomWebView；
 *   - 独立 Java 桥 HikerBridge（同步字符串），不注入首页 fongmiBridge / window.fm SDK；
 *   - 独立 assets 容器页 file:///android_asset/hiker/index.html，不受 WebHomeTarget 同源白名单限制；
 *   - 独立生命周期，onDestroy 里 destroy WebView，不干扰首页 prewarm/fmpause 事件。
 *
 * 兼容性：不引用 R、不引用 BaseActivity / App / SpiderDebug 等工程内部类，
 * 因此可用 ecj + android-all.jar 真实编译（见 build_verify）。这也是"多变兼容"的一部分：
 * 换 flavor、换主题、换皮肤都不影响本 Activity 编译。
 */
public class HikerActivity extends Activity {

    private WebView webView;
    private HikerBridge bridge;

    /** <input type=file> 的回调（规则里「选本地文件」用它）。 */
    private android.webkit.ValueCallback<android.net.Uri[]> filePathCallback;
    private static final int REQ_FILE_CHOOSER = 0x4b1;

    /** 规则文件导入（.hkzip/.zip/.json），由桥触发，结果用 Base64 回传给页面。 */
    private static final int REQ_RULE_FILE = 0x4b2;
    private static final int MAX_RULE_BYTES = 32 * 1024 * 1024;

    @Override
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* 方向：一个字都不干预。
         * 清单里不写 screenOrientation，代码里也不调 setRequestedOrientation ——
         * 容器直接继承宿主当前方向，进来是什么方向就一直是什么方向。
         * （旧实现是清单写 sensorLandscape + onCreate 再改 FULL_USER，两次翻转，
         *   这就是「海阔模块启动切横屏」的来源，已彻底删掉。） */

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF101014);   // 与 App 的 Material3 深色背景一致
        // 避开透明状态栏/导航栏与刘海，否则 sticky header 与底部按钮被系统栏盖住。
        root.setFitsSystemWindows(true);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF101014);
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        // 视口适配：让 index.html 的 viewport meta 按设备宽度生效，修「显示不全」。
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setTextZoom(100);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(HikerContract.UA);
        s.setMediaPlaybackRequiresUserGesture(false);

        // 关键：只注入同步桥，绝不注入首页 getSdk() 的异步 window.fm / fongmiBridge。
        bridge = new HikerBridge(this);
        webView.addJavascriptInterface(bridge, HikerEntry.BRIDGE_NAME);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // hiker:// 由页面内 HikerCompat 路由表处理，不交给系统浏览器、不进首页白名单
                if (url != null && url.startsWith("hiker://")) return true;
                if (url != null && url.startsWith("http")) return false;
                return true;
            }
        });
        /* prompt() 不给默认实现的话，WebView 会直接返回 null ——
         * 规则里的「输入框」条目（$(默认值, 提示).input(回调)）和搜索都会点不动。
         * 这里补一个原生输入框。 */
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsPrompt(WebView view, String url, String message,
                                      String defaultValue, final JsPromptResult result) {
                final android.widget.EditText input = new android.widget.EditText(HikerActivity.this);
                input.setText(defaultValue == null ? "" : defaultValue);
                input.setSelection(input.getText().length());
                if (message != null && message.length() > 0) input.setHint(message);
                new android.app.AlertDialog.Builder(HikerActivity.this)
                        .setTitle("请输入")
                        .setView(input)
                        .setPositiveButton("确定", (d, w) -> result.confirm(input.getText().toString()))
                        .setNegativeButton("取消", (d, w) -> result.cancel())
                        .setOnCancelListener(d -> result.cancel())
                        .show();
                return true;
            }

            /* 没有这个回调，页面里的 <input type=file> 点了毫无反应 ——
             * 「从文件导入小程序」这条路在真机上就是死的。 */
            @Override
            public boolean onShowFileChooser(WebView view,
                                             android.webkit.ValueCallback<android.net.Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    if (params != null && params.getAcceptTypes() != null
                            && params.getAcceptTypes().length > 0) {
                        String[] types = params.getAcceptTypes();
                        if (types.length == 1 && types[0] != null && types[0].contains("/")) {
                            intent.setType(types[0]);
                        }
                        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                                new String[]{"*/*", "application/zip", "application/json",
                                        "text/plain", "application/octet-stream"});
                    }
                    startActivityForResult(Intent.createChooser(intent, "选择小程序文件"), REQ_FILE_CHOOSER);
                } catch (Throwable e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl(HikerEntry.ASSET_URL);
    }

    /** 桥事件回传（结果、播放、日志）；当前仅做日志，后续可接宿主 EventBus / 播放器。 */
    public void onBridgeEvent(String type, String payload) {
        // 刻意不调用首页任何类；保留扩展点，避免与首页生命周期耦合。
        android.util.Log.d("HikerBridge", type + ": "
                + (payload == null ? "" : payload.substring(0, Math.min(160, payload.length()))));
    }

    /**
     * 桥的「从文件导入」入口：拉起系统文件选择器。
     * 支持 .hkzip / .zip（取里面的 rule.json，其次任意 *.json）
     * 和直接选 .json / 文本规则。
     */
    public void requestRuleFile() {
        runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQ_RULE_FILE);
            } catch (Throwable e) {
                deliverFileResult("", "无法打开文件选择器: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE_CHOOSER) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(
                        (resultCode == RESULT_OK && data != null && data.getData() != null)
                                ? new android.net.Uri[]{data.getData()} : null);
                filePathCallback = null;
            }
            return;
        }
        if (requestCode == REQ_RULE_FILE) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                deliverFileResult("", "已取消");
                return;
            }
            final android.net.Uri uri = data.getData();
            String name = queryName(uri);
            new Thread(() -> {
                try {
                    byte[] raw = readAll(uri);
                    if (raw == null || raw.length == 0) {
                        deliverFileResult(name, "文件是空的");
                        return;
                    }
                    String text = extractRuleText(raw, name);
                    if (text == null || text.trim().isEmpty()) {
                        deliverFileResult(name, "这个文件里没有找到规则（不是小程序文件？）");
                        return;
                    }
                    deliverFileResult(name, null, text);
                } catch (Throwable e) {
                    deliverFileResult(name, "读取失败: " + e.getMessage());
                }
            }, "hiker-file").start();
        }
    }

    private String queryName(android.net.Uri uri) {
        try (android.database.Cursor c = getContentResolver()
                .query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Throwable ignored) {
        }
        String p = uri.getPath();
        return p == null ? "文件" : p.substring(p.lastIndexOf('/') + 1);
    }

    private byte[] readAll(android.net.Uri uri) throws Exception {
        try (java.io.InputStream in = getContentResolver().openInputStream(uri);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            if (in == null) return null;
            byte[] buf = new byte[16384];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_RULE_BYTES) break;
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    /** zip/hkzip 取 rule.json；不是压缩包就当文本。 */
    private String extractRuleText(byte[] raw, String name) throws Exception {
        boolean zip = raw.length > 4 && raw[0] == 'P' && raw[1] == 'K';
        if (!zip) return new String(raw, "UTF-8");
        String best = null, anyJson = null;
        try (java.util.zip.ZipInputStream zin =
                     new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(raw))) {
            java.util.zip.ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                String en = e.getName() == null ? "" : e.getName();
                if (e.isDirectory()) continue;
                String lower = en.toLowerCase(java.util.Locale.ROOT);
                boolean isRule = lower.endsWith("/rule.json") || lower.equals("rule.json");
                boolean isJson = lower.endsWith(".json");
                if (!isRule && !isJson) continue;
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n, total = 0;
                while ((n = zin.read(buf)) > 0) {
                    total += n;
                    if (total > MAX_RULE_BYTES) break;
                    bo.write(buf, 0, n);
                }
                byte[] bytes = bo.toByteArray();
                /* libs.zip / data.zip：规则包把真正的引擎模块放在内层压缩包里，
                 * 文件名是哈希（f639e0….js），require('f639e0….js') 才能取到。
                 * 把它们平铺解到 filesDir/hiker/，引擎的 readFile 直接就能读到。 */
                if (lower.endsWith(".zip")) {
                    extractNested(bytes);
                    continue;
                }
                String text = new String(bytes, "UTF-8");
                if (isRule && text.trim().startsWith("{")) return text;
                if (isJson && anyJson == null && text.trim().startsWith("{")) anyJson = text;
                if (best == null) best = text;
            }
        }
        return anyJson != null ? anyJson : best;
    }

    /** 把内层 zip（libs.zip/data.zip）里的 .js 模块平铺解到 filesDir/hiker/。 */
    private void extractNested(byte[] zipBytes) {
        try {
            java.io.File dir = new java.io.File(getFilesDir(), "hiker");
            if (!dir.exists()) dir.mkdirs();
            try (java.util.zip.ZipInputStream zin =
                         new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
                java.util.zip.ZipEntry e;
                while ((e = zin.getNextEntry()) != null) {
                    String en = e.getName() == null ? "" : e.getName();
                    if (e.isDirectory()) continue;
                    String base = en.substring(en.lastIndexOf('/') + 1);
                    if (!base.toLowerCase(java.util.Locale.ROOT).endsWith(".js")) continue;
                    java.io.File out = new java.io.File(dir, base);
                    try (java.io.FileOutputStream fo = new java.io.FileOutputStream(out)) {
                        byte[] buf = new byte[16384];
                        int n;
                        while ((n = zin.read(buf)) > 0) fo.write(buf, 0, n);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 回传：文件名 + 错误（二选一）+ 正文 Base64。用 Base64 避免任何转义/换行炸 JS。 */
    private void deliverFileResult(String name, String error) {
        deliverFileResult(name, error, null);
    }

    private void deliverFileResult(final String name, final String error, final String text) {
        if (webView == null) return;
        final String b64 = text == null ? "" :
                android.util.Base64.encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        android.util.Base64.NO_WRAP);
        runOnUiThread(() -> {
            if (webView == null) return;
            String js = "window.HikerFile&&window.HikerFile.onText("
                    + org.json.JSONObject.quote(name == null ? "" : name) + ","
                    + org.json.JSONObject.quote(error == null ? "" : error) + ","
                    + org.json.JSONObject.quote(b64) + ")";
            try {
                webView.evaluateJavascript(js, null);
            } catch (Throwable ignored) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        /* 页面栈在 JS 里（HikerUI.STATE.stack）。先问页面能不能退：
         * 能退就让页面自己回上一级，退到底了才关掉这个 Activity。 */
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function(){try{return window.HikerUI && HikerUI.back ? (HikerUI.back(), '1') : '0';}"
                            + "catch(e){return '0';}})()",
                    value -> {
                        boolean handled = value != null && value.indexOf('1') >= 0;
                        if (!handled) {
                            if (webView != null && webView.canGoBack()) webView.goBack();
                            else finish();
                        }
                    });
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
