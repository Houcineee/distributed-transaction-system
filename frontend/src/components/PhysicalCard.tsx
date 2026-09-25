import React from "react";
import type { components } from "../types/api";
import { CineSeal } from "./CineLogo";

type CardResponse = components["schemas"]["CardResponse"];

interface PhysicalCardProps {
  card?: CardResponse | null;
  userName?: string;
  currency?: string;
}

export const PhysicalCard: React.FC<PhysicalCardProps> = ({ card, userName = "Houcine Gahboub", currency = "USD" }) => {
  const isCredit = card?.cardType === "CREDIT";
  const maskedNumber = card?.cardNumber
    ? `•••• •••• •••• ${card.cardNumber.slice(-4)}`
    : "•••• •••• •••• 8842";

  return (
    <div className="relative group w-full max-w-[380px] aspect-[1.586] rounded-[22px] p-6 flex flex-col justify-between overflow-hidden transition-all duration-500 ease-out hover:scale-[1.02] cursor-pointer select-none"
         style={{
           background: isCredit 
             ? "linear-gradient(135deg, #1C1C1E 0%, #2C2C2E 50%, #0A0A0C 100%)"
             : "linear-gradient(135deg, #FDFDFE 0%, #ECEEF2 45%, #DCE0E8 100%)",
           boxShadow: isCredit
             ? "0 24px 48px -12px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(197, 160, 89, 0.3) inset"
             : "0 24px 48px -12px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(197, 160, 89, 0.25) inset",
           color: isCredit ? "#FFFFFF" : "#1D1D1F"
         }}>
      
      {/* Iridescent / Metallic Sheen Overlay */}
      <div 
        className="absolute inset-0 opacity-40 pointer-events-none transition-opacity duration-700 group-hover:opacity-75"
        style={{
          background: isCredit
            ? "radial-gradient(circle at 80% 20%, rgba(255,255,255,0.15) 0%, transparent 60%)"
            : "radial-gradient(circle at 80% 20%, rgba(0, 208, 108, 0.12) 0%, rgba(0, 113, 227, 0.08) 50%, transparent 80%)"
        }}
      />

      {/* Subtle fine gold border highlight */}
      <div className="absolute inset-0 rounded-[22px] border border-[#C5A059]/20 pointer-events-none" />

      {/* Top Row: Brand & Contactless */}
      <div className="relative z-10 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <CineSeal size={22} />
          <span
            className="font-bold text-xs uppercase drop-shadow-sm opacity-95"
            style={{ fontFamily: "'Cinzel', serif", letterSpacing: "0.20em", textIndent: "0.20em" }}
          >
            CINE
          </span>
        </div>

        {/* Contactless Waves SVG */}
        <div className="opacity-70 group-hover:opacity-100 transition-opacity">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M8.5 16.5a5 5 0 0 1 0-9" />
            <path d="M12 19a8.5 8.5 0 0 0 0-14" />
            <path d="M15.5 21.5a12 12 0 0 0 0-19" />
          </svg>
        </div>
      </div>

      {/* Middle Row: EMV Chip & Type Badge */}
      <div className="relative z-10 flex items-center justify-between my-auto pt-2">
        {/* Realistic EMV Chip */}
        <div className="w-11 h-8 rounded-md bg-gradient-to-br from-[#E6CA65] via-[#F3E5AB] to-[#C5A059] p-0.5 shadow-inner flex items-center justify-center border border-[#9E8238]/30">
          <div className="w-full h-full rounded-[4px] border border-black/20 flex flex-col justify-between p-0.5 opacity-70">
            <div className="w-full h-[1px] bg-black/30" />
            <div className="flex justify-between w-full h-[6px]">
              <div className="w-[1px] h-full bg-black/30" />
              <div className="w-2.5 h-full rounded-sm border border-black/30" />
              <div className="w-[1px] h-full bg-black/30" />
            </div>
            <div className="w-full h-[1px] bg-black/30" />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span 
            className="text-[10px] font-semibold tracking-widest px-2.5 py-1 rounded-full uppercase border"
            style={{
              backgroundColor: isCredit ? "rgba(197, 160, 89, 0.14)" : "rgba(197, 160, 89, 0.10)",
              color: isCredit ? "#F5E5BA" : "#8A6729",
              borderColor: "rgba(197, 160, 89, 0.28)",
            }}
          >
            <span>{card?.cardType || "DEBIT"} • {currency}</span>
          </span>
        </div>
      </div>

      {/* Bottom Row: Number, Name, Expiry */}
      <div className="relative z-10 space-y-3">
        <div className="font-mono text-[17px] tracking-[0.18em] font-medium opacity-90 drop-shadow-sm">
          {maskedNumber}
        </div>

        <div className="flex justify-between items-end">
          <div>
            <div className="text-[8px] uppercase tracking-widest opacity-50 mb-0.5">Cardholder</div>
            <div className="font-medium text-xs tracking-wide uppercase truncate max-w-[190px]">
              {userName}
            </div>
          </div>

          <div className="text-right">
            <div className="text-[8px] uppercase tracking-widest opacity-50 mb-0.5">Expires</div>
            <div className="font-mono text-xs tracking-wider">
              08/29
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
