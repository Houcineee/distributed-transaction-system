import React, { useState, useRef, useEffect } from "react";
import { useAuth } from "../context/useAuth";
import { client, getApiErrorMessage } from "../lib/api";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { CurrencyIcon } from "./CurrencyIcon";

const createAccountSchema = z.object({
  currency: z.enum(["USD", "EUR", "GBP", "MAD"]),
  cardType: z.enum(["DEBIT", "CREDIT"]),
});

type CreateAccountFormData = z.infer<typeof createAccountSchema>;

interface CreateAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAccountCreated: (newAccountId: string) => void;
}

export const CreateAccountModal: React.FC<CreateAccountModalProps> = ({
  isOpen,
  onClose,
  onAccountCreated,
}) => {
  const { user } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const [isCurrencyOpen, setIsCurrencyOpen] = useState(false);
  const [isCardTypeOpen, setIsCardTypeOpen] = useState(false);
  const currencyRef = useRef<HTMLDivElement>(null);
  const cardTypeRef = useRef<HTMLDivElement>(null);

  const {
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { isSubmitting },
  } = useForm<CreateAccountFormData>({
    resolver: zodResolver(createAccountSchema),
    defaultValues: {
      currency: "MAD",
      cardType: "DEBIT",
    },
  });

  const watchCurrency = watch("currency");
  const watchCardType = watch("cardType");

  useEffect(() => {
    if (isOpen) {
      setServerError(null);
      setIsCurrencyOpen(false);
      setIsCardTypeOpen(false);
      reset({ currency: "MAD", cardType: "DEBIT" });
    }
  }, [isOpen, reset]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (currencyRef.current && !currencyRef.current.contains(event.target as Node)) {
        setIsCurrencyOpen(false);
      }
      if (cardTypeRef.current && !cardTypeRef.current.contains(event.target as Node)) {
        setIsCardTypeOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (!isOpen) return null;

  const onSubmit = async (data: CreateAccountFormData) => {
    setServerError(null);
    if (!user || !user.id) return;

    try {
      const { data: accData, error } = await client.POST("/api/v1/accounts", {
        body: {
          userId: user.id,
          currency: data.currency,
          cardType: data.cardType,
        },
      });

      if (error) {
        setServerError(getApiErrorMessage(error, "Could not open account. Please try again."));
        return;
      }

      if (accData?.id) {
        onAccountCreated(accData.id);
      }
      onClose();
    } catch {
      setServerError("Account service unreachable. Please check connection.");
    }
  };

  const currencyOptions = [
    { code: "MAD" as const },
    { code: "USD" as const },
    { code: "EUR" as const },
    { code: "GBP" as const },
  ];

  const cardOptions = [
    { type: "DEBIT" as const, label: "Debit" },
    { type: "CREDIT" as const, label: "Credit" },
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/40 backdrop-blur-sm animate-in fade-in duration-200 select-none"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-[28px] max-w-[480px] w-full shadow-[0_25px_60px_-15px_rgba(0,0,0,0.3)] border border-black/10 animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="p-6 md:p-8 border-b border-black/[0.06] flex items-center justify-between">
          <h2 className="text-xl font-normal tracking-[-0.02em] text-[#1D1D1F]">
            Open Account
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-black/5 hover:bg-black/10 flex items-center justify-center text-[#86868B] hover:text-[#1D1D1F] transition-colors cursor-pointer"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* Content & Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="p-6 md:p-8 space-y-5">
          {serverError && (
            <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 flex items-center gap-3">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <div className="text-xs font-medium">{serverError}</div>
            </div>
          )}

          {/* Full-width Currency Selector */}
          <div className="relative" ref={currencyRef}>
            <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-2">
              Currency
            </label>

            <button
              type="button"
              onClick={() => {
                setIsCurrencyOpen(!isCurrencyOpen);
                setIsCardTypeOpen(false);
              }}
              className="w-full flex items-center justify-between px-4 py-3.5 rounded-2xl bg-[#F5F5F7] hover:bg-[#EBEBED] border border-transparent focus:border-black/20 focus:bg-white transition-all text-sm font-medium text-[#1D1D1F] cursor-pointer group"
            >
              <div className="flex items-center gap-3">
                <CurrencyIcon currency={watchCurrency} size={22} />
                <span className="font-semibold">{watchCurrency}</span>
              </div>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                className={`text-[#86868B] transition-transform duration-200 group-hover:text-[#1D1D1F] ${
                  isCurrencyOpen ? "rotate-180" : ""
                }`}
              >
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>

            {/* Spanning Currency Popover */}
            {isCurrencyOpen && (
              <div className="absolute left-0 right-0 top-full mt-2 w-full bg-white border border-black/[0.08] shadow-[0_20px_48px_-8px_rgba(0,0,0,0.18)] rounded-2xl p-1.5 z-50 animate-in fade-in zoom-in-95 duration-150">
                <div className="space-y-1">
                  {currencyOptions.map((opt) => {
                    const isSelected = watchCurrency === opt.code;
                    return (
                      <button
                        key={opt.code}
                        type="button"
                        onClick={() => {
                          setValue("currency", opt.code);
                          setIsCurrencyOpen(false);
                        }}
                        className={`w-full flex items-center justify-between p-3 rounded-xl transition-all text-left cursor-pointer ${
                          isSelected
                            ? "bg-[#F5F5F7] text-[#1D1D1F]"
                            : "hover:bg-black/[0.03] text-[#1D1D1F]"
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <CurrencyIcon currency={opt.code} size={22} />
                          <span className="text-sm font-semibold">{opt.code}</span>
                        </div>
                        {isSelected && (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#00A855" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="20 6 9 17 4 12" />
                          </svg>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* Full-width Card Type Selector */}
          <div className="relative" ref={cardTypeRef}>
            <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-2">
              Card Type
            </label>

            <button
              type="button"
              onClick={() => {
                setIsCardTypeOpen(!isCardTypeOpen);
                setIsCurrencyOpen(false);
              }}
              className="w-full flex items-center justify-between px-4 py-3.5 rounded-2xl bg-[#F5F5F7] hover:bg-[#EBEBED] border border-transparent focus:border-black/20 focus:bg-white transition-all text-sm font-medium text-[#1D1D1F] cursor-pointer group"
            >
              <span className="font-semibold">
                {watchCardType === "DEBIT" ? "Debit Card" : "Credit Card"}
              </span>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                className={`text-[#86868B] transition-transform duration-200 group-hover:text-[#1D1D1F] ${
                  isCardTypeOpen ? "rotate-180" : ""
                }`}
              >
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>

            {/* Spanning Card Type Popover */}
            {isCardTypeOpen && (
              <div className="absolute left-0 right-0 top-full mt-2 w-full bg-white border border-black/[0.08] shadow-[0_20px_48px_-8px_rgba(0,0,0,0.18)] rounded-2xl p-1.5 z-50 animate-in fade-in zoom-in-95 duration-150">
                <div className="space-y-1">
                  {cardOptions.map((opt) => {
                    const isSelected = watchCardType === opt.type;
                    return (
                      <button
                        key={opt.type}
                        type="button"
                        onClick={() => {
                          setValue("cardType", opt.type);
                          setIsCardTypeOpen(false);
                        }}
                        className={`w-full flex items-center justify-between p-3 rounded-xl transition-all text-left cursor-pointer ${
                          isSelected
                            ? "bg-[#F5F5F7] text-[#1D1D1F]"
                            : "hover:bg-black/[0.03] text-[#1D1D1F]"
                        }`}
                      >
                        <span className="text-sm font-semibold">{opt.label} Card</span>
                        {isSelected && (
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#00A855" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="20 6 9 17 4 12" />
                          </svg>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* Single Primary Action Button */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-4 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-sm font-semibold tracking-wide transition-all shadow-sm hover:shadow active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2"
            >
              {isSubmitting ? (
                <div className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin" />
              ) : (
                <span>Open Account</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
