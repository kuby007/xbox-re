/*!
 * hiker-js-selftest.js —— 海阔 JS 运行时离线自测（Node，不进 assets）
 *
 * 用法：node docs/hiker-js-selftest.js
 * 以 vm + 最小 window/document/localStorage 桩，按 index.html 的 WebView
 * 加载链顺序加载全部 JS（不含 hiker_jst4.js —— QuickJS 专用；
 * 不含 hiker_app.js —— 与 hiker_ui 站点模型冲突，见 index.html 注释），
 * 然后断言方案 A 的关键能力：CryptoJS 已知向量、$().select、
 * getResCode/setResCode、fetch 家族、d 注入、MY_PARAMS/host/input 真值、
 * UI boot 不崩。HikerBridge 用桩实现（aes/fetchFull 返回固定值），
 * 所以 AES 只验存在性，摘要算法验已知答案向量。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const ASSETS = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'hiker');
const CHAIN = [
  'hiker_bridge_adapter.js',
  'hiker_runtime_shim.js',
  'hiker_api_surface.js',
  'hiker_scheme.js',
  'hiker_compat.js',
  'hiker_engine.js',
  'hiker_import.js',
  'hiker_ui.js'
];

/* ---------------- 最小 DOM / 环境桩 ---------------- */
function makeEl(id) {
  return {
    id: id || '', children: [], style: {}, textContent: '', innerHTML: '', value: '',
    files: [], scrollTop: 0, parentNode: null,
    classList: {
      _s: {},
      add(c) { this._s[c] = 1; },
      remove(c) { delete this._s[c]; },
      contains(c) { return !!this._s[c]; }
    },
    addEventListener() {}, removeEventListener() {},
    appendChild(c) { this.children.push(c); return c; },
    removeChild(c) { return c; },
    querySelector() { return null; },
    querySelectorAll() { return []; },
    setAttribute() {}, getAttribute() { return null; },
    focus() {}, click() {}, matches() { return false; }
  };
}
const elCache = {};
const lsData = {};
const sb = {
  console,
  setTimeout, clearTimeout, setInterval, clearInterval,
  JSON, Math, Date, RegExp, Array, Object, String, Number, Boolean, Error, TypeError,
  parseInt, parseFloat, isNaN, isFinite, encodeURIComponent, decodeURIComponent,
  encodeURI, decodeURI, escape, unescape, btoa, atob, Promise, Symbol, Map, Set, WeakMap,
  Uint8Array, ArrayBuffer, DataView, URL,
  navigator: { userAgent: 'Mozilla/5.0 (Linux; Android 13) HikerSelfTest', clipboard: null },
  location: { href: 'hiker://home', protocol: 'hiker:' },
  localStorage: {
    getItem(k) { return Object.prototype.hasOwnProperty.call(lsData, k) ? lsData[k] : null; },
    setItem(k, v) { lsData[k] = String(v); },
    removeItem(k) { delete lsData[k]; }
  },
  document: {
    getElementById(id) { return elCache[id] || (elCache[id] = makeEl(id)); },
    createElement(tag) { return makeEl(tag); },
    addEventListener() {},
    querySelector() { return null; },
    hidden: false,
    body: makeEl('body')
  },
  prompt() { return '1'; },
  confirm() { return true; },
  CustomEvent: function (type, opts) { this.type = type; this.detail = opts && opts.detail; },
  DOMParser: function () {},
  FileReader: function () {},
  HikerBridge: {
    req() { return ''; },
    call(name) {
      if (name === 'fetchFull') return JSON.stringify({ status: 200, headers: {}, body: 'FULLBODY' });
      if (name === 'getCookie') return '';
      if (name === 'unzipRule') return JSON.stringify({ ok: false, error: 'stub' });
      return '';
    },
    report() {},
    play() { return true; },
    toast() {},
    readClipboard() { return ''; },
    writeClipboard() {}
  },
  addEventListener() {},
  dispatchEvent() { return true; }
};
sb.window = sb;
sb.self = sb;
sb.globalThis = sb;
sb.DOMParser.prototype.parseFromString = function () {
  return { querySelectorAll() { return []; }, querySelector() { return null; } };
};

const ctx = vm.createContext(sb);

let pass = 0, fail = 0;
function check(name, cond, extra) {
  if (cond) { pass++; console.log('PASS  ' + name); }
  else { fail++; console.log('FAIL  ' + name + (extra !== undefined ? '  => ' + extra : '')); }
}

/* ---------------- 按 WebView 顺序加载 ---------------- */
for (const f of CHAIN) {
  const code = fs.readFileSync(path.join(ASSETS, f), 'utf8');
  try {
    vm.runInContext(code, ctx, { filename: f });
    console.log('LOAD  ' + f);
  } catch (e) {
    check('load ' + f, false, e && e.message);
  }
}

const HK = sb.HikerEngine;
check('HikerEngine 存在', !!HK);
check('HikerImport 存在', !!sb.HikerImport);
check('HikerUI 存在', !!sb.HikerUI);

/* boot（installGlobals + 站点渲染 + 钩子接线）不得抛错 */
let bootErr = '';
try { sb.HikerUI.boot(); } catch (e) { bootErr = (e && e.message) || String(e); }
check('HikerUI.boot() 不抛错', bootErr === '', bootErr);

/* ---------------- 全局层断言 ---------------- */
check('全局 CryptoJS 已安装', typeof sb.CryptoJS === 'object' && !!sb.CryptoJS);
check('全局 getCryptoJS 是函数', typeof sb.getCryptoJS === 'function');
check('全局 joinUrl 是函数', typeof sb.joinUrl === 'function');
check('全局 getResCode 是函数', typeof sb.getResCode === 'function');

const CJ = sb.CryptoJS || {};
check("CryptoJS.MD5('abc')", CJ.MD5 && CJ.MD5('abc').toString() === '900150983cd24fb0d6963f7d28e17f72',
  CJ.MD5 && CJ.MD5('abc').toString());
check("CryptoJS.SHA1('abc')", CJ.SHA1 && CJ.SHA1('abc').toString() === 'a9993e364706816aba3e25717850c26c9cd0d89d',
  CJ.SHA1 && CJ.SHA1('abc').toString());
check("CryptoJS.SHA256('abc')", CJ.SHA256 && CJ.SHA256('abc').toString() === 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
  CJ.SHA256 && CJ.SHA256('abc').toString());
check("CryptoJS.HmacSHA1(fox,'key')", CJ.HmacSHA1 && CJ.HmacSHA1('The quick brown fox jumps over the lazy dog', 'key').toString() === 'de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9',
  CJ.HmacSHA1 && CJ.HmacSHA1('The quick brown fox jumps over the lazy dog', 'key').toString());
check('CryptoJS.AES.encrypt/decrypt 存在', !!(CJ.AES && typeof CJ.AES.encrypt === 'function' && typeof CJ.AES.decrypt === 'function'));
check('CryptoJS.enc.Base64 往返', CJ.enc && CJ.enc.Utf8.stringify(CJ.enc.Base64.parse(CJ.enc.Base64.stringify(CJ.enc.Utf8.parse('hiker-海阔')))) === 'hiker-海阔');

/* ---------------- 作用域层断言（经 HK.run 真跑规则代码） ---------------- */
function runRes(code, vars) {
  HK.resetOut();
  const r = HK.run(code, vars || {});
  if (!r.ok) return { err: r.err };
  return { value: r.result };
}

let t = runRes('js:setResult(typeof CryptoJS + "/" + typeof getCryptoJS + "/" + typeof fetch + "/" + typeof fetchPC + "/" + typeof joinUrl + "/" + typeof $("x").select)');
check('作用域: CryptoJS(object)/getCryptoJS/fetch/fetchPC/joinUrl/$().select 已定义',
  t.value === 'object/function/function/function/function/function', JSON.stringify(t));

t = runRes('js:eval(getCryptoJS()); setResult(CryptoJS.SHA256("abc").toString())');
check('作用域: eval(getCryptoJS()) 后 CryptoJS 可用', t.value === 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad', JSON.stringify(t));

t = runRes('js:setResCode("PRE-FETCHED"); setResult(getResCode())');
check('作用域: setResCode/getResCode 往返', t.value === 'PRE-FETCHED', JSON.stringify(t));

t = runRes('js:d.push({ title: "只有d.push", url: "hiker://empty" })');
check('全局 d 已注入: 只 d.push 也能成为结果',
  Array.isArray(t.value) && t.value.length === 1 && t.value[0].title === '只有d.push', JSON.stringify(t));

HK.setPage({ rule: { title: 'T', url: 'https://a.b.com/' }, url: 'https://a.b.com/x?p=1', page: 1, input: '' });
t = runRes('js:setResult([MY_PARAMS.p, host, input, MY_PAGE].join("|"))',
  { MY_URL: 'https://a.b.com/x?p=1', input: '关键词' });
check('MY_PARAMS/host/input/MY_PAGE 真实值', t.value === '1|a.b.com|关键词|1', JSON.stringify(t));

t = runRes('js:setResult($(["甲", "乙"], 0, "选一个").select(function (v) { return v; }))');
const selKeys = Object.keys(sb.__HK_SELECTS__ || {});
check('$().select 生成登记链接（不静默选第一项）',
  typeof t.value === 'string' && t.value.indexOf('__hkSelectExec') >= 0 && selKeys.length === 1,
  JSON.stringify(t) + ' keys=' + selKeys.length);

t = runRes('js:var w = JSON.parse(fetch("https://x.test/", { withHeaders: true })); setResult(w.body + "/" + w.statusCode)');
check('fetch withHeaders 走 fetchFull 包装语义', t.value === 'FULLBODY/200', JSON.stringify(t));

t = runRes('js:putMyVar("tk", "v123"); setResult(getMyVar("tk"))');
check('putMyVar/getMyVar 往返', t.value === 'v123', JSON.stringify(t));
check('变量已落 localStorage', (lsData.hk_myvars_v1 || '').indexOf('v123') >= 0, lsData.hk_myvars_v1);

t = runRes('js:setResult(joinUrl("https://a.b.com/dir/page.html", "../img/1.jpg"))');
check('joinUrl 相对路径补全', t.value === 'https://a.b.com/img/1.jpg', JSON.stringify(t));

/* _pd 家族只验存在性：pdfh 的选择器走原生 DOMParser+querySelectorAll，
 * Node 桩的 DOMParser 是空实现，取值行为须在真 WebView 里验（桩限制，非缺陷） */
t = runRes('js:setResult(typeof _pd + "/" + typeof _pdfa + "/" + typeof _pdfh + "/" + typeof _pdfl)');
check('_pd/_pdfa/_pdfh/_pdfl 别名已定义',
  t.value === 'function/function/function/function', JSON.stringify(t));

t = runRes('js:setResult(getColTypes().indexOf("flex_button") >= 0 ? "yes" : "no")');
check('getColTypes 含 flex_button', t.value === 'yes', JSON.stringify(t));

/* ---------------- [FIX2] 七项运行时修复的回归用例 ---------------- */
const UI2 = sb.HikerUI;

/* BUG-1: fyclass/fypage 模板替换（UI 层纯函数） */
UI2.STATE.site = { name: 'TplT', raw: { class_name: '最新&AI短剧', class_url: 'home&ai-duanju' }, homePage: 'hiker://empty##fyclass##fypage' };
UI2.STATE.curClass = '';
check('BUG-1 substTpl 默认首个分类+第1页',
  UI2.substTpl('hiker://empty##fyclass##fypage', 1) === 'hiker://empty##home##1',
  UI2.substTpl('hiker://empty##fyclass##fypage', 1));
UI2.STATE.curClass = 'ai-duanju';
check('BUG-1 substTpl 当前分类+翻页',
  UI2.substTpl('hiker://empty##fyclass##fypage', 3) === 'hiker://empty##ai-duanju##3');
check('BUG-1 无占位 URL 原样返回', UI2.substTpl('https://a.b/c?x=1', 2) === 'https://a.b/c?x=1');
UI2.STATE.site = null; UI2.STATE.curClass = '';

/* BUG-2: encodeLazy 带参箭头函数必须加括号（CMS模板选集实测炸点） */
t = runRes('js:setResult($("hiker://empty").lazyRule((arg) => { return "got:" + arg; }, "X"))');
const lazyPart = (function () { try { return HK.parseUrl(t.value).lazy || ''; } catch (e) { return ''; } })();
check('BUG-2 lazyRule 箭头函数带参已整体加括号',
  lazyPart.indexOf('((arg)') === 0 && lazyPart.slice(-6) === ')("X")', lazyPart);
HK.resetOut();
const lr2 = HK.runInline(lazyPart);
check('BUG-2 编码后的 lazy 可执行出结果', lr2.ok && lr2.value === 'got:X', JSON.stringify(lr2));

/* BUG-3: $().rule(fn) 裸函数点击时执行函数体（HoHoJ 卡片形态） */
HK.resetOut();
const brA = HK.runInline('js:() => { return "bare-called"; }');
check('BUG-3 裸箭头函数被调用而非当值返回', brA.ok && brA.value === 'bare-called', JSON.stringify(brA));
HK.resetOut();
const brB = HK.runInline('js:function () { return "fn-called"; }');
check('BUG-3 裸 function 被调用', brB.ok && brB.value === 'fn-called', JSON.stringify(brB));
HK.resetOut();
const brC = HK.runInline('js:1 + 2');
check('BUG-3 普通表达式不被误调用', brC.ok && brC.value === 3, JSON.stringify(brC));

/* BUG-4: extra 注入 MY_PARAMS 后 getParam 能读到（影探4K extra.id 形态） */
HK.setPage({ rule: { title: 'T4', url: 'hiker://empty' }, ruleName: 'T4', url: 'hiker://page/detail', page: 1, input: '' });
t = runRes('js:setResult(getParam("id") + "/" + MY_PARAMS.id)',
  { MY_URL: 'hiker://page/detail', MY_PARAMS: { id: 'vod42' } });
check('BUG-4 getParam 读到 MY_PARAMS(extra)', t.value === 'vod42/vod42', JSON.stringify(t));

/* BUG-5: $.require('hiker://page/<别名>') 回退到已落盘核心文件（HoHoJ 形态） */
HK.setPage({ rule: { title: 'HoHoJT', pages: [] }, ruleName: 'HoHoJT', url: 'hiker://empty', page: 1, input: '' });
HK.STORE.files['hiker://files/rules/HoHoJT/HoHoJT.js'] = '$.exports = { ping: "pong" };';
t = runRes('js:setResult($.require("hiker://page/HoHoJTCore").ping)');
check('BUG-5 page 别名按 basename 回退落盘文件', t.value === 'pong', JSON.stringify(t));

/* BUG-6: config/initConfig 全局（聚阅形态）+ java.lang.Thread.sleep（DrpyHiker 形态） */
t = runRes('js:initConfig({T6: "u1"}); setResult(config.T6 + "/" + typeof config.MISSING)');
check('BUG-6 config/initConfig 往返、无值不报错', t.value === 'u1/undefined', JSON.stringify(t));
t = runRes('js:setResult(typeof java.lang.Thread.sleep)');
check('BUG-6 java.lang.Thread.sleep 存在', t.value === 'function', JSON.stringify(t));

/* BUG-7: 同规则顶层定义跨 run→runInline 共享（中国X站 jGet 形态） */
HK.setPage({ rule: { title: 'T7', pages: [] }, ruleName: 'T7', url: 'hiker://empty', page: 1, input: '' });
runRes('js:function jGet(x) { return "JG" + x; } var t7ver = "v9"; setResult("page-done")');
HK.resetOut();
const sh7 = HK.runInline('js:jGet("!") + "/" + t7ver');
check('BUG-7 lazy 可见同页顶层 function/var', sh7.ok && sh7.value === 'JG!/v9', JSON.stringify(sh7));

console.log('---');
console.log('selftest: ' + pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
