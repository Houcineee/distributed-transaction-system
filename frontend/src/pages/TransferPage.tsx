import { useEffect, useState } from "react";
import { useAuth } from "../context/useAuth";
import { client, getApiErrorMessage } from "../lib/api";
import { SideNav } from "../components/SideNav";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import type { components } from "../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];

const transferSchema = z.object({
    destAccount: z.string().min(10, "A valid UUID is required."),
    amount: z.number().positive("Amount must be greater than zero."),
});

type TransferFormData = z.infer<typeof transferSchema>;

export function TransferPage() {
    const { user } = useAuth();
    const [accounts, setAccounts] = useState<AccountResponse[]>([]);
    const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    
    const [serverError, setServerError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<TransferFormData>({
        resolver: zodResolver(transferSchema),
    });

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

    const onSubmit = async (data: TransferFormData) => {
        setServerError(null);
        setSuccessMessage(null);
        
        const selectedAccount = accounts.find(a => a.id === selectedAccountId);
        if (!selectedAccount) return;

        const availableBalance = Number(selectedAccount.balance ?? 0);
        if (data.amount > availableBalance) {
            setServerError(`Insufficient funds: attempted ${data.amount.toLocaleString()} ${selectedAccount.currency}, available ${availableBalance.toLocaleString()}. The bank said no. We asked nicely.`);
            return;
        }

        try {
            const { error: txError } = await client.POST("/api/v1/transfers", {
                body: { srcAccount: selectedAccount.id!, destAccount: data.destAccount, amount: data.amount }
            });

            if (txError) {
                setServerError(getApiErrorMessage(txError, "Transaction rejected by core system."));
                return;
            }

            setSuccessMessage("Transfer complete. Money has successfully left the building.");
            reset();
            
            const { data: accData } = await client.GET("/api/v1/users/{id}/accounts", { params: { path: { id: user!.id! } } });
            if (accData && accData.length > 0) setAccounts(accData);
            
        } catch {
            setServerError("System timeout. Please verify transaction state before retrying.");
        }
    };

    if (loading) return (
        <div className="app-shell">
            <SideNav />
            <main className="app-main" style={{ display: "grid", placeItems: "center" }}>
                <div style={{ textAlign: "center" }}>
                    <div className="eyebrow">Initializing secure channel…</div>
                </div>
            </main>
        </div>
    );

    const selectedAccount = accounts.find(a => a.id === selectedAccountId);

    return (
        <div className="app-shell">
            <SideNav />
            <main className="app-main">
                <header className="mb-8">
                    <h1 style={{ fontSize: "3.5rem", lineHeight: 1 }}>Execute Payment</h1>
                    <p className="muted mt-2" style={{ fontSize: "0.85rem" }}>Wire funds internally or to external entities.</p>
                </header>

                <div className="grid-2">
                    <div className="panel">
                        <div className="panel-header">
                            <span className="eyebrow">Transfer Parameters</span>
                        </div>
                        <div className="panel-body">
                            {successMessage && (
                                <div className="alert alert-success">
                                    <span className="alert-title">Executed</span>
                                    <span className="alert-sub">{successMessage}</span>
                                </div>
                            )}

                            {serverError && (
                                <div className="alert alert-error">
                                    <span className="alert-title">Failed</span>
                                    <span className="alert-sub">{serverError}</span>
                                </div>
                            )}

                            <form onSubmit={handleSubmit(onSubmit)}>
                                <div className="field">
                                    <label>Source Account</label>
                                    <select value={selectedAccountId || ''} onChange={(e) => setSelectedAccountId(e.target.value)}>
                                        {accounts.map((acc, idx) => (
                                            <option key={acc.id} value={acc.id}>Account {String(idx + 1).padStart(2, '0')} — {Number(acc.balance).toLocaleString()} {acc.currency}</option>
                                        ))}
                                    </select>
                                </div>

                                <div className="field mt-4">
                                    <label>Destination UUID</label>
                                    <input type="text" {...register("destAccount")} placeholder="00000000-0000-0000-0000-000000000000" />
                                    {errors.destAccount && <div className="field-error">{errors.destAccount.message}</div>}
                                </div>

                                <div className="field mt-4">
                                    <label>Amount ({selectedAccount?.currency})</label>
                                    <input type="number" step="0.01" {...register("amount", { valueAsNumber: true })} placeholder="0.00" />
                                    {errors.amount && <div className="field-error">{errors.amount.message}</div>}
                                </div>

                                <div className="mt-8 pt-4" style={{ borderTop: "1px solid var(--color-rule)", display: "flex", justifyContent: "flex-end" }}>
                                    <button type="submit" className="btn btn-primary" disabled={isSubmitting || !selectedAccountId}>
                                        {isSubmitting ? "PROCESSING..." : "AUTHORIZE TRANSFER"}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>

                    <div>
                        <div className="panel">
                            <div className="panel-header">
                                <span className="eyebrow">Source Details</span>
                            </div>
                            <div className="panel-body">
                                <div className="flex-between mb-4">
                                    <span className="eyebrow muted">UUID</span>
                                    <span className="mono" style={{ fontSize: "0.75rem" }}>{selectedAccount?.id}</span>
                                </div>
                                <div className="flex-between mb-4">
                                    <span className="eyebrow muted">Currency</span>
                                    <span className="mono" style={{ fontSize: "0.8rem" }}>{selectedAccount?.currency}</span>
                                </div>
                                <div className="flex-between pt-4" style={{ borderTop: "1px solid var(--color-rule)" }}>
                                    <span className="eyebrow">Max Drawable</span>
                                    <span className="mono" style={{ fontSize: "1rem", fontWeight: 700 }}>
                                        {Number(selectedAccount?.balance ?? 0).toLocaleString()} {selectedAccount?.currency}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
}
