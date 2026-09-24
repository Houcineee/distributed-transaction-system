import { useState } from "react";
import { useAuth } from "../context/useAuth";
import { client, getApiErrorMessage } from "../lib/api";
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
                setServerError(getApiErrorMessage(error, "We could not open that account. Check the details and try again."));
                return;
            }

            navigate("/dashboard");
        } catch {
            setServerError("We could not reach the account service. Check your connection and try again.");
        }
    };

    return (
        <div className="app-shell">
            <SideNav />
            <main className="app-main">
                <div className="page-wrap">
                    <div className="transfer-layout">
                        <header className="transfer-brief">
                            <p className="eyebrow">Accounts / new</p>
                            <h1>Give a balance<br /><span className="serif-accent">its own room.</span></h1>
                            <p>Add another account in a currency that suits your plans. The backend will handle the serious part.</p>
                        </header>
                        <section className="transfer-form">
                            <div className="form-frame" style={{ width: "100%" }}>
                                <p className="eyebrow">Account preferences</p>
                        
                        <form className="form-stack" onSubmit={handleSubmit(onSubmit)}>
                            
                            {serverError && (
                                <div className="alert">
                                    {serverError}
                                </div>
                            )}

                            <div className="form-stack">
                                <div className="field">
                                    <label>Base currency</label>
                                    <select
                                        {...register("currency")}
                                    >
                                        <option value="MAD">MAD (Dirham)</option>
                                        <option value="USD">USD (Dollar)</option>
                                        <option value="EUR">EUR (Euro)</option>
                                        <option value="GBP">GBP (Pound)</option>
                                    </select>
                                    {errors.currency && <p className="field-error">{errors.currency.message}</p>}
                                </div>

                                <div className="field">
                                    <label>Card type</label>
                                    <select
                                        {...register("cardType")}
                                    >
                                        <option value="DEBIT">Debit Card</option>
                                        <option value="CREDIT">Credit Card</option>
                                    </select>
                                    {errors.cardType && <p className="field-error">{errors.cardType.message}</p>}
                                </div>
                            </div>

                            <div className="form-actions">
                                <button
                                    type="button"
                                    onClick={() => navigate("/dashboard")}
                                    className="btn-quiet"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={isSubmitting}
                                    className="btn-primary"
                                >
                                    {isSubmitting ? "Processing..." : "Open Account"}
                                </button>
                            </div>
                        </form>
                            </div>
                        </section>
                    </div>
                </div>
            </main>
        </div>
    );
}
