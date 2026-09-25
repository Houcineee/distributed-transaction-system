import { useAuth } from "../context/useAuth";
import { SideNav } from "../components/SideNav";
import { client } from "../lib/api";
import { useNavigate } from "react-router-dom";

export function SettingsPage() {
  const { user, setUser } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await client.POST("/api/v1/auth/logout");
    } catch {
      // ignore network errors
    }
    setUser(null);
    navigate("/login");
  };

  const memberSince = user?.createdAt
    ? new Date(user.createdAt).toLocaleDateString("en-US", {
        month: "long",
        day: "numeric",
        year: "numeric",
      })
    : null;

  return (
    <div className="app-shell">
      <SideNav />
      <main className="app-main">
        {/* Header */}
        <header className="mb-6 pb-4 md:mb-10 md:pb-6 border-b border-black/[0.06]">
          <h1 className="text-2xl md:text-4xl font-normal tracking-[-0.03em] text-[#1D1D1F]">
            Profile & Account
          </h1>
        </header>

        <div className="max-w-3xl space-y-6">
          {/* Profile Card */}
          <div className="bg-white border border-black/[0.06] rounded-[24px] p-8 shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)]">
            <div className="flex items-center gap-5 pb-8 border-b border-black/[0.06]">
              {/* Initials Avatar */}
              <div className="w-16 h-16 rounded-full bg-[#E5E5EA] text-[#1D1D1F] flex items-center justify-center text-xl font-medium tracking-tight shadow-inner ring-2 ring-[#C5A059]/30 shrink-0">
                {user?.name ? user.name.slice(0, 2).toUpperCase() : "HG"}
              </div>

              <div>
                <h2 className="text-xl font-medium tracking-tight text-[#1D1D1F]">
                  {user?.name || "Client"}
                </h2>
                <div className="text-sm text-[#86868B] mt-0.5">
                  {user?.email || "user@domain.com"}
                </div>
              </div>
            </div>

            {/* Profile Fields */}
            <div className="divide-y divide-black/[0.04]">
              <div className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <span className="text-xs font-semibold text-[#86868B] uppercase tracking-wider">
                  Full Name
                </span>
                <span className="text-sm font-medium text-[#1D1D1F]">
                  {user?.name || "—"}
                </span>
              </div>

              <div className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <span className="text-xs font-semibold text-[#86868B] uppercase tracking-wider">
                  Email Address
                </span>
                <span className="text-sm font-medium text-[#1D1D1F]">
                  {user?.email || "—"}
                </span>
              </div>



              {memberSince && (
                <div className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <span className="text-xs font-semibold text-[#86868B] uppercase tracking-wider">
                    Member Since
                  </span>
                  <span className="text-sm font-medium text-[#1D1D1F]">
                    {memberSince}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Account Actions Card */}
          <div className="bg-white border border-black/[0.06] rounded-[24px] p-8 shadow-[0_4px_24px_-4px_rgba(0,0,0,0.03)] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="text-sm font-medium text-[#1D1D1F]">
                Active Session
              </div>
              <div className="text-xs text-[#86868B] mt-0.5">
                Sign out of your current browser session
              </div>
            </div>

            <button
              type="button"
              onClick={handleLogout}
              className="px-5 py-2.5 rounded-full bg-red-50 hover:bg-red-100 text-red-600 text-xs font-semibold transition-all active:scale-[0.98] cursor-pointer self-start sm:self-auto"
            >
              Sign Out
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
