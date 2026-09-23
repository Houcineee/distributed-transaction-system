import { useAuth } from "../context/AuthContext";

export function DashboardPage() {
    const { user, setUser } = useAuth();
    
    return (
        <div className="min-h-screen bg-gray-50 p-8">
            <div className="max-w-4xl mx-auto bg-white p-6 rounded-lg shadow">
                <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
                <p className="mt-4 text-gray-600">Welcome back, {user?.name}!</p>
                
                <button 
                    onClick={() => setUser(null)}
                    className="mt-6 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                >
                    Mock Logout
                </button>
            </div>
        </div>
    );
}
