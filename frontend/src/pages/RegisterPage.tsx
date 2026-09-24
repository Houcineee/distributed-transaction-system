import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { client, getApiErrorMessage } from "../lib/api";

const registerSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters"),
  email: z.string().email("Invalid email address"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  currency: z.enum(["USD", "EUR", "GBP", "MAD"]),
  cardType: z.enum(["DEBIT", "CREDIT"]),
});

type RegisterFormData = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: { currency: "MAD", cardType: "DEBIT" }
  });

  const onSubmit = async (data: RegisterFormData) => {
    setServerError(null);
    try {
      const { error } = await client.POST("/api/v1/auth/register", {
        body: { name: data.name, email: data.email, password: data.password, currency: data.currency, cardType: data.cardType },
      });
      if (error) {
        setServerError(getApiErrorMessage(error, "Registration rejected."));
        return;
      }
      navigate("/login?registered=true");
    } catch {
      setServerError("System unreachable. Verify connection.");
    }
  };

  return (
    <div className="auth-layout">
      <div className="auth-box" style={{ maxWidth: "500px" }}>
        <div style={{ marginBottom: "2.5rem", display: "flex", alignItems: "center", gap: "0.75rem", justifyContent: "center" }}>
            <div style={{ width: "24px", height: "24px", background: "var(--color-ink)", color: "white", display: "grid", placeItems: "center", fontSize: "0.75rem", fontFamily: "var(--font-mono)", fontWeight: "bold" }}>B</div>
            <span style={{ fontSize: "0.85rem", fontWeight: 700, letterSpacing: "0.05em", textTransform: "uppercase" }}>Bank of Houcine</span>
        </div>

        <div style={{ textAlign: "center", marginBottom: "2.5rem" }}>
            <div className="eyebrow mb-2">Entity Creation</div>
            <h1 style={{ fontSize: "2.5rem", lineHeight: 1 }}>Establish Presence</h1>
            <p className="muted mt-2" style={{ fontSize: "0.8rem" }}>Provide your legal identity and initial instrument preferences.</p>
        </div>

        {serverError && (
            <div className="alert alert-error">
                <span className="alert-title">Creation Failed</span>
                <span className="alert-sub">{serverError}</span>
            </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="field">
            <label>Legal Name</label>
            <input type="text" {...register("name")} placeholder="John Doe" />
            {errors.name && <div className="field-error">{errors.name.message}</div>}
          </div>

          <div className="field">
            <label>Identity (Email)</label>
            <input type="email" {...register("email")} placeholder="system@example.com" />
            {errors.email && <div className="field-error">{errors.email.message}</div>}
          </div>

          <div className="field">
            <label>Passphrase</label>
            <input type="password" {...register("password")} placeholder="••••••••" />
            {errors.password && <div className="field-error">{errors.password.message}</div>}
          </div>
          
          <div className="grid-2">
            <div className="field">
              <label>Base Currency</label>
              <select {...register("currency")}>
                <option value="MAD">MAD (Dirham)</option>
                <option value="USD">USD (Dollar)</option>
                <option value="EUR">EUR (Euro)</option>
                <option value="GBP">GBP (Pound)</option>
              </select>
              {errors.currency && <div className="field-error">{errors.currency.message}</div>}
            </div>

            <div className="field">
              <label>Instrument Type</label>
              <select {...register("cardType")}>
                <option value="DEBIT">Debit</option>
                <option value="CREDIT">Credit</option>
              </select>
              {errors.cardType && <div className="field-error">{errors.cardType.message}</div>}
            </div>
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: "100%", marginTop: "1rem" }} disabled={isSubmitting}>
            {isSubmitting ? "PROCESSING..." : "REGISTER ENTITY"}
          </button>
          
          <div style={{ textAlign: "center", marginTop: "1.5rem" }}>
            <Link to="/login" className="muted mono" style={{ fontSize: "0.7rem", textDecoration: "none", textTransform: "uppercase", letterSpacing: "0.05em", borderBottom: "1px solid var(--color-rule)", paddingBottom: "0.1rem" }}>
              Already Registered? Authenticate
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
