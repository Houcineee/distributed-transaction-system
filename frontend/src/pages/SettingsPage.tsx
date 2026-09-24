import { useAuth } from "../context/AuthContext";
import { SideNav } from "../components/SideNav";

export function SettingsPage() {
    const { user } = useAuth();

    return (
        <div className="min-h-screen bg-[var(--bg-surface)] text-[var(--text-main)] font-sans flex flex-col md:flex-row">
            <SideNav />

            <main className="flex-1 p-6 md:p-12 overflow-y-auto mesh-bg relative">
                <header className="mb-12 relative z-10">
                    <h2 className="text-5xl font-serif text-[var(--text-main)] tracking-tight">Configuration</h2>
                    <p className="text-[var(--text-muted)] mt-4 font-sans text-lg font-medium">Tweak your prestigious profile.</p>
                </header>

                <div className="max-w-3xl relative z-10 grid gap-8">
                    
                    <div className="glass-panel p-8 rounded-xl flex items-start gap-6">
                        <div className="w-16 h-16 rounded-full bg-[var(--accent)] flex items-center justify-center text-white font-serif text-3xl shadow-lg shadow-[var(--accent)]/20 shrink-0">
                            {user?.name?.charAt(0) || '?'}
                        </div>
                        <div className="flex-1">
                            <h3 className="text-2xl font-serif text-[var(--text-main)]">{user?.name}</h3>
                            <p className="text-[var(--text-muted)] font-medium mb-4">{user?.email}</p>
                            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-green-100 text-green-700 text-xs font-semibold uppercase tracking-widest">
                                <span className="w-1.5 h-1.5 rounded-full bg-green-500"></span>
                                Identity Verified by Admin
                            </span>
                        </div>
                        <button className="px-4 py-2 bg-[var(--bg)] border border-[var(--border)] rounded-md text-sm font-semibold hover:border-[var(--accent)] transition-colors">
                            Edit Profile
                        </button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="glass-panel p-8 rounded-xl">
                            <h3 className="text-lg font-semibold uppercase tracking-widest text-[var(--text-main)] mb-6">Security Config</h3>
                            <ul className="space-y-4">
                                <li className="flex justify-between items-center pb-4 border-b border-[var(--border)]">
                                    <div>
                                        <p className="font-semibold text-sm">Two-Factor Authentication</p>
                                        <p className="text-xs text-[var(--text-muted)]">Handled securely by our imagination.</p>
                                    </div>
                                    <div className="w-10 h-6 bg-[var(--accent)] rounded-full relative">
                                        <div className="absolute right-1 top-1 w-4 h-4 bg-white rounded-full"></div>
                                    </div>
                                </li>
                                <li className="flex justify-between items-center pb-4 border-b border-[var(--border)]">
                                    <div>
                                        <p className="font-semibold text-sm">Session Timeout</p>
                                        <p className="text-xs text-[var(--text-muted)]">Before the butler logs you out.</p>
                                    </div>
                                    <span className="text-sm font-semibold">Never</span>
                                </li>
                                <li className="flex justify-between items-center pt-2">
                                    <div>
                                        <p className="font-semibold text-sm text-red-500">Close All Accounts</p>
                                        <p className="text-xs text-[var(--text-muted)]">Burn the paperwork.</p>
                                    </div>
                                    <button className="px-3 py-1.5 border border-red-200 text-red-500 hover:bg-red-50 rounded text-xs font-semibold uppercase tracking-wider transition-colors">
                                        Execute
                                    </button>
                                </li>
                            </ul>
                        </div>

                        <div className="glass-panel p-8 rounded-xl flex flex-col justify-between">
                            <div>
                                <h3 className="text-lg font-semibold uppercase tracking-widest text-[var(--text-main)] mb-2">Instance Info</h3>
                                <p className="text-sm text-[var(--text-muted)] mb-6">Running on a prestigious, handcrafted localhost port.</p>
                                
                                <div className="space-y-3 font-mono text-xs">
                                    <div className="flex justify-between">
                                        <span className="text-[var(--text-muted)]">App Type</span>
                                        <span className="text-[var(--text-main)]">Monolithic Masterpiece</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-[var(--text-muted)]">Real Value</span>
                                        <span className="text-[var(--text-main)]">$0.00</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-[var(--text-muted)]">Uptime</span>
                                        <span className="text-green-500">Yes</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-[var(--text-muted)]">Bugs</span>
                                        <span className="text-[var(--text-main)]">Features</span>
                                    </div>
                                </div>
                            </div>
                            
                            <div className="mt-8 p-3 bg-[var(--accent)]/10 border border-[var(--accent)]/20 rounded-md">
                                <p className="text-xs text-[var(--accent)] text-center font-medium">System status: Immaculate.</p>
                            </div>
                        </div>
                    </div>

                </div>
            </main>
        </div>
    );
}
