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

console.log('---');
console.log('selftest: ' + pass + ' passed, ' + fail + ' failed');
process.exit(fail ? 1 : 0);
