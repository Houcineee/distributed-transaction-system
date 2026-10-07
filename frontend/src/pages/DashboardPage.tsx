import { useEffect, useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { client } from "../lib/api";
import { SideNav } from "../components/SideNav";
import { PhysicalCard } from "../components/PhysicalCard";
import { AccountSelector } from "../components/AccountSelector";
import { CreateAccountModal } from "../components/CreateAccountModal";
import { CurrencyIcon } from "../components/CurrencyIcon";
import { getCurrencyDetails, formatCurrencyAmount } from "../lib/currency";
import { CopyableId } from "../components/CopyableId";
import type { components } from "../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];
type CardResponse = components["schemas"]["CardResponse"];
type TransactionResponse = components["schemas"]["TransactionResponse"];

export function DashboardPage() {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [isNewAccountOpen, setIsNewAccountOpen] = useState(false);

  // Activity Pagination State
  const [txPage, setTxPage] = useState(0);
  const [txPageSize] = useState(10);
  const [txTotalPages, setTxTotalPages] = useState(1);
  const [txTotalElements, setTxTotalElements] = useState(0);
  const [txIsLast, setTxIsLast] = useState(true);
  const [txLoading, setTxLoading] = useState(false);

  const [summaryTransactions, setSummaryTransactions] = useState<TransactionResponse[]>([]);

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

  // Reset page to 0 when selected account changes
  useEffect(() => {
    setTxPage(0);
  }, [selectedAccountId]);

  // Fetch cards and summary transactions for monthly stats
  useEffect(() => {
    if (!selectedAccountId) return;
    const accountId = selectedAccountId;
    async function fetchAccountDetails() {
      try {
        const [{ data: cardData }, { data: summaryData }] = await Promise.all([
          client.GET("/api/v1/cards/account/{accountId}", {
            params: { path: { accountId } },
          }),
          client.GET("/api/v1/accounts/{id}/transactions", {
            params: { path: { id: accountId }, query: { pageable: { page: 0, size: 100 } } },
          }),
        ]);
        setCards(cardData || []);
        setSummaryTransactions(summaryData?.content || []);
      } catch (err) {
        console.error("Failed to fetch account overview details", err);
      }
    }
    fetchAccountDetails();
  }, [selectedAccountId]);

  // Fetch paginated transactions for selected account and page
  useEffect(() => {
    if (!selectedAccountId) return;
    const accountId = selectedAccountId;
    let isMounted = true;
    async function fetchTx() {
      setTxLoading(true);
      try {
        const { data } = await client.GET("/api/v1/accounts/{id}/transactions", {
          params: {
            path: { id: accountId },
            query: { pageable: { page: txPage, size: txPageSize } },
          },
        });
        if (isMounted && data) {
          setTransactions(data.content || []);
          setTxTotalPages(data.totalPages ?? 1);
          setTxTotalElements(data.totalElements ?? 0);
          setTxIsLast(Boolean(data.last));
        }
      } catch (err) {
        console.error("Failed to fetch transactions", err);
      } finally {
        if (isMounted) {
          setTxLoading(false);
        }
      }
    }
    fetchTx();
    return () => {
      isMounted = false;
    };
  }, [selectedAccountId, txPage, txPageSize]);

  const selectedAccount = accounts.find((acc) => acc.id === selectedAccountId) || accounts[0];
  const primaryCard = cards[0];
  const currencyInfo = getCurrencyDetails(selectedAccount?.currency);

  // Only show COMPLETED (succeeded) transactions in activity table
  const successfulTransactions = useMemo(() => {
    return transactions.filter((tx) => tx.status === "COMPLETED");
  }, [transactions]);

  // Compute real monthly inflow and outflow from account summary transactions (independent of active page)
  const { monthlyInflow, monthlyOutflow } = useMemo(() => {
    if (!selectedAccount?.id) return { monthlyInflow: 0, monthlyOutflow: 0 };
    
    const now = new Date();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();

    let inflow = 0;
    let outflow = 0;

    summaryTransactions
      .filter((tx) => tx.status === "COMPLETED")
      .forEach((tx) => {
        const txDate = tx.createdAt ? new Date(tx.createdAt) : null;
        const isThisMonth = txDate && txDate.getMonth() === currentMonth && txDate.getFullYear() === currentYear;
        
        if (isThisMonth) {
          const amount = Number(tx.amount || 0);
          if (tx.destinationAccountId === selectedAccount.id) {
            inflow += amount;
          } else if (tx.sourceAccountId === selectedAccount.id) {
            outflow += amount;
          }
        }
      });

    return { monthlyInflow: inflow, monthlyOutflow: outflow };
  }, [summaryTransactions, selectedAccount]);

  if (loading) {
    return (
      <div className="app-shell">
        <SideNav />
        <main className="app-main flex items-center justify-center min-h-screen">
          <div className="flex flex-col items-center gap-3">
            <div className="w-8 h-8 rounded-full border-2 border-black/10 border-t-[#1D1D1F] animate-spin" />
            <span className="text-xs uppercase tracking-widest text-[#86868B] font-medium">
              Updating Portfolio
            </span>
          </div>
        </main>
      </div>
    );
  }

  const handleAccountCreated = async (newAccountId: string) => {
    if (!user?.id) return;
    try {
      const { data } = await client.GET("/api/v1/users/{id}/accounts", {
        params: { path: { id: user.id } },
      });
      if (data?.length) {
        setAccounts(data);
        setSelectedAccountId(newAccountId);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const formattedBalance = selectedAccount?.balance
    ? Number(selectedAccount.balance).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : "0.00";

  return (
    <div className="app-shell">
      <SideNav />
      <main className="app-main">
        {/* Top Header / Account Switcher */}
        <header className="flex flex-col md:flex-row md:items-center justify-between gap-3 md:gap-4 mb-6 pb-4 md:mb-10 md:pb-6 border-b border-black/[0.06]">
          <div>
            <h1 className="text-2xl md:text-4xl font-normal tracking-[-0.03em] text-[#1D1D1F]">
              Good day, {user?.name?.split(" ")[0] || "Houcine"}
            </h1>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <AccountSelector
              accounts={accounts}
              selectedAccountId={selectedAccountId}
              onSelectAccount={(id) => setSelectedAccountId(id)}
            />

            <button
              type="button"
              onClick={() => setIsNewAccountOpen(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white hover:bg-[#F5F5F7] border border-black/[0.08] shadow-[0_1px_3px_rgba(0,0,0,0.03)] hover:border-black/15 text-xs font-semibold text-[#1D1D1F] transition-all active:scale-[0.98] cursor-pointer"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>New Account</span>
            </button>
          </div>
        </header>

        {/* Hero Section: Balance & Physical Card */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start mb-12">
          
          {/* Left Column: Liquid Balance & Send Action */}
          <div className="lg:col-span-7 flex flex-col justify-between space-y-6">
            <div className="bg-white border border-black/[0.06] rounded-[24px] p-8 shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)] hover:shadow-[0_12px_32px_-8px_rgba(0,0,0,0.06)] transition-all duration-300">
              <div className="flex items-center justify-between mb-4">
                {/* Apple-style Currency Pill */}
                <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#F5F5F7] border border-black/[0.05]">
                  <CurrencyIcon currency={selectedAccount?.currency} size={18} />
                  <span className="text-xs font-semibold text-[#1D1D1F]">
                    {currencyInfo.code}
                  </span>
                </div>
              </div>

              {/* Commanding Balance Display with Adaptive Currency Placement */}
              <div className="flex items-baseline gap-2 mb-3">
                {currencyInfo.isPrefix && (
                  <span className="text-3xl md:text-4xl font-light text-[#86868B] select-none">
                    {currencyInfo.symbol}
                  </span>
                )}
                <span className="text-5xl md:text-6xl lg:text-7xl font-light tracking-[-0.04em] text-[#1D1D1F] tabular-nums">
                  {formattedBalance}
                </span>
                {!currencyInfo.isPrefix && (
                  <span className="text-2xl md:text-3xl font-normal text-[#86868B] select-none ml-1.5">
                    {currencyInfo.symbol}
                  </span>
                )}
              </div>

              {/* Clean Account ID Badge (Replaces raw monospace terminal font) */}
              <div className="flex items-center gap-3 pt-2">
                <CopyableId id={selectedAccount?.id} label="Account ID" truncate={true} />
              </div>

              {/* Send Money Pill Action */}
              <div className="pt-8 mt-6 border-t border-black/[0.06]">
                <Link
                  to="/transfers"
                  className="inline-flex items-center gap-2 px-6 py-3.5 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-sm font-medium transition-all shadow-sm hover:shadow active:scale-[0.98] hover:ring-1 hover:ring-[#C5A059]/40"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="5" y1="12" x2="19" y2="12" />
                    <polyline points="12 5 19 12 12 19" />
                  </svg>
                  <span>Send Money</span>
                </Link>
              </div>
            </div>

            {/* Real Calculated Monthly Stats */}
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-white/80 backdrop-blur-sm border border-black/[0.06] rounded-[20px] p-6 shadow-sm">
                <div className="text-[11px] uppercase tracking-wider text-[#86868B] font-semibold mb-1">
                  Monthly Inflow
                </div>
                <div className="text-2xl font-normal tracking-tight text-[#00A855] tabular-nums">
                  +{formatCurrencyAmount(monthlyInflow, selectedAccount?.currency)}
                </div>
              </div>

              <div className="bg-white/80 backdrop-blur-sm border border-black/[0.06] rounded-[20px] p-6 shadow-sm">
                <div className="text-[11px] uppercase tracking-wider text-[#86868B] font-semibold mb-1">
                  Monthly Outflow
                </div>
                <div className="text-2xl font-normal tracking-tight text-[#1D1D1F] tabular-nums">
                  -{formatCurrencyAmount(monthlyOutflow, selectedAccount?.currency)}
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Physical Card Showcase */}
          <div className="lg:col-span-5 flex flex-col items-center justify-center p-8 bg-white border border-black/[0.06] rounded-[24px] shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)]">
            <div className="w-full flex items-center justify-between mb-6">
              <span className="text-xs font-semibold uppercase tracking-wider text-[#86868B]">
                Active Card
              </span>
              <span className="text-xs text-[#86868B]">
                {primaryCard ? primaryCard.cardType : "Debit"}
              </span>
            </div>

            {/* The Tactile Card */}
            <PhysicalCard 
              card={primaryCard} 
              userName={user?.name || "Houcine Gahboub"} 
              currency={selectedAccount?.currency || "USD"} 
            />
          </div>
        </div>

        {/* Recent Transactions: Only Successful Transactions */}
        <div className="bg-white border border-black/[0.06] rounded-[24px] shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)] overflow-hidden">
          <div className="p-6 md:p-8 flex items-center justify-between border-b border-black/[0.06]">
            <div className="flex items-baseline gap-3">
              <h2 className="text-xl md:text-2xl font-normal tracking-tight text-[#1D1D1F]">
                Activity
              </h2>
              {txTotalElements > 0 && (
                <span className="text-xs text-[#86868B] font-medium tracking-tight">
                  ({txTotalElements} {txTotalElements === 1 ? "transaction" : "transactions"})
                </span>
              )}
            </div>
            
            <Link
              to="/transfers"
              className="text-xs font-semibold text-[#0071E3] hover:text-[#0051A8] transition-colors"
            >
              New Transfer →
            </Link>
          </div>

          {txLoading ? (
            <div className="py-20 flex flex-col items-center justify-center gap-3">
              <div className="w-6 h-6 rounded-full border-2 border-black/10 border-t-[#1D1D1F] animate-spin" />
              <span className="text-xs text-[#86868B]">Loading transactions...</span>
            </div>
          ) : successfulTransactions.length > 0 ? (
            <>
              <div className="divide-y divide-black/[0.04]">
                {successfulTransactions.map((tx) => {
                  const isOut = tx.sourceAccountId === selectedAccount?.id;
                  const counterpartyId = isOut ? tx.destinationAccountId : tx.sourceAccountId;
                  const dateObj = new Date(tx.createdAt || 0);
                  const dateString = dateObj.toLocaleDateString("en-US", {
                    month: "short",
                    day: "numeric",
                  });
                  const timeString = dateObj.toLocaleTimeString("en-US", {
                    hour: "2-digit",
                    minute: "2-digit",
                  });

                  return (
                    <div
                      key={tx.id}
                      className="p-5 md:px-8 flex items-center justify-between hover:bg-[#FBFBFD] transition-colors group"
                    >
                      <div className="flex items-center gap-4">
                        {/* Direction Icon Bubble */}
                        <div
                          className={`w-11 h-11 rounded-full flex items-center justify-center transition-transform group-hover:scale-105 ${
                            isOut
                              ? "bg-[#F5F5F7] text-[#1D1D1F]"
                              : "bg-[#00D06C]/10 text-[#00A855]"
                          }`}
                        >
                          {isOut ? (
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                              <line x1="7" y1="17" x2="17" y2="7" />
                              <polyline points="7 7 17 7 17 17" />
                            </svg>
                          ) : (
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                              <line x1="17" y1="7" x2="7" y2="17" />
                              <polyline points="17 17 7 17 7 7" />
                            </svg>
                          )}
                        </div>

                        <div>
                          <div className="text-sm font-medium text-[#1D1D1F]">
                            {isOut ? "Transfer Out" : "Deposit / Transfer In"}
                          </div>
                          <div className="text-xs text-[#86868B] font-normal tracking-tight mt-0.5">
                            {isOut ? "To: " : "From: "}
                            <span className="font-medium text-[#1D1D1F]">
                              {counterpartyId ? `${counterpartyId.slice(0, 8)}••••${counterpartyId.slice(-4)}` : "—"}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="text-right">
                        <div
                          className={`text-base font-semibold tracking-tight tabular-nums ${
                            isOut ? "text-[#1D1D1F]" : "text-[#00A855]"
                          }`}
                        >
                          {isOut ? "-" : "+"}
                          {formatCurrencyAmount(tx.amount || 0, selectedAccount?.currency)}
                        </div>
                        <div className="text-[11px] text-[#86868B] mt-0.5">
                          {dateString} at {timeString}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Activity Pagination Controls */}
              {txTotalPages > 1 && (
                <div className="p-4 md:px-8 border-t border-black/[0.06] bg-[#FBFBFD] flex items-center justify-between text-xs">
                  <span className="text-[#86868B] font-medium">
                    Page {txPage + 1} of {Math.max(1, txTotalPages)}
                  </span>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setTxPage((p) => Math.max(0, p - 1))}
                      disabled={txPage === 0 || txLoading}
                      className="px-3 py-1.5 rounded-lg border border-black/[0.08] bg-white text-xs font-medium text-[#1D1D1F] hover:bg-black/[0.02] active:bg-black/[0.04] disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors shadow-2xs"
                    >
                      ← Previous
                    </button>
                    <button
                      type="button"
                      onClick={() => setTxPage((p) => p + 1)}
                      disabled={txIsLast || txLoading}
                      className="px-3 py-1.5 rounded-lg border border-black/[0.08] bg-white text-xs font-medium text-[#1D1D1F] hover:bg-black/[0.02] active:bg-black/[0.04] disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors shadow-2xs"
                    >
                      Next →
                    </button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="py-20 text-center flex flex-col items-center justify-center">
              <div className="w-12 h-12 rounded-full bg-[#F5F5F7] flex items-center justify-center text-[#86868B] mb-3">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
              </div>
              <div className="text-sm font-medium text-[#1D1D1F]">No settled transactions yet</div>
              <div className="text-xs text-[#86868B] mt-1">Completed transfers will appear here automatically.</div>
            </div>
          )}
        </div>
      </main>

      {/* Create Account Modal Window */}
      <CreateAccountModal
        isOpen={isNewAccountOpen}
        onClose={() => setIsNewAccountOpen(false)}
        onAccountCreated={handleAccountCreated}
      />
    </div>
  );
}
