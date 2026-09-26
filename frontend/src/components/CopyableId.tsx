import React, { useState } from "react";

interface CopyableIdProps {
  id?: string;
  truncate?: boolean;
  className?: string;
  label?: string;
}

export const CopyableId: React.FC<CopyableIdProps> = ({
  id = "",
  truncate = false,
  className = "",
  label,
}) => {
  const [copied, setCopied] = useState(false);

  if (!id) return <span className="text-[#86868B] text-xs">—</span>;

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(id);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  const displayText = truncate
    ? `${id.slice(0, 8)}••••${id.slice(-6)}`
    : id;

  return (
    <button
      type="button"
      onClick={handleCopy}
      title="Click to copy full ID"
      className={`inline-flex items-center gap-2 px-2.5 py-1 rounded-xl bg-black/[0.04] hover:bg-black/[0.07] border border-black/[0.06] transition-all group active:scale-[0.98] cursor-pointer ${className}`}
    >
      {label && (
        <span className="text-[10px] uppercase tracking-wider font-semibold text-[#86868B]">
          {label}
        </span>
      )}

      {/* Proportional, sleek typography instead of chunky monospace */}
      <span className="font-sans text-xs font-medium tracking-tight text-[#1D1D1F] select-all">
        {displayText}
      </span>

      {/* Dynamic Feedback Icon */}
      <span className="text-[#86868B] group-hover:text-[#1D1D1F] transition-colors shrink-0">
        {copied ? (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#00A855]">
            <svg
              width="13"
              height="13"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <span>Copied</span>
          </span>
        ) : (
          <svg
            width="12"
            height="12"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="opacity-50 group-hover:opacity-100"
          >
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
          </svg>
        )}
      </span>
    </button>
  );
};
