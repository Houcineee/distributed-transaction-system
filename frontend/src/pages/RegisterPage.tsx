import { useState, useRef, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { client, getApiErrorMessage } from "../lib/api";
import { CurrencyIcon } from "../components/CurrencyIcon";
import { CineLogo } from "../components/CineLogo";

const registerSchema = z.object({
  name: z.string().min(2, "Please enter your full legal name."),
  email: z.string().email("Please enter a valid email address."),
  password: z.string().min(8, "Password must be at least 8 characters."),
  currency: z.enum(["USD", "EUR", "GBP", "MAD"]),
  cardType: z.enum(["DEBIT", "CREDIT"]),
  terms: z.boolean().refine((val) => val === true, {
    message: "You must agree to the Terms of Service to continue.",
  }),
});

type RegisterFormData = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const [isCurrencyOpen, setIsCurrencyOpen] = useState(false);
  const [isCardTypeOpen, setIsCardTypeOpen] = useState(false);
  const [isTermsOpen, setIsTermsOpen] = useState(false);
  const currencyRef = useRef<HTMLDivElement>(null);
  const cardTypeRef = useRef<HTMLDivElement>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: { currency: "MAD", cardType: "DEBIT", terms: false },
  });

  const watchCurrency = watch("currency");
  const watchCardType = watch("cardType");
  const watchTerms = watch("terms");

  // Close modal on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isTermsOpen) {
        setIsTermsOpen(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isTermsOpen]);

  // Close custom popovers on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (currencyRef.current && !currencyRef.current.contains(event.target as Node)) {
        setIsCurrencyOpen(false);
      }
      if (cardTypeRef.current && !cardTypeRef.current.contains(event.target as Node)) {
        setIsCardTypeOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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
        setServerError(getApiErrorMessage(error, "Unable to create account. Please review your details and try again."));
        return;
      }
      navigate("/login?registered=true");
    } catch {
      setServerError("Unable to connect to Cine servers. Please check your internet connection.");
    }
  };

  const currencyOptions = [
    { code: "MAD" as const },
    { code: "USD" as const },
    { code: "EUR" as const },
    { code: "GBP" as const },
  ];

  const cardOptions = [
    { type: "DEBIT" as const, label: "Debit", sub: "Debit card" },
    { type: "CREDIT" as const, label: "Credit", sub: "Credit card" },
  ];

  return (
    <div className="min-h-screen bg-[#FBFBFD] flex items-center justify-center p-6 select-none">
      <div className="w-full max-w-[480px] p-8 md:p-10 bg-white border border-black/[0.08] rounded-[28px] shadow-[0_20px_48px_-12px_rgba(0,0,0,0.06)]">
        {/* Brand */}
        <div className="flex items-center justify-center mb-8">
          <CineLogo size={36} showWordmark={true} />
        </div>

        {/* Headings */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-normal tracking-[-0.03em] text-[#1D1D1F] mb-2">
            Open an account
          </h1>
          <p className="text-xs text-[#86868B] leading-relaxed">
            Takes less than a minute. Free to get started.
          </p>
        </div>

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
          {/* Full Name */}
          <div>
            <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-1.5">
              Legal Name
            </label>
            <input
              type="text"
              {...register("name")}
              placeholder="Jane Doe"
              className="w-full px-4 py-3.5 rounded-2xl border border-transparent bg-[#F5F5F7] focus:bg-white focus:border-black/20 text-sm text-[#1D1D1F] focus:outline-none transition-all placeholder:text-[#86868B]/50"
            />
            {errors.name && (
              <div className="text-xs text-red-500 mt-1.5">
                {errors.name.message}
              </div>
            )}
          </div>

          {/* Email */}
          <div>
            <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-1.5">
              Email Address
            </label>
            <input
              type="email"
              {...register("email")}
              placeholder="houcine@example.com"
              className="w-full px-4 py-3.5 rounded-2xl border border-transparent bg-[#F5F5F7] focus:bg-white focus:border-black/20 text-sm text-[#1D1D1F] focus:outline-none transition-all placeholder:text-[#86868B]/50"
            />
            {errors.email && (
              <div className="text-xs text-red-500 mt-1.5">
                {errors.email.message}
              </div>
            )}
          </div>

          {/* Password */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-semibold text-[#86868B] uppercase tracking-wider">
                Password
              </label>
              <span className="text-[10px] text-[#86868B]/80 italic">
                At least 8 characters
              </span>
            </div>
            <input
              type="password"
              {...register("password")}
              placeholder="••••••••••••"
              className="w-full px-4 py-3.5 rounded-2xl border border-transparent bg-[#F5F5F7] focus:bg-white focus:border-black/20 text-sm text-[#1D1D1F] focus:outline-none transition-all placeholder:text-[#86868B]/50"
            />
            {errors.password && (
              <div className="text-xs text-red-500 mt-1.5">
                {errors.password.message}
              </div>
            )}
          </div>

          {/* Custom Selectors: Currency & Card Type */}
          <div className="grid grid-cols-2 gap-4">
            {/* Custom Currency Dropdown */}
            <div className="relative" ref={currencyRef}>
              <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-1.5">
                Base Currency
              </label>

              <button
                type="button"
                onClick={() => {
                  setIsCurrencyOpen(!isCurrencyOpen);
                  setIsCardTypeOpen(false);
                }}
                className="w-full flex items-center justify-between px-3.5 py-3 rounded-2xl bg-[#F5F5F7] hover:bg-[#EBEBED] border border-transparent focus:border-black/20 focus:bg-white transition-all text-xs font-medium text-[#1D1D1F] cursor-pointer group"
              >
                <div className="flex items-center gap-2">
                  <CurrencyIcon currency={watchCurrency} size={18} />
                  <span className="font-semibold">{watchCurrency}</span>
                </div>
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className={`text-[#86868B] transition-transform duration-200 group-hover:text-[#1D1D1F] ${
                    isCurrencyOpen ? "rotate-180" : ""
                  }`}
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>

              {/* Currency Popover */}
              {isCurrencyOpen && (
                <div className="absolute left-0 right-0 top-full mt-2 bg-white border border-black/[0.08] shadow-[0_16px_40px_-8px_rgba(0,0,0,0.14)] rounded-2xl p-1.5 z-50 animate-in fade-in zoom-in-95 duration-150">
                  <div className="space-y-1">
                    {currencyOptions.map((opt) => {
                      const isSelected = watchCurrency === opt.code;
                      return (
                        <button
                          key={opt.code}
                          type="button"
                          onClick={() => {
                            setValue("currency", opt.code);
                            setIsCurrencyOpen(false);
                          }}
                          className={`w-full flex items-center justify-between p-2.5 rounded-xl transition-all text-left cursor-pointer ${
                            isSelected
                              ? "bg-[#F5F5F7] text-[#1D1D1F]"
                              : "hover:bg-black/[0.03] text-[#1D1D1F]"
                          }`}
                        >
                          <div className="flex items-center gap-2.5">
                            <CurrencyIcon currency={opt.code} size={20} />
                            <span className="text-xs font-semibold">{opt.code}</span>
                          </div>
                          {isSelected && (
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#00A855" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                              <polyline points="20 6 9 17 4 12" />
                            </svg>
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            {/* Custom Card Type Dropdown */}
            <div className="relative" ref={cardTypeRef}>
              <label className="block text-xs font-semibold text-[#86868B] uppercase tracking-wider mb-1.5">
                Instrument
              </label>

              <button
                type="button"
                onClick={() => {
                  setIsCardTypeOpen(!isCardTypeOpen);
                  setIsCurrencyOpen(false);
                }}
                className="w-full flex items-center justify-between px-3.5 py-3 rounded-2xl bg-[#F5F5F7] hover:bg-[#EBEBED] border border-transparent focus:border-black/20 focus:bg-white transition-all text-xs font-medium text-[#1D1D1F] cursor-pointer group"
              >
                <span className="font-semibold truncate">
                  {watchCardType === "DEBIT" ? "Debit" : "Credit"}
                </span>
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className={`text-[#86868B] transition-transform duration-200 group-hover:text-[#1D1D1F] shrink-0 ml-1 ${
                    isCardTypeOpen ? "rotate-180" : ""
                  }`}
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>

              {/* Card Type Popover */}
              {isCardTypeOpen && (
                <div className="absolute left-0 right-0 top-full mt-2 bg-white border border-black/[0.08] shadow-[0_16px_40px_-8px_rgba(0,0,0,0.14)] rounded-2xl p-1.5 z-50 animate-in fade-in zoom-in-95 duration-150">
                  <div className="space-y-1">
                    {cardOptions.map((opt) => {
                      const isSelected = watchCardType === opt.type;
                      return (
                        <button
                          key={opt.type}
                          type="button"
                          onClick={() => {
                            setValue("cardType", opt.type);
                            setIsCardTypeOpen(false);
                          }}
                          className={`w-full flex items-center justify-between p-2.5 rounded-xl transition-all text-left cursor-pointer ${
                            isSelected
                              ? "bg-[#F5F5F7] text-[#1D1D1F]"
                              : "hover:bg-black/[0.03] text-[#1D1D1F]"
                          }`}
                        >
                          <div>
                            <div className="text-xs font-semibold">{opt.label}</div>
                            <div className="text-[10px] text-[#86868B]">{opt.sub}</div>
                          </div>
                          {isSelected && (
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#00A855" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                              <polyline points="20 6 9 17 4 12" />
                            </svg>
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Terms & Conditions Checkbox */}
          <div className="!mt-7">
            <label className="flex items-start gap-3 cursor-pointer group select-none">
              <div className="relative flex items-center justify-center mt-0.5 shrink-0">
                <input
                  type="checkbox"
                  {...register("terms")}
                  className="sr-only"
                />
                <div
                  className={`w-5 h-5 rounded-lg border transition-all flex items-center justify-center ${
                    watchTerms
                      ? "bg-[#1D1D1F] border-[#1D1D1F]"
                      : "bg-[#F5F5F7] border-black/20 group-hover:border-black/40"
                  }`}
                >
                  {watchTerms && (
                    <svg
                      className="w-3.5 h-3.5 text-white"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="3.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                  )}
                </div>
              </div>
              <span className="text-xs text-[#86868B] leading-snug">
                I agree to the{" "}
                <button
                  type="button"
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setIsTermsOpen(true);
                  }}
                  className="font-semibold text-[#1D1D1F] underline underline-offset-2 decoration-black/20 hover:decoration-black cursor-pointer"
                >
                  Terms of Service
                </button>
                .
              </span>
            </label>
            {errors.terms && (
              <div className="text-xs text-red-500 mt-1.5 pl-8">
                {errors.terms.message}
              </div>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-4 !mt-2.5 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-sm font-semibold tracking-wide transition-all shadow-sm hover:shadow active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2"
          >
            {isSubmitting ? (
              <div className="flex items-center gap-2 text-xs">
                <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                <span>Creating account...</span>
              </div>
            ) : (
              <span>Create Account</span>
            )}
          </button>

          <div className="text-center !mt-3">
            <Link
              to="/login"
              className="text-xs text-[#86868B] hover:text-[#1D1D1F] transition-colors"
            >
              Already have an account?{" "}
              <span className="font-semibold underline underline-offset-4 decoration-black/20 hover:decoration-black text-[#1D1D1F]">
                Sign in
              </span>
            </Link>
          </div>
        </form>
      </div>

      {/* Terms of Surrender Modal */}
      {isTermsOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/40 backdrop-blur-sm animate-in fade-in duration-200"
          onClick={() => setIsTermsOpen(false)}
        >
          <div
            className="bg-white rounded-[28px] max-w-lg w-full max-h-[85vh] flex flex-col shadow-[0_25px_60px_-15px_rgba(0,0,0,0.3)] border border-black/10 overflow-hidden animate-in zoom-in-95 duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="p-6 md:p-7 border-b border-black/[0.06] flex items-start justify-between bg-[#FAFAFC]">
              <div>
                <span className="inline-block px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-black/5 text-[#1D1D1F] mb-2">
                  Account Terms
                </span>
                <h2 className="text-xl font-semibold tracking-tight text-[#1D1D1F]">
                  Terms of Service
                </h2>
                <p className="text-xs text-[#86868B] mt-0.5">
                  Standard account terms and regulatory disclosures.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="w-8 h-8 rounded-full bg-black/5 hover:bg-black/10 flex items-center justify-center text-[#86868B] hover:text-[#1D1D1F] transition-colors cursor-pointer shrink-0 ml-4"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            {/* Scrollable Clauses */}
            <div className="p-6 md:p-7 overflow-y-auto space-y-4 text-xs text-[#1D1D1F] leading-relaxed">
              <div className="p-4 rounded-2xl bg-[#F5F5F7] border border-black/[0.04]">
                <div className="font-semibold text-[#1D1D1F] mb-1">
                  1. Account Custody & Funds Segregation
                </div>
                <p className="text-[#86868B]">
                  Client deposits are maintained in segregated depository accounts with regulated banking partners. Cine maintains strict balance fidelity; client funds are never commingled with corporate operational capital.
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F5F5F7] border border-black/[0.04]">
                <div className="font-semibold text-[#1D1D1F] mb-1">
                  2. Data Privacy & Cryptographic Security
                </div>
                <p className="text-[#86868B]">
                  All account interactions, credentials, and transaction histories are protected by AES-256 encryption at rest and TLS 1.3 in transit. Cine does not sell personal financial records, transaction metadata, or telemetry to third parties.
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F5F5F7] border border-black/[0.04]">
                <div className="font-semibold text-[#1D1D1F] mb-1">
                  3. Transaction Execution & Audit Trail
                </div>
                <p className="text-[#86868B]">
                  Every balance movement is backed by an immutable ledger entry and cryptographic authorization. All debit and credit settlements are triggered solely by authenticated account holders.
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F5F5F7] border border-black/[0.04]">
                <div className="font-semibold text-[#1D1D1F] mb-1">
                  4. High Availability & Balance Protection
                </div>
                <p className="text-[#86868B]">
                  Cine operates redundant, multi-region distributed infrastructure with continuous automated replication. Account balances remain fully protected and restorable from verifiable ledger checkpoints.
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F5F5F7] border border-black/[0.04]">
                <div className="font-semibold text-[#1D1D1F] mb-1">
                  5. Dispute Resolution & Cardholder Protection
                </div>
                <p className="text-[#86868B]">
                  Cardholders benefit from standard zero-liability protection on unauthorized transactions. In the event of a billing dispute or unexpected charge, inquiries can be raised directly for prompt investigation.
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F5F5F7] border border-black/[0.04]">
                <div className="font-semibold text-[#1D1D1F] mb-1">
                  6. Account Alerts & Communications
                </div>
                <p className="text-[#86868B]">
                  Cine delivers real-time transactional alerts, security confirmations, and periodic statements. Customers may customize notification channels and preferences within their account settings.
                </p>
              </div>
            </div>

            {/* Footer */}
            <div className="p-5 md:p-6 border-t border-black/[0.06] bg-[#FAFAFC] flex flex-col sm:flex-row items-center gap-3">
              <button
                type="button"
                onClick={() => {
                  setValue("terms", true, { shouldValidate: true });
                  setIsTermsOpen(false);
                }}
                className="w-full sm:flex-1 py-3 px-4 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-xs font-semibold tracking-wide transition-all shadow-sm active:scale-[0.98] cursor-pointer text-center"
              >
                Accept Terms
              </button>
              <button
                type="button"
                onClick={() => setIsTermsOpen(false)}
                className="w-full sm:w-auto py-3 px-4 rounded-full bg-black/5 hover:bg-black/10 text-[#1D1D1F] text-xs font-semibold transition-all active:scale-[0.98] cursor-pointer text-center"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
