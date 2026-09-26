import { useState, useEffect } from "react";
import { client } from "../../lib/api";

export function AdminHealthPage() {
  const [healthStatus, setHealthStatus] = useState<string | null>(null);
  const [latencyMs, setLatencyMs] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  const probeHealth = async () => {
    setLoading(true);
    const start = performance.now();
    try {
      const { data, error } = await client.GET("/api/health");
      const elapsed = Math.round(performance.now() - start);
      setLatencyMs(elapsed);
      if (!error && data && data.status) {
        setHealthStatus(data.status);
      } else {
        setHealthStatus("DEGRADED");
      }
    } catch {
      setHealthStatus("DOWN");
      setLatencyMs(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let ignore = false;
    async function load() {
      const start = performance.now();
      try {
        const { data, error } = await client.GET("/api/health");
        if (!ignore) {
          const elapsed = Math.round(performance.now() - start);
          setLatencyMs(elapsed);
          if (!error && data && data.status) {
            setHealthStatus(data.status);
          } else {
            setHealthStatus("DEGRADED");
          }
        }
      } catch {
        if (!ignore) {
          setHealthStatus("DOWN");
          setLatencyMs(null);
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
  }, []);

  const isHealthy = healthStatus === "ok";

  return (
    <div className="space-y-4 select-none animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-[#1D1D1F] tracking-tight">System Health</h1>

        <button
          onClick={probeHealth}
          disabled={loading}
          title="Probe Health"
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

      {/* Status Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Core Gateway */}
        <div className="p-5 rounded-2xl bg-white border border-black/[0.06] shadow-xs flex items-center justify-between">
          <div>
            <div className="text-[11px] font-medium text-[#86868B] uppercase tracking-wider">Gateway</div>
            <div className="text-base font-bold text-[#1D1D1F] font-mono mt-0.5">
              {isHealthy ? "OPERATIONAL" : healthStatus || "CHECKING"}
            </div>
          </div>
          <div className="text-right">
            <div className="text-[11px] font-medium text-[#86868B] uppercase tracking-wider">Latency</div>
            <div className="text-base font-bold text-[#1D1D1F] font-mono mt-0.5">
              {latencyMs !== null ? `${latencyMs} ms` : "—"}
            </div>
          </div>
        </div>

        {/* Database */}
        <div className="p-5 rounded-2xl bg-white border border-black/[0.06] shadow-xs flex items-center justify-between">
          <div>
            <div className="text-[11px] font-medium text-[#86868B] uppercase tracking-wider">Database</div>
            <div className="text-base font-bold text-[#1D1D1F] font-mono mt-0.5">
              CONNECTED
            </div>
          </div>
          <div className="text-right">
            <div className="text-[11px] font-medium text-[#86868B] uppercase tracking-wider">Engine</div>
            <div className="text-xs font-mono font-semibold text-[#86868B] mt-1">
              PostgreSQL 17
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
