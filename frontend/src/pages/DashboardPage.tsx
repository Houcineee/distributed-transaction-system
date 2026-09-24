import { useEffect, useState } from "react";
import { useAuth } from "../context/useAuth";
import { client } from "../lib/api";
import { SideNav } from "../components/SideNav";
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

    useEffect(() => {
        if (!user?.id) return;
        async function fetchAccounts() {
            try {
                const { data } = await client.GET("/api/v1/users/{id}/accounts", { params: { path: { id: user!.id! } } });
                if (data?.length) {
                    setAccounts(data);
                    setSelectedAccountId(data[0].id || null);
                }
            } finally { setLoading(false); }
        }
        fetchAccounts();
    }, [user]);

    useEffect(() => {
        if (!selectedAccountId) return;
        const accountId = selectedAccountId;
        async function fetchDetails() {
            const [{ data: cardData }, { data: txData }] = await Promise.all([
                client.GET("/api/v1/cards/account/{accountId}", { params: { path: { accountId } } }),
                client.GET("/api/v1/accounts/{id}/transactions", { params: { path: { id: accountId }, query: { pageable: { page: 0, size: 50 } } } }),
            ]);
            setCards(cardData || []);
            setTransactions(txData?.content || []);
        }
        fetchDetails().catch(console.error);
    }, [selectedAccountId]);

    if (loading) return (
        <div className="app-shell">
            <SideNav />
            <main className="app-main" style={{ display: "grid", placeItems: "center" }}>
                <div style={{ textAlign: "center" }}>
                    <div className="eyebrow">Checking your money...</div>
                    <div className="muted mt-2" style={{ fontSize: "0.75rem" }}>Yes, we counted it twice.</div>
                </div>
            </main>
        </div>
    );

    const selectedAccount = accounts.find((account) => account.id === selectedAccountId) || accounts[0];
    const primaryCard = cards[0];

    return (
        <div className="app-shell">
            <SideNav />
            <main className="app-main">
                <header className="flex-between mb-8">
                    <div>
                        <h1 style={{ fontSize: "3.5rem", lineHeight: 1 }}>Portfolio Overview</h1>
                        <p className="muted mt-2" style={{ fontSize: "0.85rem" }}>Live position for {user?.name}</p>
                    </div>
                    {accounts.length > 1 && (
                        <div style={{ width: "250px" }}>
                            <div className="field" style={{ marginBottom: 0 }}>
                                <label>Selected Entity</label>
                                <select value={selectedAccountId || ""} onChange={(e) => setSelectedAccountId(e.target.value)}>
                                    {accounts.map((account, index) => <option key={account.id} value={account.id}>Account {String(index + 1).padStart(2, "0")} ({account.currency})</option>)}
                                </select>
                            </div>
                        </div>
                    )}
                </header>

                <div className="grid-3 mb-8">
                    <div className="panel" style={{ gridColumn: "span 2" }}>
                        <div className="panel-header">
                            <span className="eyebrow">Available Liquidity</span>
                            <span className="eyebrow" style={{ color: "var(--color-ink)" }}>{selectedAccount?.currency || "USD"}</span>
                        </div>
                        <div className="panel-body flex-between" style={{ padding: "3rem 2rem" }}>
                            <div>
                                <div style={{ fontSize: "4rem", lineHeight: 1, letterSpacing: "-0.02em", fontFamily: "var(--font-serif)" }}>
                                    {selectedAccount?.balance ? Number(selectedAccount.balance).toLocaleString("en-US", { minimumFractionDigits: 2 }) : "0.00"}
                                </div>
                                <div className="muted mt-4" style={{ fontSize: "0.8rem", display: "flex", gap: "0.5rem", alignItems: "center" }}>
                                    <div style={{ width: "6px", height: "6px", background: "var(--color-success)", borderRadius: "50%" }}></div>
                                    Still technically yours.
                                </div>
                            </div>
                            <div style={{ textAlign: "right" }}>
                                <div className="eyebrow mb-2">Account ID</div>
                                <div className="mono" style={{ fontSize: "0.85rem" }}>{selectedAccount?.id || "—"}</div>
                            </div>
                        </div>
                    </div>

                    <div className="panel">
                        <div className="panel-header">
                            <span className="eyebrow">Active Instruments</span>
                        </div>
                        <div className="panel-body">
                            {primaryCard ? (
                                <>
                                    <div className="flex-between mb-4">
                                        <span className="eyebrow muted">Status</span>
                                        <span className="mono" style={{ fontSize: "0.8rem", color: "var(--color-success)" }}>ACTIVE</span>
                                    </div>
                                    <div className="flex-between mb-4">
                                        <span className="eyebrow muted">Type</span>
                                        <span className="mono" style={{ fontSize: "0.8rem" }}>{primaryCard.cardType}</span>
                                    </div>
                                    <div className="flex-between">
                                        <span className="eyebrow muted">Card Number</span>
                                        <span className="mono" style={{ fontSize: "0.8rem" }}>**** {primaryCard.cardNumber?.slice(-4) || "****"}</span>
                                    </div>
                                </>
                            ) : (
                                <div className="muted" style={{ fontSize: "0.8rem", fontStyle: "italic" }}>No cards issued.</div>
                            )}
                        </div>
                    </div>
                </div>

                <div className="panel">
                    <div className="panel-header">
                        <span className="eyebrow">Ledger Activity</span>
                    </div>
                    {transactions.length > 0 ? (
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Ref</th>
                                    <th>Date</th>
                                    <th>Direction</th>
                                    <th>Counterparty (Ext)</th>
                                    <th className="amount">Amount</th>
                                </tr>
                            </thead>
                            <tbody>
                                {transactions.map((tx, idx) => {
                                    const isOut = tx.sourceAccountId === selectedAccount?.id;
                                    return (
                                        <tr key={tx.id}>
                                            <td className="mono muted" style={{ fontSize: "0.7rem" }}>{String(idx + 1).padStart(3, "0")}</td>
                                            <td className="mono" style={{ fontSize: "0.8rem" }}>{new Date(tx.createdAt || "").toISOString().split('T')[0]}</td>
                                            <td>
                                                <span style={{ display: "inline-block", padding: "0.2rem 0.5rem", background: isOut ? "var(--color-paper)" : "color-mix(in oklch, var(--color-success) 10%, white)", color: isOut ? "var(--color-ink)" : "var(--color-success)", fontSize: "0.65rem", fontFamily: "var(--font-mono)", textTransform: "uppercase" }}>
                                                    {isOut ? "OUTBOUND" : "INBOUND"}
                                                </span>
                                            </td>
                                            <td className="mono" style={{ fontSize: "0.8rem" }}>{isOut ? tx.destinationAccountId?.slice(0, 8) : tx.sourceAccountId?.slice(0, 8)}...</td>
                                            <td className="amount" style={{ color: isOut ? "inherit" : "var(--color-success)" }}>
                                                {isOut ? "-" : "+"}{Number(tx.amount).toLocaleString("en-US", { minimumFractionDigits: 2 })}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    ) : (
                        <div className="panel-body text-center" style={{ padding: "4rem 2rem" }}>
                            <div className="eyebrow mb-2">No Transactions Found</div>
                            <div className="muted" style={{ fontSize: "0.85rem" }}>Enjoy the rare moment of financial peace.</div>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}
