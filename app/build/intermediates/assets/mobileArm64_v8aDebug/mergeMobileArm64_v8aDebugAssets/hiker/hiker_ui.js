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
    busy: false,
    /* [A-P0-10] 分页上下文：{code, baseVars, page, title}，由 runPage 的
     * pagerMode 建立（home/search/detail），「加载下一页」按它递增 MY_PAGE */
    pager: null,
    pagerCls: ''
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
      html += '<div class="card site" data-idx="' + i + '">'
        + '<div class="site-ic">'
        + esc(str(s.name).slice(0, 1)) + '</div>'
        + '<div class="site-nm">' + esc(s.name) + '</div>'
        + '<div class="site-mt">'
        + '<span class="pill">' + esc(s.type || 'video') + '</span>'
        + (s.group ? '<span class="pill">' + esc(s.group) + '</span>' : '')
        + '<span class="pill">' + (s.pages && s.pages.length ? s.pages.length + ' 模块' : '单页') + '</span>'
        + '</div>'
        + '<button class="del" data-del="' + i + '">移除</button>'
        + '</div>';
    }
    box.innerHTML = html;
  }

  function pickColor(i) {
    var c = ['#5db4ff', '#4fd39a', '#ffb45e', '#c58bff', '#ff8fa3', '#6fe0d0'];
    return c[i % c.length];
  }
  function esc(s) {
    return str(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  UI.esc = esc;

  /* ------------------------------------------------------------------ 导入 */
  function openImport() {
    $('importMask').classList.add('show');
    var ta = $('importText');
    ta.value = '';
    ta.focus();
  }
  function closeImport() { $('importMask').classList.remove('show'); }

  /* 站点数组入库（去重覆盖同 key），导入各路共用 */
  function addSites(sites) {
    for (var i = 0; i < sites.length; i++) {
      var s = sites[i], dup = -1;
      for (var j = 0; j < STATE.sites.length; j++) {
        if (STATE.sites[j].key === s.key) { dup = j; break; }
      }
      if (dup >= 0) STATE.sites[dup] = s; else STATE.sites.push(s);
    }
    saveSites();
    renderSites();
  }
  UI.addSites = addSites;

  function doImport() {
    var text = $('importText').value;
    if (!text || !text.trim()) { toast('先把口令粘进来'); return; }
    doImportText(text);
  }

  /* [A-输入] 文本导入：口令/分享口令/裸 JSON/订阅地址，密码框的值透传给
   * importText 的 options.password（原来恒传 {}，带密码口令永远进不来） */
  function doImportText(text) {
    var pwdEl = $('importPwd');
    var pwd = pwdEl ? pwdEl.value : '';
    setBusy(true, '正在取回规则…');
    /* 云口令要联网，放一小会儿让 loading 先渲染出来 */
    setTimeout(function () {
      var r;
      try { r = IMP.importText(text, { password: pwd || '' }); }
      catch (e) { setBusy(false); toast('解析出错：' + (e && e.message)); return; }
      setBusy(false);
      if (!r.ok) {
        toast(r.notes && r.notes[0] ? r.notes[0] : '没有解析到站点');
        log(r.notes);
        return;
      }
      addSites(r.sites);
      closeImport();
      toast('导入成功：' + r.sites.length + ' 个站点');
      log(r.notes);
    }, 30);
  }
  UI.doImportText = doImportText;

  /* [A-输入] 文件导入：.json/.txt 走文本导入；.zip/.hkzip 读为 base64 调
   * bridge.call('unzipRule')（Java 侧解包 rule.json 并把 require.json/
   * libs.zip/data.zip 落盘），返回的 rules 逐条 buildSites 入库，
   * stored 文件名提示给用户。 */
  function bufToB64(buf) {
    var bytes = new Uint8Array(buf), bin = '', CH = 0x8000, i;
    for (i = 0; i < bytes.length; i += CH) {
      bin += String.fromCharCode.apply(null, bytes.subarray(i, i + CH));
    }
    return btoa(bin);
  }
  function doImportFile(file) {
    if (!file) return;
    var name = str(file.name || '');
    var isZip = /\.(zip|hkzip)$/i.test(name);
    var reader = new FileReader();
    setBusy(true, '正在读文件…');
    reader.onerror = function () { setBusy(false); toast('文件读取失败'); };
    if (!isZip) {
      reader.onload = function () { setBusy(false); doImportText(str(reader.result || '')); };
      reader.readAsText(file);
      return;
    }
    reader.onload = function () {
      setBusy(false);
      var bridge = global.HikerBridge;
      if (!bridge || typeof bridge.call !== 'function') {
        toast('当前环境无原生桥，不支持 zip 导入');
        return;
      }
      var r;
      try { r = JSON.parse(bridge.call('unzipRule', JSON.stringify([bufToB64(reader.result)]))); }
      catch (e) { toast('解包失败：' + (e && e.message)); return; }
      if (!r || !r.ok) { toast('解包失败：' + ((r && r.error) || '包内没有 rule.json')); return; }
      var all = [];
      for (var i = 0; i < (r.rules || []).length; i++) {
        var ss = IMP.buildSites(r.rules[i], { source: '文件导入' });
        for (var j = 0; j < ss.length; j++) all.push(ss[j]);
      }
      if (!all.length) { toast('包内规则没有解析到站点'); return; }
      addSites(all);
      closeImport();
      toast('文件导入成功：' + all.length + ' 个站点'
        + (r.stored && r.stored.length ? '（已存文件：' + r.stored.join('、') + '）' : ''));
    };
    reader.readAsArrayBuffer(file);
  }
  UI.doImportFile = doImportFile;

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
    /* [A] 进站重置：选择登记表（旧回调指向已离开页面的作用域）与分页上下文 */
    global.__HK_SELECTS__ = {};
    STATE.pager = null;
    var rule = s.raw;
    if (typeof rule.pages === 'string') { try { rule.pages = JSON.parse(rule.pages); } catch (e) { rule.pages = []; } }
    HK.setPage({ rule: rule, ruleName: s.name, url: s.homePage, keyword: '', page: 1, title: s.name, pic: '', input: '' });
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
    runPage(s.rules.home, { MY_URL: s.homePage, MY_PAGE: 1, input: '', MY_KEYWORD: '' }, s.name, { pager: 'home' });
  }
  UI.openSite = openSite;

  /* 执行一个页面规则并把结果推进页面栈。
   * opts.pager（'home'/'search'/'detail'）给出时记录分页上下文：结果不少于
   * 一屏时列表底部出「加载下一页」，MY_PAGE 真实递增、追加不覆盖 [A-P0-10] */
  function runPage(code, vars, title, opts) {
    if (!code) { toast('这个站点没有配置该页规则'); return; }
    opts = opts || {};
    STATE.lastRun = { code: code, vars: vars || {}, title: title || '' };
    setBusy(true, title || '加载中…');
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
      if (opts.pager) {
        STATE.pager = {
          mode: opts.pager, code: code,
          baseVars: JSON.parse(JSON.stringify(vars || {})),
          page: (vars && vars.MY_PAGE) || 1,
          title: title || ''
        };
      }
      STATE.stack.push({ title: title || '', items: items, url: HK.page.url });
      renderItems(items, title, !!(opts.pager && items.length >= 4));
    }, 30);
  }
  UI.runPage = runPage;

  /* [A-P0-10] 加载下一页：同一条规则以 MY_PAGE+1 重跑，结果追加到当前页 */
  function loadNextPage() {
    var pg = STATE.pager;
    if (!pg || STATE.busy) return;
    var next = (pg.page || 1) + 1;
    var vars = JSON.parse(JSON.stringify(pg.baseVars || {}));
    vars.MY_PAGE = next;
    STATE.lastRun = { code: pg.code, vars: vars, title: pg.title };
    setBusy(true, '加载第 ' + next + ' 页…');
    setTimeout(function () {
      HK.resetOut();
      var r = HK.run(pg.code, vars);
      setBusy(false);
      if (!r.ok) { toast('下一页加载失败：' + r.err); return; }
      var items = normalizeResult(r.result);
      if (!items.length) { toast('已经到底了'); STATE.pager = null; renderMoreOnly(false); return; }
      var top = STATE.stack[STATE.stack.length - 1];
      if (top) top.items = top.items.concat(items);
      pg.page = next;
      var box = $('content');
      var st = box ? box.scrollTop : 0;
      renderItems(top ? top.items : items, pg.title, items.length >= 4);
      if (box) box.scrollTop = st;
      toast('已加载第 ' + next + ' 页');
    }, 30);
  }
  UI.loadNextPage = loadNextPage;

  /* 只刷新底部的「加载下一页」按钮（到底时去掉） */
  function renderMoreOnly(canMore) {
    var old = $('moreBtn');
    if (old && old.parentNode) old.parentNode.removeChild(old);
    if (canMore) {
      var box = $('content');
      if (box) {
        var b = document.createElement('button');
        b.className = 'btn more-btn';
        b.id = 'moreBtn';
        b.textContent = '加载下一页';
        box.appendChild(b);
      }
    }
  }

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

  /* [A-P1-1] col_type 全映射（语料 37 种 + 官方 60 常量）：
   * POSTER_COLS —— 海报/图标网格及列数；HGROUP —— 单行横滑按钮组；
   * 其余走 renderSingle，未知类型一律降级为带图行/行按钮，绝不空白。 */
  var POSTER_COLS = {
    'movie_1': 1, 'pic_1': 1, 'pic_1_card': 1, 'pic_1_center': 1, 'pic_1_full': 1, 'card_pic_1': 1,
    'movie_2': 2, 'pic_2': 2, 'pic_2_card': 2, 'card_pic_2': 2, 'card_pic_2_2': 2, 'card_pic_2_2_left': 2,
    'icon_2': 2, 'icon_2_round': 2,
    'movie_3': 3, 'pic_3': 3, 'pic_3_square': 3, 'card_pic_3': 3, 'card_pic_3_center': 3,
    'movie_1_vertical_pic': 3, 'icon_3_fill': 3, 'icon_3_round_fill': 3,
    'icon_small_4': 4, 'icon_round_small_4': 4, 'icon_4': 4, 'icon_4_card': 4, 'icon_round_4': 4,
    'icon_small_3': 3,
    'icon_5': 5, 'icon_5_no_crop': 5
  };
  var HGROUP = { 'scroll_button': 1, 'flex_button': 1, 'text_4': 1, 'text_5': 1 };

  function renderItems(items, title, canMore) {
    var box = $('content');
    if (!box) return;
    /* 记住条目，供 updateItem/findItem 用 */
    for (var i = 0; i < items.length; i++) {
      if (items[i] && items[i].extra && items[i].extra.id) HK.STORE.items[str(items[i].extra.id)] = items[i];
    }
    var html = '', i = 0;
    while (i < items.length) {
      var it = items[i] || {};
      var ct = ctOf(it);
      if (ct === 'movie_1_vertical_pic_blur') { html += renderHero(it); i++; continue; }
      if (POSTER_COLS[ct]) {
        var cols = POSTER_COLS[ct], group = [];
        while (i < items.length && POSTER_COLS[ctOf(items[i])] === cols) { group.push(items[i]); i++; }
        html += renderPosterGrid(group, cols);
        continue;
      }
      if (HGROUP[ct]) {
        var hg = [];
        while (i < items.length && HGROUP[ctOf(items[i])]) { hg.push(items[i]); i++; }
        html += renderHGroup(hg);
        continue;
      }
      html += renderSingle(it, ct);
      i++;
    }
    /* [A-P0-10] 分页：底部「加载下一页」（点击走 onContentClick 委托） */
    if (canMore) html += '<button class="btn more-btn" id="moreBtn">加载下一页</button>';
    box.innerHTML = html || '<div class="empty">没有内容</div>';
    box.scrollTop = 0;
    $('ruleTitle').textContent = title || (STATE.site ? STATE.site.name : '');
    $('backBtn').style.display = 'flex';
  }
  UI.renderItems = renderItems;

  /* [A-UI] 按钮组单行横滑：一行排布、横向滚动查看全部操作，不再多行折行 */
  function renderHGroup(group) {
    var html = '<div class="hscroll">';
    for (var i = 0; i < group.length; i++) {
      var it = group[i] || {};
      var bg = (it.extra && it.extra.backgroundColor) ? ' style="background:' + esc(it.extra.backgroundColor) + '"' : '';
      var pic = str(it.pic_url || it.pic || it.img || '');
      html += '<button class="chip" data-ref="' + esc(refId(it)) + '"' + bg + '>'
        + (pic ? '<img class="chip-ic" src="' + esc(pic) + '" referrerpolicy="no-referrer" onerror="this.style.display=\'none\'">' : '')
        + esc(stripTags(it.title)) + '</button>';
    }
    return html + '</div>';
  }

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

  function renderPosterGrid(group, cols) {
    cols = cols || 3;
    var square = false, g0 = group[0] || {};
    if (/square/.test(ctOf(g0))) square = true;
    var html = '<div class="grid" style="grid-template-columns:repeat(' + cols + ',1fr)">';
    for (var i = 0; i < group.length; i++) {
      var it = group[i] || {};
      var pic = str(it.pic_url || it.pic || it.img || '');
      html += '<div class="poster" data-i="' + i + '" data-ref="' + esc(refId(it)) + '">'
        + '<div class="ph' + (square ? ' sq' : '') + '">' + (pic
          ? '<img src="' + esc(pic) + '" referrerpolicy="no-referrer" onerror="this.style.display=\'none\'">'
          : esc(str(it.title).slice(0, 4)))
        + '</div>'
        + '<div class="pt"><b>' + esc(it.title) + '</b>'
        + (it.desc ? '<span>' + esc(it.desc) + '</span>' : '')
        + '</div></div>';
    }
    html += '</div>';
    return html;
  }

  /* 左图右文行（movie_1_left_pic / icon_1_left_pic / avatar） */
  function renderMediaRow(it, round) {
    var pic = str(it.pic_url || it.pic || it.img || '');
    return '<div class="media-row" data-ref="' + esc(refId(it)) + '">'
      + '<div class="mr-pic' + (round ? ' round' : '') + '">'
      + (pic ? '<img src="' + esc(pic) + '" referrerpolicy="no-referrer" onerror="this.style.display=\'none\'">'
             : esc(str(it.title).slice(0, 1)))
      + '</div><div class="mr-tx"><b>' + esc(stripTags(it.title)) + '</b>'
      + (it.desc ? '<span>' + esc(it.desc) + '</span>' : '')
      + '</div></div>';
  }

  function renderSingle(it, ct) {
    var title = str(it.title);
    var bg = (it.extra && it.extra.backgroundColor) ? ' style="background:' + esc(it.extra.backgroundColor) + '"' : '';
    var pic = str(it.pic_url || it.pic || it.img || '');
    switch (ct) {
      case 'line':
      case 'line_blank':
        return '<div class="hr"></div>';
      /* [A-P1-1] 间隔块：按官方三档高度留白，不再当未知类型渲染成按钮 */
      case 'blank_block':
        return '<div class="spacer s1"></div>';
      case 'big_blank_block':
        return '<div class="spacer s2"></div>';
      case 'big_big_blank_block':
        return '<div class="spacer s3"></div>';
      case 'header':
        return '<div class="sect-t">' + esc(stripTags(title)) + '</div>';
      case 'footer':
        return '<div class="center-line dim">' + esc(stripTags(title)) + '</div>';
      case 'text_1':
        return '<div class="sect-t">' + esc(stripTags(title)) + '</div>';
      case 'text_center_1':
        return '<div class="center-line" data-ref="' + esc(refId(it)) + '">' + esc(stripTags(title)) + '</div>';
      case 'text_2':
      case 'text_3':
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '"' + bg + '>' + esc(stripTags(title)) + '</button>';
      case 'text_icon':
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '"' + bg + '>'
          + (pic ? '<img class="row-ic" src="' + esc(pic) + '" referrerpolicy="no-referrer" onerror="this.style.display=\'none\'">' : '')
          + esc(stripTags(title)) + '</button>';
      case 'movie_1_left_pic':
      case 'icon_1_left_pic':
        return renderMediaRow(it, false);
      case 'avatar':
        return renderMediaRow(it, true);
      case 'movie_3_marquee':
        return '<div class="marquee" data-ref="' + esc(refId(it)) + '"><span>' + esc(stripTags(title)) + '</span></div>';
      case 'input':
        return '<div class="input-row"><input class="f" placeholder="' + esc(it.desc || '输入关键词搜索') + '" '
          + 'data-ref="' + esc(refId(it)) + '" value="' + esc(HK.page.keyword || '') + '">'
          + '<button class="btn go" data-ref="' + esc(refId(it)) + '">搜索</button></div>';
      case 'icon_1_search':
        return '<div class="input-row"><input class="f" placeholder="' + esc(it.desc || stripTags(title) || '搜索') + '" '
          + 'data-ref="' + esc(refId(it)) + '" value="' + esc(HK.page.keyword || '') + '">'
          + '<button class="btn go" data-ref="' + esc(refId(it)) + '">搜索</button></div>';
      case 'rich_text':
        return '<div class="rich" data-ref="' + esc(refId(it)) + '">' + title + '</div>';
      case 'long_text':
        return '<div class="rich plain" data-ref="' + esc(refId(it)) + '">' + esc(title) + '</div>';
      case 'video':
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '"' + bg + '>▶ ' + esc(stripTags(title)) + '</button>';
      case 'x5_webview_single':
        /* 内嵌网页在 WebView 壳里退化为「打开网页」行，点了走 openWeb，不空白 */
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '">🌐 ' + esc(stripTags(title) || '打开网页内容') + '</button>';
      default:
        /* 未知类型降级：带图走左图行、无图走行按钮——绝不空白 [A-P1-1] */
        if (pic) return renderMediaRow(it, false);
        return '<button class="row-btn" data-ref="' + esc(refId(it)) + '"' + bg + '>' + esc(stripTags(title)) + '</button>';
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
    /* [A-P0-10] 分页按钮 */
    if (closest(t, '.more-btn')) { loadNextPage(); return; }
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
    HK.setPage({ keyword: kw, url: 'hiker://search?s=' + encodeURIComponent(kw), input: kw });
    var rule = STATE.site.rules.search;
    if (!rule) { toast('这个站点没有搜索规则'); return; }
    /* [A-P0-13] input 传真实关键词（原来恒传 ''，靠 input 取词的搜索规则全灭） */
    runPage(rule, {
      MY_KEYWORD: kw, MY_URL: 'hiker://search?s=' + encodeURIComponent(kw),
      input: kw, MY_PAGE: 1
    }, '搜索：' + kw, { pager: 'search' });
  }
  UI.doSearch = doSearch;

  /* [A-P0-6] 动作 scheme 分发：规则把 toast://、copy://、video://、web:// 等
   * 当条目 url 用（语料 toast:// 307 次），点它们不是进详情页，而是执行动作。
   * 返回 true 表示已处理。 */
  function dispatchAction(url, it) {
    var u = str(url), m;
    if ((m = /^toast:\/\/(.*)$/i.exec(u))) { toast(decodeSoft(m[1])); return true; }
    if ((m = /^copy:\/\/(.*)$/i.exec(u))) {
      var cp = decodeSoft(m[1]);
      try { if (navigator.clipboard) navigator.clipboard.writeText(cp); } catch (e) {}
      var wb = global.HikerBridge;
      if (wb && typeof wb.writeClipboard === 'function') { try { wb.writeClipboard(cp); } catch (e) {} }
      toast('已复制');
      return true;
    }
    if ((m = /^video:\/\/(.*)$/i.exec(u))) {
      PLAY_TARGET = it;
      var ok = HK.play(decodeSoft(m[1]), it.title || '', it.pic_url || it.pic || '');
      toast(ok ? '正在播放：' + (it.title || '') : '交给播放器失败');
      return true;
    }
    if ((m = /^(?:web|x5webview|webview):\/\/(.*)$/i.exec(u))) { openWeb(/^https?:/i.test(m[1]) ? m[1] : 'https://' + m[1]); return true; }
    if ((m = /^download:\/\/(.*)$/i.exec(u))) {
      var dl = decodeSoft(m[1]);
      var r = HK.bridgeCall('downloadFile', [dl]);
      toast(r ? '已开始下载' : '下载通道不可用');
      return true;
    }
    if (/^(magnet:|thunder:|ed2k:)/i.test(u)) {
      PLAY_TARGET = it;
      HK.play(u, it.title || '', it.pic_url || '');
      return true;
    }
    if (/^(none:\/\/|#|javascript:)/i.test(u)) return true;
    if (/^tel:/i.test(u)) { openWeb(u); return true; }
    return false;
  }
  UI.dispatchAction = dispatchAction;

  function decodeSoft(s) {
    var t = str(s);
    try { return decodeURIComponent(t); } catch (e) { return t; }
  }

  /* [A-P0-8] 页面内选择弹层：$().select 登记的选项在这里真选，
   * 选中后把回调以「选中值」为参数在当前页上下文里执行（支持 setResult 换页） */
  function openSelectSheet(entry, it) {
    var mask = $('selectMask'), list = $('selectList'), ttl = $('selectTitle');
    if (!mask || !list) { /* 无弹层 markup 时退回 prompt 真选择 */
      var v = HK.pickOption(entry.options, entry.def, entry.title);
      if (v !== null) execSelectCallback(entry, v, it);
      return;
    }
    ttl.textContent = entry.title || '请选择';
    list.innerHTML = '';
    (entry.options || []).forEach(function (o, idx) {
      var label = (o && typeof o === 'object') ? str(o.title || o.name || o.value || '') : str(o);
      var value = (o && typeof o === 'object' && o.value !== undefined) ? o.value : o;
      var b = document.createElement('button');
      b.className = 'chip sel-opt' + (idx === (entry.def || 0) ? ' on' : '');
      b.textContent = label;
      b.onclick = function () {
        closeSelectSheet();
        execSelectCallback(entry, value, it);
      };
      list.appendChild(b);
    });
    mask.classList.add('show');
  }
  function closeSelectSheet() {
    var mask = $('selectMask');
    if (mask) mask.classList.remove('show');
  }
  UI.closeSelectSheet = closeSelectSheet;

  function execSelectCallback(entry, value, it) {
    setBusy(true, '处理中…');
    setTimeout(function () {
      HK.resetOut();
      var code = 'js:return (' + entry.fn + ')(' + JSON.stringify(value === undefined ? '' : value) + ');';
      var r = HK.runInline(code, { input: HK.page.url, MY_URL: HK.page.url });
      setBusy(false);
      if (!r.ok) { toast('执行失败：' + r.err); log(r.err); return; }
      var items = HK.OUT.hasResult ? normalizeResult(HK.OUT.result) : null;
      if (items && items.length) {
        STATE.stack.push({ title: (it && it.title) || '', items: items, url: HK.page.url });
        renderItems(items, (it && it.title) || '');
        return;
      }
      if (typeof r.value === 'string' && r.value && r.value !== 'hiker://empty') { navigate(r.value); return; }
      var top = STATE.stack[STATE.stack.length - 1];
      if (top) renderItems(top.items, top.title);
    }, 20);
  }

  function handleItem(it) {
    var ex = it.extra || {};
    var url = str(it.url);
    /* 海阔的合集框架里，卡片的真实动作常放在 extra.url（item.url 只是个占位壳，
     * 比如 hiker://page/frameLoad）。这里优先用带执行语义的 extra.url。 */
    if (ex.url && typeof ex.url === 'string' && /(\.js:|lazyRule|@rule=)/.test(ex.url)) url = ex.url;

    /* input 条目的 onChange */
    if (str(it.col_type) === 'input' && ex.onChange) {
      try { HK.runInline(str(ex.onChange), { input: HK.page.keyword || '' }); } catch (e) {}
      return;
    }

    if (!url || url === 'javascript:;') return;

    /* [A-P0-6] 动作 scheme 先行分发（toast:// 等不再当详情 URL 打开） */
    if (dispatchAction(url, it)) return;

    var pu = HK.parseUrl(url);

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
      /* [A-P0-8] $().select 的链接先弹页面内选择层，真选择后再执行回调 */
      var selM = /__hkSelectExec\(["']?(s\d+)["']?\)/.exec(code);
      if (selM && global.__HK_SELECTS__ && global.__HK_SELECTS__[selM[1]]) {
        openSelectSheet(global.__HK_SELECTS__[selM[1]], it);
        return;
      }
      setBusy(true, '处理中…');
      setTimeout(function () {
        HK.resetOut();
        /* [A-P0-12] @rule= 链路：先抓目标页正文存进 resCode，规则里
         * getResCode() 才能拿到（海阔的预取正文通道） */
        if (pu.rule && pu.base && /^https?:\/\//.test(pu.base)) {
          try { HK.run('js:setResCode(fetch(MY_URL))', { MY_URL: pu.base }); HK.resetOut(); } catch (e) {}
        }
        var r = HK.runInline(code, { input: HK.page.url, MY_URL: HK.page.url });
        setBusy(false);
        if (!r.ok) { toast('执行失败：' + r.err); log(r.err); return; }
        var back = r.value;
        var items = HK.OUT.hasResult ? normalizeResult(HK.OUT.result) : null;
        if (items && items.length) { STATE.stack.push({ title: it.title, items: items, url: HK.page.url }); renderItems(items, it.title); return; }
        /* 没有结果就当作刷新当前页 */
        if (typeof back === 'string' && back && back !== 'hiker://empty' && back.indexOf('hiker://empty#') !== 0) {
          navigate(back); return;
        }
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
  function navigate(url) {
    if (!STATE.site) return;
    /* [A-P0-6] 跳转目标也可能是动作 scheme（lazy 返回值等路径） */
    if (dispatchAction(url, PLAY_TARGET || {})) return;
    HK.setPage({ url: url, title: PLAY_TARGET ? PLAY_TARGET.title : HK.page.title, pic: PLAY_TARGET ? PLAY_TARGET.pic_url : '', input: url });
    var code = STATE.site.rules.detail || STATE.site.rules.sdetail || STATE.site.rules.home;
    runPage(code, { MY_URL: url, MY_TITLE: HK.page.title, MY_PIC: HK.page.pic, input: url, MY_PAGE: 1 },
      PLAY_TARGET ? PLAY_TARGET.title : '详情', { pager: 'detail' });
  }

  function refreshCurrent() {
    if (!STATE.site) return;
    HK.setPage({ url: STATE.site.homePage });
    runPage(STATE.site.rules.home, { MY_URL: STATE.site.homePage, MY_PAGE: 1, input: '', MY_KEYWORD: '' }, '首页', { pager: 'home' });
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

  /* [A-输入] 剪贴板自动识别：桥 readClipboard 优先，无桥用 navigator.clipboard
   * 兜底；命中云口令/分享口令且未处理过（localStorage 记 hash）时自动打开
   * 导入弹层并预填——只预填，等用户点导入，不自动入库。 */
  var CLIP_SEEN_KEY = 'hk_clip_seen_v1';
  function clipHash(s) {
    var h = 0;
    for (var i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
    return 'h' + h;
  }
  function checkClipboardAuto() {
    var mask = $('importMask');
    if (mask && mask.classList.contains('show')) return;
    var done = function (t) {
      t = str(t || '');
      if (!t) return;
      var hit = /云\d+[a-z]+\/[^\s]{4,}/.test(t) || t.indexOf('海阔视界') >= 0 || /小程序￥home_rule/.test(t);
      if (!hit) return;
      var h = clipHash(t);
      try {
        if (localStorage.getItem(CLIP_SEEN_KEY) === h) return;
        localStorage.setItem(CLIP_SEEN_KEY, h);
      } catch (e) {}
      openImport();
      $('importText').value = t;
      toast('检测到剪贴板口令，已填入导入框，确认后点导入');
    };
    var bridge = global.HikerBridge;
    if (bridge && typeof bridge.readClipboard === 'function') {
      try { done(bridge.readClipboard()); return; } catch (e) {}
    }
    if (navigator.clipboard && navigator.clipboard.readText) {
      navigator.clipboard.readText().then(done, function () {});
    }
  }
  UI.checkClipboardAuto = checkClipboardAuto;

  /* ------------------------------------------------------------------ 启动 */
  function boot() {
    /* [A-P0-2] 全局补齐：api_surface 按契约填缺口 + engine 的 CryptoJS/
     * getCryptoJS 真实现 + scheme 分发包装（加载链已保证这些文件先于 engine
     * 就位；installGlobals 内部 delegate 优先，不覆盖真实现） */
    if (HK.installGlobals) { try { HK.installGlobals(); } catch (e) {} }
    loadSites();
    renderSites();

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
    /* [A-输入] 文件导入：按钮触发隐藏 file input，change 后按扩展名分发 */
    on('importFileBtn', function () { var f = $('importFile'); if (f) f.click(); });
    var fileEl = $('importFile');
    if (fileEl) fileEl.addEventListener('change', function () {
      if (fileEl.files && fileEl.files[0]) doImportFile(fileEl.files[0]);
      fileEl.value = '';
    });
    /* [A-P0-8] 选择弹层：遮罩点空白/取消都关 */
    on('selectCancel', closeSelectSheet);
    var selMask = $('selectMask');
    if (selMask) selMask.addEventListener('click', function (ev) { if (ev.target === selMask) closeSelectSheet(); });
    on('clipBtn', function () {
      if (navigator.clipboard && navigator.clipboard.readText) {
        navigator.clipboard.readText().then(function (t) { $('importText').value = t; }, function () { toast('读剪贴板失败，请手动粘贴'); });
      } else { toast('请手动粘贴口令'); }
    });
    $('content').addEventListener('click', onContentClick);

    /* 规则事件 → 界面 [A-P1-4]
     * 走 engine 的 HK.__emitHook（engine 内部 emit 先经桥 report 再调钩子）。
     * 不能再覆盖 HK.emit：新版 emit 内部会回调 HK.emit 本身，覆盖即递归。 */
    HK.__emitHook = function (name, payload) {
      if (name === 'hk.toast') toast(payload);
      else if (name === 'hk.error') log('ERROR ' + payload);
      else if (name === 'hk.log') log(str(payload));
      else if (name === 'hk.degrade') log('DEGRADE ' + payload);
      else if (name === 'play') { if (payload && payload.url) log('播放出口: ' + payload.url); }
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
          renderItems(top.items, top.title, !!STATE.pager);
        }
      }
      else if (name === 'hk.addItem') {
        /* [A-P1-4] addItemAfter/Before：['after'|'before', 锚点id, 新条目]，
         * 原来事件发了没人消费，条目永远不出现 */
        var a = payload;
        if (Object.prototype.toString.call(a) === '[object Array]' && STATE.stack.length && a[2]) {
          var topA = STATE.stack[STATE.stack.length - 1];
          var arr = topA.items, idx = -1, aid = str(a[1]);
          for (var q = 0; q < arr.length; q++) {
            if (arr[q] && arr[q].extra && str(arr[q].extra.id) === aid) { idx = q; break; }
          }
          if (idx < 0) arr.push(a[2]);
          else arr.splice(a[0] === 'before' ? idx : idx + 1, 0, a[2]);
          if (a[2].extra && a[2].extra.id) HK.STORE.items[str(a[2].extra.id)] = a[2];
          renderItems(topA.items, topA.title, !!STATE.pager);
        }
      }
      else if (name === 'hk.deleteItem') {
        /* [A-P1-4] deleteItem(id)：从当前页条目数组里真删并重渲染 */
        var did = str(payload);
        if (STATE.stack.length) {
          var topD = STATE.stack[STATE.stack.length - 1];
          topD.items = topD.items.filter(function (x) {
            return !(x && x.extra && str(x.extra.id) === did);
          });
          renderItems(topD.items, topD.title, !!STATE.pager);
        }
      }
      else if (name === 'hk.open') {
        /* [A-P2-3] openPage/openHikerPage/openDetailPage 原来没有消费方 */
        var ou = str(payload);
        if (ou) { if (STATE.site) navigate(ou); else openWeb(ou); }
      }
      else if (name === 'hk.back') { back(); }
    };

    /* [A-输入] 剪贴板自动识别：启动查一次，回前台/获焦再查（hash 去重） */
    checkClipboardAuto();
    document.addEventListener('visibilitychange', function () {
      if (!document.hidden) checkClipboardAuto();
    });
    global.addEventListener('focus', checkClipboardAuto);

    showView('sites');
  }

  UI.boot = boot;
  global.HikerUI = UI;
})(typeof window !== 'undefined' ? window : this);
