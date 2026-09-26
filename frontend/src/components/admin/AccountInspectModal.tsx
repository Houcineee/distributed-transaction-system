import { useState, useEffect } from "react";
import { client } from "../../lib/api";
import { formatCurrencyAmount } from "../../lib/currency";
import { ClickableId } from "../ClickableId";
import type { components } from "../../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];
type CardResponse = components["schemas"]["CardResponse"];
type TransactionResponse = components["schemas"]["TransactionResponse"];
type UserResponse = components["schemas"]["UserResponse"];

interface AccountInspectModalProps {
  account: AccountResponse | null;
  onClose: () => void;
  onAccountUpdated?: () => void;
}

export function AccountInspectModal({ account, onClose }: AccountInspectModalProps) {
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [owner, setOwner] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [cardActionLoading, setCardActionLoading] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"cards" | "transactions" | "owner">("cards");

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    if (!account?.id) return;
    const accountId = account.id;
    const userId = account.userId;

    async function loadData() {
      setLoading(true);
      try {
        const [cardsRes, txRes, userRes] = await Promise.all([
          client.GET("/api/v1/cards/account/{accountId}", {
            params: { path: { accountId } },
          }),
          client.GET("/api/v1/accounts/{id}/transactions", {
            params: {
              path: { id: accountId },
              query: { pageable: { page: 0, size: 20 } },
            },
          }),
          userId
            ? client.GET("/api/v1/users/{id}", {
                params: { path: { id: userId } },
              })
            : Promise.resolve({ data: null }),
        ]);

        setCards(cardsRes.data || []);
        setTransactions(txRes.data?.content || []);
        setOwner(userRes.data || null);
      } catch (err) {
        console.error("Failed to load details", err);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [account]);

  if (!account) return null;

  const handleToggleCardStatus = async (card: CardResponse) => {
    if (!card.id) return;
    setCardActionLoading(card.id);
    try {
      if (card.status === "ACTIVE") {
        const { data } = await client.PATCH("/api/v1/cards/{cardId}/block", {
          params: { path: { cardId: card.id } },
        });
        if (data) {
          setCards((prev) => prev.map((c) => (c.id === card.id ? data : c)));
        }
      } else {
        const { data } = await client.PATCH("/api/v1/cards/{cardId}/activate", {
          params: { path: { cardId: card.id } },
        });
        if (data) {
          setCards((prev) => prev.map((c) => (c.id === card.id ? data : c)));
        }
      }
    } catch (err) {
      console.error("Failed to update card", err);
    } finally {
      setCardActionLoading(null);
    }
  };

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs select-none animate-in fade-in duration-150 cursor-pointer"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-xl bg-white border border-black/[0.08] rounded-2xl shadow-xl overflow-hidden flex flex-col max-h-[85vh] cursor-default"
      >
        {/* Header */}
        <div className="px-5 py-3.5 border-b border-black/[0.06] flex items-center justify-between bg-[#FBFBFD]">
          <div className="flex items-center gap-2">
            <span className="font-bold text-sm text-[#1D1D1F]">Account</span>
            <ClickableId id={account.id} truncate={false} className="text-xs text-[#86868B]" />
          </div>
          <button
            onClick={onClose}
            className="w-7 h-7 rounded-full bg-black/[0.04] hover:bg-black/[0.08] text-[#86868B] hover:text-[#1D1D1F] flex items-center justify-center transition-colors cursor-pointer text-xs"
          >
            ✕
          </button>
        </div>

        {/* Balance Strip */}
        <div className="px-5 py-4 bg-white border-b border-black/[0.06] flex items-baseline justify-between">
          <div>
            <div className="text-[10px] font-medium text-[#86868B] uppercase tracking-wider">Balance</div>
            <div className="text-xl font-bold text-[#1D1D1F] font-mono mt-0.5">
              {formatCurrencyAmount(account.balance ?? 0, account.currency)}
            </div>
          </div>
          <div className="text-right">
            <div className="text-[10px] font-medium text-[#86868B] uppercase tracking-wider">Currency</div>
            <div className="text-sm font-semibold text-[#1D1D1F] mt-0.5">{account.currency}</div>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="px-5 pt-2 border-b border-black/[0.06] bg-[#FBFBFD] flex gap-4 text-xs">
          <button
            onClick={() => setActiveTab("cards")}
            className={`pb-2 font-medium transition-colors border-b-2 cursor-pointer ${
              activeTab === "cards"
                ? "border-[#1D1D1F] text-[#1D1D1F] font-semibold"
                : "border-transparent text-[#86868B]"
            }`}
          >
            Cards ({cards.length})
          </button>
          <button
            onClick={() => setActiveTab("transactions")}
            className={`pb-2 font-medium transition-colors border-b-2 cursor-pointer ${
              activeTab === "transactions"
                ? "border-[#1D1D1F] text-[#1D1D1F] font-semibold"
                : "border-transparent text-[#86868B]"
            }`}
          >
            Ledger ({transactions.length})
          </button>
          <button
            onClick={() => setActiveTab("owner")}
            className={`pb-2 font-medium transition-colors border-b-2 cursor-pointer ${
              activeTab === "owner"
                ? "border-[#1D1D1F] text-[#1D1D1F] font-semibold"
                : "border-transparent text-[#86868B]"
            }`}
          >
            Owner
          </button>
        </div>

        {/* Content Body */}
        <div className="p-5 overflow-y-auto flex-1 min-h-[180px]">
          {loading ? (
            <div className="py-10 flex flex-col items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-[#C5A059]/30 border-t-[#C5A059] rounded-full animate-spin" />
              <span className="text-xs text-[#86868B] font-mono">Loading...</span>
            </div>
          ) : (
            <>
              {/* Cards */}
              {activeTab === "cards" && (
                <div>
                  {cards.length === 0 ? (
                    <div className="py-8 text-center text-xs text-[#86868B]">No cards.</div>
                  ) : (
                    <div className="space-y-2.5">
                      {cards.map((card) => {
                        const isBlocked = card.status === "BLOCKED";
                        const lastFour = card.cardNumber ? card.cardNumber.slice(-4) : "••••";
                        return (
                          <div
                            key={card.id}
                            className="p-3 rounded-xl border border-black/[0.06] bg-[#FBFBFD] flex items-center justify-between"
                          >
                            <div className="flex items-center gap-3">
                              <div className="w-9 h-9 rounded-lg bg-black text-white flex items-center justify-center font-mono text-[9px] font-bold">
                                {card.cardType || "DEBIT"}
                              </div>
                              <div>
                                <div className="text-xs font-mono font-medium text-[#1D1D1F]">
                                  •••• {lastFour}
                                </div>
                                <div className="text-[10px] text-[#86868B]">
                                  {card.expirationDate} • {card.cardholderName}
                                </div>
                              </div>
                            </div>

                            <div className="flex items-center gap-2">
                              <span
                                className={`px-1.5 py-0.5 rounded text-[9px] font-mono font-semibold uppercase ${
                                  isBlocked
                                    ? "bg-red-50 text-red-600 border border-red-200"
                                    : "bg-emerald-50 text-emerald-600 border border-emerald-200"
                                }`}
                              >
                                {card.status}
                              </span>

                              <button
                                onClick={() => handleToggleCardStatus(card)}
                                disabled={cardActionLoading === card.id}
                                className={`px-2.5 py-1 rounded-lg text-xs font-medium cursor-pointer transition-colors ${
                                  isBlocked
                                    ? "bg-black text-white"
                                    : "bg-red-50 text-red-600 hover:bg-red-100 border border-red-200"
                                }`}
                              >
                                {cardActionLoading === card.id
                                  ? "..."
                                  : isBlocked
                                  ? "Activate"
                                  : "Block"}
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}

              {/* Transactions */}
              {activeTab === "transactions" && (
                <div>
                  {transactions.length === 0 ? (
                    <div className="py-8 text-center text-xs text-[#86868B]">No transactions.</div>
                  ) : (
                    <div className="divide-y divide-black/[0.04] border border-black/[0.06] rounded-xl overflow-hidden">
                      {transactions.map((tx) => {
                        const isDebit = tx.sourceAccountId === account.id;
                        return (
                          <div
                            key={tx.id}
                            className="p-3 bg-white flex items-center justify-between text-xs"
                          >
                            <div className="flex items-center gap-2.5">
                              <span className={`font-mono ${isDebit ? "text-red-500" : "text-emerald-500"}`}>
                                {isDebit ? "↑" : "↓"}
                              </span>
                              <span className="text-[11px] text-[#86868B] font-mono">
                                {tx.createdAt ? new Date(tx.createdAt).toLocaleDateString() : "—"}
                              </span>
                            </div>
                            <div className="font-mono font-semibold text-[#1D1D1F]">
                              {isDebit ? "-" : "+"}
                              {formatCurrencyAmount(tx.amount ?? 0, account.currency)}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}

              {/* Owner */}
              {activeTab === "owner" && (
                <div>
                  {owner ? (
                    <div className="p-4 rounded-xl border border-black/[0.06] bg-[#FBFBFD] space-y-2 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-[#1D1D1F]">{owner.name}</span>
                        <span className="text-[#86868B] font-mono">{owner.email}</span>
                      </div>
                      <div className="flex items-center justify-between pt-2 border-t border-black/[0.06] text-[11px]">
                        <span className="text-[#86868B]">ID</span>
                        <ClickableId id={owner.id} truncate={false} className="text-[#1D1D1F]" />
                      </div>
                    </div>
                  ) : (
                    <div className="py-8 text-center text-xs text-[#86868B]">No owner data.</div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
