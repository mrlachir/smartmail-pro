"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function SettingsContent() {
  const { data: session, status } = useSession();
  const [apiKey, setApiKey] = useState("");
  const [message, setMessage] = useState("");
  const [isConfigured, setIsConfigured] = useState(false);

  // THE UPGRADE: Wait for session, then fetch Vault data securely
  useEffect(() => {
    if (session?.user?.email) {
      const checkVaultStatus = async () => {
        try {
          const res = await fetch("http://localhost:8080/api/vault", {
            headers: { "X-User-Email": session.user.email } // PASS IDENTIFIER
          });
          if (res.ok) {
            const data = await res.json();
            if (data && data.geminiApiKeyEncrypted) {
              setIsConfigured(true);
            }
          }
        } catch (error) {
          console.error("Backend not running or unreachable.", error);
        }
      };
      checkVaultStatus();
    }
  }, [session]);

  const handleSave = async (e) => {
    e.preventDefault();
    setMessage("Saving...");
    try {
      const res = await fetch("http://localhost:8080/api/vault", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Email": session.user.email // PASS IDENTIFIER
        },
        body: JSON.stringify({ geminiApiKeyEncrypted: apiKey })
      });
      if (res.ok) {
        setMessage("✅ Vault updated securely.");
        setApiKey("");
        setIsConfigured(true);
      } else {
        setMessage("❌ Failed to save to Vault.");
      }
    } catch (error) {
      setMessage("❌ Error connecting to server.");
    }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Please log in first.</p>;

  return (
    <div className="max-w-2xl mx-auto p-8 mt-10 bg-white rounded shadow text-black">
      <h1 className="text-2xl font-bold mb-6">API Vault Settings</h1>
      
      <div className="mb-6 p-6 border rounded bg-gray-50 shadow-sm">
        <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold">Google Gemini API</h2>
            <p className="text-sm">
              Status: {isConfigured ? <span className="text-green-600 font-bold bg-green-100 px-2 py-1 rounded">Configured ✅</span> : <span className="text-red-600 font-bold bg-red-100 px-2 py-1 rounded">Not Configured ❌</span>}
            </p>
        </div>

        <form onSubmit={handleSave} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1 text-gray-700">Update Gemini API Key</label>
            <input 
              type="password" 
              value={apiKey} 
              onChange={(e) => setApiKey(e.target.value)} 
              placeholder="Paste your key here..." 
              className="w-full p-2 border rounded"
              required
            />
          </div>
          <button type="submit" className="w-full py-2 bg-blue-600 text-white font-bold rounded hover:bg-blue-700">
            Save to Encrypted Vault
          </button>
        </form>
        {message && <p className="mt-4 text-sm font-medium text-center">{message}</p>}
      </div>
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