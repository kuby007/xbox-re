/*!
 * hiker_engine.js —— 海阔小程序运行时内核（纯 JS，跑在 WebView 里）
 *
 * 设计要点（这一版是重写，不是补丁）：
 *   1. 能用 WebView 原生能力的，一律不经 Java 桥：atob/btoa、localStorage、
 *      setTimeout、DOMParser（pd/pdfh 的选择器全部走原生 querySelectorAll，
 *      比手写 HTML parser 准得多）。
 *   2. 必须走原生的，只有一个：**同步 HTTP**。海阔规则的硬约束是
 *      `var html = fetch(url)` 同步拿字符串，浏览器 fetch 是 Promise，不能用。
 *      所以这条走 Java 同步桥（HikerBridge.req）。
 *   3. 模块系统（$.require / $.exports / $.toString）、hiker:// 协议、
 *      lazyRule、@rule=、col_type 结果集 —— 全部在这层实现。
 *
 * 兼容语法：规则自身的代码可能是 ES6（箭头函数/const），由 WebView 解析；
 * 内核自身用 ES5 写，避免老 WebView（Chrome 51）跑不起来。
 */
(function (global) {
  'use strict';

  var HK = { version: '3.0.0' };
  var bridge = global.HikerBridge || null;

  /* ==================================================================== *
   * 0. 基础工具
   * ==================================================================== */
  function str(v) { return (v === null || v === undefined) ? '' : String(v); }
  function trim(v) { return str(v).replace(/^[\s\uFEFF]+|[\s\uFEFF]+$/g, ''); }
  function has(o, k) { return Object.prototype.hasOwnProperty.call(o, k); }
  function jparse(s, d) { try { return JSON.parse(s); } catch (e) { return d; } }
  function isArr(v) { return Object.prototype.toString.call(v) === '[object Array]'; }
  function now() { return new Date().getTime(); }

  /* ---- base64：优先原生（WebView 自带），无则自实现，绝不转发给 Java ---- */
  var B64C = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  function utf8Encode(s) { return unescape(encodeURIComponent(str(s))); }
  function utf8Decode(s) { return decodeURIComponent(escape(str(s))); }

  function b64e(s) {
    var raw = utf8Encode(s);
    if (global.btoa) { try { return global.btoa(raw); } catch (e) { /* fallthrough */ } }
    var out = '', i, c1, c2, c3;
    for (i = 0; i < raw.length; i += 3) {
      c1 = raw.charCodeAt(i); c2 = raw.charCodeAt(i + 1); c3 = raw.charCodeAt(i + 2);
      out += B64C.charAt(c1 >> 2);
      out += B64C.charAt(((c1 & 3) << 4) | (isNaN(c2) ? 0 : c2 >> 4));
      out += isNaN(c2) ? '=' : B64C.charAt(((c2 & 15) << 2) | (isNaN(c3) ? 0 : c3 >> 6));
      out += isNaN(c3) ? '=' : B64C.charAt(c3 & 63);
    }
    return out;
  }

  function b64d(s) {
    var t = str(s).replace(/[\s\r\n]/g, '').replace(/-/g, '+').replace(/_/g, '/');
    var pad = t.length % 4; if (pad) { t += pad === 2 ? '==' : pad === 3 ? '=' : '=='; }
    if (global.atob) { try { return utf8Decode(global.atob(t)); } catch (e) { /* fallthrough */ } }
    var out = '', i, b0, b1, b2, b3;
    for (i = 0; i < t.length; i += 4) {
      b0 = B64C.indexOf(t.charAt(i)); b1 = B64C.indexOf(t.charAt(i + 1));
      b2 = B64C.indexOf(t.charAt(i + 2)); b3 = B64C.indexOf(t.charAt(i + 3));
      if (b0 < 0 || b1 < 0) continue;
      out += String.fromCharCode((b0 << 2) | (b1 >> 4));
      if (b2 >= 0) out += String.fromCharCode(((b1 & 15) << 4) | (b2 >> 2));
      if (b3 >= 0) out += String.fromCharCode(((b2 & 3) << 6) | b3);
    }
    return utf8Decode(out);
  }
  HK.b64d = b64d; HK.b64e = b64e;

  /* ---- MD5（纯 JS，规则里 md5() 用得很多，不能靠桥） ---- */
  var MD5 = (function () {
    function add32(a, b) { return (a + b) & 0xFFFFFFFF; }
    function cmn(q, a, b, x, s, t) {
      a = add32(add32(a, q), add32(x, t));
      return add32((a << s) | (a >>> (32 - s)), b);
    }
    function ff(a, b, c, d, x, s, t) { return cmn((b & c) | ((~b) & d), a, b, x, s, t); }
    function gg(a, b, c, d, x, s, t) { return cmn((b & d) | (c & (~d)), a, b, x, s, t); }
    function hh(a, b, c, d, x, s, t) { return cmn(b ^ c ^ d, a, b, x, s, t); }
    function ii(a, b, c, d, x, s, t) { return cmn(c ^ (b | (~d)), a, b, x, s, t); }
    function md5cycle(x, k) {
      var a = x[0], b = x[1], c = x[2], d = x[3];
      a = ff(a, b, c, d, k[0], 7, -680876936); d = ff(d, a, b, c, k[1], 12, -389564586);
      c = ff(c, d, a, b, k[2], 17, 606105819); b = ff(b, c, d, a, k[3], 22, -1044525330);
      a = ff(a, b, c, d, k[4], 7, -176418897); d = ff(d, a, b, c, k[5], 12, 1200080426);
      c = ff(c, d, a, b, k[6], 17, -1473231341); b = ff(b, c, d, a, k[7], 22, -45705983);
      a = ff(a, b, c, d, k[8], 7, 1770035416); d = ff(d, a, b, c, k[9], 12, -1958414417);
      c = ff(c, d, a, b, k[10], 17, -42063); b = ff(b, c, d, a, k[11], 22, -1990404162);
      a = ff(a, b, c, d, k[12], 7, 1804603682); d = ff(d, a, b, c, k[13], 12, -40341101);
      c = ff(c, d, a, b, k[14], 17, -1502002290); b = ff(b, c, d, a, k[15], 22, 1236535329);
      a = gg(a, b, c, d, k[1], 5, -165796510); d = gg(d, a, b, c, k[6], 9, -1069501632);
      c = gg(c, d, a, b, k[11], 14, 643717713); b = gg(b, c, d, a, k[0], 20, -373897302);
      a = gg(a, b, c, d, k[5], 5, -701558691); d = gg(d, a, b, c, k[10], 9, 38016083);
      c = gg(c, d, a, b, k[15], 14, -660478335); b = gg(b, c, d, a, k[4], 20, -405537848);
      a = gg(a, b, c, d, k[9], 5, 568446438); d = gg(d, a, b, c, k[14], 9, -1019803690);
      c = gg(c, d, a, b, k[3], 14, -187363961); b = gg(b, c, d, a, k[8], 20, 1163531501);
      a = gg(a, b, c, d, k[13], 5, -1444681467); d = gg(d, a, b, c, k[2], 9, -51403784);
      c = gg(c, d, a, b, k[7], 14, 1735328473); b = gg(b, c, d, a, k[12], 20, -1926607734);
      a = hh(a, b, c, d, k[5], 4, -378558); d = hh(d, a, b, c, k[8], 11, -2022574463);
      c = hh(c, d, a, b, k[11], 16, 1839030562); b = hh(b, c, d, a, k[14], 23, -35309556);
      a = hh(a, b, c, d, k[1], 4, -1530992060); d = hh(d, a, b, c, k[4], 11, 1272893353);
      c = hh(c, d, a, b, k[7], 16, -155497632); b = hh(b, c, d, a, k[10], 23, -1094730640);
      a = hh(a, b, c, d, k[13], 4, 681279174); d = hh(d, a, b, c, k[0], 11, -358537222);
      c = hh(c, d, a, b, k[3], 16, -722521979); b = hh(b, c, d, a, k[6], 23, 76029189);
      a = hh(a, b, c, d, k[9], 4, -640364487); d = hh(d, a, b, c, k[12], 11, -421815835);
      c = hh(c, d, a, b, k[15], 16, 530742520); b = hh(b, c, d, a, k[2], 23, -995338651);
      a = ii(a, b, c, d, k[0], 6, -198630844); d = ii(d, a, b, c, k[7], 10, 1126891415);
      c = ii(c, d, a, b, k[14], 15, -1416354905); b = ii(b, c, d, a, k[5], 21, -57434055);
      a = ii(a, b, c, d, k[12], 6, 1700485571); d = ii(d, a, b, c, k[3], 10, -1894986606);
      c = ii(c, d, a, b, k[10], 15, -1051523); b = ii(b, c, d, a, k[1], 21, -2054922799);
      a = ii(a, b, c, d, k[8], 6, 1873313359); d = ii(d, a, b, c, k[15], 10, -30611744);
      c = ii(c, d, a, b, k[6], 15, -1560198380); b = ii(b, c, d, a, k[13], 21, 1309151649);
      a = ii(a, b, c, d, k[4], 6, -145523070); d = ii(d, a, b, c, k[11], 10, -1120210379);
      c = ii(c, d, a, b, k[2], 15, 718787259); b = ii(b, c, d, a, k[9], 21, -343485551);
      x[0] = add32(a, x[0]); x[1] = add32(b, x[1]); x[2] = add32(c, x[2]); x[3] = add32(d, x[3]);
    }
    function md5blk(s) {
      var md5blks = [], i;
      for (i = 0; i < 64; i += 4)
        md5blks[i >> 2] = s.charCodeAt(i) + (s.charCodeAt(i + 1) << 8) +
          (s.charCodeAt(i + 2) << 16) + (s.charCodeAt(i + 3) << 24);
      return md5blks;
    }
    var hexChr = '0123456789abcdef'.split('');
    function rhex(n) {
      var s = '', j;
      for (j = 0; j < 4; j++) s += hexChr[(n >> (j * 8 + 4)) & 0x0F] + hexChr[(n >> (j * 8)) & 0x0F];
      return s;
    }
    return function (s) {
      var n = str(s).length, state = [1732584193, -271733879, -1732584194, 271733878], i, tail;
      var blk;
      for (i = 64; i <= n; i += 64) md5cycle(state, md5blk(str(s).substring(i - 64, i)));
      s = str(s).substring(i - 64);
      tail = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
      for (i = 0; i < s.length; i++) tail[i >> 2] |= s.charCodeAt(i) << ((i % 4) << 3);
      tail[i >> 2] |= 0x80 << ((i % 4) << 3);
      if (i > 55) { md5cycle(state, tail); for (i = 0; i < 16; i++) tail[i] = 0; }
      tail[14] = n * 8;
      md5cycle(state, tail);
      blk = state; var out = '';
      for (i = 0; i < 4; i++) out += rhex(blk[i]);
      return out;
    };
  })();
  HK.md5 = MD5;

  /* ==================================================================== *
   * 1. 同步网络：只此一条走 Java 桥
   * ==================================================================== */
  function reqSync(url, options) {
    var u = trim(url);
    if (!u) return '';
    var o = options || {};
    if (typeof o === 'string') o = jparse(o, {}) || {};
    var payload = JSON.stringify({
      method: (o.method || 'GET'), headers: (o.headers || {}), body: (o.body || o.data || ''),
      timeout: (o.timeout || 20000)
    });
    if (bridge && typeof bridge.req === 'function') {
      try { var r = bridge.req(u, payload); return (r === null || r === undefined) ? '' : String(r); }
      catch (e) { return ''; }
    }
    /* 调试/离线：无桥时用同步 XHR（浏览器内直接打开 index.html 时才有意义） */
    try {
      var x = new XMLHttpRequest();
      x.open((o.method || 'GET').toUpperCase(), u, false);
      if (o.headers) for (var k in o.headers) if (has(o.headers, k)) { try { x.setRequestHeader(k, o.headers[k]); } catch (e2) {} }
      x.send(o.body || null);
      return x.responseText || '';
    } catch (e3) { return ''; }
  }
  HK.req = reqSync;

  /* ==================================================================== *
   * 2. DOM 选择器（pd / pdfh / pdfa / parseDom*）
   *    全部基于原生 DOMParser + querySelectorAll，语法对齐海阔：
   *      "&&" 逐级下钻、"tag.class"、"#id"、"[attr=val]"、":eq(n)"、
   *      末尾 "--attr" 取属性，--Text / --Html / --outerHtml 取文本与 HTML
   * ==================================================================== */
  var docCache = { html: null, doc: null };
  function toDoc(html) {
    var h = str(html);
    if (docCache.html === h && docCache.doc) return docCache.doc;
    var doc = null;
    try {
      if (typeof DOMParser !== 'undefined') doc = new DOMParser().parseFromString(h, 'text/html');
    } catch (e) {}
    if (!doc) {
      doc = global.document.createElement('div');
      doc.innerHTML = h;
    }
    docCache.html = h; docCache.doc = doc;
    return doc;
  }

  function normSeg(seg) {
    var s = trim(seg);
    if (!s) return '';
    /* 海阔里 ":eq(2)" 转成 CSS :nth-of-type 语义不好等价，单独处理 */
    return s;
  }

  function pickAll(root, seg) {
    var s = normSeg(seg);
    if (!s) return [];
    var eq = /:eq\((\d+)\)$/.exec(s);
    var lt = /:lt\((\d+)\)$/.exec(s);
    var gt = /:gt\((\d+)\)$/.exec(s);
    var base = eq ? s.slice(0, eq.index) : (lt ? s.slice(0, lt.index) : (gt ? s.slice(0, gt.index) : s));
    var list = [];
    try { list = Array.prototype.slice.call(root.querySelectorAll(base || '*')); }
    catch (e) { list = []; }
    if (eq) { var n = parseInt(eq[1], 10); return list[n] ? [list[n]] : []; }
    if (lt) { return list.slice(0, parseInt(lt[1], 10)); }
    if (gt) { return list.slice(parseInt(gt[1], 10) + 1); }
    return list;
  }

  /**
   * 逐级下钻。返回节点数组。
   * rule 形如 "class.list&&li&&a"，或 "body&&div.item:eq(0)&&Text"。
   */
  function walk(root, rule, all) {
    var segs = str(rule).split('&&'), cur = [root], i, j, nx;
    var prop = '';
    if (segs.length) {
      var last = trim(segs[segs.length - 1]);
      var pi = last.indexOf('--');
      if (pi >= 0) { prop = trim(last.slice(pi + 2)); segs[segs.length - 1] = last.slice(0, pi); }
    }
    for (i = 0; i < segs.length; i++) {
      var seg = trim(segs[i]);
      if (!seg) continue;
      nx = [];
      for (j = 0; j < cur.length; j++) nx = nx.concat(pickAll(cur[j], seg));
      cur = nx;
      if (!cur.length) return all ? [] : null;
      if (!all && i < segs.length - 1) { /* 继续下钻 */ }
    }
    return all ? cur : (cur[0] || null);
  }

  function valueOf(node, prop) {
    if (!node) return '';
    var p = trim(prop).toLowerCase();
    if (!p || p === 'text' || p === 'textnode') return trim(node.textContent || '');
    if (p === 'html' || p === 'innerhtml') return node.innerHTML || '';
    if (p === 'outerhtml') return node.outerHTML || '';
    if (p === 'ownhtml') return node.innerHTML || '';
    return node.getAttribute ? (node.getAttribute(prop) || '') : '';
  }

  function pdfh(html, rule) {
    var root = toDoc(html);
    var body = root.body || root.documentElement || root;
    var node = walk(body, rule, false);
    if (!node) return '';
    var segs = str(rule).split('&&');
    var last = trim(segs[segs.length - 1] || '');
    var pi = last.indexOf('--');
    var prop = pi >= 0 ? trim(last.slice(pi + 2)) : '';
    return valueOf(node, prop);
  }

  function pdfa(html, rule) {
    var root = toDoc(html);
    var body = root.body || root.documentElement || root;
    var segs = str(rule).split('&&');
    var last = trim(segs[segs.length - 1] || '');
    var pi = last.indexOf('--');
    var prop = pi >= 0 ? trim(last.slice(pi + 2)) : '';
    var nodes = walk(body, rule, true) || [];
    var out = [];
    for (var i = 0; i < nodes.length; i++) out.push(valueOf(nodes[i], prop));
    return out;
  }
  HK.pdfh = pdfh; HK.pdfa = pdfa;

  /* ==================================================================== *
   * 3. hiker:// 协议
   *    hiker://empty#noLoading##@rule=js:<code>
   *    hiker://empty#@lazyRule=.js:<code>(<args>)
   *    普通 http(s) -> 播放/打开
   * ==================================================================== */
  var RE_RULE = /(?:^|#)@?rule=js:([\s\S]*)$/;
  var RE_LAZY = /(?:^|#)@?lazyRule=(?:\.?js:)?([\s\S]*)$/;

  function parseHikerUrl(url) {
    var u = str(url);
    /* [P1] 合集框架用全角分隔符拼入口（？？ / ；； / ＃ / ＝ / ＆）。
     * 不归一化的话 scheme 里会带全角字符，page/模块名永远匹配不上，
     * 表现就是「点子站没反应」。在解析最前端一次性换成半角。 */
    u = u.replace(/\uFF1F/g, '?').replace(/\uFF1B/g, ';')
         .replace(/\uFF03/g, '#').replace(/\uFF1D/g, '=')
         .replace(/\uFF06/g, '&').replace(/\uFF20/g, '@')
         .replace(/\uFF1A/g, ':');
    var out = { raw: str(url), rawNormalized: u, base: u, flags: [], rule: null,
                lazy: null, video: false, search: null, page: null };
    var im = /(?:^|#)@?input=([^#]*)/.exec(u);
    if (im) {
      try { out.inputArg = JSON.parse(decodeURIComponent(im[1])); } catch (e) { out.inputArg = null; }
    }
    var m = RE_LAZY.exec(u);
    if (m) { out.lazy = m[1]; out.base = u.slice(0, m.index); }
    else {
      var m2 = RE_RULE.exec(u);
      if (m2) { out.rule = m2[1]; out.base = u.slice(0, m2.index); }
    }
    var head = out.base.replace(/^hiker:\/\//, '');
    var hp = head.split('#');
    out.scheme = hp[0] || '';
    for (var i = 1; i < hp.length; i++) if (trim(hp[i])) out.flags.push(trim(hp[i]));
    if (/isVideo=true/i.test(u)) out.video = true;
    var sm = /^search\?s=([\s\S]*)$/.exec(out.scheme);
    if (sm) out.search = decodeURIComponent(sm[1]);
    var pm = /^page\/([\s\S]*)$/.exec(out.scheme);
    if (pm) out.page = pm[1];
    return out;
  }
  HK.parseUrl = parseHikerUrl;

  /* ==================================================================== *
   * 4. 作用域：每条规则拿到的 API 集合
   * ==================================================================== */
  var STORE = {
    myVars: {},      /* 规则名@key -> value */
    vars: {},
    items: {},       /* 当前页面条目 id -> item（updateItem/findItem 用） */
    kv: {},          /* [A-P1-3] getItem/setItem 的持久 KV（与页面条目注册表分开，
                        对齐官方 kvFile 语义；findItem/updateItem 仍走 items） */
    files: {},       /* 内存文件（未落盘时的兜底） */
    modules: {},     /* 模块缓存 ruleKey/path -> exports */
    rawRules: {}     /* 规则原文缓存 */
  };
  HK.STORE = STORE;

  /* [A-P1-3] 变量持久化：vars / myVars / kv 落 localStorage，重启不丢登录态。
   * 只存可 JSON 化的值；存不了（循环引用/超限）就静默跳过，不打断规则。 */
  function persistSave() {
    try {
      if (!global.localStorage) return;
      global.localStorage.setItem('hk_vars_v1', JSON.stringify(STORE.vars));
      global.localStorage.setItem('hk_myvars_v1', JSON.stringify(STORE.myVars));
      global.localStorage.setItem('hk_kv_v1', JSON.stringify(STORE.kv));
    } catch (e) {}
  }
  function persistLoad() {
    try {
      if (!global.localStorage) return;
      var v = global.localStorage.getItem('hk_vars_v1'); if (v) STORE.vars = jparse(v, {}) || {};
      var m = global.localStorage.getItem('hk_myvars_v1'); if (m) STORE.myVars = jparse(m, {}) || {};
      var k = global.localStorage.getItem('hk_kv_v1'); if (k) STORE.kv = jparse(k, {}) || {};
    } catch (e) {}
  }
  persistLoad();
  HK.persistSave = persistSave;

  var UAM = 'Mozilla/5.0 (Linux; Android 13; SM-S9080) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36';
  var UAP = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';

  function myKey(k, ruleName) { return (ruleName || HK.ruleName() || '') + '@' + str(k); }

  /* 页面上下文：当前站点/规则/参数 */
  var PAGE = { rule: null, ruleName: '', url: '', keyword: '', page: 1, title: '', pic: '',
               input: '', params: {}, reversed: false };
  HK.page = PAGE;
  HK.setPage = function (o) { for (var k in o) if (has(o, k)) PAGE[k] = o[k]; };
  HK.ruleName = function () { return PAGE.ruleName || (PAGE.rule && PAGE.rule.title) || ''; };

  /* 结果收集器：setResult / setHomeResult / d.push 最终都落这里 */
  var OUT = { result: undefined, hasResult: false, items: [], events: [], d: [], preResult: undefined };
  HK.OUT = OUT;
  HK.resetOut = function () { OUT.result = undefined; OUT.hasResult = false; OUT.items = []; OUT.d = []; OUT.preResult = undefined; };
  /* [A-P0-12] @rule= 预取正文通道：UI 在跑 @rule 代码前把目标页正文放进来，
   * 规则用 getResCode() 取（HoHoJ 的 searchParse 就靠它）。 */
  HK.resCode = '';
  HK.lastChapterRule = '';
  HK.setResCode = function (v) { HK.resCode = str(v); return HK.resCode; };
  HK.getResCode = function () { return HK.resCode || ''; };

  function rawEmit(name, payload) {
    try { if (bridge && typeof bridge.report === 'function') bridge.report(name, typeof payload === 'string' ? payload : JSON.stringify(payload)); }
    catch (e) {}
  }
  /* [A-P1-1] 原实现里 UI 覆盖 HK.emit 并不能拦截引擎内部 emit（内部走的是
   * 本地函数引用），updateItem/addItem 等事件 UI 永远收不到。这里改成钩子：
   * 引擎内部统一走 emit()，UI 通过 HK.__emitHook 订阅，rawEmit 保证桥上报不丢。 */
  function emit(name, payload) {
    rawEmit(name, payload);
    if (HK.__emitHook) { try { HK.__emitHook(name, payload); } catch (e) {} }
  }
  HK.emit = emit;
  HK.rawEmit = rawEmit;

  function fileRead(p) {
    var path = str(p);
    if (bridge && typeof bridge.call === 'function') {
      try {
        var r = bridge.call('readFile', JSON.stringify([path]));
        if (r !== null && r !== undefined && String(r) !== '') return String(r);
      } catch (e) {}
    }
    return has(STORE.files, path) ? STORE.files[path] : '';
  }
  function fileWrite(p, c) {
    var path = str(p), body = str(c);
    STORE.files[path] = body;
    if (bridge && typeof bridge.call === 'function') {
      try { bridge.call('writeFile', JSON.stringify([path, body])); } catch (e) {}
    }
    return true;
  }
  HK.readFile = fileRead; HK.writeFile = fileWrite;

  /**
   * fetch 的协议分发：
   *   hiker://files/...  -> 本地文件（规则自己 writeFile 出来的缓存）
   *   hiker://page/NAME  -> 该模块的源码，按 {"rule": "..."} 返回（dataLoad 就是这么读的）
   *   其它               -> 走同步 HTTP
   */
  function hkFetch(url, opt) {
    var u = trim(url);
    if (u.indexOf('hiker://files/') === 0) return fileRead(u);
    if (u.indexOf('hiker://page/') === 0) {
      var nm = decodeURIComponent((/^hiker:\/\/page\/([^?#]+)/.exec(u) || [])[1] || '');
      var pg = findPage(nm);
      if (pg) return JSON.stringify({ rule: str(pg.rule || '') });
      return '';
    }
    return reqSync(u, opt);
  }
  HK.fetch = hkFetch;

  function findPage(name) {
    var rule = PAGE.rule || {};
    var pages = rule.pages;
    if (typeof pages === 'string') { try { pages = JSON.parse(pages); } catch (e) { pages = []; } }
    if (!isArr(pages)) return null;
    for (var i = 0; i < pages.length; i++) if (pages[i] && str(pages[i].path) === str(name)) return pages[i];
    return null;
  }
  HK.findPage = findPage;

  /* ==================================================================== *
   * 4b. [A] 桥接调用 / URL 工具 / 选择 / CryptoJS / fetch 语义 / 播放解析
   * ==================================================================== */
  function bridgeCall(name, args) {
    if (bridge && typeof bridge.call === 'function') {
      try { var r = bridge.call(name, JSON.stringify(args || [])); return (r === null || r === undefined) ? null : String(r); }
      catch (e) { return null; }
    }
    return null;
  }
  HK.bridgeCall = bridgeCall;

  function hostOf(u) {
    var m = /^https?:\/\/([^\/:?#]+)/i.exec(str(u));
    return m ? m[1] : '';
  }
  HK.hostOf = hostOf;

  function parseQuery(u) {
    var out = {}, s = str(u), i = s.indexOf('?');
    if (i < 0) return out;
    var qs = s.slice(i + 1).split('#')[0].split('&');
    for (var j = 0; j < qs.length; j++) {
      if (!qs[j]) continue;
      var p = qs[j].split('=');
      try { out[decodeURIComponent(p[0])] = decodeURIComponent(p.slice(1).join('=') || ''); }
      catch (e) { out[p[0]] = p.slice(1).join('=') || ''; }
    }
    return out;
  }
  HK.parseQuery = parseQuery;

  /* [A-P1-7] joinUrl：相对地址按 base 补全（死文件 jst4 里有、运行时缺） */
  function joinUrl(base, rel) {
    var b = str(base), r = str(rel);
    if (!r) return b;
    if (/^[a-z][a-z0-9+.-]*:/i.test(r) || r.indexOf('//') === 0) return r;
    var m = /^([a-z][a-z0-9+.-]*:\/\/[^\/]+)/i.exec(b);
    if (!m) return r;
    var origin = m[1];
    if (r.charAt(0) === '/') return origin + r;
    var path = b.slice(origin.length).split('#')[0].split('?')[0];
    var dir = path.slice(0, path.lastIndexOf('/') + 1) || '/';
    var parts = (dir + r).split('/'), out = [];
    for (var i = 0; i < parts.length; i++) {
      if (parts[i] === '..') { if (out.length > 1) out.pop(); }
      else if (parts[i] !== '.') out.push(parts[i]);
    }
    return origin + out.join('/');
  }
  HK.joinUrl = joinUrl;

  /* [A-P0-8] 真选择：优先原生 prompt（HikerActivity 的 onJsPrompt 是真对话框，
   * 用户实际选，不是静默第一项）；无 prompt 的环境才退回默认项并留痕。
   * 选项可为字符串或 {title/name, value} 对象；返回选中项的 value（对象）或原文。 */
  function pickOption(opts, defIdx, title) {
    var list = [], i, o;
    if (Object.prototype.toString.call(opts) === '[object Array]') {
      for (i = 0; i < opts.length; i++) {
        o = opts[i];
        if (o && typeof o === 'object') list.push({ label: str(o.title || o.name || o.value || ''), value: (o.value !== undefined ? o.value : (o.title || o.name || '')) });
        else list.push({ label: str(o), value: o });
      }
    }
    if (!list.length) return null;
    var d = (typeof defIdx === 'number' && defIdx >= 0 && defIdx < list.length) ? defIdx : 0;
    if (global.prompt) {
      var lines = [];
      for (i = 0; i < list.length; i++) lines.push((i + 1) + '. ' + list[i].label);
      var ans = global.prompt((title ? str(title) + '\n' : '') + lines.join('\n'), String(d + 1));
      if (ans === null || ans === undefined) return null;
      var n = parseInt(ans, 10);
      if (!isNaN(n) && n >= 1 && n <= list.length) return list[n - 1].value;
      for (i = 0; i < list.length; i++) if (list[i].label === str(ans)) return list[i].value;
      return list[d].value;
    }
    emit('hk.log', 'pickOption: 无 prompt 环境，退回默认项');
    return list[d].value;
  }
  HK.pickOption = pickOption;

  HK.COL_TYPES = ['avatar','big_big_blank_block','big_blank_block','blank_block','card_pic_1',
    'card_pic_2','card_pic_2_2','card_pic_2_2_left','card_pic_3','card_pic_3_center','flex_button',
    'footer','header','icon_1_left_pic','icon_1_search','icon_2','icon_2_round','icon_3_fill',
    'icon_3_round_fill','icon_4','icon_4_card','icon_5','icon_5_no_crop','icon_round_4',
    'icon_round_small_4','icon_small_3','icon_small_4','input','line','line_blank','long_text',
    'movie_1','movie_1_left_pic','movie_1_vertical_pic','movie_1_vertical_pic_blur','movie_2',
    'movie_3','movie_3_marquee','pic_1','pic_1_card','pic_1_center','pic_1_full','pic_2',
    'pic_2_card','pic_3','pic_3_square','rich_text','scroll_button','text_1','text_2','text_3',
    'text_4','text_5','text_center_1','text_icon','video','x5_webview_single'];

  /* ---------- CryptoJS 兼容子集 [A-P0-3] ----------
   * 覆盖语料实际用到的面：enc.Hex/Utf8/Base64/Latin1、MD5/SHA1/SHA256、
   * HmacMD5/HmacSHA1/HmacSHA256、AES(CBC/ECB + Pkcs7，经 Java 桥真 AES)。
   * 摘要/HMAC 为纯 JS 真实现（有已知向量自测，见 docs/hiker-js-selftest.js）；
   * AES 的密钥/IV 以 Latin1 字节串经桥传给 HikerApi.aesEncode（ASCII 密钥精确，
   * 非 ASCII 字节密钥受 Java String 语义限制，属已知边界）。
   * getCryptoJS() 按官方语义返回「源码字符串」：规则 eval(getCryptoJS()) 后
   * CryptoJS 变量就指向 HK.cryptoObject，与直接读作用域 CryptoJS 等价。 */
  function bytesToLatin1(bytes) { var s = ''; for (var i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i] & 0xFF); return s; }
  function latin1ToBytes(s) { var t = str(s), o = []; for (var i = 0; i < t.length; i++) o.push(t.charCodeAt(i) & 0xFF); return o; }
  function rawB64eBytes(bytes) {
    var out = '', i, c1, c2, c3;
    for (i = 0; i < bytes.length; i += 3) {
      c1 = bytes[i] & 0xFF; c2 = i + 1 < bytes.length ? bytes[i + 1] & 0xFF : NaN; c3 = i + 2 < bytes.length ? bytes[i + 2] & 0xFF : NaN;
      out += B64C.charAt(c1 >> 2);
      out += B64C.charAt(((c1 & 3) << 4) | (isNaN(c2) ? 0 : c2 >> 4));
      out += isNaN(c2) ? '=' : B64C.charAt(((c2 & 15) << 2) | (isNaN(c3) ? 0 : c3 >> 6));
      out += isNaN(c3) ? '=' : B64C.charAt(c3 & 63);
    }
    return out;
  }
  function rawB64dBytes(s) {
    var t = str(s).replace(/[\s\r\n]/g, '').replace(/-/g, '+').replace(/_/g, '/');
    var pad = t.length % 4; if (pad) t += pad === 2 ? '==' : (pad === 3 ? '=' : '==');
    var out = [], i, b0, b1, b2, b3;
    for (i = 0; i < t.length; i += 4) {
      b0 = B64C.indexOf(t.charAt(i)); b1 = B64C.indexOf(t.charAt(i + 1));
      b2 = B64C.indexOf(t.charAt(i + 2)); b3 = B64C.indexOf(t.charAt(i + 3));
      if (b0 < 0 || b1 < 0) continue;
      out.push((b0 << 2) | (b1 >> 4));
      if (b2 >= 0 && t.charAt(i + 2) !== '=') out.push(((b1 & 15) << 4) | (b2 >> 2));
      if (b3 >= 0 && t.charAt(i + 3) !== '=') out.push(((b2 & 3) << 6) | b3);
    }
    return out;
  }
  function hexToBytesArr(h) {
    var s = str(h).replace(/[^0-9a-fA-F]/g, ''), o = [];
    for (var i = 0; i + 1 < s.length; i += 2) o.push(parseInt(s.substr(i, 2), 16));
    return o;
  }
  function bytesToHex(bytes) {
    var s = ''; for (var i = 0; i < bytes.length; i++) { var h = (bytes[i] & 0xFF).toString(16); s += h.length === 1 ? '0' + h : h; }
    return s;
  }

  function WA(words, sigBytes) { this.words = words || []; this.sigBytes = (sigBytes === undefined) ? this.words.length * 4 : sigBytes; }
  WA.prototype.toString = function (enc) { return (enc || CJ.enc.Hex).stringify(this); };
  WA.prototype.concat = function (w2) {
    var a = bytesFromWA(this), b = bytesFromWA(w2), i;
    for (i = 0; i < b.length; i++) a.push(b[i]);
    var r = waFromBytes(a); this.words = r.words; this.sigBytes = r.sigBytes; return this;
  };
  function waFromBytes(bytes) {
    var words = [], i;
    for (i = 0; i < bytes.length; i++) words[i >>> 2] = (words[i >>> 2] || 0) | ((bytes[i] & 0xFF) << (24 - (i % 4) * 8));
    return new WA(words, bytes.length);
  }
  function bytesFromWA(w) {
    var out = [], i;
    for (i = 0; i < w.sigBytes; i++) out.push((w.words[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xFF);
    return out;
  }
  function waCreate(x) {
    if (x instanceof WA) return x;
    if (typeof x === 'string') return CJ.enc.Utf8.parse(x);
    if (Object.prototype.toString.call(x) === '[object Array]') return waFromBytes(x);
    if (x && typeof x === 'object' && x.words) return new WA(x.words, x.sigBytes);
    return new WA([], 0);
  }

  function sha1Bytes(msg) {
    var bytes = msg.slice(), ml = bytes.length;
    bytes.push(0x80);
    while (bytes.length % 64 !== 56) bytes.push(0);
    var hi = Math.floor(ml / 0x20000000), lo = (ml * 8) >>> 0, i, t;
    for (i = 3; i >= 0; i--) bytes.push((hi >>> (i * 8)) & 0xFF);
    for (i = 3; i >= 0; i--) bytes.push((lo >>> (i * 8)) & 0xFF);
    var h = [0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0], w = new Array(80);
    function rol(x, n) { return (x << n) | (x >>> (32 - n)); }
    for (var off = 0; off < bytes.length; off += 64) {
      for (t = 0; t < 16; t++) w[t] = (bytes[off + 4 * t] << 24) | (bytes[off + 4 * t + 1] << 16) | (bytes[off + 4 * t + 2] << 8) | bytes[off + 4 * t + 3];
      for (t = 16; t < 80; t++) w[t] = rol(w[t - 3] ^ w[t - 8] ^ w[t - 14] ^ w[t - 16], 1);
      var a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f, k;
      for (t = 0; t < 80; t++) {
        if (t < 20) { f = (b & c) | ((~b) & d); k = 0x5A827999; }
        else if (t < 40) { f = b ^ c ^ d; k = 0x6ED9EBA1; }
        else if (t < 60) { f = (b & c) | (b & d) | (c & d); k = 0x8F1BBCDC; }
        else { f = b ^ c ^ d; k = 0xCA62C1D6; }
        var tmp = (rol(a, 5) + f + e + k + w[t]) | 0;
        e = d; d = c; c = rol(b, 30); b = a; a = tmp;
      }
      h[0] = (h[0] + a) | 0; h[1] = (h[1] + b) | 0; h[2] = (h[2] + c) | 0; h[3] = (h[3] + d) | 0; h[4] = (h[4] + e) | 0;
    }
    var out = [];
    for (i = 0; i < 5; i++) { out.push((h[i] >>> 24) & 0xFF, (h[i] >>> 16) & 0xFF, (h[i] >>> 8) & 0xFF, h[i] & 0xFF); }
    return out;
  }

  var SHA256K = [0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2];
  function sha256Bytes(msg) {
    var bytes = msg.slice(), ml = bytes.length, i, t;
    bytes.push(0x80);
    while (bytes.length % 64 !== 56) bytes.push(0);
    var hi = Math.floor(ml / 0x20000000), lo = (ml * 8) >>> 0;
    for (i = 3; i >= 0; i--) bytes.push((hi >>> (i * 8)) & 0xFF);
    for (i = 3; i >= 0; i--) bytes.push((lo >>> (i * 8)) & 0xFF);
    var h = [0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19], w = new Array(64);
    function rr(v, a) { return (v >>> a) | (v << (32 - a)); }
    for (var off = 0; off < bytes.length; off += 64) {
      for (t = 0; t < 16; t++) w[t] = (bytes[off + 4 * t] << 24) | (bytes[off + 4 * t + 1] << 16) | (bytes[off + 4 * t + 2] << 8) | bytes[off + 4 * t + 3];
      for (t = 16; t < 64; t++) {
        var s0 = rr(w[t - 15], 7) ^ rr(w[t - 15], 18) ^ (w[t - 15] >>> 3);
        var s1 = rr(w[t - 2], 17) ^ rr(w[t - 2], 19) ^ (w[t - 2] >>> 10);
        w[t] = (w[t - 16] + s0 + w[t - 7] + s1) | 0;
      }
      var a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
      for (t = 0; t < 64; t++) {
        var S1 = rr(e, 6) ^ rr(e, 11) ^ rr(e, 25);
        var ch = (e & f) ^ ((~e) & g);
        var t1 = (hh + S1 + ch + SHA256K[t] + w[t]) | 0;
        var S0 = rr(a, 2) ^ rr(a, 13) ^ rr(a, 22);
        var mj = (a & b) ^ (a & c) ^ (b & c);
        var t2 = (S0 + mj) | 0;
        hh = g; g = f; f = e; e = (d + t1) | 0; d = c; c = b; b = a; a = (t1 + t2) | 0;
      }
      h[0] = (h[0] + a) | 0; h[1] = (h[1] + b) | 0; h[2] = (h[2] + c) | 0; h[3] = (h[3] + d) | 0;
      h[4] = (h[4] + e) | 0; h[5] = (h[5] + f) | 0; h[6] = (h[6] + g) | 0; h[7] = (h[7] + hh) | 0;
    }
    var out = [];
    for (i = 0; i < 8; i++) { out.push((h[i] >>> 24) & 0xFF, (h[i] >>> 16) & 0xFF, (h[i] >>> 8) & 0xFF, h[i] & 0xFF); }
    return out;
  }
  function md5BytesOf(bytes) { return hexToBytesArr(MD5(bytesToLatin1(bytes))); }
  function hmacBytes(hashFn, msgBytes, keyBytes) {
    var k = keyBytes.slice();
    if (k.length > 64) k = hashFn(k);
    while (k.length < 64) k.push(0);
    var ip = [], op = [], i;
    for (i = 0; i < 64; i++) { ip.push(k[i] ^ 0x36); op.push(k[i] ^ 0x5C); }
    return hashFn(op.concat(hashFn(ip.concat(msgBytes))));
  }
  function cjAesArgs(key, cfg) {
    var kw = waCreate(key);
    var keyStr = CJ.enc.Latin1.stringify(kw);
    var ivStr = (cfg && cfg.iv) ? CJ.enc.Latin1.stringify(waCreate(cfg.iv)) : '';
    var mode = (cfg && cfg.mode && cfg.mode.name === 'ECB') ? 'ECB' : '';
    return [keyStr, ivStr, mode, kw];
  }

  var CJ = {
    lib: { WordArray: { create: waCreate } },
    enc: {
      Hex: {
        stringify: function (w) { return bytesToHex(bytesFromWA(w)); },
        parse: function (s) { return waFromBytes(hexToBytesArr(s)); }
      },
      Utf8: {
        stringify: function (w) { return utf8Decode(bytesToLatin1(bytesFromWA(w))); },
        parse: function (s) { return waFromBytes(latin1ToBytes(utf8Encode(s))); }
      },
      Latin1: {
        stringify: function (w) { return bytesToLatin1(bytesFromWA(w)); },
        parse: function (s) { return waFromBytes(latin1ToBytes(s)); }
      },
      Base64: {
        stringify: function (w) { return rawB64eBytes(bytesFromWA(w)); },
        parse: function (s) { return waFromBytes(rawB64dBytes(s)); }
      }
    },
    MD5: function (x) { return waFromBytes(md5BytesOf(bytesFromWA(waCreate(x)))); },
    SHA1: function (x) { return waFromBytes(sha1Bytes(bytesFromWA(waCreate(x)))); },
    SHA256: function (x) { return waFromBytes(sha256Bytes(bytesFromWA(waCreate(x)))); },
    HmacMD5: function (x, k) { return waFromBytes(hmacBytes(md5BytesOf, bytesFromWA(waCreate(x)), bytesFromWA(waCreate(k)))); },
    HmacSHA1: function (x, k) { return waFromBytes(hmacBytes(sha1Bytes, bytesFromWA(waCreate(x)), bytesFromWA(waCreate(k)))); },
    HmacSHA256: function (x, k) { return waFromBytes(hmacBytes(sha256Bytes, bytesFromWA(waCreate(x)), bytesFromWA(waCreate(k)))); },
    mode: { CBC: { name: 'CBC' }, ECB: { name: 'ECB' }, CFB: { name: 'CFB' }, OFB: { name: 'OFB' }, CTR: { name: 'CTR' } },
    pad: { Pkcs7: { name: 'Pkcs7' }, NoPadding: { name: 'NoPadding' }, ZeroPadding: { name: 'ZeroPadding' }, AnsiX923: { name: 'AnsiX923' }, Iso10126: { name: 'Iso10126' } },
    AES: {
      encrypt: function (plain, key, cfg) {
        var a = cjAesArgs(key, cfg || {});
        var text = CJ.enc.Utf8.stringify(waCreate(plain));
        var res = bridgeCall('aesEncode', [text, a[0], a[1], a[2]]);
        var wa = res ? CJ.enc.Base64.parse(res) : new WA([], 0);
        wa.ciphertext = wa;
        wa.toString = function () { return res || ''; };
        return wa;
      },
      decrypt: function (data, key, cfg) {
        var a = cjAesArgs(key, cfg || {});
        var b64 = (typeof data === 'string') ? data
          : (data && data.ciphertext ? CJ.enc.Base64.stringify(waCreate(data.ciphertext)) : str(data));
        var res = bridgeCall('aesDecode', [b64, a[0], a[1], a[2]]);
        return CJ.enc.Utf8.parse(res || '');
      }
    }
  };
  HK.cryptoObject = CJ;
  HK.getCryptoJSSource = function () { return 'var CryptoJS = HikerEngine.cryptoObject;'; };

  /* ---------- fetch 语义 [A-P0-4/P0-5] ----------
   * 缺省返回正文串；withHeaders/withStatusCode 时按官方 wrapFetch 形返回
   * JSON 串 {body,headers,url,statusCode,error}；onlyHeaders 时 body=null。
   * 走桥契约 fetchFull（Java 侧与 HikerHttp 共用 HikerCookies，cookie 自动带/存，
   * timeout 由 options 透传）；fetchFull 不可用时降级正文 + 空头并留痕。 */
  function fetchFull(url, o) {
    var r = bridgeCall('fetchFull', [url, JSON.stringify(o || {})]);
    if (r === null) return null;
    var f = jparse(r, null);
    if (!f || typeof f !== 'object' || typeof f.body !== 'string') return null;
    return f;
  }
  function fetchImpl(u, o) {
    var url = trim(u);
    o = (typeof o === 'string') ? (jparse(o, {}) || {}) : (o || {});
    if (!url) return '';
    var wantWrap = !!(o.withHeaders || o.withStatusCode || o.onlyHeaders);
    if (!wantWrap) return hkFetch(url, o);
    var full = fetchFull(url, o);
    if (!full) {
      emit('hk.log', 'fetchFull 不可用，withHeaders 降级为空头包装: ' + url);
      return JSON.stringify({ body: o.onlyHeaders ? null : hkFetch(url, o), headers: {}, url: url, statusCode: 0, error: 'fetchFull-unavailable' });
    }
    return JSON.stringify({
      body: o.onlyHeaders ? null : full.body,
      headers: full.headers || {},
      url: url,
      statusCode: (typeof full.status === 'number') ? full.status : 0,
      error: null
    });
  }
  HK.fetchImpl = fetchImpl;

  /* ---------- 播放目标解析 [A-P0-7] ----------
   * 地址尾段：#标记# 收集为 flag；;{K@V&&K2@V2} 拆为 headers 对象。 */
  function parsePlayTarget(raw, extra) {
    var u = str(raw), flags = [], headers = {}, m;
    var hm = /;\{([\s\S]*)\}\s*$/.exec(u);
    if (hm) {
      var pairs = hm[1].split('&&');
      for (var i = 0; i < pairs.length; i++) {
        var kv = pairs[i].split('@');
        if (kv.length >= 2) headers[str(kv[0]).replace(/^\s+|\s+$/g, '')] = kv.slice(1).join('@');
      }
      u = u.slice(0, hm.index);
    }
    u = u.replace(/#([^#]*)#/g, function (mm, seg) { if (seg) flags.push(seg); return ''; });
    if (extra && extra.headers) {
      var eh = extra.headers;
      if (typeof eh === 'string') { var pr = eh.split('&&'); for (var j = 0; j < pr.length; j++) { var kv2 = pr[j].split('@'); if (kv2.length >= 2) headers[kv2[0]] = kv2.slice(1).join('@'); } }
      else if (typeof eh === 'object') for (var k in eh) if (has(eh, k)) headers[k] = str(eh[k]);
    }
    return { url: u.replace(/^\s+|\s+$/g, ''), headers: headers, flag: flags.join('#') };
  }
  HK.parsePlayTarget = parsePlayTarget;

  /* 全局补齐（UI boot 调用一次）：先让 api_surface 按契约填全局缺口
   * （delegate 优先——已存在的真实现不覆盖），再由 engine 盖上自己的
   * CryptoJS / getCryptoJS / joinUrl / getResCode 真实现；
   * 最后装 scheme 分发包装全局 fetch（hiker:// 内部地址不走网络）。 */
  HK.installGlobals = function () {
    try {
      if (global.HikerApiSurface && typeof global.HikerApiSurface.install === 'function') {
        global.HikerApiSurface.install(global, { network: true });
      }
    } catch (e) {}
    global.CryptoJS = CJ;
    global.getCryptoJS = HK.getCryptoJSSource;
    global.getCryptoJS2 = HK.getCryptoJSSource;
    if (typeof global.joinUrl !== 'function') global.joinUrl = joinUrl;
    if (typeof global.getResCode !== 'function') global.getResCode = HK.getResCode;
    if (typeof global.setResCode !== 'function') global.setResCode = HK.setResCode;
    try {
      if (global.HikerScheme && typeof global.HikerScheme.install === 'function') {
        global.HikerScheme.install({ allowLocal: true });
      }
    } catch (e) {}
    return true;
  };

  /* 建一个作用域：所有规则/模块代码都在这里面跑 */
  function makeScope(extra) {
    var sc = {};
    var ruleName = HK.ruleName();

    /* 编码 / 摘要 */
    sc.md5 = function (s) { return MD5(s); };
    sc.MD5 = sc.md5;
    sc.base64Encode = function (s) { return b64e(s); };
    sc.base64Decode = function (s) { return b64d(s); };
    sc.atob = function (s) { return b64d(s); };
    sc.btoa = function (s) { return b64e(s); };
    sc.encodeStr = function (s, enc) { return (enc === 'base64' || enc === 'b64') ? b64e(s) : encodeURIComponent(str(s)); };
    sc.decodeStr = function (s, enc) { return (enc === 'base64' || enc === 'b64') ? b64d(s) : decodeURIComponent(str(s)); };
    sc.encodeToString = function (s, enc) { return sc.encodeStr(s, enc); };
    sc.hexToBytes = function (h) {
      var s = str(h).replace(/[^0-9a-fA-F]/g, ''), o = [];
      for (var i = 0; i + 1 < s.length; i += 2) o.push(parseInt(s.substr(i, 2), 16));
      return o;
    };
    sc.hexToBase64 = function (h) { return b64e(String.fromCharCode.apply(null, sc.hexToBytes(h))); };
    sc.strToBase64 = sc.base64Encode;

    /* 网络（同步）[A-P0-5]：走 fetchImpl，withHeaders/onlyHeaders/withStatusCode
     * 返回官方 wrapFetch 包装串；fetchPC/postPC 强制 PC UA；timeout 透传到桥。 */
    sc.fetch = function (u, o) { return fetchImpl(u, o); };
    sc.request = function (u, o) { return fetchImpl(u, o); };
    sc.fetchSync = sc.fetch;
    sc.post = function (u, body, o) {
      o = o || {}; o.method = 'POST';
      o.body = (body !== null && typeof body === 'object') ? JSON.stringify(body) : body;
      return fetchImpl(u, o);
    };
    sc.fetchPC = function (u, o) {
      o = (typeof o === 'string') ? (jparse(o, {}) || {}) : (o || {});
      o.headers = o.headers || {};
      if (!o.headers['User-Agent'] && !o.headers['user-agent']) o.headers['User-Agent'] = UAP;
      return fetchImpl(u, o);
    };
    sc.postPC = function (u, body, o) {
      o = o || {}; o.method = 'POST';
      o.body = (body !== null && typeof body === 'object') ? JSON.stringify(body) : body;
      o.headers = o.headers || {};
      if (!o.headers['User-Agent'] && !o.headers['user-agent']) o.headers['User-Agent'] = UAP;
      return fetchImpl(u, o);
    };

    /* DOM */
    sc.pd = function (h, r) { return pdfh(h, r); };
    sc.pdfh = function (h, r) { return pdfh(h, r); };
    sc.pjf = function (h, r) { return pdfh(h, r); };
    sc.pf = function (h, r) { return pdfh(h, r); };
    sc.pdfl = function (h, r) { return pdfh(h, r); };
    sc.pdfa = function (h, r) { return pdfa(h, r); };
    sc.pjfa = function (h, r) { return pdfa(h, r); };
    sc.parseDomForHtml = function (h, r) { return pdfh(h, r); };
    sc.parseDom = function (h, r) { return pdfh(h, r); };
    sc.parseDomForArray = function (h, r) { return pdfa(h, r); };
    /* [A-P1-7] _pd 系列别名（参考 HkJsRuntime:1104-1119，规则有 var pdfa=_pdfa 式转发） */
    sc._pd = function (h, r) { return pdfh(h, r); };
    sc._pdfh = function (h, r) { return pdfh(h, r); };
    sc._pdfa = function (h, r) { return pdfa(h, r); };
    sc._pdfl = function (h, r) { return pdfh(h, r); };
    sc.joinUrl = function (b, r) { return joinUrl(b || str(PAGE.url || ''), r); };

    /* 结果输出 */
    function setResult(v) { OUT.result = v; OUT.hasResult = true; return v; }
    sc.setResult = setResult;
    /* [P4] 合集框架用的是 Rhino 的 Java 反射写法：
     *   method_setResult.invoke(javaContext, result)
     *   method_setHomeResult.invoke(javaContext, …)
     * WebView 没有 Rhino，这几个符号一出现就 ReferenceError，整条规则中断。
     * 这里把它们 shim 到同一个 setResult，语义等价（都是把结果交给宿主）。 */
    var invokeShim = function () {
      var a = arguments, v = a[a.length - 1];
      return setResult(v);
    };
    sc.javaContext = { toString: function () { return '[javaContext shim]'; } };
    sc.method_setResult = { invoke: invokeShim };
    sc.method_setHomeResult = { invoke: invokeShim };
    sc.method_setSearchResult = { invoke: invokeShim };
    sc.method_setStrResult = { invoke: invokeShim };
    sc.method_setLastChapterResult = { invoke: invokeShim };
    /* [P4] hijackEnv(newRule)：把合集子站的规则对象挂成当前上下文。
     * 常见调用是 hijackEnv($.require('框架', 对象参数)) 或直接传规则对象。 */
    sc.hijackEnv = function (r) {
      var rule = r;
      if (typeof r === 'string') rule = jparse(r, null);
      if (rule && typeof rule === 'object') {
        HK.setPage({ rule: rule, ruleName: str(rule.title || HK.ruleName()) });
        /* 模块缓存按规则名隔离，换规则后必须清，否则拿到上一个站点的 exports */
        STORE.modules = {};
      }
      return rule || {};
    };
    sc.setHomeResult = setResult;
    sc.setSearchResult = setResult;
    sc.setStrResult = setResult;
    sc.setLastChapterResult = setResult;
    sc.nsetHomeResult = setResult;
    /* [A-P1-7] setPreResult：预结果，setResult 未调用时作最终结果（run 收尾处理） */
    sc.setPreResult = function (v) { OUT.preResult = v; return v; };
    /* [A-P1-7] setLastChapterRule：原来只有 setLastChapterResult，差一位即报错 */
    sc.setLastChapterRule = function (r) { HK.lastChapterRule = r; return true; };
    sc.nsetLastChapterRule = sc.setLastChapterRule;
    /* [A-P0-12] 预取正文通道 */
    sc.getResCode = function () { return HK.resCode || ''; };
    sc.setResCode = function (v) { HK.resCode = str(v); return HK.resCode; };
    sc.getCode = sc.getResCode;
    /* [A-P0-3] CryptoJS：作用域直给真对象；getCryptoJS 按官方返回源码串，
     * eval(getCryptoJS()) 后 CryptoJS 指向同一对象 */
    sc.CryptoJS = CJ;
    sc.getCryptoJS = function () { return HK.getCryptoJSSource(); };
    sc.getCryptoJS2 = sc.getCryptoJS;
    /* [A-P0-8] __hkSelectExec 兜底：UI 点击层会优先拦截弹页面内选择层；
     * 若链接在规则内部被直接执行，走 prompt 真选择后执行回调。 */
    sc.__hkSelectExec = function (id) {
      var e = global.__HK_SELECTS__ && global.__HK_SELECTS__[id];
      if (!e) return '';
      var v = pickOption(e.options, e.def, e.title);
      if (v === null) return '';
      var fn = eval('(' + e.fn + ')');
      return fn(v);
    };

    /* 变量（[A-P1-3] 全部落 localStorage 持久化，put/clear 后即存） */
    sc.getVar = function (k, d) { return has(STORE.vars, str(k)) ? STORE.vars[str(k)] : (d === undefined ? '' : d); };
    sc.putVar = function (k, v) { STORE.vars[str(k)] = v; persistSave(); return v; };
    sc.putVar2 = sc.putVar;
    sc.clearVar = function (k) { if (k === undefined) STORE.vars = {}; else delete STORE.vars[str(k)]; persistSave(); return true; };
    sc.getMyVar = function (k, d, r) { var q = myKey(k, r); return has(STORE.myVars, q) ? STORE.myVars[q] : (d === undefined ? '' : d); };
    sc.putMyVar = function (k, v, r) { STORE.myVars[myKey(k, r)] = v; persistSave(); return v; };
    sc.clearMyVar = function (k, r) { delete STORE.myVars[myKey(k, r)]; persistSave(); return true; };
    sc.listMyVarKeys = function (r) {
      var pre = (r || ruleName) + '@', out = [];
      for (var q in STORE.myVars) if (has(STORE.myVars, q) && q.indexOf(pre) === 0) out.push(q.slice(pre.length));
      return out;
    };
    /* [A-P1-3] getItem/setItem 改走持久 KV（按规则名隔离），不再与页面条目
     * 注册表 STORE.items 混用——后者只服务 findItem/updateItem 的渲染条目。 */
    sc.getItem = function (k, d) { var q = myKey(k); return has(STORE.kv, q) ? STORE.kv[q] : (d === undefined ? '' : d); };
    sc.setItem = function (k, v) { STORE.kv[myKey(k)] = v; persistSave(); return v; };
    sc.clearItem = function (k) {
      if (k === undefined) { var pre = ruleName + '@'; for (var q in STORE.kv) if (has(STORE.kv, q) && q.indexOf(pre) === 0) delete STORE.kv[q]; }
      else delete STORE.kv[myKey(k)];
      persistSave(); return true;
    };
    /* hiker 的 storage0..9：按槽位隔离的持久存储 */
    var lsKey = function (slot, k) { return 'hk_s' + slot + '_' + ruleName + '_' + str(k); };
    for (var si = 0; si < 10; si++) {
      (function (slot) {
        sc['storage' + slot] = {
          getItem: function (k, d) {
            try { var v = global.localStorage.getItem(lsKey(slot, k)); return v === null ? (d === undefined ? '' : d) : v; }
            catch (e) { return d === undefined ? '' : d; }
          },
          setItem: function (k, v) { try { global.localStorage.setItem(lsKey(slot, k), str(v)); } catch (e) {} return true; },
          removeItem: function (k) { try { global.localStorage.removeItem(lsKey(slot, k)); } catch (e) {} return true; },
          clear: function () { return true; }
        };
      })(si);
    }
    /* 公共 KV：不按规则隔离（跨规则共享），同样持久化 */
    sc.getPublicItem = function (k, d) { return has(STORE.kv, 'pub@' + str(k)) ? STORE.kv['pub@' + str(k)] : (d === undefined ? '' : d); };
    sc.setPublicItem = function (k, v) { STORE.kv['pub@' + str(k)] = v; persistSave(); return v; };
    sc.clearPublicItem = function (k) { delete STORE.kv['pub@' + str(k)]; persistSave(); return true; };
    /* [A-P0-4] Cookie 作用域通道：与 HikerHttp 共用 HikerCookies（桥契约），
     * setCookie(url, cookieStr) / getCookie(url) */
    sc.getCookie = function (u) {
      var r = bridgeCall('getCookie', [str(u || PAGE.url || '')]);
      return r === null ? '' : r;
    };
    sc.setCookie = function (u, c) {
      var r = bridgeCall('setCookie', [str(u || ''), str(c || '')]);
      return r !== null;
    };

    /* 页面参数 */
    sc.getParam = function (k, d, u) {
      var url = str(u || PAGE.url || ''), i = url.indexOf('?');
      if (i < 0) return d === undefined ? '' : d;
      var qs = url.slice(i + 1).split('#')[0].split('&');
      for (var j = 0; j < qs.length; j++) {
        var p = qs[j].split('=');
        if (decodeURIComponent(p[0]) === str(k)) { try { return decodeURIComponent(p.slice(1).join('=')); } catch (e) { return p.slice(1).join('='); } }
      }
      return d === undefined ? '' : d;
    };
    sc.getUrl = function () { return str(PAGE.url || ''); };
    sc.buildUrl = function (u, o) {
      var base = str(u), q = [];
      for (var k in o) if (has(o, k)) q.push(encodeURIComponent(k) + '=' + encodeURIComponent(str(o[k])));
      return q.length ? base + (base.indexOf('?') >= 0 ? '&' : '?') + q.join('&') : base;
    };

    /* 页面 / 生命周期 */
    sc.getPageTitle = function () { return str(PAGE.title || ''); };
    sc.setPageTitle = function (t) { PAGE.title = str(t); return true; };
    sc.setPagePicUrl = function (u) { PAGE.pic = str(u); return true; };
    sc.setPageParams = function (p) { PAGE.params = p; return true; };
    sc.setPageReverse = function (b) { PAGE.reversed = !!b; return true; };
    sc.isPageReversed = function () { return !!PAGE.reversed; };
    sc.refreshPage = function () { emit('hk.refresh', ''); return true; };
    sc.refreshX5Desc = function () { return true; };
    sc.refresh = sc.refreshPage;
    sc.back = function () { emit('hk.back', ''); return true; };
    sc.closeMe = function () { emit('hk.back', ''); return true; };
    /* [A-P2-2] toast 唯一定义：原生桥 Toast + UI 事件双通道（原双定义已合并） */
    sc.toast = function (m) {
      if (bridge && typeof bridge.toast === 'function') { try { bridge.toast(str(m)); } catch (e) {} }
      emit('hk.toast', str(m));
      return true;
    };
    sc.log = function (m) { emit('hk.log', str(m)); return true; };
    sc.nlog = sc.log;
    sc.error = function (m) { emit('hk.error', str(m)); return true; };
    sc.setError = sc.error;
    sc.showLoading = function () { return true; };
    sc.hideLoading = function () { return true; };
    sc.getTime = function () { return now(); };
    sc.getAppVersion = function () { return 9999; };
    sc.getHeaderUrl = function () { return ''; };
    sc.getUa = function () { return UAM; };
    sc.setWebUa = function () { return true; };
    sc.setWebTitle = sc.setPageTitle;
    sc.getUaObject = function () { return JSON.stringify({ ua: UAM, pcUa: UAP }); };
    sc.getSearchMode = function () { return ''; };
    sc.setSearchMode = function () { return true; };
    sc.getRule = function (k) { var r = PAGE.rule || {}; return str(has(r, str(k)) ? r[str(k)] : ''); };
    /* [A-P1-6] 不再恒假值：getHome 给当前规则概要，getColTypes 给真支持集 */
    sc.getHome = function () {
      var r = PAGE.rule || {};
      return JSON.stringify([{ title: str(r.title || ''), url: str(r.url || ''), type: str(r.type || '') }]);
    };
    sc.getHomeSub = function () { return '[]'; };
    sc.hasHomeSub = function () { return false; };
    sc.getColTypes = function () { return JSON.stringify(HK.COL_TYPES); };
    sc.getLastRules = function () { return '[]'; };
    sc.getRuleCount = function () { return 0; };
    sc.getPrivateJS = function () { return ''; };
    sc.getJsPlugin = function () { return ''; };
    sc.getJsLazyPlugin = function () { return ''; };
    sc.addListener = function () { return true; };

    /* 交互 [A-P1-5]：confirm 兼容对象式 {title/content, confirm, cancel}
     * ——confirm/cancel 为字符串时是按钮文案（走原生 confirm 返回布尔），
     * 为函数时是回调（按结果回调并返回布尔），对齐官方语义。 */
    sc.confirm = function (o) {
      if (o && typeof o === 'object') {
        var msg = str(o.title || o.content || o.message || '确认？');
        var ok = global.confirm ? !!global.confirm(msg) : true;
        try {
          if (ok && typeof o.confirm === 'function') o.confirm();
          if (!ok && typeof o.cancel === 'function') o.cancel();
        } catch (e) {}
        return ok;
      }
      return global.confirm ? !!global.confirm(str(o)) : true;
    };
    /* [A-P0-8] showSelectOptions：真选择（prompt 编号选），返回选中项原文；
     * 入参可为数组或 {options/optionList, defaultIndex/selectIndex, title} */
    sc.showSelectOptions = function (o) {
      var opts = [], def = 0, ttl = '';
      if (isArr(o)) opts = o;
      else if (o && typeof o === 'object') {
        opts = o.options || o.optionList || o.list || [];
        def = (typeof o.defaultIndex === 'number') ? o.defaultIndex
            : (typeof o.selectIndex === 'number') ? o.selectIndex
            : (typeof o.index === 'number') ? o.index : 0;
        ttl = str(o.title || '');
      }
      var v = pickOption(opts, def, ttl);
      return v === null ? '' : str(v);
    };
    sc.copy = function (t) { emit('hk.copy', str(t)); return true; };
    sc.open = function (u) { emit('hk.open', str(u)); return true; };
    sc.openAppIntent = sc.open;
    sc.openThirdApp = sc.open;
    sc.toDetailPage = function (u) { emit('hk.open', str(u)); return true; };

    /* 列表条目操作 */
    sc.findItem = function (id) { return has(STORE.items, str(id)) ? STORE.items[str(id)] : null; };
    sc.findItemsByCls = function (cls) {
      var out = [];
      for (var k in STORE.items) { var v = STORE.items[k]; if (v && v.cls === str(cls)) out.push(v); }
      return out;
    };
    sc.updateItem = function (id, o) {
      var cur = STORE.items[str(id)] || {};
      for (var k in o) if (has(o, k)) cur[k] = o[k];
      STORE.items[str(id)] = cur;
      emit('hk.updateItem', [str(id), o]);
      return true;
    };
    sc.addItemAfter = function (id, o) { emit('hk.addItem', ['after', str(id), o]); return true; };
    sc.addItemBefore = function (id, o) { emit('hk.addItem', ['before', str(id), o]); return true; };
    sc.deleteItem = function (id) { delete STORE.items[str(id)]; emit('hk.deleteItem', str(id)); return true; };
    sc.deleteItemByCls = function (cls) {
      for (var k in STORE.items) { var v = STORE.items[k]; if (v && v.cls === str(cls)) delete STORE.items[k]; }
      return true;
    };

    /* 文件 */
    sc.readFile = function (p) { return fileRead(p); };
    sc.writeFile = function (p, c) { return fileWrite(p, c); };
    sc.saveFile = function (p, c) { return fileWrite(p, c); };
    sc.deleteFile = function (p) { delete STORE.files[str(p)]; return true; };
    /* [A-P1-9] fileExist 优先问桥（HikerApi.fileExist 按文件系统判存），
     * 不再以「读到非空内容」误判空文件/二进制文件不存在 */
    sc.fileExist = function (p) {
      var r = bridgeCall('fileExist', [str(p)]);
      if (r !== null) return r === 'true' || r === '1';
      return !!fileRead(p);
    };
    sc.getPath = function (p) { return 'hiker://files/' + str(p); };

    /* 播放 */
    sc.playVideo = function (u) { return HK.play(u, '', ''); };
    sc.playVideos = function (json) {
      var list = isArr(json) ? json : (jparse(json, []) || []);
      for (var i = 0; i < list.length; i++) {
        var it = list[i] || {};
        if (it.url) HK.play(it.url, it.title || '', it.pic || '', { headers: it.headers || null, flag: it.flag || '' });
      }
      return true;
    };

    /* 本页变量（海阔注入的全局）[A-P0-9/P0-10/P0-13]
     * 全部改成与 PAGE 绑定的 getter/setter（与 MY_RULE 同款），不再是空快照：
     *   MY_PARAMS/params —— 页面 URL 的查询参数对象（setPageParams 可覆盖）
     *   input            —— 本页输入（搜索词/输入框值/详情 URL，UI 经 vars 传入）
     *   host             —— MY_URL 的主机名（非 http 时回落规则 homePage 的主机）
     *   d                —— 全局结果集，d.push 的条目在 run 收尾并入 result */
    function liveProp(name, get, set) {
      Object.defineProperty(sc, name, { configurable: true, enumerable: true, get: get, set: set });
    }
    liveProp('MY_URL', function () { return str(PAGE.url || ''); }, function (v) { PAGE.url = str(v); });
    liveProp('MY_PAGE', function () { return PAGE.page || 1; }, function (v) { PAGE.page = v; });
    liveProp('MY_KEYWORD', function () { return str(PAGE.keyword || ''); }, function (v) { PAGE.keyword = str(v); });
    /* [P3] 合集框架的 hijackEnv() 会改写 MY_RULE。若这里是快照，改写后
     * 规则读到的还是旧对象 → 子站进不去。改成 getter，随 PAGE.rule 实时变。 */
    Object.defineProperty(sc, 'MY_RULE', {
      configurable: true,
      get: function () { return PAGE.rule || {}; },
      set: function (v) { HK.setPage({ rule: v }); }
    });
    liveProp('MY_TITLE', function () { return str(PAGE.title || ''); }, function (v) { PAGE.title = str(v); });
    liveProp('MY_PIC', function () { return str(PAGE.pic || ''); }, function (v) { PAGE.pic = str(v); });
    liveProp('MY_PARAMS', function () { return PAGE.params || {}; }, function (v) { PAGE.params = v; });
    liveProp('params', function () { return PAGE.params || {}; }, function (v) { PAGE.params = v; });
    liveProp('input', function () { return (PAGE.input === undefined || PAGE.input === null) ? '' : PAGE.input; },
             function (v) { PAGE.input = v; });
    liveProp('host', function () {
      return hostOf(PAGE.url) || hostOf((PAGE.rule || {}).url) || hostOf((PAGE.rule || {}).homePage) || '';
    }, function () {});
    liveProp('d', function () { return OUT.d; }, function (v) { OUT.d = isArr(v) ? v : []; });
    sc.MOBILE_UA = UAM;
    sc.PC_UA = UAP;

    /* ---- $ 模块系统 ---- */
    sc.$ = makeDollar(null);
    sc.require = function (name) { return sc.$.require(name); };

    /* ---- java.* 兼容层（少量真正需要的：Base64 / 字节数组） ---- */
    sc.java = JAVA_SHIM;
    sc.Packages = JAVA_PACKAGES;
    /* 海阔规则里 javax.crypto 两种写法都有：Packages.javax.… 与直接 javax.… */
    sc.javax = JAVA_PACKAGES.javax;

    /* fetchCache(url, 小时, 选项)：带本地缓存的取数。
     * 命中未过期缓存直接用；否则联网；联网失败再退回旧缓存（这个「退回」很关键：
     * 合集包的网络源经常挂，包里自带的 libs 缓存就是靠这条路撑住的）。 */
    sc.fetchCache = function (u, hours, opt) {
      var url = trim(u), ttl = (hours === undefined || hours === null) ? 24 : Number(hours);
      var key = 'hiker://files/libs/' + MD5(url) + '.js';
      var stampKey = 'hiker://files/libs/' + MD5(url) + '.ts';
      var cached = fileRead(key), stamp = Number(fileRead(stampKey) || 0) || 0;
      var fresh = cached && ttl >= 0 && (now() - stamp) < ttl * 3600000;
      if (fresh) return cached;
      var body = reqSync(url, opt || {});
      if (body && body.length > 8) {
        fileWrite(key, body);
        fileWrite(stampKey, String(now()));
        return body;
      }
      return cached || '';
    };
    sc.saveFile = function (p, c) { return fileWrite(p, c); };
    /* 私有加密 JS：海阔用 App 内置密钥解密后执行，WebView 里拿不到那把钥匙。
     * 不伪装成成功，留痕后返回空串；合集类规则靠下面的变量兜底继续跑。 */
    sc.evalPrivateJS = function (code) {
      emit('hk.degrade', 'evalPrivateJS(' + str(code).length + ' 字节，需海阔内置密钥)');
      return '';
    };

    /* 其它杂项 */
    sc.fc = function (u) { return reqSync(u, {}); };
    sc.rc = function (u) { return reqSync(u, {}); };
    sc.bf = sc.fc; sc.be = sc.fc;
    sc.syncExecute = function (f) { try { return f(); } catch (e) { return ''; } };
    sc.publishRule = function () { return true; };
    sc.registerTask = function () { return true; };
    sc.unRegisterTask = function () { return true; };
    sc.registerDNS = function () { return true; };
    sc.startProxyServer = function () { return ''; };

    /* [A-P0-2] api_surface 契约层按「缺失才填」并入作用域（delegate 优先）：
     * engine 已有真实现的（fetch/存储/DOM/结果…）一律不覆盖；只补桥转发类
     * （aes/rsa/batchFetch/getCookie 族/cacheM3u8/downloadFile/createQRCode…）
     * 与 getPastes 家族等 engine 没有的名字。 */
    try {
      if (global.HikerApiSurface && typeof global.HikerApiSurface.build === 'function') {
        var apiFill = global.HikerApiSurface.build({ network: true });
        for (var an in apiFill) {
          if (has(apiFill, an) && typeof sc[an] === 'undefined') sc[an] = apiFill[an];
        }
      }
    } catch (e) {}

    if (extra) for (var k in extra) if (has(extra, k)) sc[k] = extra[k];
    return sc;
  }

  /* ==================================================================== *
   * 5. java.* 兼容层
   *    真实规则（吃瓜718 / 阅动漫 的 imgDES）会用 Rhino 的 Java 互操作做
   *    Base64 与字节处理。WebView 没有 Rhino，这里给等价实现；
   *    做不到的（javax.crypto 任意算法、java.net.URL 同步字节流）明确抛错，
   *    由调用方 try/catch 兜住，绝不静默返回假数据。
   * ==================================================================== */
  var JAVA_SHIM = {
    lang: {
      String: function (s) { return str(s); },
      Integer: { parseInt: function (s) { return parseInt(s, 10); } },
      Byte: { TYPE: 'byte' },
      reflect: {
        Array: {
          newInstance: function (type, n) { var a = new Array(n); for (var i = 0; i < n; i++) a[i] = 0; return a; }
        }
      }
    },
    util: {
      Base64: {
        getEncoder: function () {
          return { encodeToString: function (bytes) { return b64e(bytesToString(bytes)); } };
        },
        getDecoder: function () {
          return { decode: function (s) { return stringToBytes(b64d(s)); } };
        }
      },
      Arrays: { toString: function (a) { return '[' + a.join(', ') + ']'; } }
    },
    io: {
      ByteArrayOutputStream: function () {
        this._b = [];
        this.write = function (b) { this._b = this._b.concat(toByteArray(b)); };
        this.toByteArray = function () { return this._b; };
        this.toString = function () { return bytesToString(this._b); };
      },
      ByteArrayInputStream: function (bytes) {
        this._b = toByteArray(bytes);
        this.read = function () { return this._b.length ? this._b.shift() : -1; };
      }
    }
  };

  /* ==================================================================== *
   * [P5] javax.crypto 兼容层 —— 加密封面（AES-CTR）纯 JS 实现
   * 交付说明里「吃瓜718 的封面是 AES-CTR 加密图，规则用 javax.crypto 解」
   * 原来直接抛错；这里给等价 Cipher.getInstance / init / doFinal。
   * 支持 128/192/256 位密钥、CTR / CBC / ECB、NoPadding / PKCS5Padding。
   * 用 NIST SP 800-38A AES-128-CTR 第一块向量自测（见 simulate_merge.js）。
   * ==================================================================== */
  /* FIPS-197 标准 S-box（硬编码，避免动态生成实现出错） */
  var AES_SBOX = ("63 7c 77 7b f2 6b 6f c5 30 01 67 2b fe d7 ab 76 ca 82 c9 7d fa 59 47 f0 ad d4 a2 af 9c a4 72 c0 b7 fd 93 26 36 3f f7 cc 34 a5 e5 f1 71 d8 31 15 04 c7 23 c3 18 96 05 9a 07 12 80 e2 eb 27 b2 75 09 83 2c 1a 1b 6e 5a a0 52 3b d6 b3 29 e3 2f 84 53 d1 00 ed 20 fc b1 5b 6a cb be 39 4a 4c 58 cf d0 ef aa fb 43 4d 33 85 45 f9 02 7f 50 3c 9f a8 51 a3 40 8f 92 9d 38 f5 bc b6 da 21 10 ff f3 d2 cd 0c 13 ec 5f 97 44 17 c4 a7 7e 3d 64 5d 19 73 60 81 4f dc 22 2a 90 88 46 ee b8 14 de 5e 0b db e0 32 3a 0a 49 06 24 5c c2 d3 ac 62 91 95 e4 79 e7 c8 37 6d 8d d5 4e a9 6c 56 f4 ea 65 7a ae 08 ba 78 25 2e 1c a6 b4 c6 e8 dd 74 1f 4b bd 8b 8a 70 3e b5 66 48 03 f6 0e 61 35 57 b9 86 c1 1d 9e e1 f8 98 11 69 d9 8e 94 9b 1e 87 e9 ce 55 28 df 8c a1 89 0d bf e6 42 68 41 99 2d 0f b0 54 bb 16").split(/\s+/).map(function (x) { return parseInt(x, 16); });
  function xt(a, b) { return ((a ^ b) & 0xFF); }
  function mul2(a) { var r = (a << 1) & 0xFF; if (a & 0x80) r ^= 0x1B; return r; }
  function mul(a, b) {
    var r = 0, i;
    for (i = 0; i < 8; i++) { if (b & 1) r ^= a; a = mul2(a); b >>= 1; }
    return r & 0xFF;
  }
  function aesExpand(key) {
    var nk = key.length / 4, nr = nk + 6, w = [], i, t;
    var RCON = [0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80,0x1B,0x36,0x6C,0xD8,0xAB,0x4D];
    for (i = 0; i < nk; i++) w.push([key[4*i],key[4*i+1],key[4*i+2],key[4*i+3]]);
    for (i = nk; i < 4 * (nr + 1); i++) {
      t = w[i-1].slice();
      if (i % nk === 0) {
        t = [t[1],t[2],t[3],t[0]];
        t = t.map(function (x) { return AES_SBOX[x]; });
        t[0] ^= RCON[i/nk - 1];
      } else if (nk > 6 && i % nk === 4) {
        t = t.map(function (x) { return AES_SBOX[x]; });
      }
      var prev = w[i-nk], o = [];
      for (var j = 0; j < 4; j++) o.push(prev[j] ^ t[j]);
      w.push(o);
    }
    var rounds = [];
    for (var r = 0; r <= nr; r++) rounds.push(w.slice(4*r, 4*r+4).reduce(function (a, x) { return a.concat(x); }, []));
    return { rounds: rounds, nr: nr };
  }
  function aesBlock(block, rk) {
    var s = block.slice(), nr = rk.nr, r, i, tmp;
    for (i = 0; i < 16; i++) s[i] ^= rk.rounds[0][i];
    for (r = 1; r <= nr; r++) {
      for (i = 0; i < 16; i++) s[i] = AES_SBOX[s[i]];
      /* ShiftRows: state is column-major [r + 4c] */
      var o = new Array(16);
      for (var c = 0; c < 4; c++) for (var rr = 0; rr < 4; rr++)
        o[rr + 4*c] = s[rr + 4*((c + rr) % 4)];
      s = o;
      if (r !== nr) {
        for (c = 0; c < 4; c++) {
          var a0=s[4*c],a1=s[4*c+1],a2=s[4*c+2],a3=s[4*c+3];
          s[4*c]   = mul(a0,2) ^ mul(a1,3) ^ a2 ^ a3;
          s[4*c+1] = a0 ^ mul(a1,2) ^ mul(a2,3) ^ a3;
          s[4*c+2] = a0 ^ a1 ^ mul(a2,2) ^ mul(a3,3);
          s[4*c+3] = mul(a0,3) ^ a1 ^ a2 ^ mul(a3,2);
        }
      }
      for (i = 0; i < 16; i++) s[i] ^= rk.rounds[r][i];
    }
    return s;
  }
  function ctrInc(c) { for (var i = 15; i >= 0; i--) { c[i] = (c[i] + 1) & 0xFF; if (c[i]) break; } }
  function pkcs7Unpad(b) { var p = b[b.length-1]; if (p > 0 && p <= 16) return b.slice(0, b.length - p); return b; }
  function aesCrypto(mode, keyBytes, ivBytes, dataBytes) {
    var rk = aesExpand(keyBytes), out = [], i, j, prev, ctr, ks, blk;
    if (mode === 'CTR') {
      ctr = ivBytes.slice(0, 16); while (ctr.length < 16) ctr.push(0);
      for (i = 0; i < dataBytes.length; i += 16) {
        ks = aesBlock(ctr, rk); ctrInc(ctr);
        blk = dataBytes.slice(i, i + 16);
        for (j = 0; j < blk.length; j++) out.push(blk[j] ^ ks[j]);
      }
      return out;
    }
    prev = ivBytes.slice(0, 16); while (prev.length < 16) prev.push(0);
    for (i = 0; i < dataBytes.length; i += 16) {
      blk = dataBytes.slice(i, i + 16);
      if (mode === 'CBC') {
        var dec = aesBlock(blk, rk);
        for (j = 0; j < 16; j++) out.push(dec[j] ^ prev[j]);
        prev = blk.slice();
      } else {
        var dec2 = aesBlock(blk, rk);
        for (j = 0; j < 16; j++) out.push(dec2[j]);
      }
    }
    var res = out.slice(0, dataBytes.length);
    return /PKCS/i.test(mode) ? pkcs7Unpad(res) : res;
  }
  function bytesFrom(v) {
    if (isArr(v)) return v.map(function (x) { return (Number(x) & 0xFF); });
    if (v && typeof v === 'object' && isArr(v._b)) return bytesFrom(v._b);
    if (typeof v === 'string') return stringToBytes(v);
    return [];
  }
  function makeCipher(trans) {
    var parts = str(trans).toUpperCase().split('/');
    var mode = parts[1] || 'ECB', pad = parts[2] || 'PKCS5PADDING';
    var key = null, iv = null, op = 0;
    return {
      init: function (opmode, keyObj, ivObj) {
        op = opmode;
        key = bytesFrom(keyObj && keyObj.getEncoded ? keyObj.getEncoded() : keyObj);
        iv = bytesFrom(ivObj && ivObj.getIV ? ivObj.getIV() : ivObj);
        return this;
      },
      doFinal: function (data) {
        var b = bytesFrom(data);
        /* Rhino 规则里 DECRYPT_MODE=2；加密模式这里也走同一核心（CTR 对合） */
        var out = aesCrypto(mode === 'CTR' ? 'CTR' : mode, key, iv, b);
        return out;
      },
      update: function (d) { return this.doFinal(d); },
      getIV: function () { return iv; }
    };
  }

  var JAVA_PACKAGES = {
    java: JAVA_SHIM,
    javax: {
      crypto: {
        Cipher: {
          getInstance: function (trans) { return makeCipher(trans); },
          DECRYPT_MODE: 2, ENCRYPT_MODE: 1
        },
        spec: {
          SecretKeySpec: function (bytes, alg) {
            return { getEncoded: function () { return bytesFrom(bytes); }, _alg: alg };
          },
          IvParameterSpec: function (bytes) {
            return { getIV: function () { return bytesFrom(bytes); } };
          }
        }
      }
    }
  };
  function toByteArray(v) {
    if (isArr(v)) return v.slice();
    if (typeof v === 'string') return stringToBytes(v);
    return [];
  }
  function stringToBytes(s) {
    var t = utf8Encode(s), out = [];
    for (var i = 0; i < t.length; i++) out.push(t.charCodeAt(i) & 0xFF);
    return out;
  }
  function bytesToString(bytes) {
    var s = '';
    for (var i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i] & 0xFF);
    return s;
  }

  /* ==================================================================== *
   * 6. $ 模块系统
   *    $.require('cg718')  -> 执行 MY_RULE.pages 里 path=cg718 的代码，
   *                            取 $.exports
   *    $(url).rule(fn) / .lazyRule(fn, p...)  -> 生成可执行的 hiker:// 链接
   *    $.toString(fn) -> 函数源码
   * ==================================================================== */
  function encodeLazy(fn, args) {
    var src = typeof fn === 'function' ? fn.toString() : str(fn);
    var a = [];
    for (var i = 0; i < (args || []).length; i++) a.push(JSON.stringify(args[i]));
    return (a.length ? src + '(' + a.join(',') + ')' : src);
  }

  /* [A-P0-8] $().select 的待选登记表：$(选项, 默认序, 标题).select(回调) 生成
   * __hkSelectExec("id") 链接，点击时 UI 按 id 取出选项弹「页面内选择层」，
   * 用户选完再把回调以选中值执行——真选择，不静默选第一项。 */
  HK.selectSeq = 0;
  global.__HK_SELECTS__ = global.__HK_SELECTS__ || {};

  function makeDollar(modName) {
    var d = function (x, hint, title3) {
      var base;
      if (!x || isArr(x)) base = 'hiker://empty';
      else if (str(x).indexOf('hiker://') === 0) base = str(x);
      else base = 'hiker://empty' + str(x);
      d.__inputValue = isArr(x) ? '' : str(x || '');
      d.__inputHint = str(hint || '');
      d.__selOpts = isArr(x) ? x : null;
      d.__selDef = (typeof hint === 'number') ? hint : 0;
      d.__selTitle = str(title3 || (typeof hint === 'string' ? hint : '') || '');
      return {
        rule: function (fn) { return base + '#@rule=js:' + (typeof fn === 'function' ? fn.toString() : str(fn)); },
        lazyRule: function (fn) {
          var rest = Array.prototype.slice.call(arguments, 1);
          return base + '#@lazyRule=.js:' + encodeLazy(fn, rest);
        },
        /* $(默认值, 提示语).input(回调)：点开弹输入框，把输入值作为第一个参数传给回调 */
        input: function (fn) {
          var rest = Array.prototype.slice.call(arguments, 1);
          return base + '#@input=' + encodeURIComponent(JSON.stringify({ v: str(d.__inputValue || ''), d: str(d.__inputHint || '') }))
            + '#@lazyRule=.js:' + encodeLazy(fn, rest);
        },
        /* [A-P0-8] $(选项数组, 默认序, 标题).select(回调) */
        select: function (fn) {
          var id = 's' + (++HK.selectSeq);
          global.__HK_SELECTS__[id] = {
            options: d.__selOpts || [],
            def: d.__selDef || 0,
            title: d.__selTitle || '',
            fn: (typeof fn === 'function') ? fn.toString() : str(fn)
          };
          return 'hiker://empty#@lazyRule=.js:__hkSelectExec(' + JSON.stringify(id) + ')';
        },
        toString: function () { return base; }
      };
    };
    d.toString = function (fn) { return typeof fn === 'function' ? fn.toString() : str(fn); };
    /* [P2] 合集框架用 $.require('page', 对象参数) 传子站配置。
     * 原来只接 name，第二参数被丢弃；而 requireModule 内部 str(对象) 又会
     * 变成 [object Object]。这里两个都修。 */
    d.require = function (name, param) { return requireModule(name, param); };
    d.exports = null;
    d.moduleName = modName;
    d.requireMem = function (name, param) { return requireModule(name, param); };
    d.hikerUrl = function (u) { return str(u); };
    return d;
  }

  /**
   * 加载模块：从当前规则对象的 pages 数组里按 path 找。
   * pages 可能是 JSON 字符串，也可能是数组。
   * 模块代码执行时 $ 指向它自己的模块对象，执行完取 $.exports。
   */
  function requireModule(name, importParam) {
    var key = str(name);
    var param = importParam === undefined ? null : importParam;
    /* hiker://page/<模块名>[?<参数>]：等价于「按名字加载 pages[] 里的模块」，
     * 参数通过 $.importParam 传给模块（阅动漫的 dataLoad / funcTools 就靠这个）。 */
    var pm = /^hiker:\/\/page\/([^?#]+)(?:[?#]([\s\S]*))?$/.exec(key);
    if (pm) {
      var modName = decodeURIComponent(pm[1]);
      param = pm[2] === undefined ? (param || '') : pm[2];
      key = modName;
    }
    var rule = PAGE.rule || {};
    /* [P2] 对象参数要保留类型；缓存 key 用稳定序列化，避免 [object Object] 撞车 */
    var paramKey = (param === null || param === undefined) ? ''
        : (typeof param === 'object' ? JSON.stringify(param) : str(param));
    var cacheKey = HK.ruleName() + '::' + key + (paramKey ? '::' + paramKey : '');
    if (has(STORE.modules, cacheKey)) return STORE.modules[cacheKey];

    var pages = rule.pages;
    if (typeof pages === 'string') pages = jparse(pages, []) || [];
    if (!isArr(pages)) pages = [];

    var page = null;
    for (var i = 0; i < pages.length; i++) {
      if (pages[i] && str(pages[i].path) === key) { page = pages[i]; break; }
    }
    if (!page) {
      /* 远程/本地模块：name 本身是 URL 或文件路径 */
      if (/^https?:\/\//.test(key) || /\.js$/i.test(key) || key.indexOf('hiker://') === 0) {
        var code = /^https?:\/\//.test(key) ? reqSync(key, {}) : fileRead(key);
        return runModule(key, code, cacheKey);
      }
      emit('hk.error', '模块不存在: ' + key);
      return null;
    }
    return runModule(key, str(page.rule || ''), cacheKey, param);
  }

  function runModule(key, code, cacheKey, importParam) {
    if (!trim(code)) { STORE.modules[cacheKey] = null; return null; }
    /* 只剥 "js:" 前缀 —— 后面的 "//" 是注释开头，不能一起剥掉，
     * 否则 `js:// 说明文字` 会变成非法 JS（实测第一条规则就死在这里）。 */
    var src = str(code).replace(/^\s*js:/, '');
    var mod$ = makeDollar(key);
    /* [P2] 对象参数原样透传（子站框架靠 $.importParam.site / .url 取配置） */
    mod$.importParam = (importParam === undefined || importParam === null) ? '' : importParam;
    var sc = makeScope({ '$': mod$, importParam: mod$.importParam });
    var result = null;
    try {
      var fn = new Function('__scope', 'with(__scope){\n' + src + '\n}');
      result = fn.call({}, sc);
    } catch (e) {
      emit('hk.error', '模块执行失败 ' + key + ': ' + (e && e.message ? e.message : e));
      STORE.modules[cacheKey] = null;
      return null;
    }
    var exp = mod$.exports;
    if (exp === null || exp === undefined) exp = (result && typeof result === 'object') ? result : null;
    STORE.modules[cacheKey] = exp;
    return exp;
  }
  HK.require = requireModule;

  /* ==================================================================== *
   * 7. 规则执行
   * ==================================================================== */
  /* [A-P0-9] vars 先同步进 PAGE（MY_URL→url、MY_PAGE→page、input、MY_PARAMS），
   * 作用域的 live getter 才能读到真实值；URL 带查询串时自动解析为 params。 */
  function applyVarsToPage(v) {
    if (!v) return;
    if (v.MY_URL !== undefined) PAGE.url = str(v.MY_URL);
    if (v.MY_KEYWORD !== undefined) PAGE.keyword = str(v.MY_KEYWORD);
    if (v.MY_PAGE !== undefined) PAGE.page = v.MY_PAGE;
    if (v.MY_TITLE !== undefined) PAGE.title = str(v.MY_TITLE);
    if (v.MY_PIC !== undefined) PAGE.pic = str(v.MY_PIC);
    if (v.input !== undefined) PAGE.input = v.input;
    var prm = (v.MY_PARAMS !== undefined) ? v.MY_PARAMS : v.params;
    if (prm !== undefined) PAGE.params = prm;
    else if (typeof v.MY_URL === 'string' && v.MY_URL.indexOf('?') >= 0) {
      var q = parseQuery(v.MY_URL);
      if (Object.keys(q).length) PAGE.params = q;
    }
  }
  HK.applyVarsToPage = applyVarsToPage;

  /* [A-P0-13] 收尾并入：d.push 的结果集 / setPreResult 在没有 setResult 时作结果 */
  function finishResult() {
    if (!OUT.hasResult && OUT.d && OUT.d.length) { OUT.result = OUT.d; OUT.hasResult = true; }
    if (!OUT.hasResult && OUT.preResult !== undefined) { OUT.result = OUT.preResult; OUT.hasResult = true; }
  }

  /**
   * 执行一段规则代码。返回 { ok, result, err }。
   * extraVars 会覆盖作用域里的变量（MY_URL / input / MY_KEYWORD…）。
   */
  function run(code, extraVars) {
    /* 规则字段一律可能带 "js:" 前缀（js:$.x() / js: const a = 1）。
     * 只剥这个前缀：`js: const a=…` 不剥就是语法错误；而 `js:$.x()` 不剥会被
     * 当成「label js:」侥幸跑通，掩盖问题。 */
    var src = str(code).replace(/^\s*js:/, '');
    if (!trim(src)) return { ok: false, err: '规则为空', result: undefined };
    applyVarsToPage(extraVars);
    var sc = makeScope();
    if (extraVars) for (var k in extraVars) if (has(extraVars, k)) sc[k] = extraVars[k];
    /* input 与 MY_URL 等同步到作用域 */
    try {
      var fn = new Function('__scope', 'with(__scope){\n' + src + '\n}');
      fn.call({}, sc);
      finishResult();
      return { ok: true, result: OUT.result, err: '' };
    } catch (e) {
      return { ok: false, err: (e && e.name ? e.name + ': ' : '') + str(e && e.message || e), result: undefined };
    }
  }
  HK.run = run;

  /* 执行 lazyRule / @rule= 里内联的 JS 表达式 */
  function runInline(code, extraVars) {
    var src = str(code).replace(/^\s*js:/, '');
    if (!trim(src)) return { ok: false, err: '空规则' };
    applyVarsToPage(extraVars);
    var sc = makeScope();
    if (extraVars) for (var k in extraVars) if (has(extraVars, k)) sc[k] = extraVars[k];
    try {
      var fn = new Function('__scope', 'with(__scope){\nreturn (' + src + '\n);}');
      var v = fn.call({}, sc);
      finishResult();
      return { ok: true, value: v, result: OUT.result };
    } catch (e1) {
      try {
        var fn2 = new Function('__scope', 'with(__scope){\n' + src + '\n}');
        fn2.call({}, sc);
        finishResult();
        return { ok: true, value: OUT.result, result: OUT.result };
      } catch (e2) {
        return { ok: false, err: (e2 && e2.name ? e2.name + ': ' : '') + str(e2 && e2.message || e2) };
      }
    }
  }
  HK.runInline = runInline;

  /* 播放：走宿主 [A-P0-7]
   * 契约：bridge.play(url,title,pic) 投递宿主播放器 + report('play', JSON)
   * 带 {url,title,pic,headers,flag}，headers 来自地址尾 ;{K@V&&…} 或 extra，
   * #标记# 段收进 flag，不再直接剥掉丢弃。 */
  HK.play = function (url, title, pic, extra) {
    var t = parsePlayTarget(url, extra);
    var u = t.url;
    if (!u) return false;
    var payload = { url: u, title: str(title || ''), pic: str(pic || ''), headers: t.headers, flag: str((extra && extra.flag) || t.flag || '') };
    emit('play', payload);
    if (bridge && typeof bridge.play === 'function') {
      try { return !!bridge.play(u, payload.title, payload.pic); } catch (e) { return false; }
    }
    emit('hk.play', u);
    return true;
  };

  /* 给界面层用的 myVar 读写（合集包的路由变量要在这里补） */
  HK.getMyVar = function (k) {
    var q = myKey(k);
    return has(STORE.myVars, q) ? STORE.myVars[q] : '';
  };
  HK.setMyVar = function (k, v) { STORE.myVars[myKey(k)] = v; return v; };

  global.HikerEngine = HK;
})(typeof window !== 'undefined' ? window : this);
