"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function SubscribersContent() {
  const { data: session, status } = useSession();
  const [file, setFile] = useState(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [subscribers, setSubscribers] = useState([]);

  // THE HANDSHAKE: Register user and fetch their specific data
  useEffect(() => {
    if (session?.user?.email) {
      const syncAndFetch = async () => {
        try {
          // 1. Tell backend the user is here
          await fetch("http://localhost:8080/api/users/sync", {
            method: "POST",
            headers: { "X-User-Email": session.user.email }
          });
          // 2. Fetch their isolated data
          fetchSubscribers();
        } catch (error) {
          console.error("Failed to sync user", error);
        }
      };
      syncAndFetch();
    }
  }, [session]);

  const fetchSubscribers = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/subscribers", {
        headers: { "X-User-Email": session.user.email } // PASS THE IDENTIFIER
      });
      if (res.ok) {
        const data = await res.json();
        setSubscribers(data);
      }
    } catch (error) {
      console.error("Failed to fetch subscribers", error);
    }
  };

  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      setMessage("❌ Please select a file first.");
      return;
    }

    setLoading(true);
    setMessage("Uploading...");

    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await fetch("http://localhost:8080/api/subscribers/import", {
        method: "POST",
        headers: { "X-User-Email": session.user.email }, // PASS THE IDENTIFIER
        body: formData,
      });

      const data = await res.json();

      if (res.ok) {
        setMessage("✅ " + data.message);
        setFile(null);
        document.getElementById("csvFileInput").value = "";
        fetchSubscribers();
      } else {
        setMessage("❌ " + data.message);
      }
    } catch (error) {
      setMessage("❌ Error connecting to the server.");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm("Are you sure you want to delete this subscriber?")) return;
    
    try {
      const res = await fetch(`http://localhost:8080/api/subscribers/${id}`, {
        method: "DELETE",
        headers: { "X-User-Email": session.user.email } // PASS THE IDENTIFIER
      });
      
      if (res.ok) {
        fetchSubscribers();
      } else {
        alert("Failed to delete subscriber.");
      }
    } catch (error) {
      console.error("Error deleting", error);
    }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Please log in first.</p>;

  return (
    <div className="max-w-7xl mx-auto pb-12">
      {/* Header */}
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">Gestion des Abonnés</h1>
          <p className="text-gray-500 mt-1">Importez vos listes de contacts et surveillez l'état de votre audience.</p>
        </div>
      </div>
      
      {/* CSV Import Block */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden mb-8">
        <div className="p-6">
          <div className="border-2 border-dashed border-gray-200 rounded-xl p-8 text-center bg-gray-50">
            <h2 className="text-lg font-bold text-gray-900 mb-2">Importer des Contacts CSV</h2>
            <p className="text-sm text-gray-500 mb-6">Format requis : Email, Prénom, Nom</p>
            
            <form onSubmit={handleUpload} className="space-y-4 max-w-md mx-auto">
              <input 
                id="csvFileInput"
                type="file" 
                accept=".csv" 
                onChange={handleFileChange}
                className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-bold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 transition"
              />
              <button 
                type="submit" 
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg font-medium shadow-sm transition disabled:opacity-50"
              >
                {loading ? "Importation en cours..." : "Téléverser & Importer"}
              </button>
            </form>
            {message && (
              <div className="mt-4 inline-block px-4 py-2 rounded-lg bg-white border border-gray-100 shadow-sm">
                <p className={`font-bold text-sm ${message.includes('❌') ? 'text-red-600' : 'text-green-600'}`}>
                  {message}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Subscribers Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-100 flex justify-between items-center">
          <h2 className="text-lg font-bold text-gray-900">Liste des Abonnés ({subscribers.length})</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th className="p-4 font-semibold">Email</th>
                <th className="p-4 font-semibold">Prénom</th>
                <th className="p-4 font-semibold">Nom</th>
                <th className="p-4 font-semibold">Statut</th>
                <th className="p-4 font-semibold text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-sm">
              {subscribers.length === 0 ? (
                <tr>
                  <td colSpan="5" className="p-8 text-center text-gray-500 font-medium">
                    Aucun abonné trouvé. Téléversez un CSV pour commencer.
                  </td>
                </tr>
              ) : (
                subscribers.map((sub) => (
                  <tr key={sub.id} className="hover:bg-gray-50 transition">
                    <td className="p-4 font-bold text-gray-900">{sub.email}</td>
                    <td className="p-4 text-gray-600">{sub.firstName || "-"}</td>
                    <td className="p-4 text-gray-600">{sub.lastName || "-"}</td>
                    <td className="p-4">
                      <span className="px-2.5 py-1 bg-green-100 text-green-700 rounded-full text-xs font-bold">
                        {sub.status || "Actif"}
                      </span>
                    </td>
                    <td className="p-4 text-center">
                      <button 
                        onClick={() => handleDelete(sub.id)}
                        className="text-gray-400 hover:text-red-600 transition p-1 rounded-md hover:bg-red-50 inline-flex items-center justify-center"
                        title="Supprimer l'abonné"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default function SubscribersPage() {
  return (
    <SessionProvider>
      <SubscribersContent />
    </SessionProvider>
  );
}