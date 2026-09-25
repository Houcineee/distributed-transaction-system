import { Component, type ErrorInfo, type ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught application error:", error, errorInfo);
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = "/dashboard";
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#FBFBFD] flex items-center justify-center p-6 select-none">
          <div className="w-full max-w-md bg-white border border-black/[0.08] shadow-[0_20px_48px_-12px_rgba(0,0,0,0.1)] rounded-[24px] p-8 text-center">
            {/* Warning Icon Badge */}
            <div className="w-12 h-12 rounded-2xl bg-black/[0.04] text-[#1D1D1F] flex items-center justify-center mx-auto mb-5">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </div>

            <div className="text-[11px] font-semibold uppercase tracking-wider text-[#86868B] mb-1">
              Safety Guard
            </div>
            <h1 className="text-2xl font-normal tracking-tight text-[#1D1D1F] mb-2">
              Session Interrupted
            </h1>
            <p className="text-xs text-[#86868B] leading-relaxed mb-6">
              An unexpected client-side error occurred. Your financial records and ledger balance remain completely secure.
            </p>

            <div className="space-y-2.5">
              <button
                type="button"
                onClick={this.handleReload}
                className="w-full py-3 px-4 rounded-full bg-[#1D1D1F] hover:bg-black text-white text-xs font-semibold tracking-wide transition-all shadow-sm active:scale-[0.98] cursor-pointer"
              >
                Reload Interface
              </button>

              <button
                type="button"
                onClick={this.handleGoHome}
                className="w-full py-3 px-4 rounded-full bg-[#F5F5F7] hover:bg-[#EBEBED] text-[#1D1D1F] text-xs font-semibold tracking-wide transition-all active:scale-[0.98] cursor-pointer"
              >
                Return to Overview
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
