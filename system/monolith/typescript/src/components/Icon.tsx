// The few Lucide icons (ISC licence, https://lucide.dev) the Market storefront uses, as inline SVG: the same
// icons frontend-react renders with lucide-react. They are decorative (aria-hidden) and carry no text, so they
// never change the text content the UI drivers read.

type Shape =
  | { kind: "path"; d: string }
  | { kind: "circle"; cx: number; cy: number; r: number }
  | { kind: "line"; x1: number; y1: number; x2: number; y2: number }
  | { kind: "rect"; x: number; y: number; width: number; height: number; rx: number };

const path = (d: string): Shape => ({ kind: "path", d });
const circle = (cx: number, cy: number, r: number): Shape => ({ kind: "circle", cx, cy, r });

const SHAPES = {
  "arrow-left": [path("m12 19-7-7 7-7"), path("M19 12H5")],
  "arrow-right": [path("M5 12h14"), path("m12 5 7 7-7 7")],
  check: [path("M20 6 9 17l-5-5")],
  "chevron-right": [path("m9 18 6-6-6-6")],
  "circle-alert": [
    circle(12, 12, 10),
    { kind: "line", x1: 12, y1: 8, x2: 12, y2: 12 },
    { kind: "line", x1: 12, y1: 16, x2: 12.01, y2: 16 },
  ],
  "circle-check": [circle(12, 12, 10), path("m16 9-5.5 5.5L8 12")],
  lock: [{ kind: "rect", x: 3, y: 11, width: 18, height: 11, rx: 2 }, path("M7 11V7a5 5 0 0 1 10 0v4")],
  "log-out": [path("m16 17 5-5-5-5"), path("M21 12H9"), path("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4")],
  minus: [path("M5 12h14")],
  "package-search": [
    path("M12 22V12"),
    path("M20.27 18.27 22 20"),
    path("M21 10.498V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.729l7 4a2 2 0 0 0 2 .001l.98-.559"),
    path("M3.29 7 12 12l8.71-5"),
    path("m7.5 4.27 8.997 5.148"),
    circle(18.5, 16.5, 2.5),
  ],
  plus: [path("M5 12h14"), path("M12 5v14")],
  "refresh-cw": [
    path("M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"),
    path("M21 3v5h-5"),
    path("M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"),
    path("M8 16H3v5"),
  ],
  "shield-alert": [
    path(
      "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
    ),
    path("M12 8v4"),
    path("M12 16h.01"),
  ],
  "shopping-cart": [
    path("m2.05 2.05 1.099-.028a1 1 0 0 1 1.008.815l2.69 14.347A1 1 0 0 0 7.83 18H18"),
    path("M4.563 5h16.435a1 1 0 0 1 .981 1.204l-1.026 6.226A2 2 0 0 1 18.962 14H6.25"),
    circle(18, 20, 2),
    circle(8, 20, 2),
  ],
  "sliders-horizontal": [
    path("M10 5H3"),
    path("M12 19H3"),
    path("M14 3v4"),
    path("M16 17v4"),
    path("M21 12h-9"),
    path("M21 19h-5"),
    path("M21 5h-7"),
    path("M8 10v4"),
    path("M8 12H3"),
  ],
  "trash-2": [
    path("M10 11v6"),
    path("M14 11v6"),
    path("M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"),
    path("M3 6h18"),
    path("M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
  ],
  "triangle-alert": [
    path("m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"),
    path("M12 9v4"),
    path("M12 17h.01"),
  ],
  truck: [
    path("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"),
    path("M15 18H9"),
    path("M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14"),
    circle(17, 18, 2),
    circle(7, 18, 2),
  ],
} satisfies Record<string, Shape[]>;

export type IconName = keyof typeof SHAPES;

function renderShape(shape: Shape, key: number) {
  switch (shape.kind) {
    case "path":
      return <path key={key} d={shape.d} />;
    case "circle":
      return <circle key={key} cx={shape.cx} cy={shape.cy} r={shape.r} />;
    case "line":
      return <line key={key} x1={shape.x1} y1={shape.y1} x2={shape.x2} y2={shape.y2} />;
    case "rect":
      return <rect key={key} x={shape.x} y={shape.y} width={shape.width} height={shape.height} rx={shape.rx} ry={shape.rx} />;
  }
}

interface IconProps {
  name: IconName;
  size?: number;
  strokeWidth?: number;
  className?: string;
}

/** The named Lucide icon as an aria-hidden inline SVG. */
export function Icon({ name, size = 16, strokeWidth = 2, className }: Readonly<IconProps>) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      {SHAPES[name].map(renderShape)}
    </svg>
  );
}
