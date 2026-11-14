package com.fongmi.android.tv.hiker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 同步 HTTP —— 在 JavaBridge 线程上执行，返回「字符串」，不是 Promise。
 *
 * 为什么不用宿主 HomeWebBridge / fm.req：
 *   oktv-cat 的 invoke() 走 Task.execute 线程池 + fongmiNative.resolve 回调，是异步；
 *   海阔规则形如 eval.call(null, fetch(url)) / pdfh(fetch(url), ...)，要求同步拿到正文。
 *   阶段二（hiker_54.db / home_js）与阶段四（v390 JsReqBridge）都证实必须同步。
 *
 * 本类刻意只依赖 JDK（HttpURLConnection），不 import 任何 com.fongmi.android.tv.* 业务类，
 * 因此可以用 ecj + android-all.jar 单独真实编译，作为「构建前置」的编译验证对象。
 *
 * HIKER-FULL 变更（审计 P0-4 / P0-5）：
 *   - Cookie：请求前从 HikerCookies 按 URL host 注入 Cookie 头（调用方显式 Cookie 头优先），
 *     响应后把 Set-Cookie 存回 HikerCookies —— 与 HikerApi.getCookie/fetchCookie 共用同一 jar；
 *   - 可配置超时：request(..., timeoutSeconds)，<=0 时用默认 15s；
 *   - Result：带 status 与全部响应头（含合并的 Set-Cookie）的完整结果，
 *     供 fetchFull 的 withHeaders/onlyHeaders 语义使用；老 get/post 只取 body，行为不变。
 */
public final class HikerHttp {

    private static final int TIMEOUT = 15000;
    private static final int MAX_BODY = 8 * 1024 * 1024;

    private HikerHttp() {
    }

    /** 一次请求的完整结果：状态码 + 响应头（key 保原始大小写，Set-Cookie 多值以 \n 拼接）+ 正文。 */
    public static final class Result {
        public final int status;
        public final Map<String, String> headers;
        public final String body;

        Result(int status, Map<String, String> headers, String body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }
    }

    /** 同步 GET，返回响应正文字符串（失败返回空串，绝不返回 Promise / 对象）。 */
    public static String get(String url, Map<String, String> headers) {
        return request("GET", url, null, headers, 0).body;
    }

    /** 同步 GET（可配置超时，单位秒；<=0 用默认 15s）。 */
    public static String get(String url, Map<String, String> headers, int timeoutSeconds) {
        return request("GET", url, null, headers, timeoutSeconds).body;
    }

    /** 同步 POST（表单或 JSON 字符串 body）。 */
    public static String post(String url, String body, Map<String, String> headers) {
        return request("POST", url, body, headers, 0).body;
    }

    /** 同步 POST（可配置超时，单位秒；<=0 用默认 15s）。 */
    public static String post(String url, String body, Map<String, String> headers, int timeoutSeconds) {
        return request("POST", url, body, headers, timeoutSeconds).body;
    }

    /**
     * 同步请求的完整版本：返回状态码与响应头。
     * 失败（异常/无响应）返回 status=0、空 headers、空 body，不抛异常。
     */
    public static Result request(String method, String url, String body,
                                 Map<String, String> headers, int timeoutSeconds) {
        HttpURLConnection conn = null;
        try {
            int timeout = timeoutSeconds > 0 ? timeoutSeconds * 1000 : TIMEOUT;
            boolean post = "POST".equalsIgnoreCase(method);
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(post ? "POST" : "GET");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("User-Agent", HikerContract.UA);
            /* Cookie 注入：先 jar 后显式头，显式头里已有 Cookie 就不覆盖（调用方优先）。 */
            String jarCookie = HikerCookies.get(url);
            boolean callerCookie = false;
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        if ("Cookie".equalsIgnoreCase(e.getKey())) callerCookie = true;
                        conn.setRequestProperty(e.getKey(), e.getValue());
                    }
                }
            }
            if (jarCookie.length() > 0 && !callerCookie) {
                conn.setRequestProperty("Cookie", jarCookie);
            }
            if (post) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                if (body != null) os.write(body.getBytes("UTF-8"));
                os.flush();
                os.close();
            }
            int status = conn.getResponseCode();
            Map<String, String> respHeaders = collectHeaders(conn);
            String setCookie = respHeaders.get("Set-Cookie");
            if (setCookie == null) {
                /* 大小写兜底：getHeaderFields 的 key 在不同实现上大小写不一。 */
                for (Map.Entry<String, String> e : respHeaders.entrySet()) {
                    if ("Set-Cookie".equalsIgnoreCase(e.getKey())) setCookie = e.getValue();
                }
            }
            if (setCookie != null && setCookie.length() > 0) {
                List<String> lines = new ArrayList<>();
                for (String line : setCookie.split("\n")) lines.add(line);
                HikerCookies.putFromSetCookie(url, lines);
            }
            String text = read(conn, status);
            return new Result(status, respHeaders, text);
        } catch (Throwable t) {
            return new Result(0, new LinkedHashMap<String, String>(), "");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 收响应头：同名多值以 \n 拼接（Set-Cookie 不能用逗号拼，逗号在 expires 里合法）。 */
    private static Map<String, String> collectHeaders(HttpURLConnection conn) {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, List<String>> e : conn.getHeaderFields().entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                StringBuilder sb = new StringBuilder();
                for (String v : e.getValue()) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(v == null ? "" : v);
                }
                out.put(e.getKey(), sb.toString());
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String read(HttpURLConnection conn, int code) throws Exception {
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) return "";
        if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) in = new GZIPInputStream(in);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
            if (bos.size() > MAX_BODY) break;
        }
        in.close();
        byte[] bytes = bos.toByteArray();
        Charset cs = detect(conn.getContentType(), bytes);
        return new String(bytes, cs);
    }

    /** 编码探测：海阔站源大量 GBK，Content-Type 未声明时按 UTF-8 合法性回退 GBK。 */
    private static Charset detect(String contentType, byte[] bytes) {
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (ct.contains("gbk") || ct.contains("gb2312") || ct.contains("gb18030")) {
            return forName("GBK");
        }
        if (ct.contains("utf-8") || ct.contains("utf8")) return forName("UTF-8");
        if (isUtf8(bytes)) return forName("UTF-8");
        return forName("GBK");
    }

    private static Charset forName(String name) {
        try {
            return Charset.forName(name);
        } catch (Throwable t) {
            return Charset.defaultCharset();
        }
    }

    private static boolean isUtf8(byte[] b) {
        int i = 0;
        while (i < b.length) {
            int c = b[i] & 0xFF;
            if (c < 0x80) { i++; continue; }
            int need = c >= 0xF0 ? 3 : c >= 0xE0 ? 2 : c >= 0xC0 ? 1 : -1;
            if (need < 0 || i + need >= b.length) return false;
            for (int k = 1; k <= need; k++) if ((b[i + k] & 0xC0) != 0x80) return false;
            i += need + 1;
        }
        return true;
    }
}
