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
             // THE FIX: Read the raw text first to see if the backend actually sent anything
             const text = await res.text(); 
             if (text) { 
               const data = JSON.parse(text);
               if (data && data.geminiApiKeyEncrypted) {
                 setIsConfigured(true);
               }
             } else {
               // If the text is empty, the vault doesn't exist yet.
               setIsConfigured(false); 
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
    <div className="max-w-7xl mx-auto pb-12">
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">API Vault</h1>
          <p className="text-gray-500 mt-1">Gérez vos clés API et vos intégrations d'Intelligence Artificielle en toute sécurité.</p>
        </div>
      </div>
      
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden max-w-3xl">
        <div className="p-6 border-b border-gray-100">
          <div className="flex justify-between items-center">
              <h2 className="text-lg font-bold text-gray-900">Google Gemini API</h2>
              <div>
                {isConfigured ? (
                    <span className="bg-green-100 text-green-700 px-3 py-1 rounded-full text-xs font-bold">Configuré ✅</span>
                ) : (
                    <span className="bg-red-100 text-red-700 px-3 py-1 rounded-full text-xs font-bold">Non Configuré ❌</span>
                )}
              </div>
          </div>
          <p className="text-sm text-gray-500 mt-2">Nécessaire pour les fonctionnalités d'analyse d'audience et de génération de design de SmartMail Pro.</p>
        </div>

        <div className="p-6 bg-gray-50">
          <form onSubmit={handleSave} className="space-y-5">
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2">Clé API Gemini</label>
              <input 
                type="password" 
                value={apiKey} 
                onChange={(e) => setApiKey(e.target.value)} 
                placeholder="Collez votre clé secrète ici (commence par AIza...)" 
                className="w-full p-3 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
                required
              />
              <p className="text-xs text-gray-400 mt-2">Votre clé est chiffrée (AES-256) avant d'être sauvegardée dans la base de données.</p>
            </div>
            <button type="submit" className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg shadow-sm transition">
              Enregistrer dans le Vault Sécurisé
            </button>
          </form>
          {message && (
            <div className={`mt-4 p-3 rounded-lg text-sm font-bold border text-center ${message.includes('❌') ? 'bg-red-50 text-red-700 border-red-200' : 'bg-green-50 text-green-700 border-green-200'}`}>
              {message}
            </div>
          )}
        </div>
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