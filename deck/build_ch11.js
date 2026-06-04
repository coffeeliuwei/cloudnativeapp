// 第11章 上云部署 教学课件生成脚本
const pptxgen = require("pptxgenjs");
const p = new pptxgen();
p.layout = "LAYOUT_WIDE"; // 13.33 x 7.5
p.author = "coffeeliu";
p.title = "第11章 把微服务部署到阿里云（多路径上云）";

// ---- 调色板（云/科技：深蓝主导 + 琥珀强调 + 灰=进阶）----
const NAVY = "0B2545";  // 主色：标题/封面/封底深底
const BLUE = "13457A";  // 次色
const TEAL = "1C7293";  // 支撑
const CYAN = "3FA7B8";  // 浅强调
const AMBER = "E8A427"; // 锐利强调：⭐推荐
const GRAY = "8A98A8";  // 进阶/弱化
const LIGHT = "F3F6FB"; // 内容页底
const CARD = "FFFFFF";
const INK = "1C2B3A";   // 正文
const SUB = "5A6B7B";   // 次正文
const W = 13.33, H = 7.5;
const FONT = "微软雅黑", MONO = "Consolas";

const shadow = () => ({ type: "outer", color: "0B2545", blur: 7, offset: 3, angle: 135, opacity: 0.16 });

// 内容页统一外壳：左侧深蓝竖带 + 标题色块 + 标签 + 页脚
function content(slide, kicker, title, accent = TEAL) {
  slide.background = { color: LIGHT };
  slide.addShape(p.shapes.RECTANGLE, { x: 0, y: 0, w: 0.16, h: H, fill: { color: NAVY } });
  // 标题左侧的小色块（贯穿全册的视觉母题）
  slide.addShape(p.shapes.RECTANGLE, { x: 0.62, y: 0.52, w: 0.16, h: 0.62, fill: { color: accent } });
  slide.addText(kicker, { x: 0.92, y: 0.42, w: 11.8, h: 0.3, fontFace: FONT, fontSize: 12, color: accent, bold: true, charSpacing: 2, margin: 0 });
  slide.addText(title, { x: 0.9, y: 0.66, w: 11.9, h: 0.62, fontFace: FONT, fontSize: 28, color: NAVY, bold: true, margin: 0 });
  slide.addText("第11章 · 多路径上云", { x: 10.4, y: 7.04, w: 2.5, h: 0.3, fontFace: FONT, fontSize: 9, color: GRAY, align: "right", margin: 0 });
}
let pageNo = 0;
function pg(slide) { pageNo++; slide.addText(String(pageNo), { x: 0.3, y: 7.04, w: 0.5, h: 0.3, fontFace: FONT, fontSize: 9, color: GRAY, margin: 0 }); }

// 卡片
function card(slide, x, y, w, h, fill = CARD) {
  slide.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: fill }, line: { color: "E2E8F2", width: 1 }, rectRadius: 0.08, shadow: shadow() });
}
// 小圆点编号/图标圈
function circle(slide, x, y, d, color, label) {
  slide.addShape(p.shapes.OVAL, { x, y, w: d, h: d, fill: { color } });
  slide.addText(label, { x, y, w: d, h: d, fontFace: FONT, fontSize: d > 0.6 ? 18 : 13, color: "FFFFFF", bold: true, align: "center", valign: "middle", margin: 0 });
}
function box(slide, x, y, w, h, fill, line, label, opt = {}) {
  slide.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: fill }, line: { color: line, width: 1.25 }, rectRadius: 0.06 });
  slide.addText(label, { x: x + 0.05, y, w: w - 0.1, h, fontFace: FONT, fontSize: opt.fs || 12, color: opt.color || INK, bold: opt.bold !== false, align: "center", valign: "middle", margin: 0 });
}
// 图片位：只放一个相机图标（中性细框，看起来像图片区域）。不写任何文字。
// caption 参数保留仅为调用方备注，不渲染到页面。
function photoBox(slide, x, y, w, h, caption) {
  slide.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: "EFF3F8" }, line: { color: "C9D4E0", width: 1 }, rectRadius: 0.06 });
  slide.addText("📷", { x, y, w, h, fontFace: FONT, fontSize: 46, color: "9FB0C2", align: "center", valign: "middle", margin: 0 });
}
function arrow(slide, x, y, w, h, color = TEAL, label) {
  // 规整为非负尺寸：负的宽/高会生成非法几何导致 PowerPoint 打不开
  let flipH = false, flipV = false;
  if (w < 0) { x += w; w = -w; flipH = true; }
  if (h < 0) { y += h; h = -h; flipV = true; }
  slide.addShape(p.shapes.LINE, { x, y, w, h, flipH, flipV, line: { color, width: 2, endArrowType: "triangle" } });
  if (label) slide.addText(label, { x: x - 0.3, y: y + h / 2 - 0.18, w: 1.6, h: 0.36, fontFace: FONT, fontSize: 9, color: SUB, align: "center", margin: 0 });
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
function codeBox(slide, x, y, w, h, lines) {
  slide.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: "0E2236" }, line: { color: "1C3A52", width: 1 }, rectRadius: 0.05 });
  slide.addText(lines, { x: x + 0.18, y: y + 0.08, w: w - 0.36, h: h - 0.16, fontFace: MONO, fontSize: 11.5, color: "CFE6F2", valign: "top", lineSpacingMultiple: 1.12, margin: 0 });
}

// =================== 1. 封面 ===================
{
  const s = p.addSlide();
  s.background = { color: NAVY };
  s.addShape(p.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.22, fill: { color: AMBER } });
  s.addShape(p.shapes.RECTANGLE, { x: 0, y: H - 0.12, w: W, h: 0.12, fill: { color: TEAL } });
  s.addText("第 11 章", { x: 0.9, y: 1.5, w: 6, h: 0.5, fontFace: FONT, fontSize: 20, color: CYAN, bold: true, charSpacing: 3, margin: 0 });
  s.addText("把微服务部署到阿里云", { x: 0.9, y: 2.05, w: 11.6, h: 1.0, fontFace: FONT, fontSize: 46, color: "FFFFFF", bold: true, margin: 0 });
  s.addText("多路径上云 · 从本地一步步搬上云", { x: 0.9, y: 3.05, w: 11.6, h: 0.6, fontFace: FONT, fontSize: 22, color: "CADCFC", margin: 0 });
  // 三路径徽标
  const tags = [["路径 B · EDAS", "⭐ 推荐主线", AMBER], ["路径 C · SAE", "Serverless 弹性", CYAN], ["路径 A · ECS", "🔧 进阶 / 选读", GRAY]];
  tags.forEach((t, i) => {
    const x = 0.9 + i * 4.0;
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x, y: 4.35, w: 3.7, h: 1.25, fill: { color: "112E54" }, line: { color: t[2], width: 1.5 }, rectRadius: 0.08 });
    s.addText(t[0], { x: x + 0.05, y: 4.5, w: 3.6, h: 0.45, fontFace: FONT, fontSize: 17, color: "FFFFFF", bold: true, align: "center", margin: 0 });
    s.addText(t[1], { x: x + 0.05, y: 4.95, w: 3.6, h: 0.4, fontFace: FONT, fontSize: 13, color: t[2], bold: true, align: "center", margin: 0 });
  });
  s.addText("贯穿主线：能在控制台点鼠标完成的，就不让你敲命令", { x: 0.9, y: 6.2, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 14, color: "9FB3D1", italic: true, margin: 0 });
}

// =================== 2. 本章目标 & 学习主线 ===================
{
  const s = p.addSlide(); content(s, "OBJECTIVE", "本章目标 & 学习主线"); pg(s);
  card(s, 0.85, 1.5, 6.0, 5.2);
  s.addText([
    { text: "目标：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 16 } },
    { text: "把第 5 章在本地跑通的整套项目（3 个后端 + Vue 前端 + MySQL + Nacos），原封不动搬到阿里云上运行。", options: { color: INK, breakLine: true, fontSize: 14 } },
    { text: "", options: { breakLine: true, fontSize: 8 } },
    { text: "核心理念：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 16 } },
    { text: "本地做过的每一步，在云上一一对应替换——", options: { color: INK, breakLine: true, fontSize: 14 } },
    { text: "云原生不是重学一套，而是“换个地方跑同一份 jar”。", options: { color: TEAL, bold: true, breakLine: true, fontSize: 14 } },
    { text: "", options: { breakLine: true, fontSize: 8 } },
    { text: "关键洞察：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 16 } },
    { text: "jar 一行不改，只靠环境变量 / 启动参数告诉它连云上的地址。", options: { color: INK, fontSize: 14 } },
  ], { x: 1.1, y: 1.75, w: 5.5, h: 4.7, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });

  // 右：本地 vs 云上
  card(s, 7.1, 1.5, 5.4, 5.2);
  s.addText("关键升级：一机三进程 → 一机一微服务", { x: 7.3, y: 1.7, w: 5.0, h: 0.5, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  box(s, 7.4, 2.4, 2.2, 2.5, "EEF3FA", BLUE, "");
  s.addText([{ text: "本地（第5章）", options: { bold: true, fontSize: 12, color: BLUE, breakLine: true } }, { text: "一台笔记本\n跑 3 个进程\n靠端口区分", options: { fontSize: 11, color: INK } }], { x: 7.4, y: 2.55, w: 2.2, h: 2.2, align: "center", valign: "middle", margin: 0 });
  arrow(s, 9.75, 3.65, 0.6, 0, AMBER);
  ["ECS-1 userorder", "ECS-2 expresstrack", "ECS-3 app + Nginx"].forEach((t, i) => {
    box(s, 10.5, 2.4 + i * 0.88, 1.9, 0.72, "FFF6E6", AMBER, t, { fs: 10.5 });
  });
  s.addText("共用：MSE Nacos + RDS MySQL", { x: 7.4, y: 5.05, w: 5.0, h: 0.4, fontFace: FONT, fontSize: 11, color: SUB, italic: true, align: "center", margin: 0 });
  s.addText("故障隔离 · 独立扩容 · 独立发布 —— 这才是微服务该有的样子", { x: 7.3, y: 5.55, w: 5.1, h: 0.9, fontFace: FONT, fontSize: 12, color: TEAL, bold: true, valign: "top", margin: 0 });
}

// =================== 3. 三种上云形态总览 ===================
{
  const s = p.addSlide(); content(s, "OVERVIEW", "三种上云形态：责任从重到轻"); pg(s);
  const cols = [
    ["路径 A · IaaS", "ECS 自管", ["你装系统/JDK", "你 scp 上传 jar", "你写脚本起进程", "你看日志排障"], "控制力最强 · 运维最重", GRAY],
    ["路径 B · PaaS", "EDAS 托管", ["你上传 jar", "平台装 JDK", "平台起/重启进程", "治理可视化"], "省心 · 有服务治理", AMBER],
    ["路径 C · Serverless", "SAE 托管", ["你提交代码/ jar", "平台打包伸缩", "按需自动伸缩", "按 vCPU·秒计费"], "最省运维 · 弹性", CYAN],
  ];
  cols.forEach((c, i) => {
    const x = 0.85 + i * 4.16, w = 3.9;
    card(s, x, 1.55, w, 4.55);
    s.addShape(p.shapes.RECTANGLE, { x, y: 1.55, w, h: 0.7, fill: { color: c[4] } });
    s.addText(c[0], { x: x + 0.1, y: 1.6, w: w - 0.2, h: 0.4, fontFace: FONT, fontSize: 16, color: i === 0 ? "FFFFFF" : "1C2B3A", bold: true, align: "center", margin: 0 });
    s.addText(c[1], { x: x + 0.1, y: 2.35, w: w - 0.2, h: 0.4, fontFace: FONT, fontSize: 14, color: NAVY, bold: true, align: "center", margin: 0 });
    s.addText(c[2].map((t, j) => ({ text: t, options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 6 } })), { x: x + 0.35, y: 2.85, w: w - 0.6, h: 2.3, valign: "top", margin: 0 });
    s.addText(c[3], { x: x + 0.1, y: 5.5, w: w - 0.2, h: 0.5, fontFace: FONT, fontSize: 11.5, color: c[4] === CYAN ? TEAL : c[4], bold: true, align: "center", valign: "middle", margin: 0 });
  });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 6.35, w: 11.65, h: 0.65, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "核心权衡：", options: { bold: true, color: NAVY } }, { text: "越往下，运维负担越轻，但对底层的控制力也越弱。", options: { color: INK } }], { x: 1.0, y: 6.35, w: 11.3, h: 0.65, fontFace: FONT, fontSize: 13.5, valign: "middle", margin: 0 });
}

// =================== 4. 本课程主线（点鼠标） ===================
{
  const s = p.addSlide(); content(s, "THE MAIN LINE", "本课程主线：能点鼠标，就不敲命令", AMBER); pg(s);
  s.addText("初学者习惯用鼠标而不是键盘——凡控制台能点点生成的服务，一律优先；命令行只作进阶备选。", { x: 0.9, y: 1.45, w: 11.6, h: 0.5, fontFace: FONT, fontSize: 14, color: SUB, margin: 0 });
  const rows = [
    ["⭐ 推荐主线", "路径 B · EDAS", "控制台点鼠标部署，不用 SSH、不装 JDK、不写脚本", AMBER],
    ["推荐（弹性场景）", "路径 C · SAE", "控制台点鼠标，Serverless 按需自动伸缩、免运维", CYAN],
    ["🔧 进阶 / 选读", "路径 A · ECS", "终端敲命令：装 JDK、scp、chmod、manage.sh", GRAY],
  ];
  rows.forEach((r, i) => {
    const y = 2.15 + i * 1.45;
    card(s, 0.85, y, 11.65, 1.25);
    s.addShape(p.shapes.RECTANGLE, { x: 0.85, y, w: 0.14, h: 1.25, fill: { color: r[3] } });
    s.addText(r[0], { x: 1.1, y: y + 0.12, w: 2.7, h: 1.0, fontFace: FONT, fontSize: 16, color: r[3] === CYAN ? TEAL : r[3], bold: true, valign: "middle", margin: 0 });
    s.addText(r[1], { x: 3.9, y: y + 0.12, w: 3.0, h: 1.0, fontFace: FONT, fontSize: 18, color: NAVY, bold: true, valign: "middle", margin: 0 });
    s.addText(r[2], { x: 7.0, y: y + 0.12, w: 5.3, h: 1.0, fontFace: FONT, fontSize: 13.5, color: INK, valign: "middle", margin: 0 });
  });
  s.addText("学习顺序建议：先做点击式 EDAS 跑通建立信心 → 想懂底层再回头做路径 A。", { x: 0.9, y: 6.65, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: TEAL, bold: true, italic: true, margin: 0 });
}

// =================== 5. 必懂 4 概念 ===================
{
  const s = p.addSlide(); content(s, "FOUNDATIONS", "动手前必懂的 4 个概念"); pg(s);
  const items = [
    ["1", "VPC 专有网络", "ECS / RDS / Nacos 同一 VPC 走内网：快、免费、安全。铁律：同地域、同 VPC。", BLUE],
    ["2", "ENV 开关", "spring.profiles.active=${ENV:dev}；本地走 dev，启动加 -DENV=prod 切到生产配置。", TEAL],
    ["3", "环境变量驱动", "${DB_HOST:默认值} 等——同一份 jar，靠启动参数注入云上地址，配置与代码分离。", CYAN],
    ["4", "注册中心 vs 配置中心", "都用 Nacos 但用途不同：Dubbo 用它找服务；Spring 用它拉配置（本项目 optional，可不连）。", AMBER],
  ];
  items.forEach((it, i) => {
    const x = 0.85 + (i % 2) * 5.95, y = 1.6 + Math.floor(i / 2) * 2.55;
    card(s, x, y, 5.7, 2.3);
    circle(s, x + 0.3, y + 0.32, 0.7, it[3], it[0]);
    s.addText(it[1], { x: x + 1.2, y: y + 0.32, w: 4.3, h: 0.7, fontFace: FONT, fontSize: 18, color: NAVY, bold: true, valign: "middle", margin: 0 });
    s.addText(it[2], { x: x + 0.35, y: y + 1.15, w: 5.0, h: 1.0, fontFace: FONT, fontSize: 13.5, color: INK, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
  });
}

// =================== 6. 公共云资源准备 ===================
{
  const s = p.addSlide(); content(s, "RESOURCES", "公共云资源准备（A/B/C 都要）"); pg(s);
  const res = [
    ["3 台 ECS", "每台跑 1 个微服务\nECS-1/2/3 · 2核2G", BLUE],
    ["RDS MySQL", "云数据库 8.0\n两个库 userorder/express", TEAL],
    ["MSE Nacos", "注册 + 配置中心\n开发测试版即可", CYAN],
  ];
  res.forEach((r, i) => {
    const x = 0.85 + i * 4.16;
    card(s, x, 1.6, 3.9, 2.2);
    s.addShape(p.shapes.RECTANGLE, { x: x + 0.3, y: 1.85, w: 0.16, h: 0.5, fill: { color: r[2] } });
    s.addText(r[0], { x: x + 0.55, y: 1.82, w: 3.2, h: 0.55, fontFace: FONT, fontSize: 19, color: NAVY, bold: true, valign: "middle", margin: 0 });
    s.addText(r[1], { x: x + 0.35, y: 2.5, w: 3.3, h: 1.1, fontFace: FONT, fontSize: 13, color: INK, valign: "top", margin: 0 });
  });
  // 铁律 + 内外网
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 4.05, w: 11.65, h: 0.8, fill: { color: "FBE9D0" }, line: { color: AMBER, width: 1.25 }, rectRadius: 0.06 });
  s.addText([{ text: "🔥 铁律：", options: { bold: true, color: "B5630A" } }, { text: "ECS / RDS / MSE Nacos 必须同一地域、同一 VPC——创建后大多不可改，错了只能删了重建。", options: { color: INK } }], { x: 1.05, y: 4.05, w: 11.3, h: 0.8, fontFace: FONT, fontSize: 14, valign: "middle", margin: 0 });
  card(s, 0.85, 5.1, 11.65, 1.85);
  s.addText("内外网怎么用（最常踩的坑）", { x: 1.05, y: 5.25, w: 11, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "云上应用连 RDS / Nacos → 一律用内网地址（快、免费、安全）", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 6 } },
    { text: "你本地电脑灌数据 / 调试 → 临时用外网地址 + 白名单（教学权宜）", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 6 } },
    { text: "RDS / MSE 各有独立白名单（与 VPC 无关）：SAE 实例 IP 从 VSwitch 网段动态分配，要把整个网段加进白名单", options: { bullet: { code: "2022" }, color: TEAL, bold: true, fontSize: 13.5 } },
  ], { x: 1.3, y: 5.7, w: 10.9, h: 1.15, valign: "top", margin: 0 });
}

// =================== 6b. 创建 MSE Nacos 实例（A/B/C 共用前提）===================
{
  const s = p.addSlide(); content(s, "RESOURCES", "创建 MSE Nacos 实例（A/B/C 共用前提）"); pg(s);
  s.addText("三条路径的 Dubbo 服务都注册到这同一个 Nacos——动手前必须先把它建出来（详见 06 章 Part 3）。", { x: 0.9, y: 1.4, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: SUB, margin: 0 });
  card(s, 0.85, 1.95, 6.5, 4.65);
  s.addText("① 进 MSE 控制台建 Nacos 实例", { x: 1.05, y: 2.1, w: 6.1, h: 0.4, fontFace: FONT, fontSize: 15, color: TEAL, bold: true, margin: 0 });
  s.addText("搜“微服务引擎 MSE”→ 左侧“注册配置中心”→“创建实例”：", { x: 1.05, y: 2.52, w: 6.1, h: 0.35, fontFace: FONT, fontSize: 11.5, color: SUB, margin: 0 });
  ftable(s, 1.05, 2.95, 6.2, [
    ["引擎类型", "Nacos", INK, true],
    ["版本", "2.x"],
    ["实例规格", "开发测试版（够课程用）", "B5630A", false, true],
    ["地域 / VPC", "与 ECS / RDS 完全一致", INK, true],
    ["公网访问", "勾上（本地调试用）"],
  ], { fs: 11.5, rowH: 0.46, colW: [1.9, 4.3] });
  s.addText("② 等状态“运行中”→ 详情页记下内网 + 公网两个接入地址", { x: 1.05, y: 5.45, w: 6.2, h: 0.4, fontFace: FONT, fontSize: 12.5, color: NAVY, bold: true, valign: "top", margin: 0 });
  s.addText("内网地址给云上 ECS/SAE 用（快、免费）；公网地址给本地调试（需在“网络配置→公网白名单”填 0.0.0.0/0，仅教学，生产禁用）。", { x: 1.05, y: 5.9, w: 6.2, h: 0.7, fontFace: FONT, fontSize: 11, color: INK, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
  photoBox(s, 7.55, 1.95, 4.95, 4.65, "MSE 创建 Nacos 表单（引擎 Nacos / 2.x / 开发测试版 / 同 VPC）");
  s.addText("🔥 MSE 必须和 ECS / RDS 同地域、同 VPC，否则云上走内网连不通。", { x: 0.9, y: 6.72, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12, color: "B5630A", bold: true, italic: true, margin: 0 });
}

// =================== 7. 项目模块：3库+3应用 ===================
{
  const s = p.addSlide(); content(s, "MODULES", "项目模块：3 个库 + 3 个应用"); pg(s);
  // 库
  card(s, 0.85, 1.6, 5.4, 2.4, "EEF3FA");
  s.addText("库模块（被依赖，不独立运行）", { x: 1.05, y: 1.75, w: 5.0, h: 0.4, fontFace: FONT, fontSize: 14, color: BLUE, bold: true, margin: 0 });
  ["coffee-common · 公共工具", "coffee-userorder/api · 订单接口", "coffee-expresstrack/api · 快递接口"].forEach((t, i) => {
    box(s, 1.1, 2.25 + i * 0.55, 4.9, 0.45, "FFFFFF", BLUE, t, { fs: 11.5, color: INK });
  });
  // 应用
  card(s, 6.55, 1.6, 5.95, 2.4, "FFF6E6");
  s.addText("应用模块（打成 jar 部署运行）", { x: 6.75, y: 1.75, w: 5.5, h: 0.4, fontFace: FONT, fontSize: 14, color: "B5630A", bold: true, margin: 0 });
  ["coffee-userorder/provider → ECS-1", "coffee-expresstrack/provider → ECS-2", "coffee-app（网关）→ ECS-3"].forEach((t, i) => {
    box(s, 6.8, 2.25 + i * 0.55, 5.5, 0.45, "FFFFFF", AMBER, t, { fs: 11.5, color: INK });
  });
  // 制品流
  card(s, 0.85, 4.25, 11.65, 2.55);
  s.addText("制品流：库是“中转站”，应用构建时来拉", { x: 1.05, y: 4.4, w: 11, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  box(s, 1.2, 5.1, 2.6, 1.0, "EEF3FA", BLUE, "3 个库模块", { fs: 13 });
  arrow(s, 3.9, 5.6, 0.9, 0, TEAL, "mvn deploy");
  box(s, 4.9, 5.1, 3.0, 1.0, "EAF1F8", TEAL, "云效私有仓库\n(任何机器可拉)", { fs: 12 });
  arrow(s, 8.0, 5.6, 0.9, 0, TEAL, "构建时拉取");
  box(s, 9.0, 5.1, 3.3, 1.0, "FFF6E6", AMBER, "应用 mvn package\n→ 3 个 jar → 上云", { fs: 12 });
  s.addText("⭐ 日常迭代推荐交给云效流水线自动构建，本地 mvn 命令属进阶/理解原理", { x: 1.05, y: 6.35, w: 11.3, h: 0.4, fontFace: FONT, fontSize: 12, color: "B5630A", bold: true, margin: 0 });
}

// =================== 8. 路径B EDAS 总览（推荐主线）===================
{
  const s = p.addSlide(); content(s, "PATH B · 推荐主线", "路径 B：EDAS 部署全流程（6 步点鼠标）", AMBER); pg(s);
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 1.4, w: 11.65, h: 0.58, fill: { color: "FBE9D0" }, line: { color: AMBER, width: 1 }, rectRadius: 0.06 });
  s.addText("✅ 本课程默认上云方式：全程 EDAS 网页控制台点鼠标——不用 SSH、不装 JDK、不写 manage.sh。", { x: 1.05, y: 1.4, w: 11.3, h: 0.58, fontFace: FONT, fontSize: 13, color: INK, bold: true, valign: "middle", margin: 0 });
  const steps = ["①开 Agent\n通信端口", "②建微服务\n空间", "③建 ECS 集群\n导入 3 台", "④建应用\n上传 jar", "⑤填 JVM\n参数", "⑥验证\n服务治理"];
  steps.forEach((t, i) => {
    const x = 0.85 + i * 2.04;
    const hot = (i === 0);
    box(s, x, 2.65, 1.78, 1.25, hot ? "FBE9D0" : "EAF1F8", hot ? AMBER : TEAL, t, { fs: 11.5, color: hot ? "B5630A" : INK });
    if (i < steps.length - 1) arrow(s, x + 1.79, 3.27, 0.23, 0, NAVY);
  });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 4.3, w: 11.65, h: 0.78, fill: { color: "FFF6E6" }, line: { color: AMBER, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "① 最容易漏：", options: { bold: true, color: "B5630A" } }, { text: "ECS 安全组“出方向”不放通 8442/8443/8883，应用看似部署成功，但“微服务治理”页永远“没有数据”。", options: { color: INK } }], { x: 1.05, y: 4.3, w: 11.3, h: 0.78, fontFace: FONT, fontSize: 13, valign: "middle", margin: 0 });
  s.addText("和路径 A 比：路径 A 你自己装 JDK、scp、起进程；路径 B 这些 EDAS 全自动，你只在网页上选“把 X 应用部署到 ECS-Y”。后面 5 页逐步配。", { x: 0.9, y: 5.3, w: 11.6, h: 0.8, fontFace: FONT, fontSize: 12.5, color: SUB, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
}

// =================== 8b. 端口 + 微服务空间 ===================
{
  const s = p.addSlide(); content(s, "PATH B · 步骤 ①②", "开 Agent 端口 + 建微服务空间", AMBER); pg(s);
  card(s, 0.85, 1.4, 5.7, 5.1);
  s.addText("① 给 3 台 ECS 开 Agent 出方向端口", { x: 1.05, y: 1.55, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 14.5, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "ECS 控制台 → ECS 的“安全组”→ 访问规则 → 出方向 → 手动添加", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "加 3 条 TCP（授权对象 0.0.0.0/0）：", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 6 } },
    { text: "8442/8442 · 8443/8443 · 8883/8883", options: { color: NAVY, bold: true, fontSize: 13, breakLine: true, paraSpaceAfter: 8, fontFace: MONO } },
    { text: "3 台都做（共用一个安全组就只做一次）", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5 } },
  ], { x: 1.3, y: 2.05, w: 5.1, h: 2.4, valign: "top", margin: 0 });
  s.addText("不开 → 治理页永远“没有数据”。", { x: 1.3, y: 4.45, w: 5.1, h: 0.4, fontFace: FONT, fontSize: 12, color: "B5630A", bold: true, margin: 0 });
  card(s, 6.8, 1.4, 5.7, 5.1, "FFF6E6");
  s.addText("② 创建微服务空间", { x: 7.0, y: 1.55, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 14.5, color: "B5630A", bold: true, margin: 0 });
  s.addText("EDAS 控制台 → 资源管理 → 微服务空间 → 创建：", { x: 7.0, y: 2.0, w: 5.3, h: 0.35, fontFace: FONT, fontSize: 12, color: SUB, margin: 0 });
  ftable(s, 7.0, 2.4, 5.3, [
    ["空间名称", "coffee-prod", INK, true],
    ["地域", "跟 ECS 同地域"],
    ["注册中心类型", "MSE Nacos", "B5630A", false, true],
    ["MSE 实例", "选你创建的那个实例"],
  ], { fs: 11.5, rowH: 0.5, colW: [1.7, 3.6] });
  s.addText("⚠ 必须选 MSE Nacos，不能用 EDAS 默认注册中心——它不支持 Dubbo 3.x，治理页查不到服务。", { x: 7.0, y: 5.15, w: 5.3, h: 1.2, fontFace: FONT, fontSize: 12, color: "B5630A", bold: true, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
}

// =================== 8c. ECS 集群 + 导入 ===================
{
  const s = p.addSlide(); content(s, "PATH B · 步骤 ③", "创建 ECS 集群 + 导入 3 台 ECS", AMBER); pg(s);
  card(s, 0.85, 1.45, 6.4, 5.1);
  s.addText("创建 EDAS ECS 集群", { x: 1.05, y: 1.6, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  s.addText("资源管理 → EDAS ECS 集群 → 创建集群：", { x: 1.05, y: 2.05, w: 6.0, h: 0.35, fontFace: FONT, fontSize: 12.5, color: SUB, margin: 0 });
  ftable(s, 1.05, 2.45, 6.1, [
    ["集群名称", "coffeecluster", INK, true],
    ["微服务空间", "coffee-prod（刚建的）", INK, true],
    ["网络类型", "专有网络"],
    ["VPC", "与你 ECS 相同的那个"],
  ], { fs: 12, rowH: 0.48, colW: [1.9, 4.2] });
  s.addText("导入主机：进集群详情 → 添加 ECS 实例 → 勾 ECS-1/2/3 → 下一步 → 导入。", { x: 1.05, y: 5.05, w: 6.1, h: 0.9, fontFace: FONT, fontSize: 13, color: INK, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
  s.addText("等 3-5 分钟，3 台显示“运行中”+“已安装 Agent”。", { x: 1.05, y: 5.95, w: 6.1, h: 0.5, fontFace: FONT, fontSize: 12.5, color: TEAL, bold: true, valign: "top", margin: 0 });
  photoBox(s, 7.5, 1.45, 5.0, 5.1, "EDAS 集群详情：3 台 ECS 已安装 Agent");
}

// =================== 8d. 创建应用 userorder + JVM ===================
{
  const s = p.addSlide(); content(s, "PATH B · 步骤 ④⑤", "创建应用 userorder + 填 JVM 参数", AMBER); pg(s);
  s.addText("应用管理 → 应用列表 → 创建应用，按下表填：", { x: 0.9, y: 1.3, w: 11.6, h: 0.35, fontFace: FONT, fontSize: 12.5, color: SUB, margin: 0 });
  ftable(s, 0.85, 1.7, 7.4, [
    ["应用名称", "userorder", INK, true],
    ["微服务空间", "coffee-prod"],
    ["集群", "coffeecluster"],
    ["运行环境 / JDK", "Java / OpenJDK 17", INK, true],
    ["部署包来源", "上传 JAR 包"],
    ["选择文件", "coffee-userorder/provider/target/*.jar", INK, true],
    ["部署 ECS", "只勾 ECS-1（另两台不勾）", "B5630A", false, true],
  ], { fs: 11.5, rowH: 0.47, colW: [2.2, 5.2] });
  card(s, 8.5, 1.7, 4.0, 3.5, "FFF6E6");
  s.addText("⑤ JVM 参数（填实际 RDS 值）", { x: 8.7, y: 1.85, w: 3.6, h: 0.6, fontFace: FONT, fontSize: 13, color: "B5630A", bold: true, valign: "top", margin: 0 });
  codeBox(s, 8.65, 2.5, 3.75, 2.5, "-Xms128m -Xmx512m\n-DENV=prod\n-DDB_HOST=\n  rm-xxx:3306\n-DDB_USER=\n  userordertest\n-DDB_PASSWORD=\n  你的RDS密码");
  s.addText("确认创建 → 等 2-3 分钟状态变“运行中”。注册地址由 Agent 自动接管，JVM 里不用写 Nacos 地址。", { x: 0.9, y: 5.75, w: 11.6, h: 0.9, fontFace: FONT, fontSize: 12.5, color: TEAL, bold: true, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
}

// =================== 8e. 快递+网关 + 验证 ===================
{
  const s = p.addSlide(); content(s, "PATH B · 步骤 ④⑥", "部署快递 / 网关 + 验证服务治理", AMBER); pg(s);
  card(s, 0.85, 1.4, 5.7, 2.6, "EAF1F8");
  s.addText("快递 expresstrack（改 4 处）", { x: 1.05, y: 1.55, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 14, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "应用名 expresstrack；jar 选 expresstrack 的", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "部署 ECS 只勾 ECS-2", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "JVM 参数与 userorder 完全一样（库名已硬编码）", options: { bullet: { code: "2022" }, color: INK, fontSize: 12 } },
  ], { x: 1.3, y: 2.05, w: 5.1, h: 1.8, valign: "top", margin: 0 });
  card(s, 6.8, 1.4, 5.7, 2.6, "FFF6E6");
  s.addText("网关 coffee-app", { x: 7.0, y: 1.55, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 14, color: "B5630A", bold: true, margin: 0 });
  s.addText([
    { text: "应用名 coffee-app；jar 选 coffee-app 的", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "部署 ECS 只勾 ECS-3", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "JVM 只填 -Xms128m -Xmx256m -DENV=prod（网关不连库）", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, fontFace: MONO } },
  ], { x: 7.25, y: 2.05, w: 5.1, h: 1.8, valign: "top", margin: 0 });
  s.addText("顺序：先 userorder / expresstrack，最后 coffee-app（反了网关会有几秒“找不到提供者”警告，不影响）。", { x: 0.9, y: 4.15, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12, color: SUB, italic: true, margin: 0 });
  card(s, 0.85, 4.7, 11.65, 1.85);
  s.addText("⑥ 验证服务治理", { x: 1.05, y: 4.85, w: 11, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "微服务治理 → Dubbo → 服务查询 → 顶部“所属微服务空间”选 coffee-prod", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 7 } },
    { text: "看到 com.coffee.yun.userorder.api.OrderService 等接口 → 点进“提供者”看到 ECS 私网 IP = 成功", options: { bullet: { code: "2022" }, color: TEAL, bold: true, fontSize: 13 } },
  ], { x: 1.3, y: 5.3, w: 10.9, h: 1.15, valign: "top", margin: 0 });
}

// =================== 9. 路径C SAE ===================
{
  const s = p.addSlide(); content(s, "PATH C · Serverless", "路径 C：SAE（连 ECS 都不用买）", CYAN); pg(s);
  card(s, 0.85, 1.55, 5.7, 5.0);
  s.addText("它怎么省", { x: 1.05, y: 1.7, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 16, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "不买 ECS，平台按需弹出“无服务器实例”跑 jar", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "实例数最小为 1（创建时“实例数”字段就是 1，填不了 0）", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "按 vCPU·秒 + 内存 GB·秒计费（用多少付多少）；公网 CLB 另计费", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "命名空间选 MSE Nacos，部署=上传 jar + 填环境变量", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "公网入口：给 coffee-app 加“公网 CLB 访问”（SAE 2.0 现称 CLB，经典版叫 SLB）", options: { bullet: { code: "2022" }, color: INK, fontSize: 13.5 } },
  ], { x: 1.3, y: 2.25, w: 5.1, h: 4.1, valign: "top", margin: 0 });
  card(s, 6.8, 1.55, 5.7, 5.0, "FFF8EC");
  s.addText("两个关键坑", { x: 7.0, y: 1.7, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 16, color: "B5630A", bold: true, margin: 0 });
  s.addText([
    { text: "网络坑：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 14 } },
    { text: "RDS / MSE 各有独立的访问白名单，和 VPC 无关——就算同 VPC、网络通，源 IP 不在白名单照样连不上。SAE 实例 IP 从 VSwitch 网段动态分配（弹性 ENI、会变），没有固定 IP，所以要把整个 VSwitch 网段加进 RDS / MSE 白名单。", options: { color: INK, breakLine: true, fontSize: 13.5, paraSpaceAfter: 10 } },
    { text: "弹性坑：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 14 } },
    { text: "三个应用都保持至少 1 个常驻实例；Dubbo provider 尤其不能被弹性策略回收，否则调用方瞬间报 RPC 错误。", options: { color: INK, fontSize: 13.5 } },
  ], { x: 7.05, y: 2.25, w: 5.1, h: 4.1, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
}

// =================== 9b. SAE 配置全流程总览 ===================
{
  const s = p.addSlide(); content(s, "PATH C · 详解", "SAE 配置全流程（6 步，全程点鼠标）", CYAN); pg(s);
  const steps = ["①开通 SAE", "②建命名空间", "③加白名单\n(VSwitch)", "④部署 3 应用", "⑤公网 CLB", "⑥验证"];
  steps.forEach((t, i) => {
    const x = 0.85 + i * 2.04;
    const hot = (i === 2); // 白名单是关键坑
    box(s, x, 3.0, 1.78, 1.3, hot ? "FBE9D0" : "EAF1F8", hot ? AMBER : CYAN, t, { fs: 12.5, color: hot ? "B5630A" : INK });
    if (i < steps.length - 1) arrow(s, x + 1.79, 3.65, 0.23, 0, NAVY);
  });
  s.addText("③ 加白名单是 SAE 区别于路径 A/B 的最大坑——本节用一整页专门讲（见下页）。", { x: 0.9, y: 4.6, w: 11.6, h: 0.5, fontFace: FONT, fontSize: 14, color: "B5630A", bold: true, margin: 0 });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.3, w: 11.65, h: 1.4, fill: { color: "FFFFFF" }, line: { color: "E2E8F2", width: 1 }, rectRadius: 0.08, shadow: shadow() });
  s.addText([
    { text: "和路径 B（EDAS）比，SAE 的不同：", options: { bold: true, color: NAVY, breakLine: true, fontSize: 14 } },
    { text: "不买 ECS（平台按需弹实例）· 按 vCPU·秒计费用多少付多少 · 但实例走弹性网卡 ENI，必须额外加白名单 · 部署 = 上传 jar + 填环境变量（不用 JVM 写 DB）。", options: { color: INK, fontSize: 13.5 } },
  ], { x: 1.1, y: 5.5, w: 11.2, h: 1.05, valign: "top", lineSpacingMultiple: 1.15, margin: 0 });
}

// =================== 9c. 哪些模块适合 SAE ===================
{
  const s = p.addSlide(); content(s, "PATH C · 详解", "先想清楚：哪些模块适合 Serverless", CYAN); pg(s);
  const hdr = (t) => ({ text: t, options: { fill: { color: NAVY }, color: "FFFFFF", bold: true, fontSize: 14, align: "center", valign: "middle" } });
  const c = (t, col, b) => ({ text: t, options: { color: col || INK, fontSize: 13, align: "center", valign: "middle", fontFace: FONT, bold: !!b } });
  const l = (t) => ({ text: t, options: { color: INK, fontSize: 12.5, align: "left", valign: "middle", fontFace: FONT } });
  s.addTable([
    [hdr("模块"), hdr("适合 SAE 吗"), hdr("原因"), hdr("本课程配比")],
    [c("coffee-app（HTTP 网关）", NAVY, true), c("✅ 很合适", TEAL, true), l("HTTP 触发、无状态，可随流量弹性扩容"), c("最小 1 实例", TEAL, true)],
    [c("userorder-provider（Dubbo）", NAVY, true), c("⚠️ 能跑但反直觉", "B5630A"), l("长连接 RPC，实例被回收时调用方瞬间报错"), c("最小 1 实例", TEAL, true)],
    [c("expresstrack-provider（Dubbo）", NAVY, true), c("⚠️ 同上", "B5630A"), l("同上"), c("最小 1 实例", TEAL, true)],
  ], { x: 0.85, y: 1.7, w: 11.65, colW: [3.4, 2.3, 3.95, 2.0], rowH: 0.95, border: { pt: 1, color: "D6DEE8" }, fill: { color: "FFFFFF" }, valign: "middle" });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.8, w: 11.65, h: 0.95, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "教学配比：", options: { bold: true, color: NAVY } }, { text: "三个应用都用 1 个实例（provider 必须常驻保住 Dubbo 注册，网关也至少 1 个）。SAE 的价值是免运维 + 按 vCPU·秒计费 + 可按指标弹性扩容，而非缩到 0。生产里 provider 应继续用 EDAS/ECS。", options: { color: INK } }], { x: 1.05, y: 5.8, w: 11.3, h: 0.95, fontFace: FONT, fontSize: 13.5, valign: "middle", margin: 0 });
}

// =================== 9d. 内外网三条铁律（最大坑）===================
{
  const s = p.addSlide(); content(s, "PATH C · 关键", "⚠️ 最大的坑：内外网三条铁律", AMBER); pg(s);
  const rules = [
    ["1", "永远用内网地址连 RDS / Nacos", "rm-xxx.mysql.rds.aliyuncs.com:3306 这种内网地址。公网慢、收费、还要申请——SAE 本就在 VPC 里，走内网才对。", TEAL],
    ["2", "本地灌数据才用外网地址", "你电脑在公网，第 5 章/资源准备时用 RDS 外网地址 + MySQL Workbench 灌表结构。这一步不必再开外网。", BLUE],
    ["3", "RDS / MSE 白名单要放行 SAE 的 VSwitch 网段", "白名单是独立于 VPC 的访问控制：同 VPC 网络通，但源 IP 不在白名单仍被拒。SAE 实例 IP 从 VSwitch 网段动态分配、会变，把该 VSwitch 整个网段补进 RDS 和 MSE 白名单，否则启动报连不上。", AMBER],
  ];
  rules.forEach((r, i) => {
    const y = 1.6 + i * 1.62;
    card(s, 0.85, y, 11.65, 1.42, i === 2 ? "FFF8EC" : CARD);
    s.addShape(p.shapes.RECTANGLE, { x: 0.85, y, w: 0.14, h: 1.42, fill: { color: r[3] } });
    circle(s, 1.05, y + 0.42, 0.6, r[3], r[0]);
    s.addText(r[1], { x: 1.85, y: y + 0.12, w: 10.4, h: 0.5, fontFace: FONT, fontSize: 16, color: NAVY, bold: true, margin: 0 });
    s.addText(r[2], { x: 1.85, y: y + 0.6, w: 10.5, h: 0.75, fontFace: FONT, fontSize: 13, color: INK, valign: "top", lineSpacingMultiple: 1.05, margin: 0 });
  });
  s.addText("一句话：地址用内网的，白名单加 SAE 网段的。", { x: 0.9, y: 6.62, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 14, color: "B5630A", bold: true, italic: true, margin: 0 });
}

// =================== 9e. 第1步 开通SAE + 建命名空间 ===================
{
  const s = p.addSlide(); content(s, "PATH C · 步骤 ①②", "开通 SAE + 创建命名空间", CYAN); pg(s);
  s.addText("注册中心选 MSE Nacos——3 个 SAE 应用和本地、路径 A/B 用同一个 Nacos，Dubbo 才能互相发现。", { x: 0.9, y: 1.45, w: 11.6, h: 0.45, fontFace: FONT, fontSize: 13, color: SUB, margin: 0 });
  card(s, 0.85, 2.05, 6.4, 4.6);
  s.addText("① 开通 SAE 服务", { x: 1.05, y: 2.2, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 17, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "控制台搜 “SAE” → 进 Serverless 应用引擎", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 9 } },
    { text: "首次进勾选服务协议、开通（开通不收费，跑实例才计费）", options: { bullet: { code: "2022" }, color: INK, fontSize: 14 } },
  ], { x: 1.3, y: 2.7, w: 5.8, h: 1.3, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
  s.addText("② 创建命名空间", { x: 1.05, y: 4.05, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 17, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "左侧“命名空间”→“创建命名空间”", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 9 } },
    { text: "名称 coffee-prod-sae；地域与 ECS/RDS/MSE 一致", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 9 } },
    { text: "微服务注册中心选 MSE Nacos → 选你的实例", options: { bullet: { code: "2022" }, color: TEAL, bold: true, fontSize: 14 } },
  ], { x: 1.3, y: 4.55, w: 5.8, h: 1.9, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
  photoBox(s, 7.5, 2.05, 5.0, 4.6, "SAE 开通页 / 命名空间创建表单（注册中心选 MSE Nacos）");
}

// =================== 9f. 第②.5步 白名单加 VSwitch（关键）===================
{
  const s = p.addSlide(); content(s, "PATH C · 关键步骤", "③ RDS + MSE 白名单加 SAE 的 VSwitch 网段", AMBER); pg(s);
  s.addText("这一步做错，应用一定起不来报“连不上 MySQL / Nacos”。务必在部署前做完。", { x: 0.9, y: 1.45, w: 11.6, h: 0.45, fontFace: FONT, fontSize: 14, color: "B5630A", bold: true, margin: 0 });
  // 先查网段
  card(s, 0.85, 2.05, 11.65, 1.35);
  circle(s, 1.1, 2.35, 0.6, NAVY, "查");
  s.addText("先查 SAE 要用的 VSwitch 网段", { x: 1.9, y: 2.15, w: 10.3, h: 0.45, fontFace: FONT, fontSize: 16, color: NAVY, bold: true, margin: 0 });
  s.addText("VPC 控制台 → 交换机 → 在 ECS 同 VPC 里挑一个可用 IP ≥ 64 的 VSwitch → 复制它的 CIDR 网段（形如 172.16.1.0/24）。后面部署应用也选这个 VSwitch。", { x: 1.9, y: 2.62, w: 10.4, h: 0.7, fontFace: FONT, fontSize: 13, color: INK, valign: "top", margin: 0 });
  // 两处白名单
  card(s, 0.85, 3.6, 3.75, 2.95, "EAF1F8");
  s.addText("改 RDS 白名单", { x: 1.05, y: 3.75, w: 3.4, h: 0.4, fontFace: FONT, fontSize: 15, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "实例 → 数据安全性 → 白名单设置", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "点默认分组“修改”", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "逗号追加 VSwitch 网段 → 确定", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5 } },
  ], { x: 1.25, y: 4.25, w: 3.25, h: 2.2, valign: "top", margin: 0 });
  card(s, 4.75, 3.6, 3.75, 2.95, "EAF1F8");
  s.addText("改 MSE 内网白名单", { x: 4.95, y: 3.75, w: 3.4, h: 0.4, fontFace: FONT, fontSize: 15, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "实例详情 → 网络配置", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "“内网白名单”→“修改”", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "追加同一个 VSwitch 网段 → 确定", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 8 } },
    { text: "路径 B 不用这步（EDAS 用你的 ECS，已在默认白名单）", options: { color: "B5630A", bold: true, fontSize: 11 } },
  ], { x: 4.95, y: 4.25, w: 3.4, h: 2.2, valign: "top", margin: 0 });
  photoBox(s, 8.65, 3.6, 3.85, 2.95, "RDS / MSE 白名单设置页：追加 VSwitch 网段");
}

// =================== 9g. 第④步 部署订单服务 ===================
{
  const s = p.addSlide(); content(s, "PATH C · 步骤 ④", "部署订单服务（创建应用 + 环境变量）", CYAN); pg(s);
  s.addText("创建应用：名称 userorder-sae · 命名空间 coffee-prod-sae · VPC 同 ECS · VSwitch 选已加白名单那个 · 实例数 1 · 0.5C/1G · 部署方式 JDK17 + 上传 jar", { x: 0.9, y: 1.45, w: 11.6, h: 0.7, fontFace: FONT, fontSize: 13, color: SUB, valign: "top", margin: 0 });
  s.addText("关键：环境变量用 RDS 内网地址（不是外网！）", { x: 0.9, y: 2.25, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 15, color: NAVY, bold: true, margin: 0 });
  const hdr = (t) => ({ text: t, options: { fill: { color: NAVY }, color: "FFFFFF", bold: true, fontSize: 13.5, align: "center", valign: "middle" } });
  const k = (t) => ({ text: t, options: { color: NAVY, bold: true, fontSize: 13, align: "center", valign: "middle", fontFace: MONO } });
  const v = (t) => ({ text: t, options: { color: INK, fontSize: 13, align: "left", valign: "middle", fontFace: MONO } });
  const n = (t, c) => ({ text: t, options: { color: c || INK, fontSize: 12.5, align: "left", valign: "middle", fontFace: FONT } });
  s.addTable([
    [hdr("变量名"), hdr("值"), hdr("说明")],
    [k("ENV"), v("prod"), n("触发 prod 配置")],
    [k("DB_HOST"), v("rm-xxx:3306"), n("RDS 内网地址", "B5630A")],
    [k("DB_USER"), v("userordertest"), n("准备时建的账号")],
    [k("DB_PASSWORD"), v("你的 RDS 密码"), n("准备时记下的")],
  ], { x: 0.85, y: 2.7, w: 7.4, colW: [1.8, 2.9, 2.7], rowH: 0.62, border: { pt: 1, color: "D6DEE8" }, fill: { color: "FFFFFF" }, valign: "middle" });
  photoBox(s, 8.45, 2.7, 4.05, 3.1, "SAE 创建应用：环境变量填写表单");
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.95, w: 11.65, h: 0.85, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "JVM 参数只填 ", options: { color: INK } }, { text: "-Xms256m -Xmx512m", options: { fontFace: MONO, color: NAVY, bold: true } }, { text: "；看实时日志见 HikariPool Start completed + Nacos 注册成功 = 连上了。", options: { color: INK } }], { x: 1.05, y: 5.95, w: 11.3, h: 0.85, fontFace: FONT, fontSize: 13, valign: "middle", margin: 0 });
}

// =================== 9h. 第⑤步 部署快递 + 网关（各 1 实例）===================
{
  const s = p.addSlide(); content(s, "PATH C · 步骤 ⑤", "部署快递 + 网关（实例数各 1）", CYAN); pg(s);
  card(s, 0.85, 1.55, 3.7, 2.4, "EAF1F8");
  s.addText("快递 expresstrack-sae", { x: 1.05, y: 1.7, w: 3.4, h: 0.4, fontFace: FONT, fontSize: 14.5, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "复制订单做法，只改 4 处", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "上传 expresstrack 的 jar", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "环境变量不用改（同一 RDS）", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "实例数 1（和 provider 一样常驻）", options: { bullet: { code: "2022" }, color: TEAL, bold: true, fontSize: 12 } },
  ], { x: 1.25, y: 2.18, w: 3.25, h: 1.7, valign: "top", margin: 0 });
  card(s, 4.75, 1.55, 3.7, 2.4, "FFF6E6");
  s.addText("网关 coffee-app-sae", { x: 4.95, y: 1.7, w: 3.4, h: 0.4, fontFace: FONT, fontSize: 14.5, color: "B5630A", bold: true, margin: 0 });
  s.addText([
    { text: "上传 coffee-app 的 jar", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "环境变量只填 ENV=prod", options: { bullet: { code: "2022" }, color: INK, fontSize: 12, breakLine: true, paraSpaceAfter: 6 } },
    { text: "实例数 1（部署后看实时日志）", options: { bullet: { code: "2022" }, color: INK, fontSize: 12 } },
  ], { x: 4.95, y: 2.18, w: 3.4, h: 1.7, valign: "top", margin: 0 });
  photoBox(s, 8.65, 1.55, 3.85, 2.4, "SAE 应用创建表单（实例数 1）");
  card(s, 0.85, 4.15, 11.65, 2.4);
  s.addText("可选：配弹性伸缩，按流量自动加实例（最小仍为 1）", { x: 1.05, y: 4.3, w: 11, h: 0.4, fontFace: FONT, fontSize: 16, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "应用详情 → 应用扩缩 → 弹性伸缩 → 添加指标弹性策略", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 8 } },
    { text: "最小实例数 1 · 最大实例数 3 · 触发指标 CPU > 60% → 保存（流量大自动加实例，回落到 1）", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 8 } },
    { text: "⚠ 创建和弹性里“实例数”最小都是 1，填不了 0。阿里云宣传的“缩容到 0”是 SAE 2.0 高级能力，本课程不用。", options: { color: "B5630A", bold: true, fontSize: 14 } },
  ], { x: 1.3, y: 4.75, w: 10.9, h: 1.7, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
}

// =================== 9i. 第⑥步 暴露公网 CLB ===================
{
  const s = p.addSlide(); content(s, "PATH C · 步骤 ⑥", "给网关加公网 CLB 入口", CYAN); pg(s);
  s.addText("SAE 应用默认没有公网 IP。前端静态页有 ECS-3 公网 IP，唯独 coffee-app 困在 VPC 里、浏览器又要直接调它——所以公网入口加在 coffee-app（SAE 2.0 现称 CLB，旧称 SLB），不是加在前端。", { x: 0.9, y: 1.45, w: 11.6, h: 0.5, fontFace: FONT, fontSize: 13, color: SUB, valign: "top", lineSpacingMultiple: 1.05, margin: 0 });
  card(s, 0.85, 2.05, 6.6, 4.25);
  s.addText("操作（只对 coffee-app-sae 做）", { x: 1.05, y: 2.2, w: 6.2, h: 0.4, fontFace: FONT, fontSize: 16, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "进 coffee-app-sae 详情 → 应用访问设置", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 10 } },
    { text: "公网访问 → 添加公网 CLB 访问", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 10 } },
    { text: "CLB 实例：新建（让 SAE 自动代购）", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 10 } },
    { text: "协议 HTTP · CLB 端口 80 · 容器端口 8005", options: { bullet: { code: "2022" }, color: NAVY, bold: true, fontSize: 14, breakLine: true, paraSpaceAfter: 10 } },
    { text: "确定 → 约 1 分钟返回公网 IP/域名，记下来", options: { bullet: { code: "2022" }, color: INK, fontSize: 14 } },
  ], { x: 1.3, y: 2.7, w: 6.0, h: 3.5, valign: "top", margin: 0 });
  photoBox(s, 7.7, 2.05, 4.8, 4.25, "应用访问设置：添加公网 CLB（HTTP 80 → 容器 8005）");
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 6.45, w: 11.65, h: 0.55, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "记住：", options: { bold: true, color: NAVY } }, { text: "这个 CLB 域名 = 替代“ECS-3 公网 IP”的新入口，前端 API 指向它；两个 provider 不做这步，只走 Dubbo 内部调用、不暴露公网。", options: { color: INK } }], { x: 1.05, y: 6.45, w: 11.3, h: 0.55, fontFace: FONT, fontSize: 12, valign: "middle", margin: 0 });
}

// =================== 9j. 验证 + 常见报错 ===================
{
  const s = p.addSlide(); content(s, "PATH C · 验证 & 排错", "验证上线 + 3 个最常见报错", CYAN); pg(s);
  card(s, 0.85, 1.55, 6.4, 2.3, "EAF1F8");
  s.addText("怎么算成功", { x: 1.05, y: 1.7, w: 6.0, h: 0.4, fontFace: FONT, fontSize: 16, color: TEAL, bold: true, margin: 0 });
  s.addText([
    { text: "访问 http://<CLB 公网IP>/actuator/health 返回 {\"status\":\"UP\"}", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 10 } },
    { text: "MSE Nacos 服务列表看到 3 个服务（2 个 Dubbo 接口 + coffee-app）", options: { bullet: { code: "2022" }, color: INK, fontSize: 13, breakLine: true, paraSpaceAfter: 10 } },
    { text: "看日志去 SAE 控制台“实时日志”；账单按 vCPU·秒 + GB·秒计，用多少付多少", options: { bullet: { code: "2022" }, color: SUB, fontSize: 12.5 } },
  ], { x: 1.3, y: 2.2, w: 5.85, h: 1.55, valign: "top", lineSpacingMultiple: 1.1, margin: 0 });
  photoBox(s, 7.45, 1.55, 5.05, 2.3, "验证截图：health 返回 UP / Nacos 服务列表 3 个服务");
  // 三个报错
  const errs = [
    ["Communications link failure（连不上 RDS）", "99% 是 RDS 白名单没加 SAE 的 VSwitch 网段 → 回第 ③ 步补上。"],
    ["Failed to connect to Nacos", "同根因：MSE 内网白名单没加 VSwitch 网段 → 回第 ③ 步补上。"],
    ["新部署/重启后首次访问等 5–15 秒", "Serverless 实例冷启动正常现象（现拉镜像、启 JVM）。教学演示前先 curl 一次预热。"],
  ];
  errs.forEach((e, i) => {
    const y = 4.05 + i * 0.92;
    card(s, 0.85, y, 11.65, 0.8);
    s.addShape(p.shapes.RECTANGLE, { x: 0.85, y, w: 0.12, h: 0.8, fill: { color: AMBER } });
    s.addText("✗ " + e[0], { x: 1.1, y, w: 4.7, h: 0.8, fontFace: FONT, fontSize: 12.5, color: "B5630A", bold: true, valign: "middle", margin: 0 });
    s.addText(e[1], { x: 5.9, y, w: 6.45, h: 0.8, fontFace: FONT, fontSize: 12.5, color: INK, valign: "middle", margin: 0 });
  });
}

// =================== 10. 路径A ECS（进阶）===================
{
  const s = p.addSlide(); content(s, "PATH A · 进阶 / 选读", "路径 A：ECS 自管（命令行，进阶）", GRAY); pg(s);
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 1.45, w: 11.65, h: 0.65, fill: { color: "ECEFF3" }, line: { color: GRAY, width: 1 }, rectRadius: 0.06 });
  s.addText("🔧 进阶/选读：要在终端敲一连串命令。给想搞懂“jar 到底怎么被 java -jar 跑起来”的同学。初学者先做路径 B。", { x: 1.05, y: 1.45, w: 11.3, h: 0.65, fontFace: FONT, fontSize: 13, color: INK, bold: true, valign: "middle", margin: 0 });
  const steps = [
    ["装 JDK 17", "apt/dnf 装 openjdk-17，建 ~/coffee 目录"],
    ["上传文件", "会话管理 📤 传 jar + 配置到对应 ECS"],
    ["改 NACOS_ADDR", "manage.sh 顶部填自己的 MSE Nacos 地址"],
    ["启动服务", "./manage.sh start（自动跳过本机没有的 jar）"],
    ["开安全组", "ECS-1:7001 / ECS-2:8001 / ECS-3:8005+80"],
    ["日常管理", "manage.sh status / logs / restart / stop"],
  ];
  steps.forEach((st, i) => {
    const x = 0.85 + (i % 2) * 5.95, y = 2.3 + Math.floor(i / 2) * 1.5;
    card(s, x, y, 5.7, 1.3);
    s.addShape(p.shapes.RECTANGLE, { x, y, w: 0.12, h: 1.3, fill: { color: GRAY } });
    s.addText(st[0], { x: x + 0.3, y: y + 0.15, w: 5.2, h: 0.5, fontFace: FONT, fontSize: 16, color: NAVY, bold: true, margin: 0 });
    s.addText(st[1], { x: x + 0.3, y: y + 0.66, w: 5.2, h: 0.55, fontFace: MONO, fontSize: 12, color: SUB, margin: 0 });
  });
}

// =================== 11. 前端① 本地构建 ===================
{
  const s = p.addSlide(); content(s, "FRONTEND · STEP 1", "前端①：本地构建 dist（告诉它后端地址）", TEAL); pg(s);
  s.addText("三条路径前端都一样，只有构建时 VUE_APP_BASE_URL 指向的后端地址不同。", { x: 0.9, y: 1.4, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: SUB, margin: 0 });
  s.addText("进 app-admin/ 设环境变量再构建（按你的路径选地址）：", { x: 0.9, y: 1.9, w: 11.6, h: 0.35, fontFace: FONT, fontSize: 13, color: NAVY, bold: true, margin: 0 });
  codeBox(s, 0.85, 2.35, 7.4, 1.5, "cd app-admin\n# Windows CMD：\nset VUE_APP_BASE_URL=http://x.x.x.x:8005\nnpm run build   # 生成 app-admin/dist/");
  ftable(s, 8.5, 2.35, 4.0, [
    ["路径 A / B", "ECS-3 公网 IP:8005", INK, true],
    ["路径 C", "SAE 公网 CLB 域名", INK, true],
  ], { fs: 11, rowH: 0.55, colW: [1.4, 2.6], vh: "地址填" });
  s.addText("PowerShell 用 $env:VUE_APP_BASE_URL=\"...\"；Mac/Linux 用 export VUE_APP_BASE_URL=...", { x: 0.9, y: 4.0, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 11.5, color: SUB, italic: true, margin: 0 });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 4.55, w: 11.65, h: 0.95, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "再压成 zip：", options: { bold: true, color: NAVY } }, { text: "右键 dist 文件夹 → 压缩，得到 dist.zip（一次传一个文件最稳）。下一页上传到 ECS-3。", options: { color: INK } }], { x: 1.05, y: 4.55, w: 11.3, h: 0.95, fontFace: FONT, fontSize: 13, valign: "middle", margin: 0 });
  s.addText("VUE_APP_BASE_URL 会被永久写进打包后的 JS；不设默认 localhost:8005（用户浏览器会找自己电脑，失败）。", { x: 0.9, y: 5.7, w: 11.6, h: 0.5, fontFace: FONT, fontSize: 12, color: "B5630A", bold: true, valign: "top", margin: 0 });
}

// =================== 11b. 前端② 部署到 ECS-3 ===================
{
  const s = p.addSlide(); content(s, "FRONTEND · STEP 2", "前端②：部署到 ECS-3（两种方式）", TEAL); pg(s);
  card(s, 0.85, 1.4, 5.7, 4.0, "FFF6E6");
  s.addText("方式 A · 宝塔面板  ⭐ 推荐", { x: 1.05, y: 1.55, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 14.5, color: "B5630A", bold: true, margin: 0 });
  s.addText([
    { text: "买 ECS-3 时镜像市场选预装“宝塔面板”镜像", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 7 } },
    { text: "面板登录 → 网站 → 添加站点（域名先填 ECS-3 公网 IP）", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 7 } },
    { text: "进站点“文件”→ 上传 dist.zip → 右键解压 → 内容放到站点根目录", options: { bullet: { code: "2022" }, color: INK, fontSize: 12.5, breakLine: true, paraSpaceAfter: 7 } },
    { text: "站点设置 → 伪静态 → 选 Vue/history（刷新不 404）", options: { bullet: { code: "2022" }, color: TEAL, bold: true, fontSize: 12.5 } },
  ], { x: 1.3, y: 2.05, w: 5.1, h: 3.2, valign: "top", margin: 0 });
  card(s, 6.8, 1.4, 5.7, 4.0);
  s.addText("方式 B · 命令行  🔧 进阶（裸镜像）", { x: 7.0, y: 1.55, w: 5.3, h: 0.4, fontFace: FONT, fontSize: 14, color: GRAY, bold: true, margin: 0 });
  s.addText("会话管理上传 dist.zip + deploy/coffee-admin.conf 到 /root/，整段粘：", { x: 7.0, y: 2.0, w: 5.3, h: 0.5, fontFace: FONT, fontSize: 11.5, color: SUB, valign: "top", margin: 0 });
  codeBox(s, 7.0, 2.55, 5.3, 2.05, "apt install -y nginx unzip   # 阿里云镜像用 dnf\nmkdir -p /var/www/coffee-admin\nunzip -o /root/dist.zip -d /var/www/coffee-admin\ncp /root/coffee-admin.conf \\\n   /etc/nginx/conf.d/\nnginx -t && systemctl enable --now nginx");
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 5.6, w: 11.65, h: 0.95, fill: { color: "EAF1F8" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
  s.addText([{ text: "最后开端口 + 验证：", options: { bold: true, color: NAVY } }, { text: "ECS-3 安全组入方向放通 80（三路径都要）→ 浏览器开 http://<ECS-3 IP> 看到登录页 ✅。为什么不用 OSS：复用已学 Nginx，免去 OSS 的 Bucket 权限/404 兜底。（跨域由网关 @CrossOrigin 放行）", options: { color: INK } }], { x: 1.05, y: 5.6, w: 11.3, h: 0.95, fontFace: FONT, fontSize: 12.5, valign: "middle", margin: 0 });
}

// =================== 12. 架构回顾 A/B ===================
{
  const s = p.addSlide(); content(s, "ARCHITECTURE · A/B", "架构回顾：路径 A / B 拓扑"); pg(s);
  box(s, 5.4, 1.55, 2.5, 0.6, "EEF3FA", BLUE, "用户浏览器", { fs: 13 });
  arrow(s, 6.65, 2.15, 0, 0.45, TEAL, "http :80");
  box(s, 4.4, 2.75, 4.5, 0.85, "FFF6E6", AMBER, "ECS-3 · Nginx:80 (前端) + coffee-app:8005", { fs: 12 });
  arrow(s, 6.65, 3.6, 0, 0.4, TEAL);
  s.addText("Dubbo RPC（VPC 内网）", { x: 7.0, y: 3.62, w: 3.0, h: 0.3, fontFace: FONT, fontSize: 10, color: SUB, margin: 0 });
  box(s, 3.6, 4.15, 3.0, 0.8, "EAF1F8", TEAL, "ECS-1 · userorder:7001", { fs: 12 });
  box(s, 6.9, 4.15, 3.0, 0.8, "EAF1F8", TEAL, "ECS-2 · expresstrack:8001", { fs: 12 });
  arrow(s, 5.1, 4.95, 0.9, 0.55, BLUE);
  arrow(s, 8.4, 4.95, -0.9, 0.55, BLUE);
  box(s, 5.4, 5.65, 2.5, 0.75, "E7EEF6", NAVY, "RDS MySQL", { fs: 13 });
  s.addText("JDBC（VPC 内网）", { x: 2.5, y: 5.45, w: 2.2, h: 0.3, fontFace: FONT, fontSize: 10, color: SUB, align: "center", margin: 0 });
  // Nacos 侧
  box(s, 10.6, 4.15, 2.2, 0.8, "F0F4F9", CYAN, "MSE Nacos", { fs: 13 });
  s.addText("3 台 ECS 都注册到\n同一 Nacos 互相发现", { x: 10.5, y: 5.0, w: 2.4, h: 0.7, fontFace: FONT, fontSize: 10.5, color: SUB, align: "center", valign: "top", margin: 0 });
  s.addText("前端 + 网关在 ECS-3，两个 provider 各占一台，全部在同一 VPC 内网互通。", { x: 0.9, y: 6.65, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12.5, color: TEAL, bold: true, margin: 0 });
}

// =================== 13. 架构回顾 C ===================
{
  const s = p.addSlide(); content(s, "ARCHITECTURE · C", "架构回顾：路径 C（SAE）拓扑", CYAN); pg(s);
  box(s, 5.4, 1.5, 2.5, 0.6, "EEF3FA", BLUE, "用户浏览器", { fs: 13 });
  arrow(s, 4.0, 2.1, -1.0, 0.55, TEAL);
  arrow(s, 9.0, 2.1, 1.0, 0.55, AMBER);
  s.addText("① 取前端", { x: 2.0, y: 2.35, w: 1.7, h: 0.3, fontFace: FONT, fontSize: 11, color: TEAL, bold: true, align: "right", margin: 0 });
  s.addText("③ API 请求", { x: 9.95, y: 2.35, w: 1.9, h: 0.3, fontFace: FONT, fontSize: 11, color: "B5630A", bold: true, align: "left", margin: 0 });
  box(s, 1.6, 2.75, 3.4, 0.8, "FFF6E6", AMBER, "ECS-3 · Nginx 托管前端", { fs: 12 });
  box(s, 8.6, 2.75, 3.6, 0.8, "EAF1F8", TEAL, "SAE 公网 CLB（API 入口）", { fs: 12 });
  arrow(s, 10.4, 3.55, 0, 0.4, TEAL);
  box(s, 8.6, 3.95, 3.6, 0.75, "F0F4F9", TEAL, "SAE · coffee-app（实例数 1）", { fs: 12 });
  arrow(s, 10.4, 4.7, 0, 0.35, TEAL, "Dubbo");
  box(s, 7.4, 5.05, 2.3, 0.75, "EAF1F8", TEAL, "SAE userorder", { fs: 11.5 });
  box(s, 9.9, 5.05, 2.4, 0.75, "EAF1F8", TEAL, "SAE expresstrack", { fs: 11.5 });
  arrow(s, 9.7, 5.8, -1.6, 0.45, BLUE);
  box(s, 6.5, 6.25, 2.5, 0.7, "E7EEF6", NAVY, "RDS MySQL", { fs: 12.5 });
  box(s, 1.6, 4.4, 3.0, 0.75, "F0F4F9", CYAN, "MSE Nacos", { fs: 12.5 });
  s.addText("3 个 SAE 应用注册到同一 Nacos", { x: 1.5, y: 5.2, w: 3.2, h: 0.4, fontFace: FONT, fontSize: 10.5, color: SUB, align: "center", margin: 0 });
  s.addText("变的只是“服务跑在哪”和“API 入口”；前端在 ECS-3、Nacos/RDS/Dubbo 关系都不变。", { x: 0.9, y: 6.95, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 12, color: TEAL, bold: true, margin: 0 });
}

// =================== 14. 三路径对比速查 ===================
{
  const s = p.addSlide(); content(s, "COMPARE", "三路径对比速查表"); pg(s);
  const hdr = (t) => ({ text: t, options: { fill: { color: NAVY }, color: "FFFFFF", bold: true, fontSize: 13, align: "center", valign: "middle" } });
  const cell = (t, c) => ({ text: t, options: { color: c || INK, fontSize: 12, align: "center", valign: "middle", fontFace: FONT } });
  const rows = [
    [hdr("维度"), hdr("A · ECS 🔧进阶"), hdr("B · EDAS ⭐推荐"), hdr("C · SAE")],
    [cell("操作方式", NAVY), cell("终端敲命令", GRAY), cell("控制台点鼠标", "B5630A"), cell("控制台点鼠标", TEAL)],
    [cell("谁装 JDK / 起进程", NAVY), cell("你自己"), cell("EDAS 平台"), cell("SAE 平台")],
    [cell("服务治理可视化", NAVY), cell("✗（看日志）"), cell("✓ EDAS 治理页"), cell("有限")],
    [cell("按用量计费", NAVY), cell("✗（ECS 包月）"), cell("✗（ECS 包月）"), cell("✓ vCPU·秒")],
    [cell("配置传参", NAVY), cell("命令行参数+YAML"), cell("JVM 参数+profile"), cell("环境变量")],
    [cell("本课程定位", NAVY), cell("进阶/选读", GRAY), cell("推荐主线", "B5630A"), cell("推荐(弹性)", TEAL)],
  ];
  s.addTable(rows, { x: 0.85, y: 1.7, w: 11.65, colW: [2.65, 3.0, 3.0, 3.0], rowH: 0.62, border: { pt: 1, color: "D6DEE8" }, fill: { color: "FFFFFF" }, valign: "middle" });
  s.addText("一句话：学技术看底层选 A；企业生产 + 服务治理选 B（默认）；流量稀疏想省钱选 C。", { x: 0.9, y: 6.55, w: 11.6, h: 0.4, fontFace: FONT, fontSize: 13, color: TEAL, bold: true, italic: true, margin: 0 });
}

// =================== 15. 进阶：云效流水线 ===================
{
  const s = p.addSlide(); content(s, "CI/CD · 第12章", "正式做法：云效流水线（点击式）", AMBER); pg(s);
  s.addText("手工“打 jar→上传→启动”是为了理解原理；日常迭代一律走流水线——git push 后网页点几下全自动。", { x: 0.9, y: 1.45, w: 11.6, h: 0.5, fontFace: FONT, fontSize: 14, color: SUB, margin: 0 });
  const flow = ["git push", "云效拉代码", "自动 mvn 构建 jar", "发布到 EDAS/SAE/ECS", "前端发布到 ECS-3"];
  flow.forEach((t, i) => {
    const x = 0.85 + i * 2.5;
    box(s, x, 2.4, 2.25, 1.1, i === 0 ? "FFF6E6" : "EAF1F8", i === 0 ? AMBER : TEAL, t, { fs: 12.5 });
    if (i < flow.length - 1) arrow(s, x + 2.27, 2.95, 0.2, 0, NAVY);
  });
  s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.85, y: 4.1, w: 11.65, h: 2.6, fill: { color: "FFFFFF" }, line: { color: "E2E8F2", width: 1 }, rectRadius: 0.08, shadow: shadow() });
  s.addText("它替你做了第 11 章哪些手工活", { x: 1.1, y: 4.3, w: 11, h: 0.4, fontFace: FONT, fontSize: 16, color: NAVY, bold: true, margin: 0 });
  s.addText([
    { text: "本地 mvn deploy/package  →  流水线“Java 构建上传”自动跑", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 9 } },
    { text: "会话管理 📤 上传 jar  →  “主机部署 / EDAS 发布 / SAE 发布”自动调", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 9 } },
    { text: "终端 ./manage.sh restart  →  部署任务里自动执行", options: { bullet: { code: "2022" }, color: INK, fontSize: 14, breakLine: true, paraSpaceAfter: 9 } },
    { text: "推荐后端走路径 B（EDAS）流水线作默认 —— 全程零手敲命令", options: { bullet: { code: "2022" }, color: "B5630A", bold: true, fontSize: 14 } },
  ], { x: 1.35, y: 4.8, w: 10.9, h: 1.8, valign: "top", margin: 0 });
}

// =================== 16. 小结 ===================
{
  const s = p.addSlide();
  s.background = { color: NAVY };
  s.addShape(p.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.22, fill: { color: AMBER } });
  s.addText("本章小结", { x: 0.9, y: 0.7, w: 11, h: 0.7, fontFace: FONT, fontSize: 34, color: "FFFFFF", bold: true, margin: 0 });
  const pts = [
    ["同一份 jar，换地方跑", "代码 0 行改动，靠环境变量注入云上地址——这就是云原生“环境无关”。"],
    ["主线：能点鼠标就不敲命令", "默认走 EDAS（点击部署）；路径 A 命令行降为进阶/选读。"],
    ["前端统一 ECS-3 Nginx", "推荐预装宝塔镜像点鼠标建站；不用 OSS，复用已学 Nginx。"],
    ["正式迭代走云效流水线", "git push 自动构建发布，把手工每一步交给机器。"],
  ];
  pts.forEach((pt, i) => {
    const y = 1.75 + i * 1.28;
    s.addShape(p.shapes.ROUNDED_RECTANGLE, { x: 0.9, y, w: 11.5, h: 1.1, fill: { color: "112E54" }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
    circle(s, 1.15, y + 0.28, 0.55, AMBER, String(i + 1));
    s.addText(pt[0], { x: 1.95, y: y + 0.12, w: 4.4, h: 0.85, fontFace: FONT, fontSize: 17, color: "FFFFFF", bold: true, valign: "middle", margin: 0 });
    s.addText(pt[1], { x: 6.4, y: y + 0.12, w: 5.9, h: 0.85, fontFace: FONT, fontSize: 13, color: "CADCFC", valign: "middle", margin: 0 });
  });
}

const out = process.env.OUT || "deck/第11章-上云部署-课件.pptx";
p.writeFile({ fileName: out }).then(() => console.log("WROTE", out));
