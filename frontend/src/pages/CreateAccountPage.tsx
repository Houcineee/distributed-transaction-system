import { useState, useRef, useEffect } from "react";
import { useAuth } from "../context/useAuth";
import { client, getApiErrorMessage } from "../lib/api";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useNavigate } from "react-router-dom";
import { SideNav } from "../components/SideNav";
import { CurrencyIcon } from "../components/CurrencyIcon";

const createAccountSchema = z.object({
  currency: z.enum(["USD", "EUR", "GBP", "MAD"]),
  cardType: z.enum(["DEBIT", "CREDIT"]),
});

type CreateAccountFormData = z.infer<typeof createAccountSchema>;

export function CreateAccountPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const [isCurrencyOpen, setIsCurrencyOpen] = useState(false);
  const [isCardTypeOpen, setIsCardTypeOpen] = useState(false);
  const currencyRef = useRef<HTMLDivElement>(null);
  const cardTypeRef = useRef<HTMLDivElement>(null);

  const {
    handleSubmit,
    setValue,
    watch,
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

  const onSubmit = async (data: CreateAccountFormData) => {
    setServerError(null);
    if (!user || !user.id) return;

    try {
      const { error } = await client.POST("/api/v1/accounts", {
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

      navigate("/dashboard");
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
    <div className="app-shell">
      <SideNav />
      <main className="app-main">
        {/* Header */}
        <header className="mb-10 pb-6 border-b border-black/[0.06]">
          <h1 className="text-3xl md:text-4xl font-normal tracking-[-0.03em] text-[#1D1D1F]">
            Open Account
          </h1>
        </header>

        <div className="max-w-xl">
          <div className="bg-white border border-black/[0.06] rounded-[24px] p-8 shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)]">
            {serverError && (
              <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 flex items-center gap-3">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <div className="text-xs font-medium">{serverError}</div>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                {/* Currency Dropdown */}
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
                    className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#F5F5F7] hover:bg-[#EBEBED] border border-transparent focus:border-black/20 focus:bg-white transition-all text-xs font-medium text-[#1D1D1F] cursor-pointer group"
                  >
                    <div className="flex items-center gap-2">
                      <CurrencyIcon currency={watchCurrency} size={18} />
                      <span className="font-semibold">{watchCurrency}</span>
                    </div>
                    <svg
                      width="14"
                      height="14"
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

                  {isCurrencyOpen && (
                    <div className="absolute left-0 right-0 top-full mt-2 bg-white border border-black/[0.08] shadow-[0_16px_40px_-8px_rgba(0,0,0,0.14)] rounded-2xl p-1.5 z-50 animate-in fade-in zoom-in-95 duration-150">
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
                              className={`w-full flex items-center justify-between p-2.5 rounded-xl transition-all text-left cursor-pointer ${
                                isSelected
                                  ? "bg-[#F5F5F7] text-[#1D1D1F]"
                                  : "hover:bg-black/[0.03] text-[#1D1D1F]"
                              }`}
                            >
                              <div className="flex items-center gap-2.5">
                                <CurrencyIcon currency={opt.code} size={20} />
                                <span className="text-xs font-semibold">{opt.code}</span>
                              </div>
                              {isSelected && (
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#00A855" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
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

                {/* Card Type Dropdown */}
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
                    className="w-full flex items-center justify-between p-3.5 rounded-2xl bg-[#F5F5F7] hover:bg-[#EBEBED] border border-transparent focus:border-black/20 focus:bg-white transition-all text-xs font-medium text-[#1D1D1F] cursor-pointer group"
                  >
                    <span className="font-semibold truncate">
                      {watchCardType === "DEBIT" ? "Debit" : "Credit"}
                    </span>
                    <svg
                      width="14"
                      height="14"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      className={`text-[#86868B] transition-transform duration-200 group-hover:text-[#1D1D1F] shrink-0 ml-1 ${
                        isCardTypeOpen ? "rotate-180" : ""
                      }`}
                    >
                      <polyline points="6 9 12 15 18 9" />
                    </svg>
                  </button>

                  {isCardTypeOpen && (
                    <div className="absolute left-0 right-0 top-full mt-2 bg-white border border-black/[0.08] shadow-[0_16px_40px_-8px_rgba(0,0,0,0.14)] rounded-2xl p-1.5 z-50 animate-in fade-in zoom-in-95 duration-150">
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
                              className={`w-full flex items-center justify-between p-2.5 rounded-xl transition-all text-left cursor-pointer ${
                                isSelected
                                  ? "bg-[#F5F5F7] text-[#1D1D1F]"
                                  : "hover:bg-black/[0.03] text-[#1D1D1F]"
                              }`}
                            >
                              <div className="text-xs font-semibold">{opt.label}</div>
                              {isSelected && (
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#00A855" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
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
              </div>

              <div className="flex items-center gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => navigate("/dashboard")}
                  className="flex-1 py-3.5 rounded-full bg-black/5 hover:bg-black/10 text-[#1D1D1F] text-xs font-semibold transition-all cursor-pointer text-center"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 py-3.5 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-xs font-semibold tracking-wide transition-all shadow-sm active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2"
                >
                  {isSubmitting ? (
                    <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                  ) : (
                    <span>Create Account</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
}
