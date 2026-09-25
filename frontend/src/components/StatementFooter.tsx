import { CineSeal } from "./CineLogo";

export function StatementFooter() {
    return (
        <footer className="statement-footer">
            <p>Money is serious. The interface can stay useful.</p>
            <div className="statement-footer-meta flex items-center gap-3">
                <span
                    className="inline-flex items-center gap-1.5 font-bold uppercase text-xs text-[#1D1D1F]"
                    style={{ fontFamily: "'Cinzel', serif", letterSpacing: "0.20em" }}
                >
                    <CineSeal size={14} />
                    CINE
                </span>
                <span>2026</span>
            </div>
        </footer>
    );
}
