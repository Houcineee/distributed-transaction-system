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
        setServerError((error as any).message || "Registration failed. Please try again.");
        return;
      }

      navigate("/login?registered=true");
    } catch (err) {
      console.error("Network error:", err);
      setServerError("Service temporarily unavailable. Please try again later.");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-md border border-gray-100">
        <div>
          <h2 className="mt-2 text-center text-3xl font-extrabold text-gray-900">
            Open an Account
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            One form, instant access. Get your virtual card right now.
          </p>
        </div>
        
        <form className="mt-8 space-y-6" onSubmit={handleSubmit(onSubmit)}>
          
          {serverError && (
            <div className="bg-red-50 text-red-600 p-3 rounded text-sm text-center">
              {serverError}
            </div>
          )}

          <div className="space-y-4">
            <div className="space-y-4 pt-2">
              <h3 className="text-lg font-medium text-gray-900 border-b pb-2">Personal Details</h3>
              
              <div>
                <label className="block text-sm font-medium text-gray-700">Full Name</label>
                <input
                  type="text"
                  {...register("name")}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  placeholder="John Doe"
                />
                {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Email Address</label>
                <input
                  type="email"
                  {...register("email")}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  placeholder="john@example.com"
                />
                {errors.email && <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">Password</label>
                <input
                  type="password"
                  {...register("password")}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  placeholder="••••••••"
                />
                {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>}
              </div>
            </div>

            <div className="space-y-4 pt-6">
              <h3 className="text-lg font-medium text-gray-900 border-b pb-2">Account Setup</h3>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Currency</label>
                  <select
                    {...register("currency")}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  >
                    <option value="MAD">MAD</option>
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="GBP">GBP</option>
                  </select>
                  {errors.currency && <p className="mt-1 text-sm text-red-600">{errors.currency.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700">Card Type</label>
                  <select
                    {...register("cardType")}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  >
                    <option value="DEBIT">Debit</option>
                    <option value="CREDIT">Credit</option>
                  </select>
                  {errors.cardType && <p className="mt-1 text-sm text-red-600">{errors.cardType.message}</p>}
                </div>
              </div>
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={isSubmitting}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {isSubmitting ? "Creating your account..." : "Open Account"}
            </button>
          </div>
          
          <div className="text-center text-sm">
            <Link to="/login" className="font-medium text-blue-600 hover:text-blue-500">
              Already have an account? Sign in here
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
