// 第12章 CI/CD 流水线（云效 Flow）详细可操作课件 —— 与第11章同款设计系统
const pptxgen = require("pptxgenjs");
const p = new pptxgen();
p.layout = "LAYOUT_WIDE";
p.author = "coffeeliu";
p.title = "第12章 CI/CD 流水线自动化（基于云效 Flow）";

const NAVY = "0B2545", BLUE = "13457A", TEAL = "1C7293", CYAN = "3FA7B8";
const AMBER = "E8A427", GRAY = "8A98A8", LIGHT = "F3F6FB", CARD = "FFFFFF";
const INK = "1C2B3A", SUB = "5A6B7B", W = 13.33, H = 7.5;
const FONT = "微软雅黑", MONO = "Consolas";
const shadow = () => ({ type: "outer", color: "0B2545", blur: 7, offset: 3, angle: 135, opacity: 0.16 });

function content(slide, kicker, title, accent = TEAL) {
  slide.background = { color: LIGHT };
  slide.addShape(p.shapes.RECTANGLE, { x: 0, y: 0, w: 0.16, h: H, fill: { color: NAVY } });
  slide.addShape(p.shapes.RECTANGLE, { x: 0.62, y: 0.46, w: 0.16, h: 0.58, fill: { color: accent } });
  slide.addText(kicker, { x: 0.92, y: 0.36, w: 11.8, h: 0.3, fontFace: FONT, fontSize: 11.5, color: accent, bold: true, charSpacing: 2, margin: 0 });
  slide.addText(title, { x: 0.9, y: 0.58, w: 11.95, h: 0.6, fontFace: FONT, fontSize: 25, color: NAVY, bold: true, margin: 0 });
  slide.addText("第12章 · CI/CD 流水线", { x: 10.2, y: 7.06, w: 2.7, h: 0.3, fontFace: FONT, fontSize: 9, color: GRAY, align: "right", margin: 0 });
}
let pageNo = 0;
function pg(slide) { pageNo++; slide.addText(String(pageNo), { x: 0.3, y: 7.06, w: 0.5, h: 0.3, fontFace: FONT, fontSize: 9, color: GRAY, margin: 0 }); }
function card(slide, x, y, w, h, fill = CARD) {
  slide.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: fill }, line: { color: "E2E8F2", width: 1 }, rectRadius: 0.07, shadow: shadow() });
}
function circle(slide, x, y, d, color, label) {
  slide.addShape(p.shapes.OVAL, { x, y, w: d, h: d, fill: { color } });
  slide.addText(label, { x, y, w: d, h: d, fontFace: FONT, fontSize: d > 0.55 ? 16 : 12, color: "FFFFFF", bold: true, align: "center", valign: "middle", margin: 0 });
}
function box(slide, x, y, w, h, fill, line, label, opt = {}) {
  slide.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: fill }, line: { color: line, width: 1.25 }, rectRadius: 0.06 });
  slide.addText(label, { x: x + 0.05, y, w: w - 0.1, h, fontFace: FONT, fontSize: opt.fs || 12, color: opt.color || INK, bold: opt.bold !== false, align: "center", valign: "middle", margin: 0 });
}
function arrow(slide, x, y, w, h, color = TEAL, label) {
  let flipH = false, flipV = false;
  if (w < 0) { x += w; w = -w; flipH = true; }
  if (h < 0) { y += h; h = -h; flipV = true; }
  slide.addShape(p.shapes.LINE, { x, y, w, h, flipH, flipV, line: { color, width: 2, endArrowType: "triangle" } });
  if (label) slide.addText(label, { x: x - 0.3, y: y + h / 2 - 0.16, w: 1.6, h: 0.32, fontFace: FONT, fontSize: 9, color: SUB, align: "center", margin: 0 });
}
// 字段表：[ [字段, 值, 颜色?, mono?, bold?], ... ]
function ftable(slide, x, y, w, rows, opt = {}) {
  const hdr = (t) => ({ text: t, options: { fill: { color: NAVY }, color: "FFFFFF", bold: true, fontSize: 12.5, align: "left", valign: "middle", fontFace: FONT } });
  const data = [[hdr(" 字段"), hdr(" " + (opt.vh || "填什么"))]];
  rows.forEach(r => data.push([
    { text: r[0], options: { color: NAVY, bold: true, fontSize: opt.fs || 12, align: "left", valign: "middle", fontFace: FONT, fill: { color: "F1F5FA" } } },
    { text: r[1], options: { color: r[2] || INK, fontSize: opt.fs || 12, align: "left", valign: "middle", fontFace: r[3] ? MONO : FONT, bold: !!r[4] } },
  ]));
  slide.addTable(data, { x, y, w, colW: opt.colW || [w * 0.3, w * 0.7], rowH: opt.rowH || 0.46, border: { pt: 1, color: "D6DEE8" }, fill: { color: "FFFFFF" }, valign: "middle", margin: [2, 4, 2, 4] });
}
// 代码块
function codeBox(slide, x, y, w, h, lines) {
  slide.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: "0E2236" }, line: { color: "1C3A52", width: 1 }, rectRadius: 0.05 });
  slide.addText(lines, { x: x + 0.18, y: y + 0.08, w: w - 0.36, h: h - 0.16, fontFace: MONO, fontSize: 11.5, color: "CFE6F2", valign: "top", lineSpacingMultiple: 1.12, margin: 0 });
}
// 编号步骤列表
function stepList(slide, x, y, w, items, accent = TEAL, fs = 13) {
  items.forEach((it, i) => {
    const yy = y + i * (opt_rowH(items));
    circle(slide, x, yy, 0.42, accent, String(it.n));
    slide.addText(it.t, { x: x + 0.6, y: yy - 0.05, w: w - 0.6, h: opt_rowH(items), fontFace: FONT, fontSize: fs, color: INK, valign: "top", lineSpacingMultiple: 1.05, margin: 0 });
  });
}
function opt_rowH(items) { return items.__rh || 0.62; }

// =============== 1. 封面 ===============
{
  const s = p.addSlide();
  s.background = { color: NAVY };
  s.addShape(p.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.22, fill: { color: AMBER } });
  s.addShape(p.shapes.RECTANGLE, { x: 0, y: H - 0.12, w: W, h: 0.12, fill: { color: TEAL } });
  s.addText("第 12 章", { x: 0.9, y: 1.4, w: 6, h: 0.5, fontFace: FONT, fontSize: 20, color: CYAN, bold: true, charSpacing: 3, margin: 0 });
  s.addText("CI/CD 流水线自动化", { x: 0.9, y: 1.95, w: 11.6, h: 1.0, fontFace: FONT, fontSize: 44, color: "FFFFFF", bold: true, margin: 0 });
  s.addText("基于云效 Flow · 跟着每页字段表，就能把流水线配出来", { x: 0.9, y: 2.95, w: 11.6, h: 0.6, fontFace: FONT, fontSize: 21, color: "CADCFC", margin: 0 });
  const flow = ["git push", "云效自动构建", "自动发布上云"];
  flow.forEach((t, i) => {
    const x = 0.9 + i * 4.0;
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y: 4.3, w: 3.5, h: 1.0, fill: { color: i === 0 ? "3A2A12" : "112E54" }, line: { color: i === 0 ? AMBER : CYAN, width: 1.5 }, rectRadius: 0.08 });
    s.addText(t, { x: x + 0.05, y: 4.3, w: 3.4, h: 1.0, fontFace: FONT, fontSize: 17, color: "FFFFFF", bold: true, align: "center", valign: "middle", margin: 0 });
    if (i < flow.length - 1) arrow(s, x + 3.52, 4.8, 0.45, 0, AMBER);
  });
  s.addText("把第 11 章每一个手工动作，都让机器替你做 —— 全程零手敲命令", { x: 0.9, y: 6.0, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 14, color: "9FB3D1", italic: true, margin: 0 });
}

// =============== 2. 目标 & 主线 ===============
{
  const s = p.addSlide(); content(s, "OBJECTIVE", "本章目标 & 学习主线", AMBER); pg(s);
  card(s, 0.85, 1.4, 5.4, 5.3);
  s.addText([
    { text: "目标：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 16 } },
    { text: "把第 11 章手工做的“本地打包 → 上传 jar → 启动进程”全部自动化——你只管 git push。", options: { color: INK, breakLine: true, fontSize: 14 } },
    { text: "", options: { breakLine: true, fontSize: 8 } },
    { text: "理念：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 16 } },
    { text: "CI/CD 不是“换工具”，而是把第 11 章每个手工动作都让机器替你做。", options: { color: TEAL, bold: true, breakLine: true, fontSize: 14 } },
    { text: "", options: { breakLine: true, fontSize: 8 } },
    { text: "怎么用这份课件：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 16 } },
    { text: "每个配置页都给了字段表 / 命令 / 点击顺序，照着填即可建成流水线。", options: { color: INK, fontSize: 14 } },
  ], { x: 1.1, y: 1.62, w: 4.9, h: 4.9, valign: "top", lineSpacingMultiple: 1.12, margin: 0 });

  card(s, 6.5, 1.4, 6.0, 5.3);
  s.addText("手工动作 ──► 流水线替你做", { x: 6.75, y: 1.58, w: 5.5, h: 0.4, fontFace: FONT, fontSize: 16, color: NAVY, bold: true, margin: 0 });
  const map = [
    ["本地 mvn deploy/package", "“Java 构建上传”任务"],
    ["会话管理 📤 上传 jar", "“主机部署”自动 scp"],
    ["终端 ./manage.sh restart", "“部署脚本”框自动跑"],
    ["EDAS 控制台点部署", "“EDAS 应用发布”任务"],
    ["SAE 控制台点部署", "“SAE 应用发布”任务"],
    ["本地 npm run build + 传 dist", "“Node 构建 + 主机部署”"],
  ];
  map.forEach((m, i) => {
    const y = 2.12 + i * 0.72;
    box(s, 6.75, y, 2.95, 0.58, "EEF3FA", BLUE, m[0], { fs: 10.5, color: INK });
    arrow(s, 9.72, y + 0.29, 0.32, 0, AMBER);
    box(s, 10.15, y, 2.2, 0.58, "FFF6E6", AMBER, m[1], { fs: 10.5, color: INK });
  });
}

// =============== 3. 6 条流水线全景 ===============
{
  const s = p.addSlide(); content(s, "BIG PICTURE", "6 条流水线全景：微服务 × 路径"); pg(s);
  const hdr = (t) => ({ text: t, options: { fill: { color: NAVY }, color: "FFFFFF", bold: true, fontSize: 13.5, align: "center", valign: "middle" } });
  const c = (t, col, b) => ({ text: t, options: { color: col || INK, fontSize: 12.5, align: "center", valign: "middle", fontFace: FONT, bold: !!b } });
  s.addTable([
    [hdr("微服务 ＼ 路径"), hdr("路径 A · 主机部署"), hdr("路径 B · EDAS 发布 ⭐"), hdr("路径 C · SAE 发布")],
    [c("coffee-userorder", NAVY, true), c("…-pipeline-A"), c("…-pipeline-B", "B5630A"), c("…-pipeline-C")],
    [c("coffee-expresstrack", NAVY, true), c("…-pipeline-A"), c("…-pipeline-B", "B5630A"), c("…-pipeline-C")],
    [c("coffee-app（网关）", NAVY, true), c("…-pipeline-A"), c("…-pipeline-B", "B5630A"), c("…-pipeline-C")],
  ], { x: 0.85, y: 1.55, w: 11.65, colW: [3.35, 2.8, 2.9, 2.6], rowH: 0.78, border: { pt: 1, color: "D6DEE8" }, fill: { color: "FFFFFF" }, valign: "middle" });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.05, w: 11.65, h: 0.68, fill: { color: "FFF6E6" }, line: { color: AMBER, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "+ 1 条前端流水线 ", options: { bold: true, color: "B5630A" } }, { text: "coffee-front-pipeline —— 三条路径共用一条，只改 FRONT_API_URL。", options: { color: INK } }], { x: 1.05, y: 5.05, w: 11.3, h: 0.68, fontFace: FONT, fontSize: 13.5, valign: "middle", margin: 0 });
  s.addText("教学挑一条路径建 3 条后端 + 1 条前端即可跑通。本章 Part 3 先配通“构建阶段”（三路径共用），Part 4/5/6 只换“部署任务”。", { x: 0.9, y: 5.95, w: 11.6, h: 0.6, fontFace: FONT, fontSize: 12.5, color: TEAL, bold: true, valign: "top", margin: 0 });
}

// =============== 4. 必懂 5 概念 ===============
{
  const s = p.addSlide(); content(s, "FOUNDATIONS", "云效 Flow 必懂的 5 个概念"); pg(s);
  const items = [
    ["1", "流水线 Pipeline", "一条“从代码到运行实例”的传送带 = 微服务 × 路径。", BLUE],
    ["2", "阶段→任务→步骤", "流水线分三层：阶段(顺序) → 任务(构建/部署) → 步骤。", TEAL],
    ["3", "流水线源 Source", "拉代码来源。推荐云效 Codeup（国内最快），GitHub 镜像同步过去。", CYAN],
    ["4", "变量 / 服务连接", "别硬编码！变量=明文每线独立；服务连接=加密全局共用(AK/密码)。", AMBER],
    ["5", "触发器 Trigger", "何时自动跑：代码 push(默认 main) / 定时 / 手动。", BLUE],
  ];
  items.forEach((it, i) => {
    const col = i % 3, row = Math.floor(i / 3);
    const x = 0.85 + col * 3.95, y = 1.55 + row * 2.55;
    card(s, x, y, 3.7, 2.3);
    circle(s, x + 0.25, y + 0.28, 0.6, it[3], it[0]);
    s.addText(it[1], { x: x + 1.0, y: y + 0.28, w: 2.55, h: 0.6, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, valign: "middle", margin: 0 });
    s.addText(it[2], { x: x + 0.3, y: y + 1.05, w: 3.15, h: 1.1, fontFace: FONT, fontSize: 12.5, color: INK, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
  });
  const x = 0.85 + 2 * 3.95, y = 1.55 + 1 * 2.55;
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w: 3.7, h: 2.3, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.08 });
  s.addText([{ text: "先搞清名词，再点鼠标。\n", options: { bold: true, color: NAVY, fontSize: 14 } }, { text: "否则面对云效控制台会一头雾水——这一节零操作。", options: { color: INK, fontSize: 12.5 } }], { x: x + 0.3, y: y + 0.35, w: 3.1, h: 1.6, fontFace: FONT, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
}

// =============== 5. 准备① 代码托管 + 全局设置入口 ===============
{
  const s = p.addSlide(); content(s, "准备 · STEP 1", "把代码推到 Codeup + 找到全局设置"); pg(s);
  card(s, 0.85, 1.35, 6.4, 5.25);
  s.addText("① 代码推到云效 Codeup", { x: 1.05, y: 1.5, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 15.5, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "登录 devops.aliyun.com → 应用切换器进“代码管理 Codeup”", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 7 } },
    { text: "新建代码库，名 cloudnativeapp", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 7 } },
    { text: "本地把云效仓库当作另一个远程推上去：", options: { bullet: { code: "2022" }, color: INK, fontSize: 13 } },
  ], { x: 1.3, y: 1.95, w: 5.8, h: 1.3, valign: "top", margin: 0 });
  codeBox(s, 1.3, 3.15, 5.85, 0.8, "git remote add codeup https://codeup.aliyun.com/<命名空间>/cloudnativeapp.git\ngit push codeup main");
  s.addText("GitHub 仍是 source of truth，可在 Codeup 仓库设置里“代码同步”自动镜像。", { x: 1.3, y: 4.05, w: 5.8, h: 0.6, fontFace: FONT, fontSize: 11.5, color: SUB, italic: true, valign: "top", margin: 0 });
  s.addText("② 找到“全局设置”入口（很多人卡这）", { x: 1.05, y: 4.75, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 15.5, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "流水线 Flow 首页 → 左侧导航最下方 ⚙ 全局设置", options: { bullet: { code: "2022" }, color: NAVY, bold: true, fontSize: 13, breakLine: true, paraSpaceAfter: 6 } },
    { text: "里面会用到：服务连接 / 主机组管理 / 通用变量组", options: { bullet: { code: "2022" }, color: INK, fontSize: 13 } },
  ], { x: 1.3, y: 5.2, w: 5.8, h: 1.2, valign: "top", margin: 0 });
  // 右图位
  card(s, 7.5, 1.35, 5.0, 5.25, "EFF3F8");
  s.addText("📷", { x: 7.5, y: 1.35, w: 5.0, h: 5.25, fontFace: FONT, fontSize: 46, color: "9FB0C2", align: "center", valign: "middle", margin: 0 });
}

// =============== 6. 准备② 建 2 个服务连接 ===============
{
  const s = p.addSlide(); content(s, "准备 · STEP 2", "建 2 个服务连接（加密存凭据）"); pg(s);
  s.addText("位置：全局设置 → 服务连接 → 新建服务连接。两个连接后面所有流水线共用。", { x: 0.9, y: 1.3, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: SUB, margin: 0 });
  card(s, 0.85, 1.85, 5.7, 4.7);
  s.addText("连接 1 · aliyun-ak（部署 B/C 用）", { x: 1.05, y: 2.0, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  ftable(s, 1.05, 2.5, 5.3, [
    ["类型", "阿里云"],
    ["AccessKey", "RAM 子账号 AK/SK", "B5630A", false, true],
    ["名称", "aliyun-ak", INK, true],
  ], { fs: 12, rowH: 0.55, colW: [1.5, 3.8] });
  s.addText("⚠ 进 RAM 控制台用子账号生成 AK/SK，绝不用主账号。", { x: 1.05, y: 4.5, w: 5.3, h: 0.8, fontFace: FONT, fontSize: 12, color: "B5630A", bold: true, valign: "top", margin: 0 });
  card(s, 6.8, 1.85, 5.7, 4.7);
  s.addText("连接 2 · yunxiao-maven（构建用）", { x: 7.0, y: 2.0, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  ftable(s, 7.0, 2.5, 5.3, [
    ["类型", "私有 Maven 仓库 / 用户名密码"],
    ["用户名密码", "本地 settings.xml 的 <server>"],
    ["名称", "yunxiao-maven", INK, true],
  ], { fs: 12, rowH: 0.55, colW: [1.6, 3.7] });
  s.addText("找不到账号？云效制品仓库 → 右上角“配置指引”里复制凭据。", { x: 7.0, y: 4.5, w: 5.3, h: 0.8, fontFace: FONT, fontSize: 12, color: SUB, italic: true, valign: "top", margin: 0 });
}

// =============== 7. 准备③ RAM 授权 ===============
{
  const s = p.addSlide(); content(s, "准备 · STEP 3", "给 RAM 子账号授必要权限", AMBER); pg(s);
  card(s, 0.85, 1.5, 7.0, 3.5);
  s.addText("授权步骤", { x: 1.05, y: 1.65, w: 6.5, h: 0.4, fontFace: FONT, fontSize: 15.5, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "RAM 控制台 → 找到上一页那个子账号", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 10 } },
    { text: "权限管理 → 新增授权", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 10 } },
    { text: "系统策略搜并勾选这 2 个：", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 8 } },
    { text: "AliyunEDASFullAccess（路径 B 用）", options: { color: NAVY, bold: true, fontSize: 13.5, breakLine: true, paraSpaceAfter: 5, fontFace: MONO } },
    { text: "AliyunSAEFullAccess（路径 C 用）", options: { color: NAVY, bold: true, fontSize: 13.5, fontFace: MONO } },
  ], { x: 1.3, y: 2.15, w: 6.4, h: 2.7, valign: "top", margin: 0 });
  card(s, 8.05, 1.5, 4.45, 3.5, "EFF3F8");
  s.addText("📷", { x: 8.05, y: 1.5, w: 4.45, h: 3.5, fontFace: FONT, fontSize: 42, color: "9FB0C2", align: "center", valign: "middle", margin: 0 });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.25, w: 11.65, h: 1.25, fill: { color: "FBE9D0" }, line: { color: AMBER, width: 1.25 }, rectRadius: 0.06 });
  s.addText([{ text: "🔒 硬铁律：", options: { bold: true, color: "B5630A" } }, { text: "绝不用主账号 AK！子账号 + 最小权限——云效日志可下载，AK 写进日志就是事故。这是任何 CI/CD 体系的底线。", options: { color: INK } }], { x: 1.05, y: 5.25, w: 11.3, h: 1.25, fontFace: FONT, fontSize: 14, valign: "middle", margin: 0 });
}

// =============== 8. 构建① 新建流水线 + 选模板 ===============
{
  const s = p.addSlide(); content(s, "构建 · 三路径共用", "① 新建流水线 + 选 Java 模板"); pg(s);
  card(s, 0.85, 1.4, 6.4, 5.2);
  s.addText("点击顺序", { x: 1.05, y: 1.55, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 15.5, color: TEAL, bold: true, margin: 0 });
  const steps = [
    "流水线 Flow 首页 → 右上角蓝色“新建流水线”",
    "弹窗左侧分类点 Java",
    "选模板：“Java · 测试、构建、部署到阿里云 ECS / 自有主机”",
    "起名 + 创建 → 进入“流程配置”页",
  ];
  steps.forEach((t, i) => {
    const y = 2.05 + i * 1.05;
    circle(s, 1.1, y, 0.5, TEAL, String(i + 1));
    s.addText(t, { x: 1.8, y: y - 0.05, w: 5.3, h: 0.95, fontFace: FONT, fontSize: 13.5, color: INK, valign: "middle", margin: 0 });
  });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 7.5, y: 1.4, w: 5.0, h: 2.0, fill: { color: "FFF6E6" }, line: { color: AMBER, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "一个模板打天下：", options: { bold: true, color: "B5630A", breakLine: true, fontSize: 14 } }, { text: "3 条路径都先用这个模板创建，进去后只把“主机部署”任务删掉，换成 EDAS / SAE 任务即可。", options: { color: INK, fontSize: 13 } }], { x: 7.7, y: 1.6, w: 4.65, h: 1.7, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
  card(s, 7.5, 3.6, 5.0, 3.0, "EFF3F8");
  s.addText("📷", { x: 7.5, y: 3.6, w: 5.0, h: 3.0, fontFace: FONT, fontSize: 44, color: "9FB0C2", align: "center", valign: "middle", margin: 0 });
}

// =============== 9. 构建② 流程结构 + 配流水线源 ===============
{
  const s = p.addSlide(); content(s, "构建 · 三路径共用", "② 流程配置页结构 + 配流水线源"); pg(s);
  // 流程结构
  box(s, 0.85, 1.5, 2.6, 0.9, "EEF3FA", BLUE, "流水线源\n(拉代码)", { fs: 12 });
  arrow(s, 3.47, 1.95, 0.35, 0, NAVY);
  box(s, 3.85, 1.5, 2.8, 0.9, "EAF1F8", TEAL, "阶段1\n测试 / 构建", { fs: 12 });
  arrow(s, 6.67, 1.95, 0.35, 0, NAVY);
  box(s, 7.05, 1.5, 2.8, 0.9, "FFF6E6", AMBER, "阶段2\n部署", { fs: 12 });
  arrow(s, 9.87, 1.95, 0.35, 0, NAVY);
  box(s, 10.25, 1.5, 2.25, 0.9, "F0F4F9", CYAN, "可加更多阶段", { fs: 12 });
  s.addText("点流水线源卡片配代码源；点阶段卡片里“+ 添加任务”；点阶段间“+”圈加新阶段。", { x: 0.9, y: 2.6, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12, color: SUB, margin: 0 });
  card(s, 0.85, 3.15, 7.0, 3.4);
  s.addText("配置流水线源（点最左卡片）", { x: 1.05, y: 3.3, w: 6.5, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  ftable(s, 1.05, 3.8, 6.6, [
    ["源类型", "Codeup（或 GitHub，跟你 Step1 对应）"],
    ["代码仓库", "cloudnativeapp"],
    ["默认分支", "main", INK, true],
    ["触发方式", "勾 Push 触发（Part 8 再细调）"],
  ], { fs: 12, rowH: 0.52, colW: [1.7, 4.9] });
  card(s, 8.1, 3.15, 4.4, 3.4, "EFF3F8");
  s.addText("📷", { x: 8.1, y: 3.15, w: 4.4, h: 3.4, fontFace: FONT, fontSize: 42, color: "9FB0C2", align: "center", valign: "middle", margin: 0 });
}

// =============== 10. 构建③ Java 构建上传任务 ===============
{
  const s = p.addSlide(); content(s, "构建 · 三路径共用", "③“Java 构建上传”任务怎么填"); pg(s);
  ftable(s, 0.85, 1.4, 6.7, [
    ["JDK 版本", "OpenJDK 17"],
    ["Maven 版本", "3.8 或 3.9"],
    ["构建命令", "见右侧模板", "B5630A", false, true],
    ["构建物路径", "coffee-*/provider/target/*.jar", INK, true],
    ["制品名称", "app-jar", INK, true, true],
    ["使用凭据", "勾“使用服务连接”→ 选 yunxiao-maven"],
  ], { fs: 12, rowH: 0.56, colW: [1.9, 4.8] });
  s.addText("构建命令模板（替换 <MODULE>）", { x: 7.75, y: 1.4, w: 4.8, h: 0.35, fontFace: FONT, fontSize: 13, color: NAVY, bold: true, margin: 0 });
  codeBox(s, 7.75, 1.8, 4.75, 1.85, "mvn -B clean package -DskipTests \\\n  -pl <MODULE> -am \\\n  -s settings-yunxiao.xml \\\n  -Daliyun.repo.url=$ALIYUN_REPO_URL \\\n  -Daliyun.repo.snapshot.url=\\\n     $ALIYUN_REPO_SNAPSHOT_URL");
  s.addText("<MODULE> 三选一", { x: 7.75, y: 3.75, w: 4.8, h: 0.3, fontFace: FONT, fontSize: 12.5, color: NAVY, bold: true, margin: 0 });
  ftable(s, 7.75, 4.1, 4.75, [
    ["coffee-userorder", "coffee-userorder/provider", INK, true],
    ["coffee-expresstrack", "coffee-expresstrack/provider", INK, true],
    ["coffee-app", "coffee-app", INK, true],
  ], { fs: 10.5, rowH: 0.42, colW: [1.9, 2.85], vh: "-pl 填" });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.6, w: 6.7, h: 1.0, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "变量在哪设？ ", options: { bold: true, color: NAVY } }, { text: "流水线编辑页“变量和缓存”Tab 加 ALIYUN_REPO_URL、ALIYUN_REPO_SNAPSHOT_URL（你的云效制品仓库地址）。", options: { color: INK } }], { x: 1.05, y: 5.6, w: 6.4, h: 1.0, fontFace: FONT, fontSize: 12, valign: "middle", margin: 0 });
}

// =============== 11. 构建④ 构建物上传（头号坑）===============
{
  const s = p.addSlide(); content(s, "⚠️ 最大的坑", "④ 构建物上传：步骤间不共享文件", AMBER); pg(s);
  s.addText("构建明明绿了，部署却报“找不到 jar”——几乎一定是这一步没配。", { x: 0.9, y: 1.3, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13.5, color: "B5630A", bold: true, margin: 0 });
  box(s, 1.3, 2.0, 2.8, 0.95, "EAF1F8", TEAL, "Java 构建上传\n打出 jar", { fs: 12.5 });
  arrow(s, 4.15, 2.47, 1.3, 0, AMBER, "构建物上传");
  box(s, 5.55, 2.0, 2.8, 0.95, "FFF6E6", AMBER, "制品仓库\n(按名字存)", { fs: 12.5 });
  arrow(s, 8.4, 2.47, 1.3, 0, TEAL, "部署任务取");
  box(s, 9.8, 2.0, 2.7, 0.95, "EAF1F8", TEAL, "主机/EDAS/SAE\n部署", { fs: 11.5 });
  s.addText("在“Java 构建上传”任务里找到“构建物上传”区域，两个字段必须对得上：", { x: 0.9, y: 3.2, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13.5, color: NAVY, bold: true, margin: 0 });
  ftable(s, 0.85, 3.7, 11.65, [
    ["制品名称", "app-jar  —— 部署任务“产物”下拉靠这个名字找 jar，两端必须同名", "B5630A", false, true],
    ["打包路径（provider）", "coffee-*/provider/target/*-1.0-SNAPSHOT.jar", INK, true],
    ["打包路径（coffee-app）", "coffee-app/target/coffee-app-0.0.1-SNAPSHOT.jar", INK, true],
  ], { fs: 12.5, rowH: 0.62, colW: [2.9, 8.75] });
  s.addText("路径匹配不到 = 上传空 = 部署找不到。这是 CI/CD 新手第一大坑。", { x: 0.9, y: 6.2, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12.5, color: SUB, italic: true, margin: 0 });
}

// =============== 12. 库 vs 应用 ===============
{
  const s = p.addSlide(); content(s, "构建 · 依赖", "库模块要先 deploy 吗？"); pg(s);
  card(s, 0.85, 1.5, 5.7, 3.7, "EEF3FA");
  s.addText("库代码没改时", { x: 1.05, y: 1.65, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 16, color: BLUE, bold: true, margin: 0 });
  s.addText([
    { text: "应用用 -pl <provider> -am 构建", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 10 } },
    { text: "构建机现场把 3 个库一起编译一遍", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 10 } },
    { text: "能跑通，不需要先 deploy", options: { bullet: { code: "2022" }, color: TEAL, bold: true, fontSize: 13.5 } },
  ], { x: 1.3, y: 2.15, w: 5.1, h: 2.9, valign: "top", margin: 0 });
  card(s, 6.8, 1.5, 5.7, 3.7, "FFF6E6");
  s.addText("库代码改了时", { x: 7.0, y: 1.65, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 16, color: "B5630A", bold: true, margin: 0 });
  s.addText([
    { text: "必须先把库 deploy 到云效仓库", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 10 } },
    { text: "否则别的流水线拉不到新版本", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 10 } },
    { text: "推荐单开 coffee-libs-pipeline，监听 api/common 变化自动 deploy", options: { bullet: { code: "2022" }, color: TEAL, bold: true, fontSize: 13.5 } },
  ], { x: 7.25, y: 2.15, w: 5.1, h: 2.9, valign: "top", margin: 0 });
  s.addText("库发布流水线构建命令：", { x: 0.9, y: 5.4, w: 4, h: 0.35, fontFace: FONT, fontSize: 12.5, color: NAVY, bold: true, margin: 0 });
  codeBox(s, 0.85, 5.75, 11.65, 0.8, "mvn -B clean deploy -DskipTests \\\n  -pl coffee-common,coffee-userorder/api,coffee-expresstrack/api -s settings-yunxiao.xml");
}

// =============== 13. 路径A① 建主机组 ===============
{
  const s = p.addSlide(); content(s, "PATH A · STEP 1", "建主机组（在每台 ECS 装 Runner）", GRAY); pg(s);
  card(s, 0.85, 1.4, 7.0, 4.0);
  const steps = [
    "全局设置 → 主机组管理 → 新建主机组",
    "主机组名称：coffee-prod-ecs；接入方式选“支持自动装 Runner”那项",
    "进主机列表 → 添加主机 → 用“阿里云 ECS”子菜单 → 勾 ECS-1/2/3",
    "云效经阿里云云助手（不是 SSH）远程在 ECS 上装 Runner",
    "等约 1-2 分钟，3 台状态变“在线”",
  ];
  steps.forEach((t, i) => {
    const y = 1.62 + i * 0.72;
    circle(s, 1.1, y, 0.46, GRAY, String(i + 1));
    s.addText(t, { x: 1.75, y: y - 0.05, w: 6.0, h: 0.66, fontFace: FONT, fontSize: 12.5, color: INK, valign: "middle", margin: 0 });
  });
  card(s, 8.1, 1.4, 4.4, 4.0, "EFF3F8");
  s.addText("📷", { x: 8.1, y: 1.4, w: 4.4, h: 4.0, fontFace: FONT, fontSize: 42, color: "9FB0C2", align: "center", valign: "middle", margin: 0 });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.6, w: 11.65, h: 1.0, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "为什么装 Runner 不用密码：", options: { bold: true, color: NAVY } }, { text: "Runner 主动从云效拉 jar，ECS 无需对外开 22 端口，比 SSH 更安全。卡在“离线”→ 确认 ECS 装了云助手 Agent、出方向能上公网。", options: { color: INK } }], { x: 1.05, y: 5.6, w: 11.3, h: 1.0, fontFace: FONT, fontSize: 12.5, valign: "middle", margin: 0 });
}

// =============== 14. 路径A② 主机部署任务 ===============
{
  const s = p.addSlide(); content(s, "PATH A · STEP 2", "配“主机部署”任务（userorder）", GRAY); pg(s);
  s.addText("模板第二阶段默认就是“主机部署”任务，点开它按下表填：", { x: 0.9, y: 1.3, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: SUB, margin: 0 });
  ftable(s, 0.85, 1.8, 11.65, [
    ["制品", "选 app-jar（上游产物）", INK, true],
    ["主机组", "coffee-prod-ecs"],
    ["过滤主机", "只选 ECS-1（按标签或 IP 过滤）"],
    ["下载路径", "/root/coffee/jars/coffee-userorder-provider-1.0-SNAPSHOT.jar", INK, true],
    ["执行用户", "root", INK, true],
    ["部署脚本", "见下方代码块", "B5630A", false, true],
  ], { fs: 12.5, rowH: 0.5, colW: [2.2, 9.45] });
  codeBox(s, 0.85, 5.45, 11.65, 0.6, "cd /root/coffee && ./manage.sh restart userorder");
  s.addText("保存 → 点“运行”测试一次 → 看每个任务变绿。", { x: 0.9, y: 6.2, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12.5, color: TEAL, bold: true, margin: 0 });
}

// =============== 15. 路径A③ 复制另两条 + 演示 ===============
{
  const s = p.addSlide(); content(s, "PATH A · STEP 3", "复制另两条 + 全链路演示", GRAY); pg(s);
  card(s, 0.85, 1.4, 6.4, 5.2);
  s.addText("复制改 5 处（⋯ → 复制）", { x: 1.05, y: 1.55, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  ftable(s, 1.05, 2.05, 6.1, [
    ["名称", "…-pipeline-A 改服务名"],
    ["-pl", "coffee-expresstrack/provider / coffee-app", INK, true],
    ["构建物路径", "对应服务的 target/*.jar", INK, true],
    ["部署 ECS", "ECS-1 → ECS-2 / ECS-3"],
    ["部署脚本", "restart expresstrack / restart app", INK, true],
  ], { fs: 11.5, rowH: 0.6, colW: [1.7, 4.4] });
  card(s, 7.5, 1.4, 5.0, 5.2, "EAF1F8");
  s.addText("全链路演示", { x: 7.7, y: 1.55, w: 4.6, h: 0.4, fontFace: FONT, fontSize: 15, color: TEAL, bold: true, margin: 0 });
  const d = ["本地改一行代码（如改句日志）", "git push codeup main", "看云效列表：流水线自动开跑 → 约 90 秒变绿 ✅", "ECS-1 终端 tail 日志，看到刚改那行"];
  d.forEach((t, i) => {
    const y = 2.1 + i * 1.0;
    circle(s, 7.7, y, 0.44, TEAL, String(i + 1));
    s.addText(t, { x: 8.3, y: y - 0.05, w: 4.0, h: 0.9, fontFace: FONT, fontSize: 12.5, color: INK, valign: "middle", margin: 0 });
  });
  s.addText("🎉 从按下 git push 到生产跑起新版本，全程零手工。", { x: 7.7, y: 6.05, w: 4.7, h: 0.4, fontFace: FONT, fontSize: 11.5, color: "B5630A", bold: true, margin: 0 });
}

// =============== 16. 路径B EDAS 应用发布 ===============
{
  const s = p.addSlide(); content(s, "PATH B · 推荐主线", "路径 B：EDAS 应用发布任务", AMBER); pg(s);
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 1.3, w: 11.65, h: 0.62, fill: { color: "FBE9D0" }, line: { color: AMBER, width: 1 }, rectRadius: 0.06 });
  s.addText("准备：EDAS 控制台 → 应用列表 → 点 userorder → 复制“应用 ID”（三个应用各记一次）。", { x: 1.05, y: 1.3, w: 11.3, h: 0.62, fontFace: FONT, fontSize: 13, color: INK, bold: true, valign: "middle", margin: 0 });
  s.addText("删掉默认“主机部署”→ + 添加任务搜 EDAS → 选“EDAS 应用发布”，按下表填：", { x: 0.9, y: 2.05, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: SUB, margin: 0 });
  ftable(s, 0.85, 2.55, 7.4, [
    ["服务连接", "aliyun-ak", INK, true],
    ["地域", "和你 EDAS 同地域"],
    ["应用", "下拉选 userorder", INK, true],
    ["部署包来源", "构建产物"],
    ["构建产物", "app-jar", INK, true],
    ["JVM 参数", "留空", "B5630A", false, true],
  ], { fs: 12.5, rowH: 0.52, colW: [2.2, 5.2] });
  card(s, 8.5, 2.55, 4.0, 3.65, "EAF1F8");
  s.addText("JVM 参数为何留空", { x: 8.7, y: 2.7, w: 3.6, h: 0.4, fontFace: FONT, fontSize: 14, color: NAVY, bold: true, margin: 0 });
  s.addText("填进流水线会覆盖 EDAS 应用上已有配置。留空 = 沿用第 11 章在 EDAS 填好的 -DENV/-DDB_HOST。\n\n单一来源原则：配置只在 EDAS 控制台维护，流水线只管推 jar。", { x: 8.7, y: 3.2, w: 3.6, h: 2.9, fontFace: FONT, fontSize: 12.5, color: INK, valign: "top", lineSpacingMultiple: 1.12, margin: 0 });
  s.addText("复制另两条：改名 + 改 -pl + EDAS 任务里改选的“应用”。", { x: 0.9, y: 6.35, w: 11.6, h: 0.3, fontFace: FONT, fontSize: 12, color: TEAL, bold: true, margin: 0 });
}

// =============== 17. 路径C① 关键差异 + 准备 ===============
{
  const s = p.addSlide(); content(s, "PATH C · Serverless", "路径 C：关键差异 + 准备", CYAN); pg(s);
  card(s, 0.85, 1.45, 6.0, 3.0, "EAF1F8");
  s.addText("SAE 没有“主机组”", { x: 1.05, y: 1.6, w: 5.6, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "路径 A：jar 推到固定 ECS", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 9 } },
    { text: "路径 B：EDAS Agent 推到 ECS", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 9 } },
    { text: "路径 C：云效调 SAE OpenAPI 触发“滚动部署”——SAE 自己弹实例、拉新 jar、重启。", options: { color: TEAL, bold: true, fontSize: 13 } },
  ], { x: 1.3, y: 2.1, w: 5.4, h: 2.2, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
  card(s, 7.1, 1.45, 5.4, 3.0, "FFF6E6");
  s.addText("准备：记下 2 个 ID", { x: 7.3, y: 1.6, w: 5.0, h: 0.4, fontFace: FONT, fontSize: 15, color: "B5630A", bold: true, margin: 0 });
  s.addText([
    { text: "SAE 应用 ID（应用详情页顶部）", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 10 } },
    { text: "命名空间 ID，形如 cn-hangzhou:coffee-prod-sae", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 10, fontFace: MONO } },
    { text: "三个 SAE 应用各记一次", options: { bullet: { code: "2022" }, color: INK, fontSize: 13 } },
  ], { x: 7.35, y: 2.1, w: 5.0, h: 2.2, valign: "top", margin: 0 });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 4.7, w: 11.65, h: 1.9, fill: { color: "FFFFFF" }, line: { color: "E2E8F2", width: 1 }, rectRadius: 0.07, shadow: shadow() });
  s.addText("流水线只负责：打包 jar + 告诉 SAE 用这个新 jar。下一页是发布任务的字段表。", { x: 1.1, y: 4.9, w: 11, h: 0.4, fontFace: FONT, fontSize: 14, color: NAVY, bold: true, margin: 0 });
  s.addText("创建方式同路径 B：用 Java 模板新建 → 配好构建（与 A/B 完全一样）→ 删“主机部署”→ 加“Serverless(SAE)应用发布”任务。", { x: 1.1, y: 5.5, w: 11, h: 0.9, fontFace: FONT, fontSize: 13, color: INK, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
}

// =============== 18. 路径C② SAE 发布任务字段 ===============
{
  const s = p.addSlide(); content(s, "PATH C · Serverless", "“Serverless(SAE)应用发布”任务", CYAN); pg(s);
  ftable(s, 0.85, 1.5, 7.4, [
    ["服务连接", "aliyun-ak", INK, true],
    ["地域", "和 SAE 同地域"],
    ["命名空间", "coffee-prod-sae", INK, true],
    ["SAE 应用", "下拉选 userorder-sae", INK, true],
    ["构建产物", "app-jar", INK, true],
    ["发布策略", "分批发布"],
    ["发布批次", "1（教学环境）"],
    ["分批等待时间", "0 秒"],
  ], { fs: 12, rowH: 0.5, colW: [2.3, 5.1] });
  card(s, 8.5, 1.5, 4.0, 4.0, "FFF6E6");
  s.addText("冷启动注意", { x: 8.7, y: 1.65, w: 3.6, h: 0.4, fontFace: FONT, fontSize: 14, color: "B5630A", bold: true, margin: 0 });
  s.addText("流水线部署/重启后，SAE 实例要现拉镜像、启 JVM，首次访问会等几秒——冷启动正常现象。\n\n演示前先手动 curl 一次预热。", { x: 8.7, y: 2.15, w: 3.6, h: 3.2, fontFace: FONT, fontSize: 12.5, color: INK, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
  s.addText("保存运行；复制另两条：改名 + 改 -pl + 改选的 SAE 应用。", { x: 0.9, y: 6.25, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12.5, color: TEAL, bold: true, margin: 0 });
}

// =============== 19. 前端① Node 构建上传 ===============
{
  const s = p.addSlide(); content(s, "FRONTEND · STEP 1", "前端流水线：Node 构建上传", TEAL); pg(s);
  s.addText("新建流水线选“Node.js · 部署到 ECS/自有主机”模板，名 coffee-front-pipeline，流水线源同后端。", { x: 0.9, y: 1.3, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12.5, color: SUB, margin: 0 });
  ftable(s, 0.85, 1.6, 6.6, [
    ["Node 版本", "18.x"],
    ["工作目录", "app-admin", INK, true],
    ["构建命令", "见右侧", "B5630A"],
    ["构建物路径", "app-admin/dist/**", INK, true],
    ["制品名称", "dist", INK, true, true],
  ], { fs: 12, rowH: 0.46, colW: [1.9, 4.7] });
  s.addText("构建命令", { x: 7.65, y: 1.6, w: 4.8, h: 0.32, fontFace: FONT, fontSize: 12.5, color: NAVY, bold: true, margin: 0 });
  codeBox(s, 7.65, 1.95, 4.85, 1.5, "npm config set registry \\\n  https://registry.npmmirror.com && \\\nnpm ci && \\\nVUE_APP_BASE_URL=$FRONT_API_URL \\\n  npm run build");
  s.addText("变量 FRONT_API_URL 随后端路径填（“变量和缓存”Tab）", { x: 0.9, y: 4.5, w: 11.6, h: 0.35, fontFace: FONT, fontSize: 13, color: NAVY, bold: true, margin: 0 });
  ftable(s, 0.85, 4.9, 11.65, [
    ["路径 A / B", "http://<ECS-3 公网 IP>:8005", INK, true],
    ["路径 C（SAE）", "SAE 公网 CLB 域名（无端口，默认 80）", INK, true],
  ], { fs: 12.5, rowH: 0.52, colW: [2.6, 9.05] });
}

// =============== 20. 前端② 主机部署 ===============
{
  const s = p.addSlide(); content(s, "FRONTEND · STEP 2", "前端“主机部署”到 ECS-3 Nginx", TEAL); pg(s);
  ftable(s, 0.85, 1.5, 7.4, [
    ["主机组", "coffee-prod-ecs，过滤 ECS-3"],
    ["下载路径", "/usr/share/nginx/html/dist.tar.gz", INK, true],
    ["部署脚本", "见下方", "B5630A", false, true],
  ], { fs: 12.5, rowH: 0.55, colW: [2.0, 5.4] });
  codeBox(s, 0.85, 3.8, 7.4, 1.15, "cd /usr/share/nginx/html && \\\n[ -f dist.tar.gz ] && \\\n  tar -xzf dist.tar.gz --strip-components=1 && \\\n  rm dist.tar.gz; \\\nnginx -t && nginx -s reload");
  card(s, 8.5, 1.5, 4.0, 3.05, "FFF6E6");
  s.addText("用宝塔镜像的同学", { x: 8.7, y: 1.65, w: 3.6, h: 0.4, fontFace: FONT, fontSize: 13.5, color: "B5630A", bold: true, margin: 0 });
  s.addText("网站根目录不是 /usr/share/nginx/html，而是 /www/wwwroot/<站点名>。把脚本里的路径全部换成你的站点根目录即可。", { x: 8.7, y: 2.1, w: 3.6, h: 2.3, fontFace: FONT, fontSize: 12, color: INK, valign: "top", lineSpacingMultiple: 1.12, margin: 0 });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.2, w: 11.65, h: 1.5, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "路径 C 切前端：", options: { bold: true, color: NAVY } }, { text: "三路径共用这一条线，切到 SAE 只需把 FRONT_API_URL 改成 SAE CLB 域名后重跑。", options: { color: INK } }, { text: "\n为什么不用 OSS：", options: { bold: true, color: NAVY } }, { text: "OSS 会引出 Bucket 权限、静态页 404 兜底等额外概念；ECS-3 Nginx 复用已学部署即可。（前后端本就不同源，跨域由网关 @CrossOrigin 放行，与用不用 OSS 无关）", options: { color: INK } }], { x: 1.05, y: 5.2, w: 11.3, h: 1.5, fontFace: FONT, fontSize: 13, valign: "middle", lineSpacingMultiple: 1.15, margin: 0 });
}

// =============== 21. 触发规则 & 分支策略 ===============
{
  const s = p.addSlide(); content(s, "TRIGGER", "触发规则与分支策略"); pg(s);
  card(s, 0.85, 1.5, 5.7, 3.1, "EAF1F8");
  s.addText("最小化分支策略", { x: 1.05, y: 1.65, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 16, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "main → 自动触发“生产”流水线", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 12 } },
    { text: "dev → 手动触发，部署到同组资源", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 12 } },
    { text: "feature/* → 不触发，PR 合并到 dev 时人工把关", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5 } },
  ], { x: 1.3, y: 2.15, w: 5.1, h: 2.3, valign: "top", margin: 0 });
  s.addText("设置位置：每条流水线 → 顶部“触发设置”Tab → 代码源触发（监听 main / Push 事件）。", { x: 7.0, y: 1.6, w: 5.4, h: 0.9, fontFace: FONT, fontSize: 12.5, color: SUB, valign: "top", margin: 0 });
  s.addText("路径过滤（强烈推荐）", { x: 7.0, y: 2.5, w: 5.4, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  ftable(s, 7.0, 2.95, 5.5, [
    ["userorder 线", "coffee-userorder/** 、common", INK, true],
    ["front 线", "app-admin/**", INK, true],
    ["libs 线", "common/** 、*/api/**", INK, true],
  ], { fs: 11, rowH: 0.46, colW: [1.7, 3.8], vh: "只在这些路径改动时触发" });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.0, w: 11.65, h: 1.0, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "好处：", options: { bold: true, color: NAVY } }, { text: "每条线只在相关代码改动时触发——省构建时长，也减少误部署面积（改前端不会重启后端）。", options: { color: INK } }], { x: 1.05, y: 5.0, w: 11.3, h: 1.0, fontFace: FONT, fontSize: 13.5, valign: "middle", margin: 0 });
}

// =============== 22. 高级特性 ===============
{
  const s = p.addSlide(); content(s, "ADVANCED", "高级特性：审批 / 灰度 / 回滚"); pg(s);
  s.addText("“看过就行”的加分内容，课程不强制做。", { x: 0.9, y: 1.3, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13.5, color: SUB, margin: 0 });
  const items = [
    ["人工卡点（审批）", "部署阶段前加“人工卡点”任务，配审批人；流水线跑到这里暂停，审批通过才继续。", BLUE],
    ["灰度发布", "路径 B/C 在发布任务里选“分批发布”，每批暂停观察、加大批次间等待时间。", TEAL],
    ["一键回滚", "路径 A：把上版 jar 重新推（ECS 保留最近 3 个）。路径 B/C：EDAS/SAE 变更记录点“回滚”。", AMBER],
  ];
  items.forEach((it, i) => {
    const x = 0.85 + i * 3.95;
    card(s, x, 2.0, 3.7, 3.6);
    s.addShape(p.shapes.RECTANGLE, { x, y: 2.0, w: 3.7, h: 0.65, fill: { color: it[2] } });
    s.addText(it[0], { x: x + 0.1, y: 2.0, w: 3.5, h: 0.65, fontFace: FONT, fontSize: 15.5, color: i === 2 ? "1C2B3A" : "FFFFFF", bold: true, align: "center", valign: "middle", margin: 0 });
    s.addText(it[1], { x: x + 0.3, y: 2.9, w: 3.1, h: 2.5, fontFace: FONT, fontSize: 13.5, color: INK, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
  });
}

// =============== 23. 6 条流水线一览 ===============
{
  const s = p.addSlide(); content(s, "CHEAT SHEET", "6 条流水线一览表"); pg(s);
  const hdr = (t) => ({ text: t, options: { fill: { color: NAVY }, color: "FFFFFF", bold: true, fontSize: 13, align: "center", valign: "middle" } });
  const c = (t, col, b, mono) => ({ text: t, options: { color: col || INK, fontSize: 12, align: "left", valign: "middle", fontFace: mono ? MONO : FONT, bold: !!b } });
  s.addTable([
    [hdr("流水线"), hdr("部署任务（云效真实名）"), hdr("部署目标")],
    [c("…-pipeline-A", NAVY, true, true), c("主机部署"), c("主机组 / ECS")],
    [c("…-pipeline-B ⭐", "B5630A", true, true), c("EDAS 应用发布"), c("EDAS 应用")],
    [c("…-pipeline-C", NAVY, true, true), c("Serverless(SAE)应用发布"), c("SAE 应用")],
    [c("coffee-front-pipeline", NAVY, true, true), c("Node 构建 + 主机部署"), c("ECS-3 Nginx")],
    [c("coffee-libs-pipeline（可选）", NAVY, true, true), c("只 mvn deploy，不部署"), c("云效 Maven 仓库")],
  ], { x: 0.85, y: 1.6, w: 11.65, colW: [4.0, 4.65, 3.0], rowH: 0.72, border: { pt: 1, color: "D6DEE8" }, fill: { color: "FFFFFF" }, valign: "middle" });
  s.addText("构建阶段三路径共用，只换“部署任务”——这就是“一个模板打天下”。", { x: 0.9, y: 6.35, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: TEAL, bold: true, italic: true, margin: 0 });
}

// =============== 24. 常见排错 ===============
{
  const s = p.addSlide(); content(s, "TROUBLESHOOT", "常见报错速查", AMBER); pg(s);
  const errs = [
    ["构建绿但部署“找不到 jar”", "“构建物上传”的制品名/打包路径写错，与部署任务选的产物不一致。"],
    ["Could not find artifact …coffee-common", "库没 deploy 到云效仓库——先跑 libs 流水线，或构建命令加 -am 现场编译。"],
    ["主机组 ECS 一直“离线”", "ECS 没装云助手 Agent，或出方向不通公网（Runner 主动连出）。"],
    ["EDAS/SAE 发布 Access denied / NotFound", "RAM 子账号缺 EDAS/SAE 权限，或应用 ID、服务连接地域写错。"],
    ["路径 C 切换后前端 API 全失败", "FRONT_API_URL 还停在 ECS-3:8005，改成 SAE CLB 域名重跑前端线。"],
  ];
  errs.forEach((e, i) => {
    const y = 1.6 + i * 1.02;
    card(s, 0.85, y, 11.65, 0.9);
    s.addShape(p.shapes.RECTANGLE, { x: 0.85, y, w: 0.12, h: 0.9, fill: { color: AMBER } });
    s.addText("✗ " + e[0], { x: 1.1, y, w: 5.0, h: 0.9, fontFace: FONT, fontSize: 12.5, color: "B5630A", bold: true, valign: "middle", margin: 0 });
    s.addText(e[1], { x: 6.2, y, w: 6.15, h: 0.9, fontFace: FONT, fontSize: 12.5, color: INK, valign: "middle", margin: 0 });
  });
}

// =============== 25. 小结 ===============
{
  const s = p.addSlide();
  s.background = { color: NAVY };
  s.addShape(p.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.22, fill: { color: AMBER } });
  s.addText("本章小结", { x: 0.9, y: 0.7, w: 11, h: 0.7, fontFace: FONT, fontSize: 34, color: "FFFFFF", bold: true, margin: 0 });
  const pts = [
    ["流水线 = 微服务 × 路径", "构建阶段三路径共用，只换“部署任务”——一个模板打天下。"],
    ["构建物上传是头号坑", "制品名 + 打包路径必须对得上，否则部署“找不到 jar”。"],
    ["凭据走服务连接，子账号最小权限", "绝不硬编码、绝不用主账号 AK；配置单一来源在平台。"],
    ["git push 之后全自动", "推荐后端走路径 B（EDAS）流水线作默认，全程零手敲命令。"],
  ];
  pts.forEach((pt, i) => {
    const y = 1.75 + i * 1.28;
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.9, y, w: 11.5, h: 1.1, fill: { color: "112E54" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
    circle(s, 1.15, y + 0.28, 0.55, AMBER, String(i + 1));
    s.addText(pt[0], { x: 1.95, y: y + 0.12, w: 4.7, h: 0.85, fontFace: FONT, fontSize: 16.5, color: "FFFFFF", bold: true, valign: "middle", margin: 0 });
    s.addText(pt[1], { x: 6.7, y: y + 0.12, w: 5.6, h: 0.85, fontFace: FONT, fontSize: 12.5, color: "CADCFC", valign: "middle", margin: 0 });
  });
}

const out = process.env.OUT || "deck/第12章-CICD流水线-课件.pptx";
p.writeFile({ fileName: out }).then(() => console.log("WROTE", out));
