import { useEffect, useState, useRef } from "react";
import { useAuth } from "../context/useAuth";
import { client, getApiErrorMessage } from "../lib/api";
import { SideNav } from "../components/SideNav";
import { CurrencyIcon } from "../components/CurrencyIcon";
import { getCurrencyDetails, formatCurrencyAmount } from "../lib/currency";
import { CopyableId } from "../components/CopyableId";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import type { components } from "../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];

const transferSchema = z.object({
  destAccount: z.string().min(10, "Please enter a valid destination account ID"),
  amount: z.number().positive("Amount must be greater than zero"),
});

type TransferFormData = z.infer<typeof transferSchema>;

export function TransferPage() {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [isAccountPickerOpen, setIsAccountPickerOpen] = useState(false);
  const pickerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);

  const [serverError, setServerError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState<string>(() => crypto.randomUUID());

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
    watch,
  } = useForm<TransferFormData>({
    resolver: zodResolver(transferSchema),
  });

  const watchAmount = watch("amount");

  useEffect(() => {
    if (!user?.id) return;
    async function fetchAccounts() {
      try {
        const { data } = await client.GET("/api/v1/users/{id}/accounts", {
          params: { path: { id: user!.id! } },
        });
        if (data?.length) {
          setAccounts(data);
          setSelectedAccountId(data[0].id || null);
        }
      } finally {
        setLoading(false);
      }
    }
    fetchAccounts();
  }, [user]);

  // Click outside to close custom account picker
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (pickerRef.current && !pickerRef.current.contains(event.target as Node)) {
        setIsAccountPickerOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const onSubmit = async (data: TransferFormData) => {
    setServerError(null);
    setSuccessMessage(null);

    const selectedAccount = accounts.find((a) => a.id === selectedAccountId);
    if (!selectedAccount) return;

    const availableBalance = Number(selectedAccount.balance ?? 0);
    const currDetails = getCurrencyDetails(selectedAccount.currency);

    if (data.amount > availableBalance) {
      setServerError(
        `Insufficient funds. Available balance: ${currDetails.symbol} ${availableBalance.toLocaleString("en-US", { minimumFractionDigits: 2 })}`
      );
      return;
    }

    try {
      const amountInCents = Math.round(data.amount * 100);
      const { error: txError } = await client.POST("/api/v1/transfers", {
        headers: {
          "Idempotency-Key": idempotencyKey,
        },
        body: {
          srcAccount: selectedAccount.id!,
          destAccount: data.destAccount,
          amount: amountInCents,
        },
      });

      if (txError) {
        setServerError(getApiErrorMessage(txError, "Transfer could not be completed."));
        return;
      }

      setSuccessMessage("Transfer completed successfully.");
      reset();
      setIdempotencyKey(crypto.randomUUID());

      const { data: accData } = await client.GET("/api/v1/users/{id}/accounts", {
        params: { path: { id: user!.id! } },
      });
      if (accData && accData.length > 0) setAccounts(accData);
    } catch {
      setServerError("Network error. Please verify your connection.");
    }
  };

  if (loading) {
    return (
      <div className="app-shell">
        <SideNav />
        <main className="app-main flex items-center justify-center min-h-screen">
          <div className="flex flex-col items-center gap-3">
            <div className="w-8 h-8 rounded-full border-2 border-black/10 border-t-[#1D1D1F] animate-spin" />
            <span className="text-xs uppercase tracking-widest text-[#86868B] font-medium">
              Loading Accounts
            </span>
          </div>
        </main>
      </div>
    );
  }

  const selectedAccount = accounts.find((a) => a.id === selectedAccountId) || accounts[0];
  const selectedIndex = accounts.findIndex((a) => a.id === selectedAccountId);
  const activeCurrencyInfo = getCurrencyDetails(selectedAccount?.currency);

  return (
    <div className="app-shell">
      <SideNav />
      <main className="app-main">
        {/* Header */}
        <header className="mb-6 pb-4 md:mb-10 md:pb-6 border-b border-black/[0.06]">
          <h1 className="text-2xl md:text-4xl font-normal tracking-[-0.03em] text-[#1D1D1F]">
            Send Money
          </h1>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start max-w-5xl">
          {/* Transfer Form Box */}
          <div className="lg:col-span-7 bg-white border border-black/[0.06] rounded-[24px] p-8 shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)]">
            {successMessage && (
              <div className="mb-6 p-4 rounded-2xl bg-[#00D06C]/10 border border-[#00D06C]/20 text-[#008A44] flex items-center gap-3">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <div className="text-sm font-medium">{successMessage}</div>
              </div>
            )}

            {serverError && (
              <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 flex items-center gap-3">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <div className="text-sm font-medium">{serverError}</div>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Custom Apple-Style Source Account Picker */}
              <div className="relative" ref={pickerRef}>
                <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-2">
                  From Account
                </label>
                
                <button
                  type="button"
                  onClick={() => setIsAccountPickerOpen(!isAccountPickerOpen)}
                  className="w-full flex items-center justify-between p-4 rounded-2xl bg-[#F5F5F7] hover:bg-[#EBEBED] border border-transparent focus:border-black/20 focus:bg-white transition-all text-left cursor-pointer group"
                >
                  <div className="flex items-center gap-3">
                    {/* National Flag Medallion */}
                    <CurrencyIcon currency={selectedAccount?.currency} size={24} />

                    <div>
                      <div className="text-sm font-semibold text-[#1D1D1F]">
                        Account {selectedIndex >= 0 ? selectedIndex + 1 : 1}
                      </div>
                      <div className="text-xs text-[#86868B] tracking-tight mt-0.5 tabular-nums">
                        {formatCurrencyAmount(selectedAccount?.balance ?? 0, selectedAccount?.currency)}
                      </div>
                    </div>
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
                      isAccountPickerOpen ? "rotate-180" : ""
                    }`}
                  >
                    <polyline points="6 9 12 15 18 9" />
                  </svg>
                </button>

                {/* Popover Options with Correct Per-Account Currencies and Flags */}
                {isAccountPickerOpen && (
                  <div className="absolute left-0 right-0 top-full mt-2 bg-white border border-black/[0.08] shadow-[0_16px_40px_-8px_rgba(0,0,0,0.14)] rounded-2xl p-2 z-50 animate-in fade-in zoom-in-95 duration-150">
                    <div className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-[#86868B]">
                      Select Origin Account
                    </div>

                    <div className="space-y-1 mt-1">
                      {accounts.map((acc, idx) => {
                        const isSelected = acc.id === selectedAccountId;

                        return (
                          <button
                            key={acc.id}
                            type="button"
                            onClick={() => {
                              if (acc.id) setSelectedAccountId(acc.id);
                              setIsAccountPickerOpen(false);
                            }}
                            className={`w-full flex items-center justify-between p-3 rounded-xl transition-all text-left cursor-pointer ${
                              isSelected
                                ? "bg-[#F5F5F7] text-[#1D1D1F]"
                                : "hover:bg-black/[0.03] text-[#1D1D1F]"
                            }`}
                          >
                            <div className="flex items-center gap-3">
                              <CurrencyIcon currency={acc.currency} size={24} />

                              <div>
                                <div className="text-xs font-semibold text-[#1D1D1F]">
                                  Account {idx + 1}
                                </div>
                                <div className="text-[11px] text-[#86868B] tracking-tight mt-0.5 tabular-nums">
                                  {formatCurrencyAmount(acc.balance ?? 0, acc.currency)}
                                </div>
                              </div>
                            </div>

                            <div className="text-right flex items-center gap-2">
                              <div className="text-xs font-medium tabular-nums">
                                {formatCurrencyAmount(acc.balance ?? 0, acc.currency)}
                              </div>
                              {isSelected && (
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#00A855" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
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

              {/* Recipient Account ID */}
              <div>
                <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-2">
                  To (Recipient Account ID)
                </label>
                <input
                  type="text"
                  {...register("destAccount")}
                  placeholder="Paste account ID here..."
                  className="w-full bg-[#F5F5F7] border border-transparent focus:border-black/20 focus:bg-white text-sm text-[#1D1D1F] px-4 py-3.5 rounded-2xl focus:outline-none transition-all placeholder:text-[#86868B]/60"
                />
                {errors.destAccount && (
                  <div className="text-xs text-red-500 mt-1.5">{errors.destAccount.message}</div>
                )}
              </div>

              {/* Amount Input with Flag Medallion Pill */}
              <div>
                <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-2">
                  Amount
                </label>
                <div className="flex items-center rounded-2xl bg-[#F5F5F7] border border-transparent focus-within:border-black/20 focus-within:bg-white transition-all p-2.5">
                  <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white border border-black/[0.06] shadow-sm select-none mr-3 shrink-0">
                    <CurrencyIcon currency={selectedAccount?.currency} size={16} />
                    <span className="text-xs font-semibold text-[#1D1D1F]">
                      {activeCurrencyInfo.code}
                    </span>
                  </div>
                  <input
                    type="number"
                    step="0.01"
                    {...register("amount", { valueAsNumber: true })}
                    placeholder="0.00"
                    className="w-full bg-transparent text-2xl font-light text-[#1D1D1F] focus:outline-none tabular-nums"
                  />
                </div>
                {errors.amount && (
                  <div className="text-xs text-red-500 mt-1.5">{errors.amount.message}</div>
                )}
              </div>

              {/* Submit Pill Button */}
              <div className="pt-4">
                <button
                  type="submit"
                  disabled={isSubmitting || !selectedAccountId}
                  className="w-full py-4 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-sm font-semibold tracking-wide transition-all shadow-sm hover:shadow active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2"
                >
                  {isSubmitting ? (
                    <div className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                  ) : (
                    <>
                      <span>
                        Send {watchAmount ? formatCurrencyAmount(watchAmount, selectedAccount?.currency) : "Money"}
                      </span>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="5" y1="12" x2="19" y2="12" />
                        <polyline points="12 5 19 12 12 19" />
                      </svg>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>

          {/* Account Summary Sidebar Card */}
          <div className="lg:col-span-5 bg-white border border-black/[0.06] rounded-[24px] p-8 shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)] space-y-6">
            <div className="text-xs font-semibold uppercase tracking-wider text-[#86868B]">
              Account Summary
            </div>

            <div className="space-y-4">
              <div className="flex justify-between items-center pb-3 border-b border-black/[0.06]">
                <span className="text-xs text-[#86868B]">Available to Send</span>
                <span className="text-base font-semibold text-[#1D1D1F] tabular-nums">
                  {formatCurrencyAmount(selectedAccount?.balance ?? 0, selectedAccount?.currency)}
                </span>
              </div>

              <div className="flex justify-between items-center pb-3 border-b border-black/[0.06]">
                <span className="text-xs text-[#86868B]">Transfer Fee</span>
                <span className="text-xs font-semibold text-[#00A855]">
                  Free (0.00)
                </span>
              </div>

              <div className="flex justify-between items-center pb-3 border-b border-black/[0.06]">
                <span className="text-xs text-[#86868B]">Settlement Speed</span>
                <span className="text-xs font-medium text-[#1D1D1F]">
                  Instant
                </span>
              </div>

              <div className="pt-2">
                <span className="block text-xs text-[#86868B] mb-2">Your Account ID</span>
                <CopyableId id={selectedAccount?.id} />
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
