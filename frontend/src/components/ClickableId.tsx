import { useState } from "react";

interface ClickableIdProps {
  id?: string | null;
  truncate?: boolean;
  className?: string;
}

export function ClickableId({ id, truncate = true, className = "" }: ClickableIdProps) {
  const [copied, setCopied] = useState(false);

  if (!id) return <span className="font-mono text-[11px] text-[#86868B]">—</span>;

  const displayText = truncate ? `${id.slice(0, 8)}...` : id;

  const handleCopy = async (e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();

    let success = false;
    if (navigator.clipboard && window.isSecureContext) {
      try {
        await navigator.clipboard.writeText(id);
        success = true;
      } catch {
        success = false;
      }
    }

    if (!success) {
      try {
        const textarea = document.createElement("textarea");
        textarea.value = id;
        textarea.style.position = "fixed";
        textarea.style.left = "-9999px";
        textarea.style.top = "0";
        textarea.setAttribute("readonly", "");
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        success = document.execCommand("copy");
        document.body.removeChild(textarea);
      } catch (err) {
        console.error("Clipboard copy failed", err);
      }
    }

    if (success) {
      setCopied(true);
      setTimeout(() => setCopied(false), 1200);
    }
  };

  return (
    <button
      type="button"
      onClick={handleCopy}
      title={copied ? "Copied" : "Click to copy full ID"}
      className={`font-mono transition-colors cursor-pointer text-left inline-block ${className} ${
        copied
          ? "text-emerald-600 font-semibold"
          : "hover:text-[#C5A059]"
      }`}
    >
      {copied ? "Copied!" : displayText}
    </button>
  );
}
