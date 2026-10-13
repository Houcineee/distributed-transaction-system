import { useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { client, getApiErrorMessage } from "../lib/api";
import { useAuth } from "../context/useAuth";
import { CineLogo } from "../components/CineLogo";

const loginSchema = z.object({
  email: z.string().email("That doesn't even look like an email address."),
  password: z.string().min(1, "You can't log in on vibes alone. Enter a password."),
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

  const registeredMessage =
    new URLSearchParams(location.search).get("registered") === "true"
      ? "Account created successfully. Please sign in."
      : null;

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    setSuccessMessage(null);
    try {
      const { error: loginError } = await client.POST("/api/v1/auth/login", {
        body: { email: data.email, password: data.password },
      });
      if (loginError) {
        setServerError(
          getApiErrorMessage(loginError, "Invalid email or password. Please try again.")
        );
        return;
      }
      const { data: userData, error: meError } = await client.GET("/api/v1/users/me");
      if (meError || !userData) {
        setServerError("Unable to load account portfolio. Please refresh or try again.");
        return;
      }
      setUser(userData);
      if (userData.role === "ADMIN") {
        navigate("/admin");
      } else {
        const wasRegistered = new URLSearchParams(location.search).get("registered") === "true";
        navigate(wasRegistered ? "/dashboard?welcomeBonus=true" : "/dashboard");
      }
    } catch {
      setServerError("Unable to connect to Cine servers. Please check your internet connection.");
    }
  };

  return (
    <div className="min-h-screen bg-[#FBFBFD] flex items-center justify-center p-6 select-none">
      <div className="w-full max-w-[440px] p-8 md:p-10 bg-white border border-black/[0.08] rounded-[28px] shadow-[0_20px_48px_-12px_rgba(0,0,0,0.06)]">
        {/* Brand */}
        <div className="flex items-center justify-center mb-8">
          <CineLogo size={36} showWordmark={true} />
        </div>

        {/* Headings */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-normal tracking-[-0.03em] text-[#1D1D1F] mb-2">
            Welcome back
          </h1>
          <p className="text-xs text-[#86868B] leading-relaxed">
            Sign in to your Cine account.
          </p>
        </div>

        {/* Notifications */}
        {(successMessage || registeredMessage) && (
          <div className="mb-6 p-4 rounded-2xl bg-[#00D06C]/10 border border-[#00D06C]/20 text-[#008A44] flex items-center gap-3">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <div className="text-xs font-medium">
              {successMessage || registeredMessage}
            </div>
          </div>
        )}

        {serverError && (
          <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 flex items-center gap-3">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <div className="text-xs font-medium">{serverError}</div>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-1.5">
              Email Address
            </label>
            <input
              type="email"
              {...register("email")}
              placeholder="houcine@example.com"
              className="w-full px-4 py-3.5 rounded-2xl border border-transparent bg-[#F5F5F7] focus:bg-white focus:border-[#C5A059]/50 focus:ring-2 focus:ring-[#C5A059]/15 text-sm text-[#1D1D1F] focus:outline-none transition-all placeholder:text-[#86868B]/50"
            />
            {errors.email && (
              <div className="text-xs text-red-500 mt-1.5">
                {errors.email.message}
              </div>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-1.5">
              Password
            </label>
            <input
              type="password"
              {...register("password")}
              placeholder="••••••••••••"
              className="w-full px-4 py-3.5 rounded-2xl border border-transparent bg-[#F5F5F7] focus:bg-white focus:border-[#C5A059]/50 focus:ring-2 focus:ring-[#C5A059]/15 text-sm text-[#1D1D1F] focus:outline-none transition-all placeholder:text-[#86868B]/50"
            />
            {errors.password && (
              <div className="text-xs text-red-500 mt-1.5">
                {errors.password.message}
              </div>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-4 !mt-7 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-sm font-semibold tracking-wide transition-all shadow-sm hover:shadow active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2"
          >
            {isSubmitting ? (
              <div className="flex items-center gap-2 text-xs">
                <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                <span>Signing in...</span>
              </div>
            ) : (
              <span>Sign In</span>
            )}
          </button>

          <div className="text-center !mt-3">
            <Link
              to="/register"
              className="text-xs text-[#86868B] hover:text-[#1D1D1F] transition-colors"
            >
              Don't have an account?{" "}
              <span className="font-semibold underline underline-offset-4 decoration-black/20 hover:decoration-black text-[#1D1D1F]">
                Create an account
              </span>
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
