"use client";
import { useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function SegmentsContent() {
  const router = useRouter(); 

  const { data: session, status } = useSession();
  const [segments, setSegments] = useState([]);
  
  const [editingId, setEditingId] = useState(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [rules, setRules] = useState([{ column: "status", operator: "=", value: "" }]);
  const [message, setMessage] = useState("");

  const [availableColumns, setAvailableColumns] = useState(["status"]);
  const [evaluatedSubscribers, setEvaluatedSubscribers] = useState(null);
  const [viewingSegmentName, setViewingSegmentName] = useState("");

  const [aiSuggestions, setAiSuggestions] = useState([]);
  const [isAiLoading, setIsAiLoading] = useState(false);

  const [suggestions, setSuggestions] = useState([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [aiProvider, setAiProvider] = useState("groq"); 

  const fetchSegments = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/segments", {
        headers: { "X-User-Email": session.user.email }
      });
      if (res.ok) setSegments(await res.json());
    } catch (error) {
      console.error("Failed to fetch segments");
    }
  };

  const fetchAttributes = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/subscribers/attributes", {
        headers: { "X-User-Email": session.user.email }
      });
      if (res.ok) {
        const data = await res.json();
        setAvailableColumns(["status", ...data]);
      }
    } catch (error) {
      console.error("Failed to fetch attributes");
    }
  };

  useEffect(() => {
    if (session?.user?.email) {
      fetchSegments();
      fetchAttributes();
    }
  }, [session]);

  const handleAddRule = () => {
    setRules([...rules, { column: "status", operator: "=", value: "" }]);
  };

  const handleRemoveRule = (indexToRemove) => {
    if (rules.length === 1) return;
    setRules(rules.filter((_, index) => index !== indexToRemove));
  };

  const handleRuleChange = (index, field, newValue) => {
    const updatedRules = [...rules];
    updatedRules[index][field] = newValue;
    setRules(updatedRules);
  };

  const handleEdit = (segment) => {
    setEditingId(segment.id);
    setName(segment.name);
    setDescription(segment.description || "");
    
    try {
        const parsedRules = JSON.parse(segment.rules);
        if (Array.isArray(parsedRules)) {
            setRules(parsedRules);
        } else {
            setRules([parsedRules]);
        }
    } catch (e) {
        setRules([{ column: "status", operator: "=", value: "" }]);
    }
    
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setMessage("");
  };

  const cancelEdit = () => {
    setEditingId(null);
    setName("");
    setDescription("");
    setRules([{ column: "status", operator: "=", value: "" }]);
    setMessage("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("Saving...");

    if (rules.some(r => r.value.trim() === "")) {
        setMessage("❌ Please fill in all rule values.");
        return;
    }

    const url = editingId 
        ? `http://localhost:8080/api/segments/${editingId}` 
        : "http://localhost:8080/api/segments";
    const method = editingId ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method: method,
        headers: { 
            "Content-Type": "application/json",
            "X-User-Email": session.user.email 
        },
body: JSON.stringify({ name, description, rules: JSON.stringify(rules), byAi: "manual" }),
      });

      if (res.ok) {
        setMessage(`✅ Segment ${editingId ? 'updated' : 'saved'} successfully.`);
        cancelEdit(); 
        fetchSegments(); 
      } else {
        setMessage(`❌ Failed to ${editingId ? 'update' : 'save'} segment.`);
      }
    } catch (error) {
      setMessage("❌ Error connecting to server.");
    }
  };

  const handleDelete = async (id) => {
    if (!confirm("Are you sure you want to delete this segment?")) return;
    try {
      await fetch(`http://localhost:8080/api/segments/${id}`, { 
          method: "DELETE",
          headers: { "X-User-Email": session.user.email } 
      });
      if (editingId === id) cancelEdit(); 
      fetchSegments();
      setEvaluatedSubscribers(null);
    } catch (error) {
      console.error("Error deleting segment");
    }
  };

  const handleRunRule = async (segment) => {
    setViewingSegmentName(segment.name);
    try {
      const res = await fetch(`http://localhost:8080/api/segments/${segment.id}/subscribers`, {
          headers: { "X-User-Email": session.user.email }
      });
      if (res.ok) {
        setEvaluatedSubscribers(await res.json());
      }
    } catch (error) {
      console.error("Error running segment rules");
    }
  };

const handleAiSuggest = async () => {
    setIsAiLoading(true);
    setIsGenerating(true);
    setMessage("🤖 Asking AI to analyze your database structure...");
    try {
      const res = await fetch(`http://localhost:8080/api/ai/suggest-segments?provider=${aiProvider}`, {
        headers: { "X-User-Email": session.user.email }
      });
      
      if (res.ok) {
        const rawText = await res.text();
        try {
            const parsedData = JSON.parse(rawText);
            setAiSuggestions(parsedData);
            setMessage("✅ AI generated new segment strategies.");
        } catch (parseError) {
            console.error("Failed to parse JSON from AI", rawText);
            setMessage("❌ AI returned invalid data format.");
        }
      } else {
        let errorMsg = "Failed to get AI suggestions.";
        try {
            const errData = await res.json();
            if (errData.message) errorMsg = errData.message;
        } catch (e) { }
        setMessage(`❌ ${errorMsg}`);
      }
    } catch (error) {
      console.error(error);
      setMessage("❌ Error connecting to AI service.");
    } finally {
      setIsAiLoading(false);
      setIsGenerating(false);
    }
  };

  const handleAcceptSuggestion = async (suggestion) => {
    try {
      const res = await fetch("http://localhost:8080/api/segments", {
        method: "POST",
        headers: { 
            "Content-Type": "application/json",
            "X-User-Email": session.user.email 
        },
        body: JSON.stringify({ 
            name: suggestion.name, 
            description: suggestion.description, 
            rules: JSON.stringify(suggestion.rules),
            byAi: "ai" 
        }),
      });

      if (res.ok) {
        setMessage(`✅ Saved AI Segment: ${suggestion.name}`);
        setAiSuggestions(prev => prev.filter(s => s.name !== suggestion.name));
        fetchSegments(); 
      } else {
        setMessage("❌ Failed to save AI segment.");
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
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">Audiences & Segments</h1>
          <p className="text-gray-500 mt-1">Créez des segments dynamiques ou utilisez l'IA pour cibler vos abonnés.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
      {/* Left Column */}
      <div>
        <div className="flex flex-col items-end gap-2 mb-6">
            <div className="flex items-center gap-3">
                <div className="flex bg-white rounded border border-gray-300 p-0.5">
                    <button
                        onClick={() => setAiProvider("groq")}
                        className={`px-3 py-1 text-xs font-bold rounded transition-colors ${aiProvider === 'groq' ? 'bg-purple-100 text-purple-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                    >
                        Groq (Rapide)
                    </button>
                    <button
                        onClick={() => setAiProvider("gemini")}
                        className={`px-3 py-1 text-xs font-bold rounded transition-colors ${aiProvider === 'gemini' ? 'bg-blue-100 text-blue-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                    >
                        Gemini
                    </button>
                </div>

                <button 
                    onClick={handleAiSuggest}
                    disabled={isGenerating}
                    className={`text-white px-6 py-2 rounded-lg font-medium shadow-sm transition disabled:opacity-50 ${aiProvider === 'groq' ? 'bg-purple-600 hover:bg-purple-700' : 'bg-blue-600 hover:bg-blue-700'}`}
                >
                    {isGenerating ? "Analyse en cours..." : "✨ Suggestions IA"}
                </button>
            </div>
            {message && <p className={`text-sm font-bold ${message.includes('❌') ? 'text-red-500' : 'text-green-600'}`}>{message}</p>}
        </div>

        {/* AI Suggestions Box */}
        {aiSuggestions.length > 0 && (
            <div className="mb-8 p-6 bg-purple-50 border border-purple-100 rounded-2xl shadow-sm">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-lg font-bold text-purple-800">✨ Segments Recommandés par l'IA</h2>
                    <button onClick={() => setAiSuggestions([])} className="text-sm font-bold text-gray-500 hover:text-gray-800">Effacer</button>
                </div>
                <div className="space-y-4">
                    {aiSuggestions.map((sug, idx) => (
                        <div key={idx} className="bg-white p-4 border border-purple-100 rounded-xl shadow-sm flex flex-col gap-2">
                            <div className="flex justify-between items-start">
                                <div>
                                    <h3 className="font-bold text-md text-gray-900">{sug.name}</h3>
                                    <p className="text-xs text-gray-600">{sug.description}</p>
                                </div>
                                <button 
                                    onClick={() => handleAcceptSuggestion(sug)}
                                    className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded-lg font-medium transition"
                                >
                                    Accepter
                                </button>
                            </div>
                            <div className="bg-gray-50 p-2 rounded-lg text-xs font-mono border border-gray-100 text-gray-600">
                                {sug.rules.map((r, i) => (
                                    <div key={i}>ET {r.column} {r.operator} {r.value}</div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        )}
        
        <div className={`mb-8 p-6 rounded-2xl shadow-sm border border-gray-100 overflow-hidden ${editingId ? 'bg-yellow-50 border-yellow-200' : 'bg-white'}`}>
          <div className="flex justify-between items-center border-b border-gray-100 pb-4 mb-4">
            <h2 className="text-lg font-bold text-gray-900">
                {editingId ? "Modifier le Segment" : "Créer un Segment Dynamique"}
            </h2>
            {editingId && (
                <button onClick={cancelEdit} className="text-sm font-bold text-gray-500 hover:text-gray-800">
                    Annuler l'édition
                </button>
            )}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1">Nom du Segment</label>
              <input type="text" required value={name} onChange={(e) => setName(e.target.value)} className="w-full p-3 border border-gray-200 rounded-lg bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition" placeholder="ex: Utilisateurs Actifs Berlin > 25" />
            </div>
            
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1">Description (Optionnel)</label>
              <textarea 
                value={description} 
                onChange={(e) => setDescription(e.target.value)} 
                className="w-full p-3 border border-gray-200 rounded-lg text-sm text-gray-700 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition" 
                placeholder="Expliquez la stratégie derrière ce segment..."
                rows="2"
              ></textarea>
            </div>
            
            <div className="border border-gray-200 p-4 bg-gray-50 rounded-xl space-y-3">
                <div className="flex justify-between items-center border-b border-gray-200 pb-2">
                    <span className="font-bold text-gray-700 text-sm">Conditions (ET)</span>
                    <button type="button" onClick={handleAddRule} className="text-xs bg-blue-100 text-blue-700 px-3 py-1.5 rounded-lg hover:bg-blue-200 font-bold transition">
                        + Ajouter
                    </button>
                </div>

                {rules.map((rule, index) => (
                    <div key={index} className="flex gap-2 items-center bg-white p-2 border border-gray-200 rounded-lg shadow-sm">
                        <select 
                            value={rule.column} 
                            onChange={(e) => handleRuleChange(index, "column", e.target.value)} 
                            className="flex-1 p-2 border border-gray-200 rounded-md bg-white text-sm focus:outline-none"
                        >
                            {availableColumns.map((col) => (
                                <option key={col} value={col}>{col}</option>
                            ))}
                        </select>
                        
                        <select 
                            value={rule.operator} 
                            onChange={(e) => handleRuleChange(index, "operator", e.target.value)} 
                            className="w-20 p-2 border border-gray-200 rounded-md bg-white font-mono text-center text-sm focus:outline-none"
                        >
                            <option value="=">=</option>
                            <option value="!=">!=</option>
                            <option value=">">&gt;</option>
                            <option value="<">&lt;</option>
                            <option value=">=">&gt;=</option>
                            <option value="<=">&lt;=</option>
                        </select>
                        
                        <input 
                            type="text" 
                            required 
                            value={rule.value} 
                            onChange={(e) => handleRuleChange(index, "value", e.target.value)} 
                            className="flex-1 p-2 border border-gray-200 rounded-md text-sm focus:outline-none" 
                            placeholder="Valeur" 
                        />
                        
                        {rules.length > 1 && (
                            <button 
                                type="button" 
                                onClick={() => handleRemoveRule(index)}
                                className="text-gray-400 hover:text-red-600 transition p-1 rounded-md hover:bg-red-50 inline-flex items-center justify-center"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                            </button>
                        )}
                    </div>
                ))}
            </div>

            <button type="submit" className={`w-full py-3 px-4 font-medium rounded-lg shadow-sm text-white transition ${editingId ? 'bg-yellow-600 hover:bg-yellow-700' : 'bg-blue-600 hover:bg-blue-700'}`}>
                {editingId ? "Mettre à jour le Segment" : "Enregistrer le Segment"}
            </button>
          </form>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th className="p-4 font-semibold">Nom</th>
                <th className="p-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-sm">
              {segments.length === 0 ? (
                <tr><td colSpan="2" className="p-4 text-center text-gray-500">Aucun segment créé pour le moment.</td></tr>
              ) : (
                segments.map(seg => (
                  <tr key={seg.id} className="hover:bg-gray-50 transition">
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-gray-900">{seg.name}</span>
                        {seg.byAi === 'ai' ? (
                          <span className="bg-purple-100 text-purple-700 text-[10px] px-2 py-0.5 rounded-full font-bold">✨ IA</span>
                        ) : (
                          <span className="bg-gray-100 text-gray-600 text-[10px] px-2 py-0.5 rounded-full font-bold">👤 Manuel</span>
                        )}
                      </div>
                      
                      {seg.description && <div className="text-xs text-gray-500 mt-1">{seg.description}</div>} 
                      
                      <div className="text-xs text-gray-400 font-mono mt-2 space-y-1 bg-gray-50 p-2 rounded inline-block">
                        {Array.isArray(JSON.parse(seg.rules)) 
                            ? JSON.parse(seg.rules).map((r, i) => (
                                <div key={i}>ET {r.column} {r.operator} {r.value}</div>
                              ))
                            : <span>Format hérité (une seule règle)</span>
                        }
                      </div>
                    </td>
                    
                    <td className="p-4 text-right align-top">
                      <div className="flex justify-end gap-2">
                          <button onClick={() => handleRunRule(seg)} className="text-green-600 font-bold hover:underline text-xs bg-green-50 px-2 py-1 rounded">Exécuter</button>
                          <button onClick={() => handleEdit(seg)} className="text-blue-600 font-bold hover:underline text-xs bg-blue-50 px-2 py-1 rounded">Modifier</button>
                          <button onClick={() => handleDelete(seg.id)} className="text-red-500 hover:underline text-xs bg-red-50 px-2 py-1 rounded">Supprimer</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Right Column */}
      <div className="flex flex-col">
        <h2 className="text-xl font-bold text-gray-900 mb-6 px-2">Résultats du Moteur</h2>
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex-1">
          {!evaluatedSubscribers ? (
            <div className="h-full flex flex-col items-center justify-center text-gray-400 mt-20">
              <svg className="w-12 h-12 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
              <p>Cliquez sur "Exécuter" pour interroger la base de données.</p>
            </div>
          ) : (
             <div>
              <h3 className="text-lg font-bold text-green-700 mb-1">
                Résultats pour : {viewingSegmentName}
              </h3>
              <p className="text-sm text-gray-500 mb-4">Trouvé {evaluatedSubscribers.length} abonnés correspondant aux critères.</p>
              
              <div className="max-h-[600px] overflow-y-auto rounded-xl border border-gray-100">
                <table className="w-full text-left border-collapse">
                  <thead className="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider sticky top-0">
                    <tr>
                      <th className="p-4 font-semibold">Email</th>
                      <th className="p-4 font-semibold">Données Personnalisées</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 text-sm">
                    {evaluatedSubscribers.length === 0 ? (
                      <tr><td colSpan="2" className="p-4 text-center text-gray-500">Aucun abonné correspondant.</td></tr>
                    ) : (
                      evaluatedSubscribers.map(sub => (
                        <tr key={sub.id} className="hover:bg-gray-50 transition">
                          <td className="p-4 font-bold text-gray-900">{sub.email}</td>
                          <td className="p-4 text-xs text-gray-600 font-mono">
                            {JSON.stringify(sub.customAttributes)}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>

      </div>
    </div>
  );
}

export default function SegmentsPage() {
  return (
    <SessionProvider>
      <SegmentsContent />
    </SessionProvider>
  );
}