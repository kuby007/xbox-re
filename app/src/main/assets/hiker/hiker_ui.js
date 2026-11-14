/*!
 * hiker_ui.js —— 小程序界面层（站点管理 / 首页 / 分类 / 搜索 / 详情 / 播放）
 *
 * 页面模型和海阔一致：一条规则产出「条目数组」，每个条目有 col_type，
 * 由这里按 col_type 渲染成行（海报网格 / 侧栏按钮 / 输入框 / 富文本 …）。
 * 点条目的行为也按海阔语义分发：
 *   @lazyRule / @rule  -> 执行内联 JS -> 有结果就换页，没结果就当刷新
 *   hiker://page/...   -> 进详情
 *   http(s) + isVideo  -> 交给宿主播放器
 *   http(s) 其它       -> 打开网页（用 WebView）
 */
(function (global) {
  'use strict';

  var HK = global.HikerEngine;
  var IMP = global.HikerImport;
  var str = function (v) { return (v === null || v === undefined) ? '' : String(v); };

  var UI = { version: '3.0.0' };
  var $ = function (id) { return document.getElementById(id); };

  /* ------------------------------------------------------------------ 状态 */
  var STATE = {
    sites: [],          /* 已导入站点 */
    site: null,         /* 当前站点 */
    stack: [],          /* 页面栈 [{title, items, url}] */
    busy: false
  };
  UI.STATE = STATE;

  var LS_SITES = 'hiker_sites_v3';

  function loadSites() {
    try {
      var v = localStorage.getItem(LS_SITES);
      STATE.sites = v ? (JSON.parse(v) || []) : [];
    } catch (e) { STATE.sites = []; }
  }
  function saveSites() {
    try { localStorage.setItem(LS_SITES, JSON.stringify(STATE.sites)); } catch (e) {}
  }
  UI.saveSites = saveSites;

  /* ------------------------------------------------------------------ 提示 */
  var toastTimer = null;
  function toast(msg) {
    var el = $('toast');
    if (!el) return;
    el.textContent = str(msg);
    el.classList.add('show');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove('show'); }, 2200);
  }
  UI.toast = toast;

  function setBusy(b, text) {
    STATE.busy = b;
    var m = $('busy');
    if (!m) return;
    if (b) { m.textContent = text || '加载中…'; m.classList.add('show'); }
    else m.classList.remove('show');
  }

  /* ------------------------------------------------------------------ 站点管理 */
  function renderSites() {
    var box = $('siteList');
    if (!box) return;
    if (!STATE.sites.length) {
      box.innerHTML = '<div class="empty"><span class="big">📺</span>还没有导入小程序<br>'
        + '<span class="dim">把口令粘进来，或点下面的「导入」</span></div>';
      return;
    }
    var html = '';
    for (var i = 0; i < STATE.sites.length; i++) {
      var s = STATE.sites[i];
      /* 每个站点一个色相：一屏多个站点时便于扫读，又不破坏整体蓝调。
         用与 --grad 同构的 135° 双色渐变，饱和度压在中等，避免花。 */
      var c = pickColor(i);
      html += '<div class="card site fade-up" data-idx="' + i + '" style="animation-delay:' + (i * 28) + 'ms">'
        + '<div class="site-ic" style="background:linear-gradient(135deg,' + c[0] + ',' + c[1] + ')">'
        + esc(str(s.name).slice(0, 1)) + '</div>'
        + '<div class="site-nm">' + esc(s.name) + '</div>'
        + '<div class="site-mt">'
        + '<span class="pill">' + esc(s.type || 'video') + '</span>'
        + (s.group ? '<span class="pill">' + esc(s.group) + '</span>' : '')
        + '<span class="pill">' + (s.pages && s.pages.length ? s.pages.length + ' 模块' : '单页') + '</span>'
        + '</div>'
        + '<button class="del" data-del="' + i + '" aria-label="移除站点" title="移除">✕</button>'
        + '</div>';
    }
    box.innerHTML = html;
  }

  /* 站点图标的色相组：每项是 [主,次] 两色，同一色系不同明度，叠起来更干净 */
  function pickColor(i) {
    var c = [
      ['#1A73E8', '#4F9CFF'], ['#0E9F6E', '#48D19A'], ['#C7681A', '#F0A55A'],
      ['#7A4FD6', '#A98BF5'], ['#C23B5C', '#F2706F'], ['#0F8A9E', '#5CD0DC']
    ];
    return c[i % c.length];
  }
  /* 规则文本普遍用 HTML 实体写样式（&nbsp; / &lt;font&gt; / &#39;），
   * 直接 esc 会把 & 再转义一次，页面上就显示成字面的「&nbsp」。
   * 所以先还原实体、再统一转义：解码后的尖括号仍会被转成文本，不会变成真标签。 */
  function unent(t) {
    return str(t)
      /* 规则里普遍写 &nbsp **不带分号**（HTML5 的 legacy 解析照样认），
       * 只认带分号的形式的话，页面上就会原样显示「&nbsp」这串字。 */
      .replace(/&nbsp;?/gi, '\u00A0')
      .replace(/&lt;?/gi, '<').replace(/&gt;?/gi, '>')
      .replace(/&quot;?/gi, '"').replace(/&apos;?/gi, "'")
      .replace(/&#(\d+);/g, function (_, d) { return String.fromCharCode(parseInt(d, 10)); })
      .replace(/&#x([0-9a-f]+);/gi, function (_, h) { return String.fromCharCode(parseInt(h, 16)); })
      .replace(/&amp;?/gi, '&');
  }

  function esc(s) {
    return unent(s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  UI.esc = esc;

  /* ------------------------------------------------------------------ 导入 */
  /* 五种输入来源：口令 / 带密码口令 / 文件 / 粘贴规则 JSON / 剪贴板。
     面板里是一行可左右滑动的按钮，同一时间只显示对应那一种输入框。 */
  var IMPORT = { mode: 'code', fileName: '' };
  UI.IMPORT = IMPORT;

  var MODE_HINT = {
    code: '云口令、分享口令都行，多条一行一条。',
    pwd: '口令 + 密码。密码会拼到云便签地址上取回内容。',
    file: '选 .hkzip / .zip / .json 文件导入，规则会自动解出来。',
    json: '直接粘贴 rule.json 的内容（单个对象或数组都行）。',
    clip: '从系统剪贴板读口令，读到就直接导入。'
  };

  function setMode(m) {
    IMPORT.mode = m || 'code';
    var row = $('modeRow');
    if (row) {
      var bs = row.querySelectorAll('.mbtn');
      for (var i = 0; i < bs.length; i++) {
        bs[i].classList.toggle('on', bs[i].getAttribute('data-mode') === IMPORT.mode);
      }
    }
    var hint = $('importHint');
    if (hint) hint.textContent = MODE_HINT[IMPORT.mode] || MODE_HINT.code;
    var ta = $('importText'), pwd = $('importPwd'), fp = $('filePane');
    if (ta) ta.style.display = (IMPORT.mode === 'file' || IMPORT.mode === 'clip') ? 'none' : 'block';
    if (pwd) pwd.style.display = (IMPORT.mode === 'pwd') ? 'block' : 'none';
    if (fp) fp.style.display = (IMPORT.mode === 'file') ? 'block' : 'none';
    if (ta) { ta.placeholder = IMPORT.mode === 'json' ? '粘贴 rule.json 全文' : '云6oooole/xxxxxx/xxxx'; }
  }
  UI.setMode = setMode;

  function openImport() {
    $('importMask').classList.add('show');
    var ta = $('importText');
    if (ta) ta.value = '';
    var pw = $('importPwd');
    if (pw) pw.value = '';
    IMPORT.fileName = '';
    var fn = $('fileName');
    if (fn) fn.textContent = '';
    setMode('code');
  }
  function closeImport() { $('importMask').classList.remove('show'); }

  /* 真正入库：口令/文件都汇到这一步 */
  function applySites(sites, notes) {
    if (!sites || !sites.length) {
      toast(notes && notes[0] ? notes[0] : '没有解析到站点');
      log(notes);
      return false;
    }
    for (var i = 0; i < sites.length; i++) {
      var s = sites[i], dup = -1;
      for (var j = 0; j < STATE.sites.length; j++) {
        if (STATE.sites[j].key === s.key) { dup = j; break; }
      }
      if (dup >= 0) STATE.sites[dup] = s; else STATE.sites.push(s);
    }
    saveSites();
    renderSites();
    closeImport();
    toast('导入成功：' + sites.length + ' 个站点');
    log(notes);
    return true;
  }

  function runImport(text, opts) {
    setBusy(true, '正在取回规则…');
    setTimeout(function () {
      var r;
      try { r = IMP.importText(text, opts || {}); }
      catch (e) { setBusy(false); toast('解析出错：' + (e && e.message)); return; }
      setBusy(false);
      if (!r.ok) { toast(r.notes && r.notes[0] ? r.notes[0] : '没有解析到站点'); log(r.notes); return; }
      applySites(r.sites, r.notes);
    }, 30);
  }
  UI.runImport = runImport;

  /** 文件导入回调：Java 侧解压后把正文按 Base64 回传（避免任何转义/换行把 JS 炸掉）。 */
  global.HikerFile = {
    onText: function (name, error, b64) {
      setBusy(false);
      IMPORT.fileName = str(name);
      var fn = $('fileName');
      if (fn) fn.textContent = IMPORT.fileName ? ('已选择：' + IMPORT.fileName) : '';
      if (error) { toast('导入失败：' + error); log(['文件 ' + name + ' 导入失败: ' + error]); return; }
      var text = '';
      try { text = HK.b64d(str(b64)); } catch (e) { text = ''; }
      if (!text) { toast('文件内容读不出来'); return; }
      runImport(text, {});
    }
  };

  function doImport() {
    if (IMPORT.mode === 'file') {
      if (global.HikerBridge && typeof global.HikerBridge.pickRuleFile === 'function') {
        setBusy(true, '等待选择文件…');
        try { global.HikerBridge.pickRuleFile(); }
        catch (e) { setBusy(false); toast('打不开文件选择器'); }
      } else {
        toast('当前环境不支持文件选择');
      }
      return;
    }
    if (IMPORT.mode === 'clip') {
      if (!global.HikerBridge || typeof global.HikerBridge.clipboardText !== 'function') {
        toast('读不到剪贴板，请用「口令」粘贴');
        return;
      }
      setBusy(true, '读剪贴板…');
      var t = '';
      try { t = str(global.HikerBridge.clipboardText()); } catch (e) { t = ''; }
      setBusy(false);
      if (!t.trim()) { toast('剪贴板是空的'); return; }
      var ta0 = $('importText');
      if (ta0) ta0.value = t;
      runImport(t, {});
      return;
    }
    var text = $('importText') ? $('importText').value : '';
    if (!text || !text.trim()) { toast('先把口令粘进来'); return; }
    runImport(text, { password: $('importPwd') ? $('importPwd').value : '' });
  }


  function log(lines) {
    if (!lines) return;
    try {
      var el = $('diagLog');
      if (el) el.textContent = [].concat(lines).join('\n') + '\n' + el.textContent;
    } catch (e) {}
  }
  UI.log = log;

  /* ------------------------------------------------------------------ 打开站点 */
  function openSite(idx) {
    var s = STATE.sites[idx];
    if (!s) return;
    STATE.site = s;
    STATE.stack = [];
    var rule = s.raw;
    if (typeof rule.pages === 'string') { try { rule.pages = JSON.parse(rule.pages); } catch (e) { rule.pages = []; } }
    HK.setPage({ rule: rule, ruleName: s.name, url: s.homePage, keyword: '', page: 1, title: s.name, pic: '' });
    /* 模块缓存按站点隔离，切站点要清 */
    HK.STORE.modules = {};
    showView('rule');
    $('ruleTitle').textContent = s.name;
    /* preRule 先跑一次（阅动漫用它初始化 myCollection / 检查更新） */
    if (s.rules.pre) {
      try { HK.run(s.rules.pre, {}); } catch (e) {}
    }
    /* 合集类规则的 preRule 常常是 evalPrivateJS（海阔用内置密钥加密的），
     * WebView 里解不开 —— 这时 remoteUrl / myCollection 会是空，dataLoad 直接返回 0 条。
     * 兜底：用合集包自带的信息补上（包里 require.json 的源地址 + 自身标题）。 */
    if (!HK.getMyVar('remoteUrl')) {
      if (s.remoteUrl) HK.setMyVar('remoteUrl', s.remoteUrl);
      else if (s.libUrls && s.libUrls.length) HK.setMyVar('remoteUrl', s.libUrls[0]);
    }
    if (!HK.getMyVar('myCollection')) HK.setMyVar('myCollection', s.name);
    runPage(s.rules.home, { MY_URL: s.homePage, MY_PAGE: 1, input: '', MY_KEYWORD: '' }, s.name);
  }
  UI.openSite = openSite;

  /* 加载占位：先给网格结构，再等真实条目替换。
   * 比空白页 + 转圈体感快得多，尤其是合集类规则要连拉几个模块的时候。 */
  function showSkeleton(n) {
    var box = $('content');
    if (!box) return;
    var h = '<div class="sk-grid">';
    for (var i = 0; i < (n || 9); i++) {
      h += '<div class="sk"><div class="sk-ph"></div><div class="sk-l"></div><div class="sk-l s"></div></div>';
    }
    box.innerHTML = h + '</div>';
  }
  UI.showSkeleton = showSkeleton;

  /* 执行一个页面规则并把结果推进页面栈 */
  function runPage(code, vars, title) {
    if (!code) { toast('这个站点没有配置该页规则'); return; }
    STATE.lastRun = { code: code, vars: vars || {}, title: title || '' };
    setBusy(true, title || '加载中…');
    showSkeleton(9);
    setTimeout(function () {
      HK.resetOut();
      var r = HK.run(code, vars || {});
      setBusy(false);
      if (!r.ok) {
        showError('规则执行失败', r.err);
        log('[' + (title || '') + '] ' + r.err);
        return;
      }
      var items = normalizeResult(r.result);
      if (!items.length) {
        showError('没有拿到内容', '规则执行完成，但返回为空。可以换一个分类或稍后再试。');
        return;
      }
      STATE.stack.push({ title: title || '', items: items, url: HK.page.url });
      renderItems(items, title);
    }, 30);
  }
  UI.runPage = runPage;

  /* setResult 的几种形态统一成条目数组 */
  function normalizeResult(v) {
    if (v === null || v === undefined) return [];
    if (typeof v === 'string') {
      var d = null;
      try { d = JSON.parse(v); } catch (e) { d = null; }
      if (d) return normalizeResult(d);
      return [{ title: v.slice(0, 120), col_type: 'text_1', url: 'hiker://empty' }];
    }
    if (Object.prototype.toString.call(v) === '[object Array]') return v;
    if (typeof v === 'object') {
      var keys = ['list', 'data', 'items', 'result', 'videos'];
      for (var i = 0; i < keys.length; i++) if (Object.prototype.toString.call(v[keys[i]]) === '[object Array]') return v[keys[i]];
    }
    return [];
  }

  /* ------------------------------------------------------------------ 渲染 */
  var POSTER = { 'movie_1': 1, 'movie_2': 1, 'movie_3': 1, 'movie_4': 1, 'movie_1_vertical_pic': 1, 'icon_round_small_4': 1, 'icon_small_4': 1, 'movie_2_vertical_pic': 1 };

  /* 统一的 col_type 归一：显式声明优先；没声明但带图的按海报卡处理（很多合集规则就是这样）。
   * 必须内外层共用同一函数 —— 之前外层用补图后的类型、内层用原始类型，
   * 导致分组循环 i 不前进、字符串拼到爆内存。 */
  function ctOf(it) {
    if (!it) return 'text_1';
    if (it.col_type) return str(it.col_type);
    return (it.pic_url || it.pic || it.img) ? 'movie_3' : 'text_1';
  }

  function renderItems(items, title) {
    var box = $('content');
    if (!box) return;
    /* 记住条目，供 updateItem/findItem 用 */
    for (var i = 0; i < items.length; i++) {
      if (items[i] && items[i].extra && items[i].extra.id) HK.STORE.items[str(items[i].extra.id)] = items[i];
    }
    var html = '', i = 0;
    while (i < items.length) {
      var it = items[i] || {};
      /* 合集规则常会吐「空壳」条目：没标题、没图、没地址，只是占位。
       * 渲染出来就是一个空按钮，看着像加载失败。直接跳过。 */
      var _t = str(it.title).replace(/<[^>]+>/g, '').replace(/&nbsp;?/gi, ' ').trim();
      if (!_t && !str(it.pic_url || it.pic || it.img) && !str(it.url)) { i++; continue; }
      var ct = ctOf(it);
      if (ct === 'movie_1_vertical_pic_blur') { html += renderHero(it); i++; continue; }
      if (POSTER[ct]) {
        var group = [];
        while (i < items.length && POSTER[ctOf(items[i])]) { group.push(items[i]); i++; }
        html += renderPosterGrid(group);
        continue;
      }
      /* 分类胶囊/选集这类 scroll_button：连续的收成**一行**横向滚动。
       * 以前是一个个 inline-block 自动换行，条目一多就占掉大半屏，
       * 用户得往下翻很久才能看到正文。 */
      if (ct === 'scroll_button') {
        var chips = [];
        while (i < items.length && ctOf(items[i]) === 'scroll_button') { chips.push(items[i]); i++; }
        var ch = '';
        for (var ci = 0; ci < chips.length; ci++) ch += renderSingle(chips[ci], 'scroll_button');
        html += '<div class="scroll-row">' + ch + '</div>';
        continue;
      }
      html += renderSingle(it, ct);
      i++;
    }
    box.innerHTML = html || '<div class="empty">没有内容</div>';
    box.scrollTop = 0;
    /* 重播入场动画：先摘掉类并强制一次回流，否则连续换页时动画不会重新触发 */
    box.classList.remove('fade-up');
    void box.offsetWidth;
    box.classList.add('fade-up');
    $('ruleTitle').textContent = title || (STATE.site ? STATE.site.name : '');
    $('backBtn').style.display = 'flex';
  }
  UI.renderItems = renderItems;

  function renderHero(it) {
    var pic = str(it.pic_url || it.pic || it.img || '');
    var ex = it.extra || {};
    var initial = esc(str(it.title).slice(0, 1));
    /* 没有封面（或封面是解不出来的加密图）时给占位块，避免详情页顶部一片空白 */
    var picHtml = '<div class="hero-pic">' + (pic
      ? '<img src="' + esc(pic) + '" referrerpolicy="no-referrer" onerror="this.parentNode.innerHTML=\'' + initial + '\'">'
      : '<span class="hero-ph">' + initial + '</span>') + '</div>';
    return '<div class="hero-card' + (ex.gradient ? ' grad' : '') + '">' + picHtml
      + '<div class="hero-info"><h2>' + esc(it.title) + '</h2>'
      + (it.desc ? '<p>' + esc(it.desc) + '</p>' : '')
      + '</div></div>';
  }

  function renderPosterGrid(group) {
    var cols = 3;
    var html = '<div class="grid" style="grid-template-columns:repeat(' + cols + ',1fr)">';
    for (var i = 0; i < group.length; i++) {
      var it = group[i] || {};
      var pic = str(it.pic_url || it.pic || it.img || '');
      /* 规则没写 col_type、只给了图 —— 合集里这类是**站点入口**，
       * 用方形卡（应用墙）更像样；有明确 col_type 的才是竖版海报。 */
      var inferred = !it.col_type;
      html += '<div class="poster' + (inferred ? ' sq' : '') + '" data-i="' + i + '" data-ref="' + esc(refId(it)) + '">'
        + '<div class="ph"><span class="ph-t">' + esc(str(it.title).slice(0, 2)) + '</span>' + (pic
          /* 首字垫在底层：图挂了就自己露出来，不至于剩一块纯灰 */
          ? '<img src="' + esc(pic) + '" referrerpolicy="no-referrer" onerror="this.style.display=\'none\'">'
          : '')
        + '</div>'
        + '<div class="pt"><b>' + esc(it.title) + '</b>'
        + (it.desc ? '<span>' + esc(it.desc) + '</span>' : '')
        + '</div></div>';
    }
    html += '</div>';
    return html;
  }

  function renderSingle(it, ct) {
    var title = str(it.title);
    var bg = (it.extra && it.extra.backgroundColor) ? ' style="background:' + esc(it.extra.backgroundColor) + '"' : '';
    switch (ct) {
      case 'line':
      case 'line_blank':
        return '<div class="hr"></div>';
      case 'text_1':
        return '<div class="sect-t">' + esc(stripTags(title)) + '</div>';
      case 'text_center_1':
        return '<div class="center-line" data-ref="' + esc(refId(it)) + '">' + esc(stripTags(title)) + '</div>';
      case 'text_2':
      case 'text_3':
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '"' + bg + '>' + esc(stripTags(title)) + '</button>';
      case 'scroll_button':
        return '<button class="chip" data-ref="' + esc(refId(it)) + '"' + bg + '>' + esc(stripTags(title)) + '</button>';
      case 'input':
        return '<div class="input-row"><input class="f" placeholder="' + esc(it.desc || '输入关键词搜索') + '" '
          + 'data-ref="' + esc(refId(it)) + '" value="' + esc(HK.page.keyword || '') + '">'
          + '<button class="btn go" data-ref="' + esc(refId(it)) + '">搜索</button></div>';
      case 'rich_text':
        return '<div class="rich" data-ref="' + esc(refId(it)) + '">' + title + '</div>';
      /* 有图无字的横幅卡（合集首页常见：站点大图 / 公告图）。
       * 以前掉进 default 分支，变成一个**没有文字的空按钮**，看着像加载失败。 */
      case 'card_pic_1':
      case 'card_pic_2':
      case 'banner':
        var bpic = str(it.pic_url || it.pic || it.img);
        if (!bpic) return '';
        return '<div class="banner-card" data-ref="' + esc(refId(it)) + '">'
          + '<img src="' + esc(bpic) + '" referrerpolicy="no-referrer" onerror="this.parentNode.style.display=\'none\'">'
          + '</div>';
      case 'icon_round_small_4':
      case 'icon_small_4':
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '">' + esc(stripTags(title)) + '</button>';
      default:
        var dt = stripTags(title).replace(/&nbsp;?/gi, ' ').trim();
        /* 空标题不渲染：否则页面上会留下一条点不动也不知道是什么的空按钮 */
        if (!dt) return '';
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '"' + bg + '>' + esc(dt) + '</button>';
    }
  }

  function stripTags(s) { return str(s).replace(/<[^>]+>/g, ''); }

  /* 条目在数组里的索引：用 title+url 的短哈希，避免把整条塞进 DOM */
  var REFS = [];
  function refId(it) {
    for (var i = 0; i < REFS.length; i++) if (REFS[i] === it) return 'r' + i;
    REFS.push(it);
    if (REFS.length > 3000) REFS = REFS.slice(-1500);
    return 'r' + (REFS.length - 1);
  }
  function byRef(id) {
    var m = /^r(\d+)$/.exec(str(id));
    if (!m) return null;
    return REFS[parseInt(m[1], 10)] || null;
  }
  UI.byRef = byRef;

  function showError(title, detail) {
    var box = $('content');
    if (!box) return;
    box.innerHTML = '<div class="empty"><span class="big">⚠️</span>' + esc(title)
      + '<br><span class="dim">' + esc(detail || '') + '</span>'
      + '<br><br><button class="btn gh" id="retryBtn">重试</button></div>';
    var b = $('retryBtn');
    if (b) b.onclick = function () { if (STATE.stack.length) renderItems(STATE.stack[STATE.stack.length - 1].items, STATE.stack[STATE.stack.length - 1].title); };
  }
  UI.showError = showError;

  /* ------------------------------------------------------------------ 点击分发 */
  function onContentClick(ev) {
    var t = ev.target;
    /* 输入框的搜索按钮 */
    var go = closest(t, '.btn.go');
    if (go) { doSearchByRef(go.getAttribute('data-ref')); return; }

    var holder = closest(t, '[data-ref]');
    if (!holder) return;
    var ref = holder.getAttribute('data-ref');
    var it = byRef(ref);
    if (!it) return;

    /* input 条目：点空白不做事，由按钮触发 */
    if (str(it.col_type) === 'input') return;
    handleItem(it);
  }

  function doSearchByRef(ref) {
    var holder = document.querySelector('[data-ref="' + ref + '"]');
    if (!holder) return;
    var inp = holder.parentNode ? holder.parentNode.querySelector('input') : null;
    if (!inp) inp = holder;
    var kw = inp.value || '';
    if (!kw.trim()) { toast('请输入关键词'); return; }
    doSearch(kw.trim());
  }

  function doSearch(kw) {
    if (!STATE.site) return;
    HK.setPage({ keyword: kw, url: 'hiker://search?s=' + encodeURIComponent(kw) });
    var rule = STATE.site.rules.search;
    if (!rule) { toast('这个站点没有搜索规则'); return; }
    runPage(rule, {
      MY_KEYWORD: kw, MY_URL: 'hiker://search?s=' + encodeURIComponent(kw),
      input: '', MY_PAGE: 1
    }, '搜索：' + kw);
  }
  UI.doSearch = doSearch;

  /* 点卡片时把 extra 变成下一页的参数表（海阔 MY_PARAMS 语义）。
   * 影探/大量影视规则都是 url='hiker://page/detail' + extra={id:'123'} 这种用法，
   * 少了这一步，详情规则里 MY_PARAMS.id 为空，页面只剩「缺少影片参数」。 */
  function paramsOf(it, url) {
    var p = {}, ex = it.extra;
    if (ex && typeof ex === 'object') {
      for (var k in ex) if (Object.prototype.hasOwnProperty.call(ex, k)) p[k] = ex[k];
    }
    var q = str(url).split('#')[0];
    var i = q.indexOf('?');
    if (i >= 0) {
      var segs = q.slice(i + 1).split('&');
      for (var j = 0; j < segs.length; j++) {
        if (!segs[j]) continue;
        var kv = segs[j].split('=');
        var kk = decodeURIComponent(kv[0]);
        if (p[kk] === undefined) {
          try { p[kk] = decodeURIComponent(kv.slice(1).join('=')); } catch (e) { p[kk] = kv.slice(1).join('='); }
        }
      }
    }
    if (it.title) p.title = it.title;
    return p;
  }
  UI.paramsOf = paramsOf;

  /* lazyRule / @rule= 的返回值要按**语义**分发，这是海阔最反直觉的一处：
   *   返回 "https://…mp4#isVideo=true#"  → 这是**播放地址**，直接进播放器；
   *   返回 "toast://xxx"                 → 弹提示；
   *   返回 "hiker://empty"               → 留在原页（规则自己 refreshPage 了）；
   *   返回 "hiker://page/xxx"            → 才叫跳页。
   * 之前一律当跳页处理，于是「点选集 → 又去解析详情 → 详情加载失败」。 */
  function dispatchReturned(v, title) {
    var sv = str(v).replace(/^\s+|\s+$/g, '');
    if (!sv) return false;
    if (sv.indexOf('toast://') === 0) {
      var msg = sv.slice(8);
      try { msg = decodeURIComponent(msg); } catch (e) {}
      toast(msg);
      return true;
    }
    if (sv.indexOf('hiker://') === 0) {
      if (sv === 'hiker://empty' || sv.indexOf('hiker://empty#') === 0) return false;
      navigate(sv);
      return true;
    }
    var videoLike = /#isVideo=true/i.test(sv) || /\.(m3u8|mp4|mkv|flv|ts|avi|mov|m4v)(\?|#|$)/i.test(sv);
    if (/^https?:/i.test(sv) && videoLike) {
      var clean = sv.replace(/#[^#]*#/g, '').replace(/#.*$/, '').trim();
      var ok = HK.play(clean, title, '');
      toast(ok ? '正在播放：' + title : '交给播放器失败');
      return true;
    }
    if (/^https?:/i.test(sv)) {
      if (STATE.site && STATE.site.rules.detail) { navigate(sv); return true; }
      openWeb(sv);
      return true;
    }
    return false;
  }
  UI.dispatchReturned = dispatchReturned;

  /* 「选择器」链接：hiker://empty#@hkselect=<id>#<选项JSON> */
  function openPicker(url) {
    var m = /#@hkselect=([^#]+)#([\s\S]*)$/.exec(str(url));
    if (!m) return false;
    var id = m[1], meta = {};
    try { meta = JSON.parse(decodeURIComponent(m[2])) || {}; } catch (e) { meta = {}; }
    var items = meta.items || [];
    var row = $('pickRow'), ttl = $('pickTitle');
    if (!row) return false;
    if (ttl) ttl.textContent = meta.title || '请选择';
    var html = '';
    for (var i = 0; i < items.length; i++) {
      var label = str(items[i] && items[i].title !== undefined ? items[i].title : items[i]);
      html += '<button class="chip" data-pick="' + i + '">' + esc(label) + '</button>';
    }
    row.innerHTML = html || '<span class="dim">没有可选项</span>';
    row.onclick = function (ev) {
      var b = closest(ev.target, '[data-pick]');
      if (!b) return;
      var idx = parseInt(b.getAttribute('data-pick'), 10);
      $('pickMask').classList.remove('show');
      var r = HK.pickSelect(id, idx);
      if (!r.ok) { toast('执行失败：' + r.err); return; }
      if (!dispatchReturned(r.value, '')) refreshCurrent();
    };
    $('pickMask').classList.add('show');
    return true;
  }
  UI.openPicker = openPicker;

  function handleItem(it) {
    var ex = it.extra || {};
    var url = str(it.url);
    /* 海阔的合集框架里，卡片的真实动作常放在 extra.url（item.url 只是个占位壳，
     * 比如 hiker://page/frameLoad）。但**反过来也成立**：当 item.url 本身就是
     * hiker://page/<模块> 时，那是真正的渲染入口，extra 是它的参数（MY_PARAMS）。
     * 以前一律让 extra.url 覆盖，合集卡片的 frameLoad 入口被换成了内联 JS 串，
     * 于是子站永远打不开。这里让 page 入口优先；找不到该模块时再退回 extra.url。 */
    if (!(HK.isPageUrl(url) && HK.findPage(str(url).replace(/^hiker:\/\/page\//, '').split('?')[0]))) {
      if (ex.url && typeof ex.url === 'string' && /(\.js:|lazyRule|@rule=)/.test(ex.url)) url = ex.url;
    }

    /* 记下本条的参数，navigate 时随页面带过去 */
    CUR_PARAMS = paramsOf(it, url);

    /* 选择器链接优先处理 */
    if (url.indexOf('@hkselect=') >= 0 && openPicker(url)) return;

    /* input 条目的 onChange */
    if (str(it.col_type) === 'input' && ex.onChange) {
      try { HK.runInline(str(ex.onChange), { input: HK.page.keyword || '' }); } catch (e) {}
      return;
    }

    if (!url || url === 'javascript:;') return;

    var pu = HK.parseUrl(url);

    /* 0) hiker://page/<模块> —— 加载 pages 模块渲染整页。
     * 合集框架的子站入口就是这条：卡片 url = hiker://page/frameLoad，
     * extra = { RULE: base64(子站规则), isIndex: true } 作为 MY_PARAMS 传进去，
     * 模块里 eval hijackEnv 换掉 MY_RULE，再 setResult 吐出子站首页。
     * 没有这条分支时，点击会被当成「跳详情」，子站一律打不开。 */
    if (HK.isPageUrl(url)) {
      var pgm = /^hiker:\/\/page\/([^?#]*)/.exec(url);
      var pgPath = pgm ? pgm[1] : '';
      setBusy(true, '加载子站…');
      setTimeout(function () {
        var pr = HK.runPageModule(pgPath, CUR_PARAMS || {});
        setBusy(false);
        if (!pr.ok) { toast('子站加载失败：' + pr.err); log(pr.err); return; }
        var its = normalizeResult(pr.result);
        if (!its.length) { toast('子站没有内容'); return; }
        var ttl = str(it.title) || '子站';
        STATE.stack.push({ title: ttl, items: its, url: HK.page.url });
        renderItems(its, ttl);
      }, 20);
      return;
    }

    /* 0-1) #fypage.js:<code> —— 执行代码取回真实地址，再继续分发。
     * 合集框架把「分页替换 + 拼真实 URL」的活交给这段内联 JS。 */
    if (pu.jsPage) {
      setBusy(true, '解析入口…');
      setTimeout(function () {
        HK.resetOut();
        var jr = HK.runJsPage(pu.jsPage, {
          input: str(it.input || ''),
          MY_URL: HK.page.url,
          MY_PARAMS: CUR_PARAMS || {},
          MY_PAGE: HK.page.page || 1
        });
        setBusy(false);
        if (!jr.ok) { toast('入口解析失败：' + jr.err); log(jr.err); return; }
        var next = str(jr.value);
        if (!next) { toast('没有取到子站地址'); return; }
        navigate(next);
      }, 20);
      return;
    }

    /* 0) 输入框链接：$(默认值, 提示).input(回调) */
    if (pu.inputArg && pu.lazy) {
      var arg = pu.inputArg;
      var val = window.prompt(str(arg.d || '请输入'), str(arg.v || ''));
      if (val === null) return;
      var inputCode = pu.lazy;
      setBusy(true, '处理中…');
      setTimeout(function () {
        HK.resetOut();
        var rr = HK.runInline(inputCode, { input: val, MY_URL: HK.page.url });
        setBusy(false);
        if (!rr.ok) { toast('执行失败：' + rr.err); return; }
        var its = HK.OUT.hasResult ? normalizeResult(HK.OUT.result) : null;
        if (its && its.length) { STATE.stack.push({ title: it.title, items: its, url: HK.page.url }); renderItems(its, it.title); }
        else refreshCurrent();
      }, 20);
      return;
    }

    /* 1) 内联规则：@lazyRule / @rule=js: */
    if (pu.lazy || pu.rule) {
      var code = pu.lazy || pu.rule;
      setBusy(true, '处理中…');
      setTimeout(function () {
        HK.resetOut();
        var r = HK.runInline(code, { input: '', MY_URL: HK.page.url, MY_PARAMS: CUR_PARAMS || {} });
        setBusy(false);
        if (!r.ok) { toast('执行失败：' + r.err); log(r.err); return; }
        var back = r.value;
        var items = HK.OUT.hasResult ? normalizeResult(HK.OUT.result) : null;
        if (items && items.length) { STATE.stack.push({ title: it.title, items: items, url: HK.page.url }); renderItems(items, it.title); return; }
        if (dispatchReturned(back, it.title)) return;
        /* 没有结果、也没给出可执行返回 → 按刷新当前页处理 */
        refreshCurrent();
      }, 20);
      return;
    }

    /* 2) 直接是视频地址 */
    if (pu.video || /\.(m3u8|mp4|flv|mkv)(\?|$)/i.test(url)) {
      PLAY_TARGET = it;
      var ok = HK.play(url, it.title, it.pic_url || '');
      toast(ok ? '正在播放：' + it.title : '交给播放器失败');
      return;
    }

    /* 3) 搜索协议 */
    if (pu.search !== null && pu.search !== undefined && pu.search !== '') { doSearch(pu.search); return; }

    /* 4) 站内跳转（hiker://page/…、hiker://empty?…）→ 重新跑详情/列表规则 */
    if (/^hiker:\/\//.test(url)) { navigate(url); return; }

    /* 5) 普通网页 */
    if (/^https?:\/\//.test(url)) {
      if (STATE.site && STATE.site.rules.detail) { navigate(url); return; }
      openWeb(url);
      return;
    }
  }
  UI.handleItem = handleItem;

  var PLAY_TARGET = null;

  /* 站内跳转：把 URL 交给「详情或二级列表」规则 */
  var CUR_PARAMS = {};

  function navigate(url) {
    if (!STATE.site) return;
    var params = CUR_PARAMS || {};
    HK.setPage({ url: url, params: params,
      title: PLAY_TARGET ? PLAY_TARGET.title : HK.page.title,
      pic: PLAY_TARGET ? PLAY_TARGET.pic_url : '' });
    var code = STATE.site.rules.detail || STATE.site.rules.sdetail || STATE.site.rules.home;
    runPage(code, { MY_URL: url, MY_TITLE: HK.page.title, MY_PIC: HK.page.pic,
                    MY_PARAMS: params, input: url, MY_PAGE: 1 },
      PLAY_TARGET ? PLAY_TARGET.title : '详情');
  }

  function refreshCurrent() {
    if (!STATE.site) return;
    HK.setPage({ url: STATE.site.homePage });
    runPage(STATE.site.rules.home, { MY_URL: STATE.site.homePage, MY_PAGE: 1, input: '', MY_KEYWORD: '' }, '首页');
  }

  function back() {
    if (STATE.stack.length > 1) {
      STATE.stack.pop();
      var top = STATE.stack[STATE.stack.length - 1];
      renderItems(top.items, top.title);
    } else {
      showView('sites');
    }
  }
  UI.back = back;

  function openWeb(url) {
    var w = window.open(url, '_blank');
    if (!w) toast('打不开外链：' + url);
  }

  function closest(el, sel) {
    while (el && el !== document) {
      if (el.matches && el.matches(sel)) return el;
      el = el.parentNode;
    }
    return null;
  }

  /* ------------------------------------------------------------------ 视图 */
  function showView(v) {
    var a = $('viewSites'), b = $('viewRule');
    if (!a || !b) return;
    a.style.display = (v === 'sites') ? 'block' : 'none';
    b.style.display = (v === 'rule') ? 'block' : 'none';
    $('backBtn').style.display = (v === 'rule') ? 'flex' : 'none';
  }

  /* ------------------------------------------------------------------ 启动 */
  function boot() {
    loadSites();
    renderSites();

    /* 顶栏在滚动后加一层投影，和正文拉开层次（App 里 TopAppBar 的同款行为） */
    window.addEventListener('scroll', function () {
      var h = document.querySelector('header');
      if (!h) return;
      if (window.scrollY > 4) h.classList.add('scrolled');
      else h.classList.remove('scrolled');
    }, { passive: true });

    /* 站点卡片 */
    $('siteList').addEventListener('click', function (ev) {
      var del = closest(ev.target, '[data-del]');
      if (del) {
        var di = parseInt(del.getAttribute('data-del'), 10);
        STATE.sites.splice(di, 1); saveSites(); renderSites();
        return;
      }
      var card = closest(ev.target, '.card.site');
      if (card) { openSite(parseInt(card.getAttribute('data-idx'), 10)); }
    });

    /* 顶部操作（逐个判空：任何一个按钮缺失都不能中断后面的绑定） */
    var on = function (id, fn) { var el = $(id); if (el) el.onclick = fn; };
    on('importBtn', openImport);
    on('importBtn2', openImport);
    on('importCancel', closeImport);
    on('importOk', doImport);
    on('backBtn', back);
    /* 模式条：一行按钮，点哪个切哪个 */
    if ($('modeRow')) {
      $('modeRow').addEventListener('click', function (ev) {
        var b = closest(ev.target, '.mbtn');
        if (b) setMode(b.getAttribute('data-mode'));
      });
    }
    on('fileBtn', function () { setMode('file'); doImport(); });
    on('pickCancel', function () { var m = $('pickMask'); if (m) m.classList.remove('show'); });
    /* 剪贴板：优先走原生桥（WebView 在 file:// 下 navigator.clipboard 基本不可用），
       拿不到再退回浏览器接口。 */
    on('clipBtn', function () {
      if (global.HikerBridge && typeof global.HikerBridge.clipboardText === 'function') {
        var t = '';
        try { t = str(global.HikerBridge.clipboardText()); } catch (e) { t = ''; }
        if (t && t.trim()) { setMode('clip'); if ($('importText')) $('importText').value = t; runImport(t, {}); return; }
      }
      if (navigator.clipboard && navigator.clipboard.readText) {
        navigator.clipboard.readText().then(function (t) {
          if ($('importText')) $('importText').value = t;
          if (t && t.trim()) runImport(t, {});
        }, function () { toast('读剪贴板失败，请手动粘贴'); });
      } else { toast('请手动粘贴口令'); }
    });
    $('content').addEventListener('click', onContentClick);

    /* 规则事件 → 界面 */
    var oldEmit = HK.emit;
    HK.emit = function (name, payload) {
      try { oldEmit(name, payload); } catch (e) {}
      if (name === 'hk.toast') toast(payload);
      else if (name === 'hk.error') log('ERROR ' + payload);
      else if (name === 'hk.log') log(str(payload));
      else if (name === 'hk.copy') { try { navigator.clipboard.writeText(str(payload)); } catch (e) {} toast('已复制'); }
      else if (name === 'hk.refresh') {
        /* 规则里 refreshPage() 的语义就是「重跑当前页规则」（分页按钮、分类按钮都靠它） */
        if (STATE.lastRun && !STATE.refreshing) {
          STATE.refreshing = true;
          setTimeout(function () {
            var lr = STATE.lastRun;
            STATE.refreshing = false;
            if (lr) runPage(lr.code, lr.vars, lr.title);
          }, 16);
        }
      }
      else if (name === 'hk.updateItem') {
        /* updateItem(id, patch)：条目在原地改（展开/收起简介、切线路选中态都靠它），不换页 */
        var args = payload;
        if (Object.prototype.toString.call(args) === '[object Array]' && STATE.stack.length) {
          var id = str(args[0]), patch = args[1] || {};
          var cur = HK.STORE.items[id];
          if (cur) { for (var kk in patch) if (Object.prototype.hasOwnProperty.call(patch, kk)) cur[kk] = patch[kk]; }
          var top = STATE.stack[STATE.stack.length - 1];
          renderItems(top.items, top.title);
        }
      }
      else if (name === 'hk.back') { back(); }
    };

    showView('sites');
  }

  UI.boot = boot;
  global.HikerUI = UI;
})(typeof window !== 'undefined' ? window : this);
