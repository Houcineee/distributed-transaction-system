export const getCurrencyDetails = (currency?: string) => {
  switch (currency?.toUpperCase()) {
    case "MAD":
      return {
        code: "MAD",
        name: "Moroccan Dirham",
        symbol: "DH",
        localSymbol: "د.م.",
        isPrefix: false, // DH goes AFTER the number (e.g. 0.00 DH)
      };
    case "EUR":
      return {
        code: "EUR",
        name: "Euro",
        symbol: "€",
        localSymbol: "€",
        isPrefix: true,
      };
    case "GBP":
      return {
        code: "GBP",
        name: "British Pound",
        symbol: "£",
        localSymbol: "£",
        isPrefix: true,
      };
    case "USD":
    default:
      return {
        code: "USD",
        name: "US Dollar",
        symbol: "$",
        localSymbol: "$",
        isPrefix: true,
      };
  }
};

export function formatCurrencyAmount(amount: number | string, currency?: string) {
  const num = Number(amount || 0);
  const formatted = num.toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  const info = getCurrencyDetails(currency);
  if (info.isPrefix) {
    return `${info.symbol}${formatted}`;
  } else {
    return `${formatted} ${info.symbol}`;
  }
}
