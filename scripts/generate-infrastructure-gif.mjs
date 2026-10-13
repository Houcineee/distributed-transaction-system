import { mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const assetDir = resolve(root, "frontend/src/assets/infrastructure");
const framesDir = resolve(assetDir, ".frames");
const frames = 36;
const svgInner = (source) => source.replace(/^<svg[^>]*>/, "").replace(/<\/svg>\s*$/, "");
const serviceLogos = Object.fromEntries(
  ["react", "vite", "nginx", "springboot", "postgresql", "redis", "apachekafka", "prometheus", "grafana", "loki", "promtail"]
    .map((name) => {
      const source = readFileSync(resolve(assetDir, "logos", `${name}.svg`), "utf8");
      return [name, { fill: source.match(/<svg[^>]*fill="([^"]+)"/)?.[1] ?? "#FFFFFF", inner: svgInner(source) }];
    }),
);
const cineLogo = svgInner(readFileSync(resolve(root, "frontend/public/cine-logo.svg"), "utf8"));

mkdirSync(framesDir, { recursive: true });
rmSync(framesDir, { recursive: true, force: true });
mkdirSync(framesDir, { recursive: true });

const clamp = (value) => Math.max(0, Math.min(1, value));
const ease = (value) => {
  const x = clamp(value);
  return x * x * (3 - 2 * x);
};
const phase = (progress, start, end) => ease((progress - start) / (end - start));
const visible = (progress, start, end = 1) => clamp((progress - start) / 0.055) * clamp((end - progress) / 0.055);
const point = (from, to, value) => ({ x: from.x + (to.x - from.x) * value, y: from.y + (to.y - from.y) * value });

function logo(name, x, y, size = 42) {
  const source = serviceLogos[name];
  return `<svg x="${x}" y="${y}" width="${size}" height="${size}" viewBox="0 0 24 24" fill="${source.fill}">${source.inner}</svg>`;
}

function fraudShield(x, y) {
  return `<g transform="translate(${x} ${y})"><path d="M21 1L39 8V22C39 33 31.5 40 21 44C10.5 40 3 33 3 22V8Z" fill="#E5B75B" opacity=".18"/><path d="M21 2L38 8.5V22C38 32.5 30.8 39.2 21 43C11.2 39.2 4 32.5 4 22V8.5Z" fill="none" stroke="#E5B75B" stroke-width="2"/><path d="M13 22L18 27L29 16" fill="none" stroke="#F4D58D" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></g>`;
}

function node({ x, y, w, h, label, sublabel, logoName, iconSvg = "", accent = "#D8B46A", active = 0, children = "" }) {
  const glow = active > 0 ? `<circle cx="${x + 22}" cy="${y + 39}" r="31" fill="${accent}" opacity="${0.08 + active * 0.13}" filter="url(#glow)"/>` : "";
  const icon = logoName ? logo(logoName, x, y + 18) : iconSvg;
  const left = logoName || iconSvg ? x + 60 : x;
  return `${glow}<g>
    ${icon}<circle cx="${x + 4}" cy="${y + 69}" r="3" fill="${accent}" opacity="${0.82 + active * 0.18}"/>
    <text x="${left}" y="${y + 37}" class="node-title">${label}</text>
    <text x="${left}" y="${y + 59}" class="node-subtitle">${sublabel}</text>${children}
  </g>`;
}

function packet(p, a, b, color, text, start, end) {
  const v = phase(p, start, end);
  const opacity = visible(p, start, end);
  if (opacity <= 0) return "";
  const pos = point(a, b, v);
  return `<g opacity="${opacity}"><circle cx="${pos.x}" cy="${pos.y}" r="8" fill="${color}" filter="url(#packetGlow)"/><circle cx="${pos.x}" cy="${pos.y}" r="3" fill="#FFFFFF"/>
    <rect x="${pos.x + 15}" y="${pos.y - 27}" width="${text.length * 7 + 18}" height="22" rx="7" fill="#111B30" stroke="${color}" stroke-opacity=".55"/>
    <text x="${pos.x + 24}" y="${pos.y - 12}" class="packet-label">${text}</text></g>`;
}

function frameSvg(index) {
  const p = index / frames;
  const appActive = Math.max(phase(p, .24, .37), phase(p, .45, .72));
  const dbActive = phase(p, .39, .54);
  const redisActive = phase(p, .33, .48);
  const kafkaActive = phase(p, .57, .72);
  const obsActive = phase(p, .7, .92);
  const pulse = 0.45 + Math.sin(p * Math.PI * 4) * .12;
  const user = { x: 187, y: 451 }, nginx = { x: 402, y: 451 }, api = { x: 655, y: 451 };
  const db = { x: 926, y: 280 }, redis = { x: 926, y: 453 }, kafka = { x: 926, y: 626 };
  const fraud = { x: 1062, y: 574 }, promtail = { x: 1207, y: 695 }, loki = { x: 1387, y: 695 }, prometheus = { x: 1177, y: 280 }, grafana = { x: 1375, y: 280 };
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="900" viewBox="0 0 1600 900">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#071021"/><stop offset=".58" stop-color="#0A1630"/><stop offset="1" stop-color="#101738"/></linearGradient>
    <linearGradient id="gold" x1="0" x2="1"><stop stop-color="#F2D48A"/><stop offset="1" stop-color="#B88A3F"/></linearGradient>
    <filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="8"/></filter>
    <filter id="packetGlow" x="-200%" y="-200%" width="500%" height="500%"><feGaussianBlur stdDeviation="5"/></filter>
    <marker id="arrow" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="4.5" markerHeight="4.5" orient="auto"><path d="M0 0L8 4L0 8Z" fill="#8292AF" opacity=".9"/></marker>
    <style>
      text { font-family: 'Noto Sans', 'DejaVu Sans', sans-serif; } .eyebrow { font-size: 12px; font-weight: 700; letter-spacing: 3.2px; fill: #D8B46A; } .node-title { font-size: 17px; font-weight: 700; fill: #F4F7FE; } .node-subtitle { font-size: 12px; fill: #98A7C2; } .section-label { font-size: 11px; font-weight: 700; letter-spacing: 2px; fill: #8090AE; } .detail { font-size: 12px; fill: #AAB7CD; } .packet-label { font-size: 10px; font-weight: 700; fill: #F6F8FF; letter-spacing: .15px; }
    </style>
  </defs>
  <rect width="1600" height="900" fill="url(#bg)"/>
  <g opacity=".72"><svg x="78" y="56" width="38" height="38" viewBox="0 0 100 100">${cineLogo}</svg><text x="130" y="80" class="eyebrow">CINE BANK · DISTRIBUTED TRANSACTION SYSTEM</text></g>
  <text x="80" y="192" class="section-label">CUSTOMER EXPERIENCE</text><text x="338" y="192" class="section-label">SECURE INGRESS</text><text x="580" y="192" class="section-label">TRANSACTION CORE</text><text x="860" y="192" class="section-label">STATE &amp; EVENTS</text><text x="1155" y="192" class="section-label">OBSERVABILITY</text>
  <g fill="none" stroke-linecap="round" marker-end="url(#arrow)"><path d="M260 451H315" stroke="#4B5C79" stroke-width="3"/><path d="M480 451H560" stroke="#4B5C79" stroke-width="3"/><path d="M740 420L850 310" stroke="#4B5C79" stroke-width="3"/><path d="M740 451H850" stroke="#4B5C79" stroke-width="3"/><path d="M740 482L850 610" stroke="#4B5C79" stroke-width="3"/><path d="M1008 615L1020 574" stroke="#4B5C79" stroke-width="3"/><path d="M745 420L1100 280" stroke="#31415F" stroke-width="2" stroke-dasharray="7 9"/><path d="M735 483L1180 695" stroke="#31415F" stroke-width="2" stroke-dasharray="7 9"/><path d="M1250 280H1300" stroke="#4B5C79" stroke-width="3"/><path d="M1330 695H1360" stroke="#4B5C79" stroke-width="3"/><path d="M1377 645V330" stroke="#4B5C79" stroke-width="2" stroke-dasharray="7 9"/></g>
  ${node({x:80,y:400,w:180,h:102,label:"Cine customer",sublabel:"Transfer experience",logoName:"react",accent:"#61DAFB",active:phase(p,0,.16),children:`<text x="80" y="488" class="detail">React + Vite</text>`})}
  ${node({x:330,y:400,w:150,h:102,label:"Nginx",sublabel:"TLS · edge routing",logoName:"nginx",accent:"#009639",active:phase(p,.12,.27)})}
  ${node({x:580,y:365,w:165,h:172,label:"Bank API",sublabel:"Spring Boot",logoName:"springboot",accent:"#6DB33F",active:appActive,children:`<text x="580" y="462" class="detail">JWT · authz</text><text x="580" y="484" class="detail">Idempotency</text><text x="580" y="506" class="detail">Pessimistic TX</text>`})}
  ${node({x:850,y:230,w:158,h:100,label:"PostgreSQL",sublabel:"Accounts + audit",logoName:"postgresql",accent:"#4169E1",active:dbActive,children:`<text x="874" y="309" class="detail">row locks</text>`})}
  ${node({x:850,y:403,w:158,h:100,label:"Redis",sublabel:"Idempotency cache",logoName:"redis",accent:"#FF4438",active:redisActive})}
  ${node({x:850,y:576,w:158,h:100,label:"Kafka",sublabel:"bank.transfers",logoName:"apachekafka",accent:"#F7F9FF",active:kafkaActive,children:`<text x="850" y="664" class="detail">durable outbox</text>`})}
  ${node({x:1020,y:535,w:150,h:100,label:"Fraud engine",sublabel:"velocity checks · 60s",iconSvg:fraudShield(1020,553),accent:"#E5B75B",active:phase(p,.71,.94),children:`<text x="1020" y="623" class="detail">Kafka consumer</text>`})}
  ${node({x:1100,y:230,w:155,h:100,label:"Prometheus",sublabel:"metrics scrape",logoName:"prometheus",accent:"#E6522C",active:obsActive})}
  ${node({x:1300,y:230,w:155,h:100,label:"Grafana",sublabel:"dashboards",logoName:"grafana",accent:"#F46800",active:phase(p,.84,.99)})}
  ${node({x:1180,y:645,w:155,h:100,label:"Promtail",sublabel:"container logs",logoName:"promtail",accent:"#B2CCFF",active:obsActive})}
  ${node({x:1360,y:645,w:155,h:100,label:"Loki",sublabel:"log index",logoName:"loki",accent:"#B2CCFF",active:phase(p,.79,.98)})}
  <g opacity="${pulse}"><circle cx="770" cy="451" r="5" fill="#D8B46A"/><circle cx="820" cy="451" r="3" fill="#D8B46A"/></g>
  ${packet(p,user,nginx,"#61DAFB","POST /api/v1/transfers",0,.14)}
  ${packet(p,nginx,api,"#D8B46A","secure ingress",.14,.28)}
  ${packet(p,api,redis,"#FF4438","Idempotency-Key",.28,.42)}
  ${packet(p,api,db,"#6EA0FF","lock · debit · credit",.40,.55)}
  ${packet(p,api,kafka,"#F6F7FA","outbox event",.55,.70)}
  ${packet(p,kafka,fraud,"#E5B75B","transfer event",.69,.82)}
  ${packet(p,api,prometheus,"#E6522C","/actuator/prometheus",.69,.82)}
  ${packet(p,api,promtail,"#B2CCFF","structured logs",.70,.84)}
  ${packet(p,promtail,loki,"#B2CCFF","tail",.82,.91)}
  ${packet(p,prometheus,grafana,"#F46800","metrics",.80,.91)}
  ${packet(p,loki,grafana,"#F46800","inspect",.90,1.04)}
  <g transform="translate(80 805)"><circle cx="7" cy="7" r="5" fill="#D8B46A" filter="url(#packetGlow)"/><text x="24" y="11" class="eyebrow">TRANSFER LIFECYCLE</text><text x="24" y="35" class="detail">Authenticated request → idempotency guard → atomic balance update → durable event → measurable, searchable operation</text><text x="1300" y="16" class="eyebrow">${String(Math.floor(p * 100)).padStart(2, "0")}%</text></g>
</svg>`;
}

for (let index = 0; index < frames; index += 1) {
  const name = String(index).padStart(3, "0");
  const svg = resolve(framesDir, `${name}.svg`);
  writeFileSync(svg, frameSvg(index));
}

console.log(`Created ${frames} SVG frames in ${framesDir}`);
