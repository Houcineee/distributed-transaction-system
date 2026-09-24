import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { client } from "../lib/api";
import { SideNav } from "../components/SideNav";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import type { components } from "../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];

const transferSchema = z.object({
  destAccount: z.string().uuid("Must be a valid UUID. We use strict types here."),
  amount: z.number().positive("Cannot send negative money (nice try though).")
});

type TransferFormData = z.infer<typeof transferSchema>;

export function TransferPage() {
    const { user } = useAuth();
    const [accounts, setAccounts] = useState<AccountResponse[]>([]);
    const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const [serverError, setServerError] = useState<string | null>(null);
    const [copied, setCopied] = useState(false);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<TransferFormData>({
        resolver: zodResolver(transferSchema),
    });

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

    const onSubmit = async (data: TransferFormData) => {
        setServerError(null);
        setSuccessMessage(null);
        
        if (!selectedAccountId) {
            setServerError("Source account not found. You can't wire money you don't have.");
            return;
        }

        try {
            const { error: txError } = await client.POST("/api/v1/transfers", {
                body: { 
                    srcAccount: selectedAccountId, 
                    destAccount: data.destAccount, 
                    amount: data.amount 
                }
            });

            if (txError) {
                setServerError((txError as any).message || "Transaction failed. Our butler refused to carry the cash.");
                return;
            }

            setSuccessMessage(`Successfully routed ${data.amount} through the mainframe.`);
            reset();
            
            // Refresh account balances
            const { data: accData } = await client.GET("/api/v1/users/{id}/accounts", {
                params: { path: { id: user!.id! } }
            });
            if (accData && accData.length > 0) {
                setAccounts(accData);
            }
            
        } catch (err) {
            setServerError("The network is congested. Try again shortly.");
        }
    };

    if (loading) {
        return <div className="min-h-screen flex items-center justify-center font-serif text-[var(--accent)] bg-[var(--bg)] text-2xl italic">Connecting to wire system...</div>;
    }

    const selectedAccount = accounts.find(a => a.id === selectedAccountId);

    return (
        <div className="min-h-screen bg-[var(--bg-surface)] text-[var(--text-main)] font-sans flex flex-col md:flex-row">
            <SideNav />

            <main className="flex-1 p-6 md:p-12 overflow-y-auto mesh-bg relative">
                <header className="mb-12 relative z-10">
                    <h2 className="text-5xl font-serif text-[var(--text-main)] tracking-tight">Wire Transfer</h2>
                    <p className="text-[var(--text-muted)] mt-4 font-sans text-lg font-medium">Send entirely fake money with sub-millisecond latency.</p>
                </header>

                <div className="max-w-2xl relative z-10 space-y-8">
                    
                    <div className="glass-panel p-8 rounded-xl flex items-center justify-between">
                        <div className="w-full max-w-[280px]">
                            <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)] block mb-2">Routing From</span>
                            
                            {accounts.length > 1 ? (
                                <select 
                                    className="text-sm font-semibold bg-[var(--bg)] px-3 py-2 border border-[var(--border)] rounded text-[var(--text-main)] focus:outline-none focus:border-[var(--accent)] w-full mb-3"
                                    value={selectedAccountId || ''}
                                    onChange={(e) => setSelectedAccountId(e.target.value)}
                                >
                                    {accounts.map((acc, idx) => (
                                        <option key={acc.id} value={acc.id}>
                                            Account #{idx + 1} ({acc.currency})
                                        </option>
                                    ))}
                                </select>
                            ) : (
                                <div className="mb-3">
                                    <span className="inline-block text-sm font-semibold bg-[var(--bg)] px-3 py-2 border border-[var(--border)] rounded text-[var(--text-main)] w-full">
                                        Account #1 ({selectedAccount?.currency})
                                    </span>
                                </div>
                            )}

                            <div 
                                onClick={() => {
                                    if (selectedAccount?.id) {
                                        navigator.clipboard.writeText(selectedAccount.id);
                                        setCopied(true);
                                        setTimeout(() => setCopied(false), 2000);
                                    }
                                }}
                                className="group flex items-center justify-between gap-2 bg-[var(--bg-surface)] border border-[var(--border)] px-3 py-2.5 rounded-md cursor-pointer hover:border-[var(--accent)] transition-colors w-full shadow-sm"
                                title="Click to copy UUID"
                            >
                                <span className="text-[11px] font-mono text-[var(--text-main)] truncate select-none">
                                    {selectedAccount?.id || 'NO_ACCOUNT_FOUND'}
                                </span>
                                <span className={`text-[10px] uppercase tracking-wider font-semibold transition-opacity whitespace-nowrap ${copied ? 'text-green-500 opacity-100' : 'text-[var(--accent)] opacity-0 group-hover:opacity-100'}`}>
                                    {copied ? "Copied!" : "Copy"}
                                </span>
                            </div>
                        </div>
                        <div className="text-right">
                            <span className="text-xs font-semibold uppercase tracking-widest text-[var(--text-muted)] block mb-2">Available Liquidity</span>
                            <span className="text-xl font-serif text-[var(--accent)]">
                                {selectedAccount?.balance ? Number(selectedAccount.balance).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'} {selectedAccount?.currency}
                            </span>
                        </div>
                    </div>

                    <div className="glass-panel p-8 rounded-xl">
                        {successMessage && (
                            <div className="mb-6 bg-green-50 border border-green-200 text-green-700 p-4 text-sm font-sans rounded-md font-medium">
                                {successMessage}
                            </div>
                        )}

                        {serverError && (
                            <div className="mb-6 bg-red-50 border border-red-200 text-red-600 p-4 text-sm font-sans rounded-md font-medium">
                                {serverError}
                            </div>
                        )}

                        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                            <div>
                                <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Destination UUID</label>
                                <input
                                    type="text"
                                    {...register("destAccount")}
                                    className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] placeholder-[var(--text-muted)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md font-mono text-sm"
                                    placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                                />
                                {errors.destAccount && <p className="mt-2 text-xs text-red-600 font-medium">{errors.destAccount.message}</p>}
                            </div>

                            <div>
                                <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Amount to Wire</label>
                                <div className="relative">
                                    <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--text-muted)] font-serif italic text-lg">$</span>
                                    <input
                                        type="number"
                                        step="0.01"
                                        {...register("amount", { valueAsNumber: true })}
                                        className="w-full bg-[var(--bg-surface)] border border-[var(--border)] pl-10 pr-4 py-3 text-[var(--text-main)] placeholder-[var(--text-muted)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md font-serif text-xl"
                                        placeholder="0.00"
                                    />
                                </div>
                                {errors.amount && <p className="mt-2 text-xs text-red-600 font-medium">{errors.amount.message}</p>}
                            </div>

                            <div className="pt-4">
                                <button
                                    type="submit"
                                    disabled={isSubmitting || !selectedAccountId}
                                    className="w-full bg-[var(--text-main)] text-[var(--bg)] hover:bg-[var(--accent)] transition-colors py-4 px-4 font-serif text-lg tracking-wide rounded-md shadow-lg shadow-black/5 disabled:opacity-50"
                                >
                                    {isSubmitting ? "Executing..." : "Execute Transfer"}
                                </button>
                            </div>
                        </form>
                    </div>

                </div>
            </main>
        </div>
    );
}
