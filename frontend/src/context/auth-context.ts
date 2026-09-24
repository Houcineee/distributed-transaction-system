import { createContext } from "react";
import type { components } from "../types/api";

type UserResponse = components["schemas"]["UserResponse"];

export interface AuthContextType {
    user: UserResponse | null;
    loading: boolean;
    setUser: (user: UserResponse | null) => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
