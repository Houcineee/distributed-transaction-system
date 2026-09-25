import React from "react";

interface CurrencyIconProps {
  currency?: string;
  size?: number;
  className?: string;
}

export const CurrencyIcon: React.FC<CurrencyIconProps> = ({
  currency = "USD",
  size = 20,
  className = "",
}) => {
  const code = currency?.toUpperCase();

  switch (code) {
    case "MAD":
      // User's Authentic Moroccan Alaouite Vector Flag
      return (
        <div
          style={{ width: size, height: size }}
          className={`relative shrink-0 rounded-full overflow-hidden shadow-sm inline-flex items-center justify-center border border-black/10 bg-[#C1272D] ${className}`}
        >
          <img
            src="/morocco-flag.svg"
            alt="Morocco Flag"
            className="w-full h-full object-cover scale-[1.35]"
          />
        </div>
      );

    case "EUR":
      // European Union Flag Medallion
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 32 32"
          fill="none"
          className={`shrink-0 rounded-full shadow-sm ${className}`}
        >
          <circle cx="16" cy="16" r="16" fill="#003399" />
          {[...Array(12)].map((_, i) => {
            const angle = (i * 30 * Math.PI) / 180;
            const x = 16 + 8.5 * Math.sin(angle);
            const y = 16 - 8.5 * Math.cos(angle);
            return (
              <circle key={i} cx={x} cy={y} r="1.1" fill="#FFCC00" />
            );
          })}
        </svg>
      );

    case "GBP":
      // Great Britain Union Jack Medallion
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 32 32"
          fill="none"
          className={`shrink-0 rounded-full shadow-sm ${className}`}
        >
          <defs>
            <clipPath id="gbp-circle">
              <circle cx="16" cy="16" r="16" />
            </clipPath>
          </defs>
          <g clipPath="url(#gbp-circle)">
            <rect width="32" height="32" fill="#012169" />
            <path d="M0 0L32 32M32 0L0 32" stroke="#FFFFFF" strokeWidth="6" />
            <path d="M0 0L32 32M32 0L0 32" stroke="#C8102E" strokeWidth="2.5" />
            <path d="M16 0V32M0 16H32" stroke="#FFFFFF" strokeWidth="10" />
            <path d="M16 0V32M0 16H32" stroke="#C8102E" strokeWidth="6" />
          </g>
        </svg>
      );

    case "USD":
    default:
      // USA Flag Medallion
      return (
        <svg
          width={size}
          height={size}
          viewBox="0 0 32 32"
          fill="none"
          className={`shrink-0 rounded-full shadow-sm ${className}`}
        >
          <defs>
            <clipPath id="usd-circle">
              <circle cx="16" cy="16" r="16" />
            </clipPath>
          </defs>
          <g clipPath="url(#usd-circle)">
            <rect width="32" height="32" fill="#FFFFFF" />
            {[0, 4, 8, 12, 16, 20, 24, 28].map((y) => (
              <rect key={y} y={y} width="32" height="2.3" fill="#B31942" />
            ))}
            <rect width="15" height="15" fill="#0A3161" />
            <circle cx="4" cy="4" r="0.9" fill="#FFFFFF" />
            <circle cx="8" cy="4" r="0.9" fill="#FFFFFF" />
            <circle cx="12" cy="4" r="0.9" fill="#FFFFFF" />
            <circle cx="6" cy="7.5" r="0.9" fill="#FFFFFF" />
            <circle cx="10" cy="7.5" r="0.9" fill="#FFFFFF" />
            <circle cx="4" cy="11" r="0.9" fill="#FFFFFF" />
            <circle cx="8" cy="11" r="0.9" fill="#FFFFFF" />
            <circle cx="12" cy="11" r="0.9" fill="#FFFFFF" />
          </g>
        </svg>
      );
  }
};
