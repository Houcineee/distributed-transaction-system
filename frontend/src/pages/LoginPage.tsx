import { useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { client, getApiErrorMessage } from "../lib/api";
import { useAuth } from "../context/useAuth";

const loginSchema = z.object({
  email: z.string().email("Invalid email format"),
  password: z.string().min(1, "Password required"),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginPage() {
    const [serverError, setServerError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    
    const navigate = useNavigate();
    const location = useLocation();
    const { setUser } = useAuth();

    const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginFormData>({
        resolver: zodResolver(loginSchema),
    });

    const registeredMessage = new URLSearchParams(location.search).get("registered") === "true"
        ? "Profile established. You may now enter."
        : null;

    const onSubmit = async (data: LoginFormData) => {
        setServerError(null);
        setSuccessMessage(null);
        try {
            const { error: loginError } = await client.POST("/api/v1/auth/login", {
                body: { email: data.email, password: data.password }
            });
            if (loginError) {
                setServerError(getApiErrorMessage(loginError, "Authentication rejected."));
                return;
            }
            const { data: userData, error: meError } = await client.GET("/api/v1/users/me");
            if (meError || !userData) {
                setServerError("Failed to retrieve profile data.");
                return;
            }
            setUser(userData);
            navigate("/dashboard");
        } catch {
            setServerError("System unreachable. Verify connection.");
        }
    };

    return (
        <div className="auth-layout">
            <div className="auth-box">
                <div style={{ marginBottom: "2.5rem", display: "flex", alignItems: "center", gap: "0.75rem", justifyContent: "center" }}>
                    <div style={{ width: "24px", height: "24px", background: "var(--color-ink)", color: "white", display: "grid", placeItems: "center", fontSize: "0.75rem", fontFamily: "var(--font-mono)", fontWeight: "bold" }}>B</div>
                    <span style={{ fontSize: "0.85rem", fontWeight: 700, letterSpacing: "0.05em", textTransform: "uppercase" }}>Bank of Houcine</span>
                </div>

                <div style={{ textAlign: "center", marginBottom: "2.5rem" }}>
                    <div className="eyebrow mb-2">System Login</div>
                    <h1 style={{ fontSize: "2.5rem", lineHeight: 1 }}>Authenticate</h1>
                    <p className="muted mt-2" style={{ fontSize: "0.8rem" }}>A precision interface for capital allocation. No gamification. No noise.</p>
                </div>

                {(successMessage || registeredMessage) && (
                    <div className="alert alert-success">
                        <span className="alert-title">Success</span>
                        <span className="alert-sub">{successMessage || registeredMessage}</span>
                    </div>
                )}

                {serverError && (
                    <div className="alert alert-error">
                        <span className="alert-title">Access Denied</span>
                        <span className="alert-sub">{serverError}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit(onSubmit)}>
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

                    <button type="submit" className="btn btn-primary" style={{ width: "100%", marginTop: "1rem" }} disabled={isSubmitting}>
                        {isSubmitting ? "AUTHENTICATING..." : "INITIATE SESSION"}
                    </button>
                    
                    <div style={{ textAlign: "center", marginTop: "1.5rem" }}>
                        <Link to="/register" className="muted mono" style={{ fontSize: "0.7rem", textDecoration: "none", textTransform: "uppercase", letterSpacing: "0.05em", borderBottom: "1px solid var(--color-rule)", paddingBottom: "0.1rem" }}>
                            Establish New Profile
                        </Link>
                    </div>
                </form>
            </div>
        </div>
    );
}
