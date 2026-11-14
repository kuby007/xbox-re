package com.fongmi.android.tv.hiker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 海阔 Cookie 统一存储（HIKER-FULL 契约）：HikerHttp 与 HikerApi 共用同一份 jar，
 * 解决审计 P0-4「fetch 不存 Cookie、HikerApi 另有一套孤立 COOKIES 表」的断裂。
 *
 * 形态：
 *   - 进程级单例（静态状态），内存 LinkedHashMap：host -> (name -> value)，保序；
 *   - 落盘 filesDir/hiker/cookies.txt（root 由 HikerApi.setRoot 注入，与文件存储同根）；
 *     格式一行一条：host \t name \t value（value 内的 \t/\n 已转义），启动后首次访问惰性加载；
 *   - 线程安全：所有公开方法 synchronized（桥在 JavaBridge 线程、HTTP 在规则线程并发调用）。
 *
 * 只用 JDK，与 HikerApi 同风格，可用 javac 单独编译验证。Cookie 语义保持既有口径：
 * 只取 Set-Cookie 的第一段 name=value（与原 HikerApi.putCookie 一致），不做 domain/path
 * 匹配——规则场景里按 host 精确存取已经够用，且不改变既有 getCookie 的返回形态。
 */
public final class HikerCookies {

    private static final Map<String, LinkedHashMap<String, String>> JAR = new LinkedHashMap<>();
    private static boolean loaded = false;

    private HikerCookies() {
    }

    public static String hostOf(String url) {
        if (url == null) return "";
        try {
            String h = new URL(url).getHost();
            return h == null ? "" : h;
        } catch (Throwable t) {
            return "";
        }
    }

    /** 取某 URL 对应 host 的 Cookie 头字符串（"k=v; k2=v2"），没有返回 ""。 */
    public static synchronized String get(String url) {
        ensureLoaded();
        Map<String, String> jar = JAR.get(hostOf(url));
        if (jar == null || jar.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : jar.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /** 存响应里的 Set-Cookie 行（一行可为一整条 "name=value; Path=/; ..."）。 */
    public static synchronized void putFromSetCookie(String url, List<String> setCookies) {
        if (setCookies == null || setCookies.isEmpty()) return;
        List<String> pairs = new ArrayList<>();
        for (String line : setCookies) {
            if (line == null) continue;
            String first = line.split(";", 2)[0].trim();
            int p = first.indexOf('=');
            if (p > 0) pairs.add(first.substring(0, p) + "\u0001" + first.substring(p + 1));
        }
        if (pairs.isEmpty()) return;
        ensureLoaded();
        String host = hostOf(url);
        LinkedHashMap<String, String> jar = JAR.get(host);
        if (jar == null) {
            jar = new LinkedHashMap<>();
            JAR.put(host, jar);
        }
        for (String pair : pairs) {
            int p = pair.indexOf('\u0001');
            jar.put(pair.substring(0, p), pair.substring(p + 1));
        }
        save();
    }

    /** 存 JS 显式给的 Cookie 字符串（"k=v; k2=v2"，setCookie API 用）。 */
    public static synchronized void putFromHeader(String url, String cookieStr) {
        if (cookieStr == null || cookieStr.length() == 0) return;
        ensureLoaded();
        String host = hostOf(url);
        LinkedHashMap<String, String> jar = JAR.get(host);
        if (jar == null) {
            jar = new LinkedHashMap<>();
            JAR.put(host, jar);
        }
        for (String part : cookieStr.split(";")) {
            String kv = part.trim();
            int p = kv.indexOf('=');
            if (p > 0) jar.put(kv.substring(0, p).trim(), kv.substring(p + 1).trim());
            else if (p < 0 && kv.length() > 0) jar.put(kv, "");
        }
        save();
    }

    /** 已存 host 数（report 用）。 */
    public static synchronized int size() {
        ensureLoaded();
        return JAR.size();
    }

    /* ------------------------------------------------------------ 落盘 */

    private static File file() {
        /* 与 HikerApi 的文件根保持同一目录：ROOT 由 HikerBridge 构造时注入为
         * filesDir/hiker。这里不直接调用 HikerApi.root()（它是 private），
         * 用同一注入值经 HikerApi.rootPath() 读取。 */
        return new File(HikerApi.rootPath(), "cookies.txt");
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File f = file();
            if (!f.exists()) return;
            FileInputStream in = new FileInputStream(f);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            in.close();
            String text = new String(bos.toByteArray(), "UTF-8");
            for (String line : text.split("\n")) {
                String[] parts = line.split("\t", -1);
                if (parts.length != 3) continue;
                LinkedHashMap<String, String> jar = JAR.get(parts[0]);
                if (jar == null) {
                    jar = new LinkedHashMap<>();
                    JAR.put(parts[0], jar);
                }
                jar.put(unescape(parts[1]), unescape(parts[2]));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void save() {
        try {
            File f = file();
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, LinkedHashMap<String, String>> h : JAR.entrySet()) {
                for (Map.Entry<String, String> e : h.getValue().entrySet()) {
                    sb.append(h.getKey()).append('\t')
                            .append(escape(e.getKey())).append('\t')
                            .append(escape(e.getValue())).append('\n');
                }
            }
            FileOutputStream out = new FileOutputStream(f);
            out.write(sb.toString().getBytes("UTF-8"));
            out.close();
        } catch (Throwable ignored) {
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char e = s.charAt(++i);
                if (e == 't') sb.append('\t');
                else if (e == 'n') sb.append('\n');
                else if (e == 'r') sb.append('\r');
                else sb.append(e);   /* \\ -> \，未知转义原样保留后字 */
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
