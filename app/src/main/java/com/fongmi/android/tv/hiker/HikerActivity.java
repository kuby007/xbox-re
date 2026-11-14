package com.fongmi.android.tv.hiker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

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

    private static final int REQUEST_FILE_CHOOSER = 0x4846;   /* "HF" */

    private WebView webView;
    private HikerBridge bridge;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 横屏 bug 修复（HIKER-FULL）：方向已按 flavor 在 Manifest 分开声明 ——
        // main 改 unspecified（不再写死 sensorLandscape 让手机一进海阔先横屏）、
        // mobile 重声明 fullUser、leanback 重声明 sensorLandscape。
        // 这里保留运行时兜底：Manifest 合并结果万一仍偏离预期，leanback 设备请求
        // 横屏、其余设备跟随用户方向，与原补丁的最终效果一致，只是不再「先横后正」。
        boolean leanback = getPackageManager().hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_LEANBACK);
        setRequestedOrientation(leanback
                ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

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
            /* 文件选择兜底：JS 的 <input type="file">（.hkzip/.json 导入入口）
             * 没有它点了一律无反应。用系统单选选择器（* /*），结果经
             * filePathCallback 回传 WebView，结果处理在 onActivityResult。 */
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                }
                filePathCallback = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    String[] accepts = null;
                    try { accepts = params.getAcceptTypes(); } catch (Throwable ignored) {}
                    if (accepts != null && accepts.length == 1 && accepts[0] != null
                            && accepts[0].length() > 0 && !"*/*".equals(accepts[0])) {
                        intent.setType(accepts[0]);
                    }
                    startActivityForResult(intent, REQUEST_FILE_CHOOSER);
                } catch (Throwable t) {
                    if (filePathCallback != null) {
                        filePathCallback.onReceiveValue(null);
                        filePathCallback = null;
                    }
                }
                return true;
            }

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
        });

        webView.loadUrl(HikerEntry.ASSET_URL);
    }

    /** 文件选择结果回传 WebView（onShowFileChooser 的另一半）。 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FILE_CHOOSER) return;
        if (filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            results = new Uri[]{data.getData()};
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    /**
     * 桥事件回传。播放事件按 HIKER-FULL 契约处理：
     *   - JS 调 bridge.report("play", JSON{url,title,pic,headers,flag})（type 亦兼容
     *     HikerContract.EV_PLAY = "hiker.play"）且 payload 为 JSON 时，解析后经
     *     HikerBridge.pushRich 投递；宿主 PUSH 链只接受单个 URL 字符串（见 pushRich
     *     注释），title/pic/headers/flag 如实留痕、无法透传。与 bridge.play 的同 URL
     *     二次投递在 HikerBridge 内去重（5 秒窗口）。
     *   - 纯字符串 payload（bridge.play 已投递的回执）保持旧行为：仅日志。
     * 其余事件同样仅做日志，不调用首页任何类，避免与首页生命周期耦合。
     */
    public void onBridgeEvent(String type, String payload) {
        if (("play".equals(type) || HikerContract.EV_PLAY.equals(type))
                && payload != null && payload.trim().startsWith("{")) {
            try {
                Object parsed = HikerJson.parse(payload);
                if (parsed instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) parsed;
                    String url = HikerJson.str(m.get("url"));
                    if (url.length() > 0 && bridge != null) {
                        Map<String, String> headers = new HashMap<>();
                        Object h = m.get("headers");
                        if (h instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> hm = (Map<String, Object>) h;
                            for (Map.Entry<String, Object> e : hm.entrySet())
                                headers.put(e.getKey(), HikerJson.str(e.getValue()));
                        }
                        bridge.pushRich(url, HikerJson.str(m.get("title")),
                                HikerJson.str(m.get("pic")), headers,
                                HikerJson.str(m.get("flag")));
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        android.util.Log.d("HikerBridge", type + ": "
                + (payload == null ? "" : payload.substring(0, Math.min(160, payload.length()))));
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
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
