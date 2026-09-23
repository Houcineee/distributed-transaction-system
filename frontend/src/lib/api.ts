import createClient from 'openapi-fetch'
import type {paths} from "../types/api";


export const client = createClient<paths>({
    baseUrl: import.meta.env.VITE_API_BASE_URL,
    credentials: 'include',
});