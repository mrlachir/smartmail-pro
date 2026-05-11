"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

const CampaignStats = ({ campaignId, userEmail }) => {
    const [stats, setStats] = useState(null);

    useEffect(() => {
        if (!campaignId || !userEmail) return;
        
        fetch(`http://localhost:8080/api/campaigns/${campaignId}/stats`, {
            headers: { "X-User-Email": userEmail }
        })
        .then(res => res.json())
        .then(data => setStats(data))
        .catch(err => console.error("Failed to load stats", err));
    }, [campaignId, userEmail]);

    if (!stats) return <div className="text-xs text-gray-400 mt-2 animate-pulse">Chargement de la télémétrie...</div>;

    return (
        <div className="mt-4 grid grid-cols-3 gap-2 text-center text-xs border-t pt-3 border-gray-100">
            <div><span className="block text-lg font-bold text-gray-800">{stats.totalSent}</span><span className="text-gray-500 uppercase text-[10px] tracking-wider">Envoyés</span></div>
            <div><span className="block text-lg font-bold text-blue-600">{stats.uniqueOpens}</span><span className="text-gray-500 uppercase text-[10px] tracking-wider">Ouvertures</span></div>
            <div><span className="block text-lg font-bold text-green-600">{stats.uniqueClicks}</span><span className="text-gray-500 uppercase text-[10px] tracking-wider">Clics</span></div>
        </div>
    );
};

function CampaignsContent() {
  const { data: session, status } = useSession();
  
  const [step, setStep] = useState(1);
  const [campaigns, setCampaigns] = useState([]);
  const [segments, setSegments] = useState([]);
  const [templates, setTemplates] = useState([]);
  
  const [name, setName] = useState("");
  const [subject, setSubject] = useState("");
  const [segmentId, setSegmentId] = useState("");
  const [templateId, setTemplateId] = useState("");
  
  // New Scheduling State
  const [launchType, setLaunchType] = useState("now"); // "now" or "later"
  const [scheduledAt, setScheduledAt] = useState("");

  const [message, setMessage] = useState("");
  const [isLaunching, setIsLaunching] = useState(false);

  useEffect(() => {
    if (session?.user?.email) {
      fetchData("campaigns", setCampaigns);
      fetchData("segments", setSegments);
      fetchData("templates", setTemplates);
    }
  }, [session]);

  const fetchData = async (endpoint, setter) => {
    try {
      const res = await fetch(`http://localhost:8080/api/${endpoint}`, { headers: { "X-User-Email": session.user.email }});
      if (res.ok) setter(await res.json());
    } catch (error) { console.error(`Failed to fetch ${endpoint}`); }
  };

  const resetWizard = () => {
      setStep(1); setName(""); setSubject(""); setSegmentId(""); setTemplateId(""); setScheduledAt(""); setLaunchType("now"); setMessage("");
  };

  const handleDelete = async (id) => {
    if (window.confirm("Êtes-vous sûr de vouloir supprimer cette campagne définitivement ?")) {
      try {
        const res = await fetch(`http://localhost:8080/api/campaigns/${id}`, {
          method: 'DELETE',
          headers: {
            'X-User-Email': session?.user?.email
          }
        });

        if (res.ok) {
          setCampaigns(prevList => prevList.filter(camp => camp.id !== id));
        } else {
          // READ THE ACTUAL ERROR FROM SPRING BOOT
          const errorData = await res.json();
          console.error("Backend error:", errorData.message);
          alert(`Erreur: ${errorData.message}`);
        }
      } catch (error) {
        console.error("API error:", error);
      }
    }
  };

  const handleLaunch = async () => {
    if (!segmentId || !templateId || !name || !subject) return setMessage("❌ Champs requis manquants.");
    if (launchType === "later" && !scheduledAt) return setMessage("❌ Veuillez sélectionner une date et une heure.");

    setIsLaunching(true); 
    setMessage(launchType === "later" ? "🗓️ Planification de la campagne..." : "🚀 Lancement via Resend...");

    try {
      const res = await fetch("http://localhost:8080/api/campaigns/launch", {
        method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ name, subject, segmentId, templateId, scheduledAt: launchType === "later" ? scheduledAt : null }),
      });
      
      if (res.ok) {
        const data = await res.json();
        setMessage(`✅ ${data.message}`);
        fetchData("campaigns", setCampaigns);
        setTimeout(() => { resetWizard(); }, 4000);
      } else {
        const text = await res.text();
        try { const err = JSON.parse(text); setMessage(`❌ Erreur: ${err.message}`); } 
        catch (e) { setMessage(`❌ Erreur Serveur ${res.status}`); }
      }
    } catch (error) { setMessage(`❌ Erreur Réseau.`); } 
    finally { setIsLaunching(false); }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Accès Refusé. Veuillez vous connecter.</p>;

  // Helper to find selected segment details
  const selectedSegment = segments.find(s => s.id === segmentId);

  return (
    <div className="max-w-7xl mx-auto pb-12">
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">Campagnes</h1>
          <p className="text-gray-500 mt-1">Créez, planifiez et analysez vos envois d'emails.</p>
        </div>
      </div>

      <div className="mb-8">
        <div className="flex items-center justify-between bg-white p-4 rounded-2xl shadow-sm border border-gray-100">
            {['Configuration', 'Audience', 'Design', 'Planification'].map((label, index) => {
                const stepNumber = index + 1;
                return (
                    <div key={label} className={`flex flex-col items-center flex-1 ${stepNumber !== 4 ? 'border-r border-gray-100' : ''}`}>
                        <div className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-sm mb-2 transition-colors ${step >= stepNumber ? 'bg-blue-600 text-white shadow-sm' : 'bg-gray-100 text-gray-500'}`}>
                            {stepNumber}
                        </div>
                        <span className={`text-xs font-bold uppercase tracking-wider ${step >= stepNumber ? 'text-blue-800' : 'text-gray-400'}`}>{label}</span>
                    </div>
                );
            })}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Wizard Column */}
        <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-gray-100 min-h-[500px] flex flex-col overflow-hidden">
            <div className="p-8 border-b border-gray-100 flex-grow">
                
                {/* STEP 1: SETUP */}
                {step === 1 && (
                    <div className="space-y-6 animate-fadeIn">
                        <h2 className="text-xl font-bold text-gray-900">Détails de la Campagne</h2>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">Nom de la Campagne</label>
                            <input type="text" value={name} onChange={(e) => setName(e.target.value)} className="w-full p-3 border border-gray-200 rounded-lg bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition" placeholder="Ex: Promotion Été 2026" />
                        </div>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-2">Ligne d'Objet (Sujet de l'email)</label>
                            <input type="text" value={subject} onChange={(e) => setSubject(e.target.value)} className="w-full p-3 border border-gray-200 rounded-lg bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition" placeholder="Ex: Découvrez nos nouvelles offres !" />
                        </div>
                    </div>
                )}

                {/* STEP 2: AUDIENCE (LIST VIEW) */}
                {step === 2 && (
                    <div className="space-y-6 animate-fadeIn">
                        <div className="flex justify-between items-center mb-2">
                            <h2 className="text-xl font-bold text-gray-900">Audience Cible</h2>
                            <div className="flex items-center gap-4">
                                <button onClick={() => fetchData("segments", setSegments)} className="text-sm font-bold text-gray-500 hover:text-gray-800 transition-colors">🔄 Actualiser</button>
                                <a href="/segments" target="_blank" rel="noopener noreferrer" className="text-sm font-bold text-blue-600 hover:text-blue-800 transition-colors">+ Créer un Segment ↗</a>
                            </div>
                        </div>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {segments.map(seg => {
                                const count = seg.subscribers ? seg.subscribers.length : 0;
                                return (
                                <div key={seg.id} onClick={() => setSegmentId(seg.id)} className={`p-5 border rounded-xl cursor-pointer transition-all ${segmentId === seg.id ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-100 shadow-sm' : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'}`}>
                                    <h3 className="font-bold text-gray-900 text-lg">{seg.name}</h3>
                                    <div className="mt-3 inline-block bg-white text-gray-700 text-xs px-3 py-1.5 rounded-full font-bold border border-gray-200 shadow-sm">
                                        👤 {count} Abonnés
                                    </div>
                                </div>
                            )})}
                        </div>

                        {/* Expandable Subscriber List */}
                        {selectedSegment && (
                            <div className="mt-8 border-t border-gray-100 pt-6">
                                <h4 className="text-sm font-bold text-gray-700 mb-3">Aperçu des destinataires pour : {selectedSegment.name}</h4>
                                <div className="bg-gray-50 rounded-xl border border-gray-200 max-h-[150px] overflow-y-auto">
                                    {(selectedSegment.subscribers || []).map((sub, i) => (
                                        <div key={i} className="text-xs p-3 border-b border-gray-100 last:border-0 text-gray-600 font-mono">{sub.email}</div>
                                    ))}
                                    {(!selectedSegment.subscribers || selectedSegment.subscribers.length === 0) && (
                                        <div className="text-xs p-4 text-red-600 bg-red-50 font-bold rounded-xl">⚠️ Ce segment n'a aucun abonné. Les emails ne seront pas envoyés.</div>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* STEP 3: DESIGN (THUMBNAIL GRID) */}
                {step === 3 && (
                    <div className="space-y-6 animate-fadeIn">
                        <div className="flex justify-between items-center mb-2">
                            <h2 className="text-xl font-bold text-gray-900">Design de l'Email</h2>
                            <div className="flex items-center gap-4">
                                <button onClick={() => fetchData("templates", setTemplates)} className="text-sm font-bold text-gray-500 hover:text-gray-800 transition-colors">🔄 Actualiser</button>
                                <a href="/templates" target="_blank" rel="noopener noreferrer" className="text-sm font-bold text-blue-600 hover:text-blue-800 transition-colors">+ Studio IA ↗</a>
                            </div>
                        </div>
                        
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-5 h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                            {templates.map(tmpl => (
                                <div key={tmpl.id} onClick={() => setTemplateId(tmpl.id)} className={`group relative border rounded-xl overflow-hidden cursor-pointer transition-all h-48 ${templateId === tmpl.id ? 'border-blue-500 ring-2 ring-blue-100 shadow-sm' : 'border-gray-200 hover:border-gray-300 hover:shadow-sm'}`}>
                                    {/* Thumbnail Iframe Hack */}
                                    <div className="w-full h-[140px] bg-white overflow-hidden relative pointer-events-none">
                                        <iframe srcDoc={tmpl.htmlContent} className="w-[400%] h-[400%] origin-top-left scale-[0.25] absolute top-0 left-0 border-0" tabIndex="-1" />
                                    </div>
                                    <div className={`p-3 border-t border-gray-100 text-xs font-bold text-center truncate transition-colors ${templateId === tmpl.id ? 'bg-blue-50 text-blue-800' : 'bg-gray-50 text-gray-700 group-hover:bg-gray-100'}`}>
                                        {tmpl.name}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* STEP 4: SCHEDULE & REVIEW */}
                {step === 4 && (
                    <div className="space-y-6 animate-fadeIn">
                        <h2 className="text-xl font-bold text-gray-900">Planification de l'Envoi</h2>
                        
                        <div className="flex gap-4">
                            <label className={`flex-1 border p-5 rounded-xl cursor-pointer transition-all ${launchType === 'now' ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-100 shadow-sm' : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'}`}>
                                <input type="radio" name="launchType" value="now" checked={launchType === 'now'} onChange={() => setLaunchType('now')} className="mr-3" />
                                <span className="font-bold text-gray-900">Envoyer Immédiatement</span>
                            </label>
                            <label className={`flex-1 border p-5 rounded-xl cursor-pointer transition-all ${launchType === 'later' ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-100 shadow-sm' : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'}`}>
                                <input type="radio" name="launchType" value="later" checked={launchType === 'later'} onChange={() => setLaunchType('later')} className="mr-3" />
                                <span className="font-bold text-gray-900">Planifier pour plus tard</span>
                            </label>
                        </div>

                        {launchType === 'later' && (
                            <div className="p-5 bg-gray-50 border border-gray-200 rounded-xl animate-fadeIn">
                                <label className="block text-sm font-bold text-gray-700 mb-2">Sélectionner la Date et l'Heure</label>
                                <input type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} className="w-full p-3 border border-gray-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition" />
                            </div>
                        )}

                        {message && (
                            <div className={`text-sm font-bold text-center p-4 rounded-xl border ${message.includes('❌') ? 'bg-red-50 text-red-700 border-red-200' : 'bg-blue-50 text-blue-800 border-blue-200'}`}>
                                {message}
                            </div>
                        )}
                    </div>
                )}
            </div>

            <div className="p-6 bg-gray-50 border-t border-gray-100 flex justify-between items-center">
                {step > 1 ? <button onClick={() => setStep(step - 1)} className="px-5 py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-200 hover:text-gray-900 rounded-lg transition-colors">← Précédent</button> : <div></div>}
                {step < 4 ? (
                    <button onClick={() => setStep(step + 1)} disabled={(step === 1 && (!name || !subject)) || (step === 2 && !segmentId) || (step === 3 && !templateId)} className="px-8 py-2.5 bg-gray-900 hover:bg-black text-white text-sm font-bold rounded-lg shadow-sm disabled:opacity-50 transition-colors">
                        Continuer →
                    </button>
                ) : (
                    <button onClick={handleLaunch} disabled={isLaunching} className="px-8 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-lg uppercase tracking-wider shadow-md hover:shadow-lg disabled:opacity-50 transition-all">
                        {isLaunching ? "Initialisation..." : launchType === 'now' ? "Lancer Maintenant 🚀" : "Confirmer la Planification 🗓️"}
                    </button>
                )}
            </div>
        </div>

        {/* History Column */}
        <div className="lg:col-span-1 bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden h-fit">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center">
                <h2 className="font-bold text-gray-900">Campagnes Récentes</h2>
                <span className="text-xs bg-blue-50 text-blue-700 border border-blue-100 px-2.5 py-1 rounded-full font-bold">{campaigns.length}</span>
            </div>
            <div className="max-h-[600px] overflow-y-auto custom-scrollbar divide-y divide-gray-100">
                {campaigns.length === 0 ? <p className="p-8 text-center text-gray-500 text-sm font-medium">Aucune campagne lancée.</p> : (
                    campaigns.map(camp => (
                        <div key={camp.id} className="p-5 hover:bg-gray-50 transition-colors group">
                            <div className="font-bold text-gray-900 text-sm">{camp.name}</div>
                            <div className="text-xs text-gray-500 mb-3 truncate">{camp.subject}</div>
                            <div className="flex justify-between items-center mb-2">
                                <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wider uppercase
                                    ${camp.status === 'SENT' ? 'bg-green-100 text-green-700' : 
                                      camp.status === 'SCHEDULED' ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'}`}>
                                    {camp.status}
                                </span>
                                <div className="flex items-center gap-3">
                                  <span className="text-gray-400 text-xs font-medium">
                                      {camp.status === 'SCHEDULED' ? new Date(camp.scheduledAt).toLocaleString() : new Date(camp.createdAt).toLocaleDateString()}
                                  </span>
                                  <button onClick={() => handleDelete(camp.id)} className="text-gray-300 hover:text-red-600 transition-colors p-1.5 rounded-lg hover:bg-red-50 opacity-0 group-hover:opacity-100 focus:opacity-100">
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                                  </button>
                                </div>
                            </div>
                            <CampaignStats campaignId={camp.id} userEmail={session.user.email} />
                        </div>
                    ))
                )}
            </div>
        </div>
      </div>
    </div>
  );
}

export default function CampaignsPage() {
  return <SessionProvider><CampaignsContent /></SessionProvider>;
}