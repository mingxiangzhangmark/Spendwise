import React, { useEffect, useMemo, useState } from "react";
import { FaExchangeAlt } from "react-icons/fa";

const CURRENCY_OPTIONS = [
  { code: "AUD", name: "Australian Dollar" },
  { code: "USD", name: "US Dollar" },
  { code: "CNY", name: "Chinese Yuan" },
  { code: "EUR", name: "Euro" },
  { code: "JPY", name: "Japanese Yen" },
  { code: "GBP", name: "British Pound" },
];

const LABEL_CLASSES =
  "text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 transition-colors duration-200";

const SELECT_CLASSES =
  "mt-2 w-full rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-3 text-sm focus:border-gray-400 dark:focus:border-gray-500 focus:outline-none transition-colors duration-200";

const formatNumber = (value, currency) => {
  if (!value) {
    return "";
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  }).format(value);
};

export default function CurrencyConversion() {
  const [fromCurrency, setFromCurrency] = useState("AUD");
  const [toCurrency, setToCurrency] = useState("USD");
  const [amount, setAmount] = useState(1);
  const [convertedAmount, setConvertedAmount] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const exchangeRate = useMemo(() => {
    if (!convertedAmount || !amount) {
      return null;
    }

    return Number(convertedAmount) / Number(amount);
  }, [convertedAmount, amount]);

  useEffect(() => {
    if (amount > 0) {
      void convertCurrency();
    } else {
      setConvertedAmount("");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fromCurrency, toCurrency, amount]);

  const convertCurrency = async () => {
    if (amount <= 0 || !fromCurrency || !toCurrency) {
      return;
    }

    setIsLoading(true);
    setError("");

    try {
      const apiKey = import.meta.env.VITE_UNIRATE_API_KEY;
      const url = `https://api.unirateapi.com/api/convert?api_key=${apiKey}&amount=${amount}&from=${fromCurrency}&to=${toCurrency}&format=json`;

      const response = await fetch(url);
      const data = await response.json();

      if (!response.ok) {
        setError(data.message || "Currency conversion failed");
        console.error("API Error:", data);
        setConvertedAmount("");
        return;
      }

      const formattedResult = Number(data.result).toFixed(4);
      setConvertedAmount(formattedResult);
    } catch (convertError) {
      setError("Failed to connect to currency service");
      console.error("Fetch Error:", convertError);
      setConvertedAmount("");
    } finally {
      setIsLoading(false);
    }
  };

  const swapCurrencies = () => {
    setFromCurrency(toCurrency);
    setToCurrency(fromCurrency);
  };

  const handleAmountChange = (event) => {
    const value = event.target.value;
    if (value === "") {
      setAmount("");
      return;
    }

    const numericValue = Number(value);
    if (Number.isNaN(numericValue)) {
      return;
    }

    setAmount(numericValue);
  };

  const conversionSummary = () => {
    if (!exchangeRate) {
      return "";
    }

    return `1 ${fromCurrency} = ${exchangeRate.toFixed(4)} ${toCurrency}`;
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-10 flex flex-col gap-2">
        <span className="text-sm uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 transition-colors duration-200">Tools</span>
        <h1 className="text-3xl font-semibold text-gray-900 dark:text-white transition-colors duration-200">Currency Conversion</h1>
        <p className="text-gray-500 dark:text-gray-400 transition-colors duration-200">
          Quickly translate amounts between supported currencies using live market data.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm lg:col-span-2 transition-colors duration-200">
          <form className="space-y-6" onSubmit={(event) => event.preventDefault()}>
            <div className="grid gap-6 sm:grid-cols-2">
              <div>
                <label className={LABEL_CLASSES} htmlFor="fromCurrency">
                  From
                </label>
                <select
                  id="fromCurrency"
                  value={fromCurrency}
                  onChange={(event) => setFromCurrency(event.target.value)}
                  className={SELECT_CLASSES}
                >
                  {CURRENCY_OPTIONS.map((currency) => (
                    <option key={`from-${currency.code}`} value={currency.code}>
                      {currency.name}
                    </option>
                  ))}
                </select>

                <label className={`${LABEL_CLASSES} mt-6 block`} htmlFor="amount">
                  Amount
                </label>
                <div className="relative">
                  <input
                    id="amount"
                    type="number"
                    min="0"
                    value={amount}
                    onChange={handleAmountChange}
                    className={`${SELECT_CLASSES} pr-16`}
                    placeholder="Enter amount"
                  />
                  <span className="pointer-events-none absolute inset-y-0 right-4 flex items-center text-xs font-semibold uppercase tracking-[0.3em] text-gray-400">
                    {fromCurrency}
                  </span>
                </div>
              </div>

              <div>
                <label className={LABEL_CLASSES} htmlFor="toCurrency">
                  To
                </label>
                <select
                  id="toCurrency"
                  value={toCurrency}
                  onChange={(event) => setToCurrency(event.target.value)}
                  className={SELECT_CLASSES}
                >
                  {CURRENCY_OPTIONS.map((currency) => (
                    <option key={`to-${currency.code}`} value={currency.code}>
                      {currency.name}
                    </option>
                  ))}
                </select>

                <label className={`${LABEL_CLASSES} mt-6 block`} htmlFor="convertedAmount">
                  Converted Amount
                </label>
                <div className="relative">
                  <input
                    id="convertedAmount"
                    type="text"
                    value={
                      isLoading
                        ? "Converting..."
                        : error
                          ? "Error"
                          : convertedAmount
                          ? formatNumber(convertedAmount, toCurrency)
                          : ""
                    }
                    readOnly
                    className={`${SELECT_CLASSES} bg-gray-50 dark:bg-gray-900 pr-16`}
                  />
                  <span className="pointer-events-none absolute inset-y-0 right-4 flex items-center text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 transition-colors duration-200">
                    {toCurrency}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex flex-col items-center gap-4 sm:flex-row">
              <button
                type="button"
                onClick={swapCurrencies}
                className="inline-flex items-center gap-2 rounded-full border border-gray-200 dark:border-gray-700 px-4 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-gray-700 dark:text-gray-300 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
              >
                <FaExchangeAlt /> Swap
              </button>
              <button
                type="button"
                onClick={convertCurrency}
                className="inline-flex items-center gap-2 rounded-full bg-gray-900 dark:bg-indigo-600 px-5 py-3 text-sm font-semibold uppercase tracking-[0.3em] text-white transition-colors hover:bg-gray-700 dark:hover:bg-indigo-700"
              >
                Refresh Rate
              </button>
              {conversionSummary() && (
                <span className="text-xs font-semibold uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 transition-colors duration-200">
                  {conversionSummary()}
                </span>
              )}
            </div>

            {error && (
              <div className="rounded-2xl border border-rose-200 dark:border-rose-800 bg-rose-50 dark:bg-rose-900/20 p-4 text-sm text-rose-600 dark:text-rose-400 transition-colors duration-200">
                {error}
              </div>
            )}
          </form>
        </section>

        <section className="rounded-3xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-colors duration-200">
          <p className="text-xs uppercase tracking-[0.3em] text-gray-400 dark:text-gray-500 transition-colors duration-200">Heads Up</p>
          <h2 className="mt-2 text-xl font-semibold text-gray-900 dark:text-white transition-colors duration-200">Live Market Data</h2>
          <p className="mt-4 text-sm text-gray-500 dark:text-gray-400 transition-colors duration-200">
            Rates update in real time using Unirate. Values may fluctuate slightly; always
            refresh if you pause for a while.
          </p>

          <div className="mt-6 space-y-3 text-sm text-gray-600 dark:text-gray-400 transition-colors duration-200">
            <p>
              • We automatically convert as you type when a valid amount is present.
            </p>
            <p>• Rates include four decimal places for clarity in large transfers.</p>
            <p>• Swap currencies at any time to compare the opposite direction.</p>
          </div>
        </section>
      </div>
    </div>
  );
}
