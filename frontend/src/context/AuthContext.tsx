import { useState, useEffect, type ReactNode } from "react";
import { client } from "../lib/api.ts"
import type { components } from "../types/api";
import { AuthContext } from "./auth-context";

type UserResponse = components["schemas"]["UserResponse"];

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserResponse | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchUser() {
            try {
                const { data } = await client.GET('/api/v1/users/me');
                setUser(data ?? null);
            } catch (err) {
                console.error("Auth network error:", err);
                setUser(null);
            } finally {
                setLoading(false);
            }
        }
        fetchUser();
    }, []);

    return (
        <AuthContext.Provider value={{ user, loading, setUser }}>
            {children}
        </AuthContext.Provider>
    );
}
