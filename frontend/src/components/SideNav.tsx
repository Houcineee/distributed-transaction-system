import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { client } from "../lib/api";
import { CineLogo } from "./CineLogo";

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

  const navItems = [
    {
      name: "Overview",
      path: "/dashboard",
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="7" height="9" rx="1" />
          <rect x="14" y="3" width="7" height="5" rx="1" />
          <rect x="14" y="12" width="7" height="9" rx="1" />
          <rect x="3" y="16" width="7" height="5" rx="1" />
        </svg>
      ),
    },
    {
      name: "Transfers",
      path: "/transfers",
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="17" y1="3" x2="17" y2="21" />
          <path d="M21 7l-4-4-4 4" />
          <line x1="7" y1="21" x2="7" y2="3" />
          <path d="M3 17l4 4 4-4" />
        </svg>
      ),
    },
    {
      name: "Settings",
      path: "/settings",
      icon: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="3" />
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
        </svg>
      ),
    },
  ];

  return (
    <>
      {/* Mobile Slim Top Bar (< 768px): Single clean row */}
      <header className="md:hidden sticky top-0 z-40 bg-[#FBFBFD]/90 backdrop-blur-md border-b border-black/[0.06] px-4 py-3 select-none flex items-center justify-between">
        <Link to="/dashboard" className="flex items-center">
          <CineLogo size={24} showWordmark={true} />
        </Link>

        <div className="flex items-center gap-2.5">
          <Link
            to="/settings"
            className="w-7 h-7 rounded-full bg-[#E5E5EA] text-[#1D1D1F] flex items-center justify-center text-[10px] font-semibold uppercase ring-1.5 ring-[#C5A059]/30 shadow-sm active:scale-95 transition-transform"
            title="Profile & Settings"
          >
            {user?.name ? user.name.slice(0, 2) : "HG"}
          </Link>
          <button
            onClick={handleLogout}
            title="Sign Out"
            className="text-[#86868B] hover:text-red-600 transition-colors p-1 cursor-pointer"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
        </div>
      </header>

      {/* Mobile Cupertino Bottom Tab Bar (< 768px): Fixed, ergonomic thumb reach */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 bg-[#FBFBFD]/92 backdrop-blur-xl border-t border-black/[0.08] px-4 py-2 select-none flex items-center justify-around pb-[max(0.6rem,env(safe-area-inset-bottom))] shadow-[0_-4px_20px_rgba(0,0,0,0.03)]">
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex-1 flex flex-col items-center justify-center gap-1 py-1 px-2 rounded-xl transition-all ${
                isActive
                  ? "text-[#1D1D1F] font-semibold"
                  : "text-[#86868B] hover:text-[#1D1D1F]"
              }`}
            >
              <span className={`transition-transform duration-200 ${isActive ? "scale-110 text-[#1D1D1F]" : "text-[#86868B]"}`}>
                {item.icon}
              </span>
              <span className="text-[10.5px] tracking-tight">{item.name}</span>
            </Link>
          );
        })}
      </nav>

      {/* Desktop Sidebar (>= 768px): Only visible on desktop */}
      <aside className="hidden md:flex flex-col justify-between w-[260px] min-h-screen p-8 border-r border-black/[0.06] bg-[#FBFBFD] select-none sticky top-0 h-screen">
        <div>
          {/* Brand / Logo */}
          <div className="mb-10 px-1">
            <Link to="/dashboard" className="block group">
              <CineLogo size={32} showWordmark={true} />
            </Link>
          </div>

          {/* Navigation Links */}
          <nav className="space-y-1.5">
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-3 px-3.5 py-2.5 rounded-full text-xs font-medium transition-all ${
                    isActive
                      ? "bg-[#1D1D1F] text-white shadow-sm"
                      : "text-[#86868B] hover:text-[#1D1D1F] hover:bg-black/[0.04]"
                  }`}
                >
                  <span className={isActive ? "text-white" : "text-[#86868B]"}>
                    {item.icon}
                  </span>
                  <span>{item.name}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User Card & Sign Out */}
        <div className="pt-6 border-t border-black/[0.06]">
          <div className="flex items-center gap-3 px-2 mb-4">
            <div className="w-9 h-9 rounded-full bg-[#E5E5EA] text-[#1D1D1F] flex items-center justify-center text-xs font-semibold uppercase ring-1.5 ring-[#C5A059]/30 shadow-sm shrink-0">
              {user?.name ? user.name.slice(0, 2) : "HG"}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-xs font-semibold text-[#1D1D1F] truncate flex items-center gap-1.5">
                <span>{user?.name || "Client"}</span>
              </div>
              <div className="text-[11px] text-[#86868B] truncate">
                {user?.email || "user@domain.com"}
              </div>
            </div>
          </div>

          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-full text-xs font-medium text-[#86868B] hover:text-red-600 hover:bg-red-50/50 transition-all cursor-pointer"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  );
}
