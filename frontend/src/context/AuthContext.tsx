import { createContext, useState, useEffect, useContext, type ReactNode } from "react";
import { client } from "../lib/api.ts"
import type { components } from "../types/api";

type UserResponse = components["schemas"]["UserResponse"];

interface AuthContextType {
    user: UserResponse | null;
    loading: boolean;
    setUser: (user: UserResponse | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchUser() {
            const { data } = await client.GET('/api/v1/users/me');
            setUser(data ?? null);
            setLoading(false);
        }
        fetchUser();
    }, []);

    return (
        <AuthContext.Provider value={{ user, loading, setUser }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
