import React from "react";

interface CineLogoProps {
  size?: number;
  showWordmark?: boolean;
  theme?: "dark" | "light" | "auto";
  className?: string;
}

export const CineSeal: React.FC<{ size?: number; className?: string }> = ({
  size = 32,
  className = "",
}) => {
  const rawId = React.useId().replace(/:/g, "_");
  const gradId = `cineGold_${rawId}`;

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`shrink-0 select-none ${className}`}
    >
      <defs>
        {/* Sovereign Champagne Gold Linear Gradient with instance-unique ID */}
        <linearGradient id={gradId} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#E5CA78" />
          <stop offset="30%" stopColor="#DFC06E" />
          <stop offset="60%" stopColor="#C5A059" />
          <stop offset="85%" stopColor="#A8833E" />
          <stop offset="100%" stopColor="#8A6729" />
        </linearGradient>
      </defs>

      {/* Outer 8 Interlaced Moorish Khatam Lobes with subtle metallic gold fill */}
      <polygon
        points="50,6 61,26 50,37 39,26"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />
      <polygon
        points="81,19 75,40 61,40 61,26"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />
      <polygon
        points="94,50 75,61 64,50 75,39"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />
      <polygon
        points="81,81 61,75 61,60 75,60"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />
      <polygon
        points="50,94 39,74 50,63 61,74"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />
      <polygon
        points="19,81 25,60 39,60 39,74"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />
      <polygon
        points="6,50 25,39 36,50 25,61"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />
      <polygon
        points="19,19 39,26 39,40 25,40"
        stroke={`url(#${gradId})`}
        strokeWidth="3.8"
        strokeLinejoin="round"
        fill={`url(#${gradId})`}
        fillOpacity="0.14"
      />

      {/* Inner Interlacing Geometric Squares */}
      <rect
        x="30"
        y="30"
        width="40"
        height="40"
        stroke={`url(#${gradId})`}
        strokeWidth="3.2"
        fill="none"
        strokeLinejoin="round"
        opacity="0.9"
      />
      <rect
        x="30"
        y="30"
        width="40"
        height="40"
        stroke={`url(#${gradId})`}
        strokeWidth="3.2"
        fill="none"
        strokeLinejoin="round"
        transform="rotate(45 50 50)"
        opacity="0.9"
      />

      {/* Central Solid Vault Diamond Jewel */}
      <polygon
        points="50,38 62,50 50,62 38,50"
        fill={`url(#${gradId})`}
      />
    </svg>
  );
};

export const CineLogo: React.FC<CineLogoProps> = ({
  size = 32,
  showWordmark = true,
  theme = "auto",
  className = "",
}) => {
  const isDark = theme === "dark";

  return (
    <div className={`inline-flex items-center gap-3 select-none ${className}`}>
      {/* Unboxed Pure Vector Seal */}
      <CineSeal size={size} className="transition-transform duration-300 group-hover:scale-105" />

      {/* Bespoke Sculpted Uppercase Wordmark: C I N E */}
      {showWordmark && (
        <span
          className="font-bold uppercase transition-colors"
          style={{
            fontSize: Math.max(16, Math.round(size * 0.62)),
            color: isDark ? "#FFFFFF" : "#1D1D1F",
            fontFamily: "'Cinzel', serif",
            letterSpacing: "0.22em",
            textIndent: "0.22em", // Balances wide trailing letter-spacing
          }}
        >
          CINE
        </span>
      )}
    </div>
  );
};
