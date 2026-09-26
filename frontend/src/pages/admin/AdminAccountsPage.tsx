import { useState, useEffect, useMemo } from "react";
import { client } from "../../lib/api";
import { formatCurrencyAmount } from "../../lib/currency";
import { ClickableId } from "../../components/ClickableId";
import { AccountInspectModal } from "../../components/admin/AccountInspectModal";
import type { components } from "../../types/api";

type AccountResponse = components["schemas"]["AccountResponse"];
type PageAccountResponse = components["schemas"]["PageAccountResponse"];

export function AdminAccountsPage() {
  const [pageData, setPageData] = useState<PageAccountResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(15);
  const [searchQuery, setSearchQuery] = useState("");
  const [currencyFilter, setCurrencyFilter] = useState("ALL");
  const [selectedAccount, setSelectedAccount] = useState<AccountResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSync = async () => {
    setLoading(true);
    setError(null);
    try {
      const { data, error: apiErr } = await client.GET("/api/v1/admin/accounts", {
        params: {
          query: {
            pageable: {
              page,
              size: pageSize,
              sort: ["createdAt,desc"],
            },
          },
        },
      });

      if (apiErr) {
        setError("Failed to load accounts.");
      } else {
        setPageData(data || null);
      }
    } catch {
      setError("Network error.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        const { data, error: apiErr } = await client.GET("/api/v1/admin/accounts", {
          params: {
            query: {
              pageable: {
                page,
                size: pageSize,
                sort: ["createdAt,desc"],
              },
            },
          },
        });

        if (!ignore) {
          if (apiErr) {
            setError("Failed to load accounts.");
          } else {
            setPageData(data || null);
          }
        }
      } catch {
        if (!ignore) {
          setError("Network error.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      ignore = true;
    };
  }, [page, pageSize]);

  const rawAccounts = useMemo(() => pageData?.content || [], [pageData]);

  const filteredAccounts = useMemo(() => {
    return rawAccounts.filter((acc) => {
      const matchesSearch =
        !searchQuery.trim() ||
        (acc.userId && acc.userId.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (acc.id && acc.id.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesCurrency = currencyFilter === "ALL" || acc.currency === currencyFilter;

      return matchesSearch && matchesCurrency;
    });
  }, [rawAccounts, searchQuery, currencyFilter]);

  return (
    <div className="space-y-4 select-none animate-in fade-in duration-200">
      {/* Header + Toolbar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-baseline gap-2">
          <h1 className="text-xl font-bold text-[#1D1D1F] tracking-tight">Accounts</h1>
          <span className="text-xs font-mono text-[#86868B]">
            {pageData?.totalElements ?? 0}
          </span>
        </div>

        {/* Filter Controls */}
        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-64">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by ID..."
              className="w-full pl-8 pr-3 py-1.5 bg-white border border-black/[0.08] rounded-xl text-xs text-[#1D1D1F] placeholder-[#86868B] focus:outline-none focus:ring-1 focus:ring-[#C5A059] font-mono shadow-2xs"
            />
            <svg
              className="w-3.5 h-3.5 text-[#86868B] absolute left-2.5 top-2.5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>

          <select
            value={currencyFilter}
            onChange={(e) => setCurrencyFilter(e.target.value)}
            className="px-2.5 py-1.5 bg-white border border-black/[0.08] rounded-xl text-xs font-medium text-[#1D1D1F] focus:outline-none focus:ring-1 focus:ring-[#C5A059] cursor-pointer shadow-2xs"
          >
            <option value="ALL">All Currencies</option>
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
            <option value="GBP">GBP</option>
            <option value="MAD">MAD</option>
          </select>

          <button
            onClick={handleSync}
            disabled={loading}
            title="Refresh"
            className="p-1.5 rounded-xl bg-white border border-black/[0.08] text-[#86868B] hover:text-[#1D1D1F] hover:bg-black/[0.02] shadow-2xs transition-colors cursor-pointer"
          >
            <svg
              className={`w-4 h-4 ${loading ? "animate-spin" : ""}`}
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
            </svg>
          </button>
        </div>
      </div>

      {/* Accounts Table */}
      <div className="bg-white border border-black/[0.06] rounded-2xl shadow-xs overflow-hidden">
        {error && (
          <div className="p-3 bg-red-50 text-red-700 text-xs border-b border-red-100 flex items-center justify-between">
            <span>{error}</span>
            <button onClick={handleSync} className="font-semibold underline cursor-pointer">
              Retry
            </button>
          </div>
        )}

        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center gap-2">
            <div className="w-5 h-5 border-2 border-[#C5A059]/30 border-t-[#C5A059] rounded-full animate-spin" />
            <span className="text-xs text-[#86868B] font-mono">Loading...</span>
          </div>
        ) : filteredAccounts.length === 0 ? (
          <div className="py-16 text-center text-xs text-[#86868B]">
            No accounts found.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-black/[0.06] bg-[#FBFBFD] text-[11px] font-semibold text-[#86868B] uppercase tracking-wider">
                  <th className="py-2.5 px-4">Account ID</th>
                  <th className="py-2.5 px-4">Currency</th>
                  <th className="py-2.5 px-4 text-right">Balance</th>
                  <th className="py-2.5 px-4">Owner ID</th>
                  <th className="py-2.5 px-4 text-right"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/[0.04]">
                {filteredAccounts.map((account) => (
                  <tr
                    key={account.id}
                    className="hover:bg-[#FBFBFD] transition-colors group cursor-pointer"
                    onClick={() => setSelectedAccount(account)}
                  >
                    <td className="py-3 px-4">
                      <ClickableId id={account.id} className="text-xs text-[#1D1D1F]" />
                    </td>
                    <td className="py-3 px-4 font-semibold text-[#1D1D1F]">
                      {account.currency}
                    </td>
                    <td className="py-3 px-4 text-right font-mono font-bold text-[#1D1D1F]">
                      {formatCurrencyAmount(account.balance ?? 0, account.currency)}
                    </td>
                    <td className="py-3 px-4">
                      <ClickableId id={account.userId} className="text-[11px] text-[#86868B]" />
                    </td>
                    <td className="py-3 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={() => setSelectedAccount(account)}
                        className="px-2.5 py-1 rounded-lg bg-black/[0.04] hover:bg-black/[0.08] text-[#1D1D1F] text-xs font-medium transition-colors cursor-pointer"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Minimal Pagination */}
        <div className="p-3 border-t border-black/[0.06] bg-[#FBFBFD] flex items-center justify-between text-xs">
          <span className="text-[#86868B]">
            Page {page + 1} of {Math.max(1, pageData?.totalPages ?? 1)}
          </span>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || loading}
              className="px-2.5 py-1 rounded-lg border border-black/[0.08] bg-white text-xs text-[#1D1D1F] hover:bg-black/[0.02] disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors shadow-2xs"
            >
              Prev
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={Boolean(pageData?.last) || loading}
              className="px-2.5 py-1 rounded-lg border border-black/[0.08] bg-white text-xs text-[#1D1D1F] hover:bg-black/[0.02] disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors shadow-2xs"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Account Inspect Modal */}
      {selectedAccount && (
        <AccountInspectModal
          account={selectedAccount}
          onClose={() => setSelectedAccount(null)}
          onAccountUpdated={handleSync}
        />
      )}
    </div>
  );
}
