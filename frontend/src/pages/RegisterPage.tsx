import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { client } from "../lib/api";

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
  
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      currency: "MAD",
      cardType: "DEBIT",
    }
  });

  const onSubmit = async (data: RegisterFormData) => {
    setServerError(null);
    try {
      const { error } = await client.POST("/api/v1/auth/register", {
        body: {
            name: data.name,
            email: data.email,
            password: data.password,
            currency: data.currency,
            cardType: data.cardType,
        },
      });

      if (error) {
        setServerError((error as any).message || "Registration failed. Our butler is on strike.");
        return;
      }
      navigate("/login?registered=true");
    } catch (err) {
      setServerError("Service temporarily unavailable. Try again shortly.");
    }
  };

  return (
    <div className="min-h-screen flex w-full bg-[var(--bg)] text-[var(--text-main)] font-sans">
      
      {/* Left Pane - Editorial / Visual with Mesh */}
      <div className="hidden lg:flex w-1/2 flex-col justify-between p-12 border-r border-[var(--border)] relative mesh-bg overflow-hidden">
        
        <div className="relative z-10">
          <h1 className="text-xl font-serif font-semibold tracking-widest text-[var(--accent)] mb-2 uppercase">Bank of Houcine</h1>
          <p className="text-[var(--text-muted)] text-sm tracking-wide uppercase">Private Wealth</p>
        </div>

        <div className="relative z-10 space-y-6">
          <h2 className="text-5xl font-serif leading-tight text-[var(--text-main)]">
            Prestigious banking,<br />
            <span className="serif-italic text-[var(--accent)]">for entirely fake money.</span>
          </h2>
          <p className="text-lg text-[var(--text-main)] max-w-md leading-relaxed opacity-80">
            Welcome to the most exclusive financial institution on localhost. 
            Please do not enter real credit card numbers, I will cry.
          </p>
          
          <div className="pt-8 flex gap-8">
            <div className="flex flex-col">
              <span className="text-3xl font-serif text-[var(--text-main)]">100%</span>
              <span className="text-xs uppercase tracking-widest text-[var(--text-muted)] mt-1">Imaginary</span>
            </div>
            <div className="w-px bg-[var(--border)]"></div>
            <div className="flex flex-col">
              <span className="text-3xl font-serif text-[var(--text-main)]">$0</span>
              <span className="text-xs uppercase tracking-widest text-[var(--text-muted)] mt-1">Real value</span>
            </div>
          </div>
        </div>
      </div>

      {/* Right Pane - Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 relative bg-[var(--bg)]">
        <div className="w-full max-w-md space-y-10">
          <div>
            <h2 className="text-3xl font-serif text-[var(--text-main)]">Join the Club</h2>
            <p className="mt-2 text-sm text-[var(--text-muted)]">
              Create your account to start playing with Monopoly money.
            </p>
          </div>
          
          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
            
            {serverError && (
              <div className="bg-red-50 border border-red-100 text-red-600 p-4 text-sm font-sans rounded-md">
                {serverError}
              </div>
            )}

            <div className="space-y-5">
              <div>
                <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Legal Name</label>
                <input
                  type="text"
                  {...register("name")}
                  className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] placeholder-[var(--text-muted)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md"
                  placeholder="James Bond"
                />
                {errors.name && <p className="mt-2 text-xs text-red-600">{errors.name.message}</p>}
              </div>

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
              
              <div className="grid grid-cols-2 gap-6 pt-2">
                <div>
                  <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Currency</label>
                  <select
                    {...register("currency")}
                    className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md cursor-pointer text-sm"
                  >
                    <option value="MAD">MAD (Dirham)</option>
                    <option value="USD">USD (Dollar)</option>
                    <option value="EUR">EUR (Euro)</option>
                    <option value="GBP">GBP (Pound)</option>
                  </select>
                  {errors.currency && <p className="mt-2 text-xs text-red-600">{errors.currency.message}</p>}
                </div>

                <div>
                  <label className="block text-xs uppercase tracking-wider text-[var(--text-muted)] mb-2 font-semibold">Card Type</label>
                  <select
                    {...register("cardType")}
                    className="w-full bg-[var(--bg-surface)] border border-[var(--border)] px-4 py-3 text-[var(--text-main)] focus:outline-none focus:border-[var(--accent)] focus:ring-1 focus:ring-[var(--accent)] transition-all rounded-md cursor-pointer text-sm"
                  >
                    <option value="DEBIT">Debit</option>
                    <option value="CREDIT">Credit</option>
                  </select>
                  {errors.cardType && <p className="mt-2 text-xs text-red-600">{errors.cardType.message}</p>}
                </div>
              </div>
            </div>

            <div className="pt-6">
              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full bg-[var(--text-main)] text-[var(--bg)] hover:bg-[var(--accent)] transition-colors py-4 px-4 font-serif text-lg tracking-wide rounded-md disabled:opacity-50 shadow-lg shadow-black/5"
              >
                {isSubmitting ? "Processing..." : "Open Account"}
              </button>
            </div>
            
            <div className="text-center mt-6">
              <Link to="/login" className="text-sm text-[var(--text-muted)] hover:text-[var(--accent)] transition-colors border-b border-transparent hover:border-[var(--accent)]">
                Already a member? Sign in.
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
