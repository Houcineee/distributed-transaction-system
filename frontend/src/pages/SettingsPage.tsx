import { useAuth } from "../context/useAuth";
import { SideNav } from "../components/SideNav";

export function SettingsPage() {
    const { user } = useAuth();

    return (
        <div className="app-shell">
            <SideNav />
            <main className="app-main">
                <header className="mb-8">
                    <h1 style={{ fontSize: "3.5rem", lineHeight: 1 }}>Configuration</h1>
                    <p className="muted mt-2" style={{ fontSize: "0.85rem" }}>System parameters and identity verification.</p>
                </header>

                <div className="grid-2">
                    <div className="panel">
                        <div className="panel-header">
                            <span className="eyebrow">Identity Profile</span>
                        </div>
                        <div className="panel-body">
                            <div className="flex-between mb-6 pb-6" style={{ borderBottom: "1px solid var(--color-rule)" }}>
                                <div>
                                    <div className="eyebrow muted mb-2">Legal Entity Name</div>
                                    <div style={{ fontSize: "1.1rem", fontWeight: 600 }}>{user?.name}</div>
                                </div>
                                <div style={{ background: "var(--color-ink)", color: "white", padding: "0.5rem 1rem", fontSize: "0.75rem", fontFamily: "var(--font-mono)", fontWeight: "bold" }}>
                                    VERIFIED
                                </div>
                            </div>
                            <div className="mb-6">
                                <div className="eyebrow muted mb-2">Registered Contact</div>
                                <div className="mono" style={{ fontSize: "0.85rem" }}>{user?.email}</div>
                            </div>
                            <div>
                                <div className="eyebrow muted mb-2">Internal ID</div>
                                <div className="mono muted" style={{ fontSize: "0.75rem" }}>{user?.id}</div>
                            </div>
                        </div>
                    </div>


                </div>
            </main>
        </div>
    );
}
