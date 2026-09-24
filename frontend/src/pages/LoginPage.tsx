import { useState, useEffect } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { client } from "../lib/api";
import { useAuth } from "../context/AuthContext";

const loginSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
  password: z.string().min(1, "Password is required"),
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
        // If they just got redirected from the register page, show a success banner
        const searchParams = new URLSearchParams(location.search);
        if (searchParams.get("registered") === "true") {
            setSuccessMessage("Account created successfully! Please sign in.");
        }
    }, [location]);

    const onSubmit = async (data: LoginFormData) => {
        setServerError(null);
        setSuccessMessage(null);

        try {
            // 1. Send credentials to the backend
            const { error: loginError } = await client.POST("/api/v1/auth/login", {
                body: { email: data.email, password: data.password }
            });

            if (loginError) {
                setServerError((loginError as any).message || "Invalid email or password");
                return;
            }

            // 2. Fetch the user profile (now that we have the httpOnly cookie)
            const { data: userData, error: meError } = await client.GET("/api/v1/users/me");
            
            if (meError || !userData) {
                setServerError("Failed to load user profile");
                return;
            }

            // 3. Update the global context with the user data
            setUser(userData);

            // 4. Redirect to the protected dashboard
            navigate("/dashboard");
            
        } catch (err) {
            console.error("Login failed:", err);
            setServerError("Service temporarily unavailable. Please try again later.");
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-md border border-gray-100">
                <div>
                    <h2 className="mt-2 text-center text-3xl font-extrabold text-gray-900">
                        Sign in to your account
                    </h2>
                </div>

                {successMessage && (
                    <div className="p-3 bg-green-50 text-green-700 rounded text-sm text-center font-medium border border-green-200">
                        {successMessage}
                    </div>
                )}

                {serverError && (
                    <div className="p-3 bg-red-50 text-red-700 rounded text-sm text-center border border-red-200">
                        {serverError}
                    </div>
                )}

                <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-6">
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Email address</label>
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

                    <div>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                        >
                            {isSubmitting ? "Signing in..." : "Sign in"}
                        </button>
                    </div>
                    
                    <div className="text-center text-sm">
                        <Link to="/register" className="font-medium text-blue-600 hover:text-blue-500">
                            Don't have an account? Create one here
                        </Link>
                    </div>
                </form>
            </div>
        </div>
    );
}
