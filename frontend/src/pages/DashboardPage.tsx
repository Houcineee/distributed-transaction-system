import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { client } from "../lib/api";
import type { components } from "../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];
type CardResponse = components["schemas"]["CardResponse"];

export function DashboardPage() {
    const { user, setUser } = useAuth();
    const [accounts, setAccounts] = useState<AccountResponse[]>([]);
    const [cards, setCards] = useState<CardResponse[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user || !user.id) return;
        
        async function fetchData() {
            try {
                const { data: accData } = await client.GET("/api/v1/users/{id}/accounts", {
                    params: { path: { id: user!.id! } }
                });
                
                if (accData && accData.length > 0) {
                    setAccounts(accData);
                    
                    if (accData[0].id) {
                        const { data: cardData } = await client.GET("/api/v1/cards/account/{accountId}", {
                            params: { path: { accountId: accData[0].id } }
                        });
                        
                        if (cardData) setCards(cardData);
                    }
                }
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        }
        
        fetchData();
    }, [user]);

    async function handleLogout() {
        try {
            await client.POST("/api/v1/auth/logout");
        } finally {
            setUser(null);
        }
    }

    if (loading) {
        return <div className="min-h-screen flex items-center justify-center font-serif text-[var(--accent)] bg-[var(--bg)] text-2xl italic">Waking up the Oracle ARM instance...</div>;
    }

    const primaryAccount = accounts[0];
    const primaryCard = cards[0];

    return (
        <div className="min-h-screen bg-[var(--bg-surface)] text-[var(--text-main)] font-sans flex flex-col md:flex-row">
            
            {/* Nav: N3 Side-rail */}
            <aside className="w-full md:w-64 border-r border-[var(--border)] bg-[var(--bg)] p-8 flex flex-col justify-between shrink-0 shadow-sm relative z-10">
                <div>
                    <h1 className="text-xl font-serif font-bold tracking-widest text-[var(--accent)] mb-12 uppercase">Bank of Houcine</h1>
                    <nav className="space-y-6 text-sm font-semibold tracking-wider uppercase text-[var(--text-muted)]">
                        <a href="#" className="block text-[var(--accent)]">Dashboard</a>
                        <a href="#" className="block hover:text-[var(--text-main)] transition-colors">Transfers</a>
                        <a href="#" className="block hover:text-[var(--text-main)] transition-colors">Settings</a>
                    </nav>
                </div>
                <div className="pt-8 border-t border-[var(--border)]">
                    <p className="text-sm text-[var(--text-main)] font-medium mb-1 truncate">{user?.name}</p>
                    <p className="text-xs text-[var(--text-muted)] mb-6 truncate">{user?.email}</p>
                    <button 
                        onClick={handleLogout}
                        className="text-xs uppercase tracking-wider font-semibold text-red-500 hover:text-red-600 transition-colors"
                    >
                        Disconnect
                    </button>
                </div>
            </aside>

            {/* Main Content: Bento Grid */}
            <main className="flex-1 p-6 md:p-12 overflow-y-auto mesh-bg relative">
                <header className="mb-12 flex justify-between items-end relative z-10">
                    <div>
                        <h2 className="text-5xl font-serif text-[var(--text-main)] tracking-tight">Welcome back, {user?.name}.</h2>
                        <p className="text-[var(--text-muted)] mt-4 font-sans text-lg font-medium">Your entirely fictitious wealth is secure on The Beast.</p>
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
                                {primaryAccount?.balance ? Number(primaryAccount.balance).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'} 
                                <span className="text-3xl text-[var(--accent)] ml-3 italic">{primaryAccount?.currency || 'USD'}</span>
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
                            <div className="z-10 mt-auto text-sm opacity-70">No card issued.</div>
                        )}
                    </div>

                    {/* Bento Box 3: Market Status */}
                    <div className="glass-panel p-8 rounded-xl flex flex-col justify-between">
                        <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)]">Server Load</span>
                        <div className="mt-auto">
                            <div className="flex items-end gap-2 mb-2">
                                <span className="text-5xl font-serif text-[var(--accent)] tracking-tight">0.01%</span>
                            </div>
                            <p className="text-sm text-[var(--text-muted)] font-medium">The 24-core Oracle instance is bored.</p>
                        </div>
                    </div>

                    {/* Bento Box 4: Recent Tx (Span 2) */}
                    <div className="md:col-span-2 glass-panel p-8 rounded-xl flex flex-col">
                        <div className="flex justify-between items-center mb-6">
                            <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)]">Recent Transactions</span>
                            <span className="text-xs text-[var(--accent)] hover:text-[var(--accent-hover)] font-semibold transition-colors cursor-pointer">View all &rarr;</span>
                        </div>
                        <div className="flex-1 flex items-center justify-center border-2 border-dashed border-[var(--border)] bg-[var(--bg)]/50 rounded-lg">
                            <p className="text-sm text-[var(--text-muted)] text-center font-medium">
                                Nothing to see here. <br/>
                                <span className="text-xs opacity-70 block mt-2">Try sending someone fake money.</span>
                            </p>
                        </div>
                    </div>

                </div>
            </main>
        </div>
    );
}
