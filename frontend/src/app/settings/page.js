"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function SettingsContent() {
  const { data: session, status } = useSession();
  const [geminiKey, setGeminiKey] = useState("");
  const [gmailToken, setGmailToken] = useState("");
  const [message, setMessage] = useState("");
  const [hasKeys, setHasKeys] = useState(false);

  useEffect(() => {
    async function checkVaultStatus() {
      try {
        const res = await fetch("http://localhost:8080/api/vault/status");
        if (res.ok) {
          const data = await res.json();
          setHasKeys(data.hasKeys);
        }
      } catch (error) {
        console.error("Backend not running or unreachable.");
      }
    }
    checkVaultStatus();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("Saving...");

    try {
      const res = await fetch("http://localhost:8080/api/vault/update", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ geminiKey, gmailToken }),
      });

      if (res.ok) {
        setMessage("✅ Keys encrypted and saved securely.");
        setHasKeys(true);
        setGeminiKey("");
        setGmailToken("");
      } else {
        setMessage("❌ Failed to save keys.");
      }
    } catch (error) {
      setMessage("❌ Error connecting to the server.");
    }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Please log in first.</p>;

  return (
    <div className="max-w-2xl mx-auto p-8 mt-10 bg-white rounded shadow">
      <h1 className="text-2xl font-bold mb-6">API Vault (Encrypted)</h1>
      
      {hasKeys && (
        <div className="mb-6 p-4 bg-green-50 text-green-700 border border-green-200 rounded">
          Your API keys are currently stored and encrypted in the vault.
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Gemini API Key</label>
          <input
            type="password"
            value={geminiKey}
            onChange={(e) => setGeminiKey(e.target.value)}
            className="w-full p-2 border rounded text-black"
            placeholder={hasKeys ? "••••••••••••••••" : "Enter new Gemini Key"}
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">Gmail OAuth Token (JSON/String)</label>
          <input
            type="password"
            value={gmailToken}
            onChange={(e) => setGmailToken(e.target.value)}
            className="w-full p-2 border rounded text-black"
            placeholder={hasKeys ? "••••••••••••••••" : "Enter new Gmail Token"}
          />
        </div>

        <button 
          type="submit" 
          className="w-full py-2 px-4 bg-blue-600 text-white font-bold rounded hover:bg-blue-700"
        >
          Save to Vault
        </button>
      </form>

      {message && <p className="mt-4 text-center font-medium">{message}</p>}
    </div>
  );
}

export default function SettingsPage() {
  return (
    <SessionProvider>
      <SettingsContent />
    </SessionProvider>
  );
}