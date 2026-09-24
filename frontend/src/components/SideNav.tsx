import { useAuth } from "../context/AuthContext";
import { client } from "../lib/api";
import { Link, useLocation, useNavigate } from "react-router-dom";

export function SideNav() {
    const { user, setUser } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();

    async function handleLogout() {
        try {
            await client.POST("/api/v1/auth/logout");
        } finally {
            setUser(null);
            navigate("/login");
        }
    }

    const isActive = (path: string) => location.pathname === path;

    return (
        <aside className="w-full md:w-64 border-r border-[var(--border)] bg-[var(--bg)] p-8 flex flex-col justify-between shrink-0 shadow-sm relative z-10">
            <div>
                <h1 className="text-xl font-serif font-bold tracking-widest text-[var(--accent)] mb-12 uppercase">Bank of Houcine</h1>
                <nav className="space-y-6 text-sm font-semibold tracking-wider uppercase">
                    <Link 
                        to="/dashboard" 
                        className={`block transition-colors ${isActive("/dashboard") ? "text-[var(--accent)]" : "text-[var(--text-muted)] hover:text-[var(--text-main)]"}`}
                    >
                        Dashboard
                    </Link>
                    <Link 
                        to="/transfers" 
                        className={`block transition-colors ${isActive("/transfers") ? "text-[var(--accent)]" : "text-[var(--text-muted)] hover:text-[var(--text-main)]"}`}
                    >
                        Transfers
                    </Link>
                    <Link 
                        to="/settings" 
                        className={`block transition-colors ${isActive("/settings") ? "text-[var(--accent)]" : "text-[var(--text-muted)] hover:text-[var(--text-main)]"}`}
                    >
                        Settings
                    </Link>
                </nav>
            </div>
            <div className="pt-8 border-t border-[var(--border)]">
                <p className="text-sm text-[var(--text-main)] font-medium mb-1 truncate">{user?.name}</p>
                <p className="text-xs text-[var(--text-muted)] mb-6 truncate">{user?.email}</p>
                <button 
                    onClick={handleLogout}
                    className="text-xs uppercase tracking-wider font-semibold text-red-500 hover:text-red-600 transition-colors"
                >
                    Disconnect
                </button>
            </div>
        </aside>
    );
}
