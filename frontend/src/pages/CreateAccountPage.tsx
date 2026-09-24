import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { client } from "../lib/api";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useNavigate } from "react-router-dom";
import { SideNav } from "../components/SideNav";

const createAccountSchema = z.object({
  currency: z.enum(["USD", "EUR", "GBP", "MAD"]),
  cardType: z.enum(["DEBIT", "CREDIT"]),
});

type CreateAccountFormData = z.infer<typeof createAccountSchema>;

export function CreateAccountPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [serverError, setServerError] = useState<string | null>(null);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<CreateAccountFormData>({
        resolver: zodResolver(createAccountSchema),
        defaultValues: {
            currency: "USD",
            cardType: "DEBIT"
        }
    });

    const onSubmit = async (data: CreateAccountFormData) => {
        setServerError(null);
        if (!user || !user.id) return;

        try {
            const { error } = await client.POST("/api/v1/accounts", {
                body: { 
                    userId: user.id,
                    currency: data.currency,
                    cardType: data.cardType
                }
            });

            if (error) {
                setServerError((error as any).message || "Failed to create account. Our butler dropped the paperwork.");
                return;
            }

            navigate("/dashboard");
        } catch (err) {
            setServerError("The network is congested. Try again shortly.");
        }
    };

    return (
        <div className="min-h-screen bg-[var(--bg-surface)] text-[var(--text-main)] font-sans flex flex-col md:flex-row">
            <SideNav />

            <main className="flex-1 overflow-hidden flex">
                
                {/* Left Pane - Form */}
                <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 relative bg-[var(--bg)] z-10 shadow-2xl overflow-y-auto">
                    <div className="w-full max-w-md space-y-10">
                        <div>
                            <h2 className="text-3xl font-serif text-[var(--text-main)]">Open New Ledger</h2>
                            <p className="mt-2 text-sm text-[var(--text-muted)]">
                                Expand your fictitious financial empire with a new currency.
                            </p>
                        </div>
                        
                        <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                            
                            {serverError && (
                                <div className="bg-red-50 border border-red-100 text-red-600 p-4 text-sm font-sans rounded-md">
                                    {serverError}
                                </div>
                            )}

                            <div className="space-y-6">
                                <div>
                                    <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Base Currency</label>
                                    <select
                                        {...register("currency")}
                                        className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md cursor-pointer"
                                    >
                                        <option value="MAD">MAD (Dirham)</option>
                                        <option value="USD">USD (Dollar)</option>
                                        <option value="EUR">EUR (Euro)</option>
                                        <option value="GBP">GBP (Pound)</option>
                                    </select>
                                    {errors.currency && <p className="mt-2 text-xs text-red-600 font-medium">{errors.currency.message}</p>}
                                </div>

                                <div>
                                    <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Plastic Type</label>
                                    <select
                                        {...register("cardType")}
                                        className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md cursor-pointer"
                                    >
                                        <option value="DEBIT">Debit Card</option>
                                        <option value="CREDIT">Credit Card</option>
                                    </select>
                                    {errors.cardType && <p className="mt-2 text-xs text-red-600 font-medium">{errors.cardType.message}</p>}
                                </div>
                            </div>

                            <div className="pt-8 flex gap-4">
                                <button
                                    type="button"
                                    onClick={() => navigate("/dashboard")}
                                    className="w-1/3 bg-[var(--bg-surface)] border border-[var(--border)] text-[var(--text-muted)] hover:text-[var(--text-main)] hover:border-[var(--border-hover)] transition-colors py-4 px-4 font-serif text-lg tracking-wide rounded-md font-medium"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={isSubmitting}
                                    className="w-2/3 bg-[var(--text-main)] text-[var(--bg)] hover:bg-[var(--accent)] transition-colors py-4 px-4 font-serif text-lg tracking-wide rounded-md shadow-lg shadow-black/5 disabled:opacity-50"
                                >
                                    {isSubmitting ? "Processing..." : "Open Account"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                {/* Right Pane - Editorial with Mesh */}
                <div className="hidden lg:flex w-1/2 flex-col justify-between p-12 border-l border-[var(--border)] relative mesh-bg overflow-hidden">
                    <div className="relative z-10 self-end text-right">
                        <h1 className="text-xl font-serif font-semibold tracking-widest text-[var(--accent)] mb-2 uppercase">Bank of Houcine</h1>
                        <p className="text-[var(--text-muted)] text-sm tracking-wide uppercase">Wealth Expansion</p>
                    </div>

                    <div className="relative z-10 space-y-6">
                        <h2 className="text-5xl font-serif leading-tight text-[var(--text-main)]">
                            Because one fake account<br />
                            <span className="serif-italic text-[var(--accent)]">is never enough.</span>
                        </h2>
                        <p className="text-lg text-[var(--text-main)] max-w-md leading-relaxed opacity-80">
                            Diversify your imaginary portfolio. Open ledgers in multiple currencies and watch them all simultaneously sit at a zero balance.
                        </p>
                        
                        <div className="pt-8 flex gap-8">
                            <div className="flex flex-col">
                                <span className="text-3xl font-serif text-[var(--text-main)]">4</span>
                                <span className="text-xs uppercase tracking-widest text-[var(--text-muted)] mt-1">Currencies</span>
                            </div>
                            <div className="w-px bg-[var(--border)]"></div>
                            <div className="flex flex-col">
                                <span className="text-3xl font-serif text-[var(--text-main)]">0</span>
                                <span className="text-xs uppercase tracking-widest text-[var(--text-muted)] mt-1">Fees</span>
                            </div>
                        </div>
                    </div>
                </div>

            </main>
        </div>
    );
}
