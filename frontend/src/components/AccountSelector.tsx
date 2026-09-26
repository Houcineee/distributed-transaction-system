import React, { useState, useRef, useEffect } from "react";
import { CurrencyIcon } from "./CurrencyIcon";
import { getCurrencyDetails, formatCurrencyAmount } from "../lib/currency";
import type { components } from "../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];

interface AccountSelectorProps {
  accounts: AccountResponse[];
  selectedAccountId: string | null;
  onSelectAccount: (accountId: string) => void;
}

export const AccountSelector: React.FC<AccountSelectorProps> = ({
  accounts,
  selectedAccountId,
  onSelectAccount,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedAccount =
    accounts.find((a) => a.id === selectedAccountId) || accounts[0];
  const selectedIndex = accounts.findIndex((a) => a.id === selectedAccountId);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (accounts.length <= 1) {
    return null;
  }

  const selectedCurrency = getCurrencyDetails(selectedAccount?.currency);

  return (
    <div className="relative select-none" ref={containerRef}>
      {/* Trigger Button with Flag Medallion */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="group flex items-center gap-2.5 px-3.5 py-1.5 rounded-full bg-white hover:bg-[#F5F5F7] border border-black/[0.08] shadow-[0_1px_3px_rgba(0,0,0,0.03)] hover:border-black/15 transition-all text-xs font-medium text-[#1D1D1F] active:scale-[0.98] cursor-pointer"
      >
        {/* National Flag Medallion */}
        <CurrencyIcon currency={selectedAccount?.currency} size={18} />

        <span className="font-semibold text-[#1D1D1F]">
          Account {selectedIndex >= 0 ? selectedIndex + 1 : 1}
        </span>

        <span className="text-[#86868B] font-medium">
          {selectedCurrency.code}
        </span>

        {/* Clean Proportional ID Snippet */}
        <span className="text-[11px] text-[#86868B] font-normal tracking-tight">
          · {selectedAccount?.id?.slice(0, 6)}
        </span>

        {/* Delicate Chevron */}
        <svg
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          className={`text-[#86868B] transition-transform duration-200 ${
            isOpen ? "rotate-180 text-[#1D1D1F]" : ""
          }`}
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      {/* Floating Popover Menu */}
      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 rounded-2xl bg-white border border-black/[0.08] shadow-[0_20px_48px_-10px_rgba(0,0,0,0.14)] p-1.5 z-50 backdrop-blur-xl animate-in fade-in zoom-in-95 duration-150">
          <div className="px-3 py-2 text-[10px] font-semibold uppercase tracking-wider text-[#86868B]">
            Switch Active Portfolio
          </div>

          <div className="space-y-1">
            {accounts.map((account, index) => {
              const isSelected = account.id === selectedAccountId;
              const currencyInfo = getCurrencyDetails(account.currency);

              return (
                <button
                  key={account.id}
                  type="button"
                  onClick={() => {
                    if (account.id) onSelectAccount(account.id);
                    setIsOpen(false);
                  }}
                  className={`w-full flex items-center justify-between p-3 rounded-xl transition-all text-left cursor-pointer ${
                    isSelected
                      ? "bg-[#F5F5F7] text-[#1D1D1F]"
                      : "hover:bg-black/[0.03] text-[#1D1D1F]"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    {/* SVG Flag Medallion */}
                    <CurrencyIcon currency={account.currency} size={24} />

                    <div>
                      <div className="text-xs font-semibold flex items-center gap-1.5 text-[#1D1D1F]">
                        <span>Account {index + 1}</span>
                        <span className="text-[10px] px-1.5 py-0.2 rounded-md bg-black/[0.06] text-[#6E6E73] font-medium">
                          {currencyInfo.code}
                        </span>
                      </div>
                      <div className="text-[11px] text-[#86868B] font-normal tracking-tight mt-0.5">
                        {account.id?.slice(0, 8)}••••{account.id?.slice(-4)}
                      </div>
                    </div>
                  </div>

                  <div className="text-right flex items-center gap-2">
                    <div className="text-xs font-semibold tabular-nums text-[#1D1D1F]">
                      {formatCurrencyAmount(account.balance ?? 0, account.currency)}
                    </div>
                    {isSelected && (
                      <svg
                        width="14"
                        height="14"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="#00A855"
                        strokeWidth="3"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
