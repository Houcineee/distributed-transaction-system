import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { client } from "../lib/api";

export function SideNav() {
    const { user, setUser } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();

    const handleLogout = async () => {
        try {
            await client.POST("/api/v1/auth/logout");
        } catch {
            // ignore network errors on logout
        }
        setUser(null);
        navigate("/login");
    };

    return (
        <aside className="app-sidebar">
            <div>
                <div style={{ marginBottom: "3rem", display: "flex", alignItems: "center", gap: "0.75rem" }}>
                    <div style={{ width: "24px", height: "24px", background: "var(--color-ink)", color: "white", display: "grid", placeItems: "center", fontSize: "0.75rem", fontFamily: "var(--font-mono)", fontWeight: "bold", borderRadius: "4px" }}>B</div>
                    <span style={{ fontSize: "0.85rem", fontWeight: 700, letterSpacing: "0.05em", textTransform: "uppercase" }}>Bank of Houcine</span>
                </div>
                
                <nav style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                    <Link to="/dashboard" style={{ padding: "0.5rem 0", color: location.pathname === "/dashboard" ? "var(--color-ink)" : "var(--color-muted)", textDecoration: "none", fontSize: "0.85rem", fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                        <span>Overview</span>
                        {location.pathname === "/dashboard" && <span style={{ width: "4px", height: "4px", background: "var(--color-accent)", borderRadius: "50%" }}></span>}
                    </Link>
                    <Link to="/transfers" style={{ padding: "0.5rem 0", color: location.pathname === "/transfers" ? "var(--color-ink)" : "var(--color-muted)", textDecoration: "none", fontSize: "0.85rem", fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                        <span>Payments</span>
                        {location.pathname === "/transfers" && <span style={{ width: "4px", height: "4px", background: "var(--color-accent)", borderRadius: "50%" }}></span>}
                    </Link>
                    <Link to="/settings" style={{ padding: "0.5rem 0", color: location.pathname === "/settings" ? "var(--color-ink)" : "var(--color-muted)", textDecoration: "none", fontSize: "0.85rem", fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                        <span>Settings</span>
                        {location.pathname === "/settings" && <span style={{ width: "4px", height: "4px", background: "var(--color-accent)", borderRadius: "50%" }}></span>}
                    </Link>
                </nav>
            </div>

            <div style={{ borderTop: "1px solid var(--color-rule)", paddingTop: "1.5rem" }}>
                <div style={{ fontSize: "0.75rem", fontWeight: 600, marginBottom: "0.25rem" }}>{user?.name || "Client"}</div>
                <div className="mono muted" style={{ fontSize: "0.65rem", marginBottom: "1rem" }}>{user?.email}</div>
                <button onClick={handleLogout} style={{ background: "none", border: "none", padding: 0, color: "var(--color-muted)", fontSize: "0.75rem", fontWeight: 600, cursor: "pointer", textTransform: "uppercase", letterSpacing: "0.05em" }}>Terminate Session</button>
            </div>
        </aside>
    );
}
