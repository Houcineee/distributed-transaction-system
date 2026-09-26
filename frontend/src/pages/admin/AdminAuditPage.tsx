import { useState, useEffect, useMemo } from "react";
import { client } from "../../lib/api";
import { ClickableId } from "../../components/ClickableId";
import type { components } from "../../types/api";

type PageAuditLogResponse = components["schemas"]["PageAuditLogResponse"];
type AuditAction = components["schemas"]["AuditLogResponse"]["action"];

export function AdminAuditPage() {
  const [pageData, setPageData] = useState<PageAuditLogResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(25);
  const [selectedAction, setSelectedAction] = useState<string>("ALL");
  const [actorIdFilter, setActorIdFilter] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  const handleRefresh = async () => {
    setLoading(true);
    setError(null);
    try {
      const queryParams: {
        pageable: { page: number; size: number; sort: string[] };
        action?: AuditAction;
        actorId?: string;
      } = {
        pageable: {
          page,
          size: pageSize,
          sort: ["timestamp,desc"],
        },
      };

      if (selectedAction !== "ALL") {
        queryParams.action = selectedAction as AuditAction;
      }
      if (actorIdFilter.trim()) {
        queryParams.actorId = actorIdFilter.trim();
      }

      const { data, error: apiErr } = await client.GET("/api/v1/admin/audit-logs", {
        params: {
          query: queryParams,
        },
      });

      if (apiErr) {
        setError("Failed to load audit logs.");
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
        const queryParams: {
          pageable: { page: number; size: number; sort: string[] };
          action?: AuditAction;
          actorId?: string;
        } = {
          pageable: {
            page,
            size: pageSize,
            sort: ["timestamp,desc"],
          },
        };

        if (selectedAction !== "ALL") {
          queryParams.action = selectedAction as AuditAction;
        }
        if (actorIdFilter.trim()) {
          queryParams.actorId = actorIdFilter.trim();
        }

        const { data, error: apiErr } = await client.GET("/api/v1/admin/audit-logs", {
          params: {
            query: queryParams,
          },
        });

        if (!ignore) {
          if (apiErr) {
            setError("Failed to load audit logs.");
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
  }, [page, pageSize, selectedAction, actorIdFilter]);

  const logs = useMemo(() => pageData?.content || [], [pageData]);

  const getActionBadge = (action?: string) => {
    switch (action) {
      case "LOGIN_SUCCESS":
      case "CARD_ACTIVATED":
        return "bg-emerald-50 text-emerald-700 border-emerald-200";
      case "LOGIN_FAILURE":
      case "ACCESS_DENIED":
      case "TRANSFER_FAILED":
        return "bg-red-50 text-red-700 border-red-200";
      case "TRANSFER_EXECUTED":
        return "bg-[#C5A059]/10 text-[#C5A059] border-[#C5A059]/30";
      case "CARD_BLOCKED":
        return "bg-amber-50 text-amber-700 border-amber-200";
      case "USER_REGISTERED":
      case "ACCOUNT_CREATED":
      case "CARD_CREATED":
        return "bg-blue-50 text-blue-700 border-blue-200";
      default:
        return "bg-zinc-100 text-zinc-700 border-zinc-200";
    }
  };

  return (
    <div className="space-y-4 select-none animate-in fade-in duration-200">
      {/* Header + Toolbar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-baseline gap-2">
          <h1 className="text-xl font-bold text-[#1D1D1F] tracking-tight">Audit</h1>
          <span className="text-xs font-mono text-[#86868B]">
            {pageData?.totalElements ?? 0}
          </span>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-56">
            <input
              type="text"
              value={actorIdFilter}
              onChange={(e) => {
                setActorIdFilter(e.target.value);
                setPage(0);
              }}
              placeholder="Actor ID..."
              className="w-full pl-8 pr-3 py-1.5 bg-white border border-black/[0.08] rounded-xl text-xs text-[#1D1D1F] placeholder-[#86868B] focus:outline-none focus:ring-1 focus:ring-[#C5A059] font-mono shadow-2xs"
            />
            <svg
              className="w-3.5 h-3.5 text-[#86868B] absolute left-2.5 top-2.5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </div>

          <select
            value={selectedAction}
            onChange={(e) => {
              setSelectedAction(e.target.value);
              setPage(0);
            }}
            className="px-2.5 py-1.5 bg-white border border-black/[0.08] rounded-xl text-xs font-medium text-[#1D1D1F] focus:outline-none focus:ring-1 focus:ring-[#C5A059] cursor-pointer shadow-2xs"
          >
            <option value="ALL">All Actions</option>
            <option value="LOGIN_SUCCESS">LOGIN_SUCCESS</option>
            <option value="LOGIN_FAILURE">LOGIN_FAILURE</option>
            <option value="ACCESS_DENIED">ACCESS_DENIED</option>
            <option value="USER_REGISTERED">USER_REGISTERED</option>
            <option value="TRANSFER_EXECUTED">TRANSFER_EXECUTED</option>
            <option value="TRANSFER_FAILED">TRANSFER_FAILED</option>
            <option value="CARD_CREATED">CARD_CREATED</option>
            <option value="CARD_BLOCKED">CARD_BLOCKED</option>
            <option value="CARD_ACTIVATED">CARD_ACTIVATED</option>
            <option value="ACCOUNT_CREATED">ACCOUNT_CREATED</option>
          </select>

          <button
            onClick={handleRefresh}
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

      {/* Audit Logs Table */}
      <div className="bg-white border border-black/[0.06] rounded-2xl shadow-xs overflow-hidden">
        {error && (
          <div className="p-3 bg-red-50 text-red-700 text-xs border-b border-red-100 flex items-center justify-between">
            <span>{error}</span>
            <button onClick={handleRefresh} className="font-semibold underline cursor-pointer">
              Retry
            </button>
          </div>
        )}

        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center gap-2">
            <div className="w-5 h-5 border-2 border-[#C5A059]/30 border-t-[#C5A059] rounded-full animate-spin" />
            <span className="text-xs text-[#86868B] font-mono">Loading...</span>
          </div>
        ) : logs.length === 0 ? (
          <div className="py-16 text-center text-xs text-[#86868B]">
            No events found.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-black/[0.06] bg-[#FBFBFD] text-[11px] font-semibold text-[#86868B] uppercase tracking-wider">
                  <th className="py-2.5 px-4">Time</th>
                  <th className="py-2.5 px-4">Action</th>
                  <th className="py-2.5 px-4">Actor</th>
                  <th className="py-2.5 px-4">Target</th>
                  <th className="py-2.5 px-4">IP</th>
                  <th className="py-2.5 px-4">Details</th>
                  <th className="py-2.5 px-4 text-right">Trace</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/[0.04] font-mono">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-[#FBFBFD] transition-colors">
                    <td className="py-3 px-4 text-[11px] text-[#86868B] whitespace-nowrap">
                      {log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : "—"}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase border ${getActionBadge(
                          log.action
                        )}`}
                      >
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <ClickableId id={log.actorId} className="text-[11px] text-[#86868B]" />
                    </td>
                    <td className="py-3 px-4 text-[11px]">
                      <span className="text-[#1D1D1F]">{log.targetEntityType || "—"}</span>
                      {log.targetEntityId && (
                        <span className="text-[#86868B] block text-[10px]">
                          {log.targetEntityId.slice(0, 8)}...
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-[11px] text-[#86868B] whitespace-nowrap">
                      {log.ipAddress || "127.0.0.1"}
                    </td>
                    <td className="py-3 px-4 text-[11px] text-[#1D1D1F] max-w-xs truncate font-sans">
                      {log.details || "—"}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <ClickableId id={log.correlationId} className="text-[11px] text-[#86868B]" />
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
    </div>
  );
}
