import { useState, useEffect } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { client } from "../lib/api";
import { useAuth } from "../context/AuthContext";

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

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginFormData>({
        resolver: zodResolver(loginSchema),
    });

    useEffect(() => {
        const searchParams = new URLSearchParams(location.search);
        if (searchParams.get("registered") === "true") {
            setSuccessMessage("Welcome to the club. Please sign in.");
        }
    }, [location]);

    const onSubmit = async (data: LoginFormData) => {
        setServerError(null);
        setSuccessMessage(null);

        try {
            const { error: loginError } = await client.POST("/api/v1/auth/login", {
                body: { email: data.email, password: data.password }
            });

            if (loginError) {
                setServerError((loginError as any).message || "Invalid credentials. The bouncer says no.");
                return;
            }

            const { data: userData, error: meError } = await client.GET("/api/v1/users/me");
            
            if (meError || !userData) {
                setServerError("Failed to retrieve profile.");
                return;
            }

            setUser(userData);
            navigate("/dashboard");
            
        } catch (err) {
            setServerError("The servers are taking a nap. Try again.");
        }
    };

    return (
        <div className="min-h-screen flex w-full bg-[var(--bg)] text-[var(--text-main)] font-sans">
            
            {/* Left Pane - Form (Flipped) */}
            <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 relative bg-[var(--bg)] order-2 lg:order-1">
                <div className="w-full max-w-md space-y-10">
                    <div>
                        <h2 className="text-3xl font-serif text-[var(--text-main)]">Sign In</h2>
                        <p className="mt-2 text-sm text-[var(--text-muted)]">
                            Access your highly exclusive, completely made-up wealth.
                        </p>
                    </div>

                    {successMessage && (
                        <div className="bg-green-50 border border-green-200 text-green-700 p-4 text-sm font-sans rounded-md">
                            {successMessage}
                        </div>
                    )}

                    {serverError && (
                        <div className="bg-red-50 border border-red-200 text-red-600 p-4 text-sm font-sans rounded-md">
                            {serverError}
                        </div>
                    )}

                    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                        <div className="space-y-5">
                            <div>
                                <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Email Address</label>
                                <input
                                    type="email"
                                    {...register("email")}
                                    className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] placeholder-[var(--text-muted)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md"
                                    placeholder="007@example.com"
                                />
                                {errors.email && <p className="mt-2 text-xs text-red-600">{errors.email.message}</p>}
                            </div>

                            <div>
                                <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Password</label>
                                <input
                                    type="password"
                                    {...register("password")}
                                    className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] placeholder-[var(--text-muted)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md"
                                    placeholder="••••••••"
                                />
                                {errors.password && <p className="mt-2 text-xs text-red-600">{errors.password.message}</p>}
                            </div>
                        </div>

                        <div className="pt-4">
                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="w-full bg-[var(--text-main)] text-[var(--bg)] hover:bg-[var(--accent)] transition-colors py-4 px-4 font-serif text-lg tracking-wide rounded-md shadow-lg shadow-black/5 disabled:opacity-50"
                            >
                                {isSubmitting ? "Authenticating..." : "Enter Vault"}
                            </button>
                        </div>
                        
                        <div className="text-center mt-6">
                            <Link to="/register" className="text-sm text-[var(--text-muted)] hover:text-[var(--accent)] transition-colors border-b border-transparent hover:border-[var(--accent)]">
                                Not a member yet? Apply here.
                            </Link>
                        </div>
                    </form>
                </div>
            </div>

            {/* Right Pane - Editorial with Mesh */}
            <div className="hidden lg:flex w-1/2 flex-col justify-between p-12 border-l border-[var(--border)] relative mesh-bg overflow-hidden order-1 lg:order-2">
                <div className="relative z-10 self-end text-right">
                    <h1 className="text-xl font-serif font-semibold tracking-widest text-[var(--accent)] mb-2 uppercase">Bank of Houcine</h1>
                    <p className="text-[var(--text-muted)] text-sm tracking-wide uppercase">Members Only</p>
                </div>

                <div className="relative z-10 space-y-6">
                    <h2 className="text-5xl font-serif leading-tight">
                        We don't do branches.<br />
                        <span className="serif-italic text-[var(--accent)]">We do localhost.</span>
                    </h2>
                    <p className="text-lg text-[var(--text-main)] max-w-md leading-relaxed opacity-80">
                        Why trust a real bank when you can trust a side project? 
                        Experience the thrill of a UI that looks expensive but costs nothing to run.
                    </p>
                </div>
            </div>
        </div>
    );
}
