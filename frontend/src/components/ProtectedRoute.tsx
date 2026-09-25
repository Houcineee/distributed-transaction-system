import { Navigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import type { ReactNode } from "react";
import { CineLogo } from "./CineLogo";

interface ProtectedRouteProps {
    children: ReactNode;
    requireRole?: "ADMIN" | "CUSTOMER";
}

export function ProtectedRoute({ children, requireRole = "CUSTOMER" }: ProtectedRouteProps) {
    const { user, loading, isAdmin } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center bg-[#FBFBFD] gap-4">
                <CineLogo size={40} showWordmark={false} />
                <div className="w-5 h-5 border-2 border-[#C5A059]/30 border-t-[#C5A059] rounded-full animate-spin" />
            </div>
        );
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (requireRole === "ADMIN" && !isAdmin) {
        return <Navigate to="/dashboard" replace />;
    }

    if (requireRole === "CUSTOMER" && isAdmin) {
        return <Navigate to="/admin" replace />;
    }

    return <>{children}</>;
}
