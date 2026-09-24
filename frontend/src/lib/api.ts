import createClient from 'openapi-fetch'
import type {paths} from "../types/api";


export const client = createClient<paths>({
    baseUrl: import.meta.env.VITE_API_BASE_URL,
    credentials: 'include',
    querySerializer(params) {
        const searchParams = new URLSearchParams();
        for (const [key, value] of Object.entries(params)) {
            if (value === undefined) continue;
            
            if (key === 'pageable' && typeof value === 'object' && value !== null) {
                for (const [pKey, pVal] of Object.entries(value as Record<string, unknown>)) {
                    if (pVal !== undefined) searchParams.append(pKey, String(pVal));
                }
            } else if (Array.isArray(value)) {
                value.forEach(v => searchParams.append(key, String(v)));
            } else {
                searchParams.append(key, String(value));
            }
        }
        return searchParams.toString();
    }
});

export function getApiErrorMessage(error: unknown, fallback: string) {
    if (typeof error === "object" && error !== null && "message" in error) {
        const message = error.message;
        if (typeof message === "string" && message.length > 0) return message;
    }

    return fallback;
}