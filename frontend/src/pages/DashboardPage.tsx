import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { client } from "../lib/api";
import { SideNav } from "../components/SideNav";
import type { components } from "../types/api";
import { Link } from "react-router-dom";

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

    // Initial fetch of accounts
    useEffect(() => {
        if (!user || !user.id) return;
        
        async function fetchAccounts() {
            try {
                const { data: accData } = await client.GET("/api/v1/users/{id}/accounts", {
                    params: { path: { id: user!.id! } }
                });
                
                if (accData && accData.length > 0) {
                    setAccounts(accData);
                    setSelectedAccountId(accData[0].id || null);
                }
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        }
        
        fetchAccounts();
    }, [user]);

    // Fetch cards and transactions when selectedAccountId changes
    useEffect(() => {
        if (!selectedAccountId) return;

        async function fetchAccountDetails() {
            try {
                const [{ data: cardData }, { data: txData }] = await Promise.all([
                    client.GET("/api/v1/cards/account/{accountId}", {
                        params: { path: { accountId: selectedAccountId! } }
                    }),
                    client.GET("/api/v1/accounts/{id}/transactions", {
                        params: { 
                            path: { id: selectedAccountId! },
                            query: { pageable: { page: 0, size: 5 } } as any 
                        }
                    })
                ]);
                
                setCards(cardData || []);
                setTransactions(txData?.content || []);
            } catch (err) {
                console.error(err);
            }
        }

        fetchAccountDetails();
    }, [selectedAccountId]);

    if (loading) {
        return <div className="min-h-screen flex items-center justify-center font-serif text-[var(--accent)] bg-[var(--bg)] text-2xl italic">Opening the vault...</div>;
    }

    const selectedAccount = accounts.find(a => a.id === selectedAccountId) || accounts[0];
    const primaryCard = cards[0];
    const recentTxs = transactions.slice(0, 3);

    return (
        <div className="min-h-screen bg-[var(--bg-surface)] text-[var(--text-main)] font-sans flex flex-col md:flex-row">
            
            <SideNav />

            {/* Main Content: Bento Grid */}
            <main className="flex-1 p-6 md:p-12 overflow-y-auto mesh-bg relative">
                <header className="mb-12 flex justify-between items-end relative z-10">
                    <div>
                        <h2 className="text-5xl font-serif text-[var(--text-main)] tracking-tight">Welcome back, {user?.name}.</h2>
                        <p className="text-[var(--text-muted)] mt-4 font-sans text-lg font-medium">Your entirely fictitious wealth is secure.</p>
                    </div>
                    <div className="flex items-center gap-4">
                        {accounts.length > 1 && (
                            <div className="flex items-center gap-2">
                                <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)]">Active Ledger:</span>
                                <select 
                                    className="text-sm font-semibold bg-[var(--bg)] px-4 py-2 border border-[var(--border)] rounded-md text-[var(--text-main)] focus:outline-none focus:border-[var(--accent)] shadow-sm cursor-pointer"
                                    value={selectedAccountId || ''}
                                    onChange={(e) => setSelectedAccountId(e.target.value)}
                                >
                                    {accounts.map((acc, idx) => (
                                        <option key={acc.id} value={acc.id}>
                                            Account #{idx + 1} ({acc.currency})
                                        </option>
                                    ))}
                                </select>
                            </div>
                        )}
                        <Link to="/accounts/new" className="text-sm bg-[var(--bg)] hover:bg-[var(--bg-surface)] px-4 py-2 border border-[var(--border)] rounded-md text-[var(--text-main)] font-semibold transition-colors shadow-sm">
                            + Open Ledger
                        </Link>
                    </div>
                </header>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 auto-rows-[280px] relative z-10">
                    
                    {/* Bento Box 1: Balance (Span 2 cols) */}
                    <div className="md:col-span-2 glass-panel p-8 flex flex-col justify-between group rounded-xl">
                        <div className="flex justify-between items-start">
                            <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)]">Total Balance</span>
                            <span className="text-xs bg-[var(--accent)]/10 px-3 py-1 border border-[var(--accent)]/20 rounded-full text-[var(--accent)] uppercase tracking-wider font-semibold">
                                Checking
                            </span>
                        </div>
                        <div>
                            <h3 className="text-7xl font-serif tracking-tight mb-4 text-[var(--text-main)]">
                                {selectedAccount?.balance ? Number(selectedAccount.balance).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'} 
                                <span className="text-3xl text-[var(--accent)] ml-3 italic">{selectedAccount?.currency || 'USD'}</span>
                            </h3>
                            <p className="text-sm text-[var(--text-muted)] font-medium">
                                Almost enough to buy a coffee (if it was real).
                            </p>
                        </div>
                    </div>

                    {/* Bento Box 2: Virtual Card */}
                    <div className="glass-panel p-8 flex flex-col justify-between relative overflow-hidden rounded-xl border-none text-white shadow-xl bg-gradient-to-br from-[var(--text-main)] to-[#1e293b]">
                        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,rgba(16,185,129,0.4),transparent_70%)] mix-blend-screen"></div>
                        <span className="text-xs font-semibold uppercase tracking-widest opacity-80 z-10 text-[var(--accent)]">The Black Card</span>
                        
                        {primaryCard ? (
                            <div className="z-10 mt-auto">
                                <p className="font-serif text-3xl tracking-widest mb-6 opacity-95">
                                    {primaryCard.cardNumber?.replace(/(.{4})/g, '$1 ').trim()}
                                </p>
                                <div className="flex justify-between text-xs font-sans uppercase tracking-widest font-semibold opacity-70">
                                    <span>{user?.name}</span>
                                    <span>{primaryCard.expirationDate?.substring(2,7).replace('-','/')}</span>
                                </div>
                            </div>
                        ) : (
                            <div className="z-10 mt-auto text-sm opacity-70">No card issued for this account.</div>
                        )}
                    </div>

                    {/* Bento Box 3: Market Status */}
                    <div className="glass-panel p-8 rounded-xl flex flex-col justify-between">
                        <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)]">Portfolio</span>
                        <div className="mt-auto">
                            <div className="flex items-end gap-2 mb-2">
                                <span className="text-5xl font-serif text-[var(--accent)] tracking-tight">↑ 12.4%</span>
                            </div>
                            <p className="text-sm text-[var(--text-muted)] font-medium">Return on imaginary investments.</p>
                        </div>
                    </div>

                    {/* Bento Box 4: Recent Tx (Span 2) */}
                    <div className="md:col-span-2 glass-panel p-8 rounded-xl flex flex-col">
                        <div className="flex justify-between items-center mb-6">
                            <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)]">Recent Transactions</span>
                            <Link to="/transfers" className="text-xs text-[var(--accent)] hover:text-[var(--accent-hover)] font-semibold transition-colors">View all &rarr;</Link>
                        </div>
                        
                        {recentTxs.length > 0 ? (
                            <div className="flex-1 overflow-hidden flex flex-col gap-4">
                                {recentTxs.map(tx => {
                                    const isSender = tx.sourceAccountId === selectedAccount?.id;
                                    return (
                                        <div key={tx.id} className="flex justify-between items-center py-2 border-b border-[var(--border)] last:border-0">
                                            <div>
                                                <p className="text-sm font-semibold">{isSender ? "Outgoing Transfer" : "Incoming Transfer"}</p>
                                                <p className="text-xs text-[var(--text-muted)] font-mono mt-1">{new Date(tx.createdAt || '').toLocaleDateString()}</p>
                                            </div>
                                            <span className={`text-lg font-mono ${isSender ? "text-[var(--text-main)]" : "text-[var(--accent)]"}`}>
                                                {isSender ? "-" : "+"}{tx.amount}
                                            </span>
                                        </div>
                                    )
                                })}
                            </div>
                        ) : (
                            <div className="flex-1 flex items-center justify-center border-2 border-dashed border-[var(--border)] bg-[var(--bg)]/50 rounded-lg">
                                <p className="text-sm text-[var(--text-muted)] text-center font-medium">
                                    Nothing to see here. <br/>
                                    <span className="text-xs opacity-70 block mt-2">Try sending someone fake money.</span>
                                </p>
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}
