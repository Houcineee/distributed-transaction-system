import { useAuth } from "../context/AuthContext";
import { client } from "../lib/api.ts";

export function DashboardPage() {
    const { user, setUser } = useAuth();

    async function handleLogout() {
        try {
            const { error } = await client.POST("/api/v1/auth/logout");
            if (error) {
                console.error("Logout failed:", error);
            }
        } catch (err) {
            console.error("Network error during logout:", err);
        } finally {
            setUser(null);
        }
    }
    
    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-4xl mx-auto bg-white p-6 rounded-lg shadow">
                <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
                <p className="mt-4 text-gray-600">Welcome back, {user?.name}!</p>
                
                <button 
                    onClick={handleLogout}
                    className="mt-6 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                >
                    Sign Out
                </button>
            </div>
        </div>
    );
}
