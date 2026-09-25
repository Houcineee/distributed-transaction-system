import { Link, useLocation, useNavigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { client } from "../lib/api";
import { CineLogo } from "../components/CineLogo";

export function AdminLayout() {
  const { user, setUser } = useAuth();
  const location = useLocation();
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

  const navItems = [
    {
      name: "Accounts",
      path: "/admin",
      altPaths: ["/admin/accounts"],
      icon: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
          <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
        </svg>
      ),
    },
    {
      name: "Audit",
      path: "/admin/audit",
      altPaths: [],
      icon: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
        </svg>
      ),
    },
    {
      name: "Health",
      path: "/admin/health",
      altPaths: [],
      icon: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
        </svg>
      ),
    },
  ];

  const isNavActive = (item: typeof navItems[0]) => {
    if (location.pathname === item.path) return true;
    return item.altPaths.includes(location.pathname);
  };

  return (
    <div className="min-h-screen bg-[#FBFBFD] text-[#1D1D1F] flex flex-col font-sans select-none">
      {/* Top Console Navigation Bar */}
      <header className="sticky top-0 z-40 bg-[#FBFBFD]/90 backdrop-blur-md border-b border-black/[0.06]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between">
          {/* Brand + Admin Tag */}
          <div className="flex items-center gap-3">
            <Link to="/admin" className="flex items-center">
              <CineLogo size={22} showWordmark={true} />
            </Link>
            <span className="text-[10px] font-mono tracking-widest font-semibold uppercase text-[#86868B] px-1.5 py-0.5 rounded bg-black/[0.04]">
              Admin
            </span>
          </div>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center gap-1 bg-black/[0.03] p-1 rounded-xl">
            {navItems.map((item) => {
              const active = isNavActive(item);
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs transition-all ${
                    active
                      ? "bg-white text-[#1D1D1F] font-semibold shadow-xs"
                      : "text-[#86868B] hover:text-[#1D1D1F]"
                  }`}
                >
                  <span className={active ? "text-[#C5A059]" : "text-[#86868B]"}>{item.icon}</span>
                  {item.name}
                </Link>
              );
            })}
          </nav>

          {/* User Avatar + Logout */}
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-full bg-black text-white flex items-center justify-center text-[10px] font-semibold font-mono">
              {user?.name ? user.name.slice(0, 2).toUpperCase() : "AD"}
            </div>
            <button
              onClick={handleLogout}
              className="text-[#86868B] hover:text-red-600 transition-colors p-1.5 rounded-lg hover:bg-black/[0.03] cursor-pointer"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-24 md:pb-10">
        <Outlet />
      </main>

      {/* Mobile Cupertino Bottom Bar */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 bg-[#FBFBFD]/92 backdrop-blur-xl border-t border-black/[0.08] px-4 py-2 flex items-center justify-around pb-[max(0.6rem,env(safe-area-inset-bottom))] shadow-[0_-4px_20px_rgba(0,0,0,0.03)]">
        {navItems.map((item) => {
          const active = isNavActive(item);
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex-1 flex flex-col items-center justify-center gap-1 py-1 px-2 rounded-xl transition-all ${
                active ? "text-[#1D1D1F] font-semibold" : "text-[#86868B]"
              }`}
            >
              <div className={active ? "text-[#C5A059]" : "text-[#86868B]"}>{item.icon}</div>
              <span className="text-[10px]">{item.name}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
