"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

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

  // Scheduling State
  const [launchType, setLaunchType] = useState("now"); // "now" or "later"
  const [scheduledAt, setScheduledAt] = useState("");
  const [scheduleDate, setScheduleDate] = useState("");
  const [scheduleTime, setScheduleTime] = useState("");

  // Merge split date+time into ISO-8601 string for the backend
  useEffect(() => {
    if (scheduleDate && scheduleTime) {
      setScheduledAt(`${scheduleDate}T${scheduleTime}`);
    } else {
      setScheduledAt("");
    }
  }, [scheduleDate, scheduleTime]);

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
      const res = await fetch(`http://localhost:8080/api/${endpoint}`, { headers: { "X-User-Email": session.user.email } });
      if (res.ok) setter(await res.json());
    } catch (error) { console.error(`Failed to fetch ${endpoint}`); }
  };

  const resetWizard = () => {
    setStep(1); setName(""); setSubject(""); setSegmentId(""); setTemplateId("");
    setScheduledAt(""); setScheduleDate(""); setScheduleTime(""); setLaunchType("now"); setMessage("");
  };

  const handleDelete = async (id) => {
    if (window.confirm("Êtes-vous sûr de vouloir supprimer cette campagne définitivement ?")) {
      try {
        const res = await fetch(`http://localhost:8080/api/campaigns/${id}`, {
          method: "DELETE",
          headers: { "X-User-Email": session?.user?.email }
        });
        if (res.ok) {
          setCampaigns(prevList => prevList.filter(camp => camp.id !== id));
        } else {
          const errorData = await res.json();
          console.error("Backend error:", errorData.message);
          alert(`Erreur: ${errorData.message}`);
        }
      } catch (error) { console.error("API error:", error); }
    }
  };

  const handleLaunch = async () => {
    if (!segmentId || !templateId || !name || !subject) return setMessage("❌ Champs requis manquants.");
    if (launchType === "later" && !scheduledAt) return setMessage("❌ Veuillez sélectionner une date et une heure.");

    setIsLaunching(true);
    setMessage(launchType === "later" ? "🗓️ Planification de la campagne..." : "🚀 Lancement via Resend...");

    try {
      const res = await fetch("http://localhost:8080/api/campaigns/launch", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
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
    } catch (error) { setMessage("❌ Erreur Réseau."); }
    finally { setIsLaunching(false); }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Accès Refusé. Veuillez vous connecter.</p>;

  const selectedSegment = segments.find(s => s.id === segmentId);

  const STEPS = ["Configuration", "Audience", "Design", "Planification"];

  return (
    <div className="max-w-7xl mx-auto pb-16">

      {/* ── Page Header ── */}
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">Campagnes</h1>
          <p className="text-gray-500 mt-1">Créez, planifiez et analysez vos envois d&apos;emails.</p>
        </div>
        <span className="text-xs bg-blue-50 text-blue-700 border border-blue-100 px-3 py-1.5 rounded-full font-bold">
          {campaigns.length} campagne{campaigns.length !== 1 ? "s" : ""}
        </span>
      </div>

      {/* ── Stepper ── */}
      <div className="mb-8">
        <div className="flex items-center bg-white p-4 rounded-2xl shadow-sm border border-gray-100">
          {STEPS.map((label, index) => {
            const stepNumber = index + 1;
            const isActive = step >= stepNumber;
            const isCompleted = step > stepNumber;
            return (
              <div key={label} className="flex items-center flex-1">
                <div className="flex flex-col items-center flex-1">
                  <div className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-sm mb-1.5 transition-all duration-200 ${isActive ? "bg-blue-600 text-white shadow-md shadow-blue-200" : "bg-gray-100 text-gray-400"}`}>
                    {isCompleted
                      ? <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" /></svg>
                      : stepNumber}
                  </div>
                  <span className={`text-[11px] font-bold uppercase tracking-wider ${isActive ? "text-blue-700" : "text-gray-400"}`}>{label}</span>
                </div>
                {stepNumber !== 4 && (
                  <div className={`h-px w-8 mx-1 flex-shrink-0 transition-colors duration-300 ${step > stepNumber ? "bg-blue-400" : "bg-gray-200"}`} />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* ── Wizard (centred, single column) ── */}
      <div className="max-w-3xl mx-auto mb-12">
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 min-h-[460px] flex flex-col overflow-hidden">
          <div className="p-8 flex-grow">

            {/* STEP 1 — Configuration */}
            {step === 1 && (
              <div className="space-y-6">
                <h2 className="text-xl font-bold text-gray-900">Détails de la Campagne</h2>
                <div>
                  <label className="block mb-2 text-sm font-semibold text-gray-700">Nom de la Campagne</label>
                  <input type="text" value={name} onChange={(e) => setName(e.target.value)}
                    className="bg-gray-50 border border-gray-200 text-gray-900 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 block w-full p-3 transition"
                    placeholder="Ex: Promotion Été 2026" />
                </div>
                <div>
                  <label className="block mb-2 text-sm font-semibold text-gray-700">
                    Ligne d&apos;Objet <span className="font-normal text-gray-400">(Sujet de l&apos;email)</span>
                  </label>
                  <input type="text" value={subject} onChange={(e) => setSubject(e.target.value)}
                    className="bg-gray-50 border border-gray-200 text-gray-900 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 block w-full p-3 transition"
                    placeholder="Ex: Découvrez nos nouvelles offres !" />
                </div>
              </div>
            )}

            {/* STEP 2 — Audience */}
            {step === 2 && (
              <div className="space-y-6">
                <div className="flex justify-between items-center">
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
                      <div key={seg.id} onClick={() => setSegmentId(seg.id)}
                        className={`p-5 border rounded-xl cursor-pointer transition-all ${segmentId === seg.id ? "border-blue-500 bg-blue-50 ring-2 ring-blue-100 shadow-sm" : "border-gray-200 hover:border-gray-300 hover:bg-gray-50"}`}>
                        <h3 className="font-bold text-gray-900 text-lg">{seg.name}</h3>
                        <div className="mt-3 inline-block bg-white text-gray-700 text-xs px-3 py-1.5 rounded-full font-bold border border-gray-200 shadow-sm">👤 {count} Abonnés</div>
                      </div>
                    );
                  })}
                </div>
                {selectedSegment && (
                  <div className="border-t border-gray-100 pt-6">
                    <h4 className="text-sm font-bold text-gray-700 mb-3">Aperçu des destinataires pour : {selectedSegment.name}</h4>
                    <div className="bg-gray-50 rounded-xl border border-gray-200 max-h-[150px] overflow-y-auto">
                      {(selectedSegment.subscribers || []).map((sub, i) => (
                        <div key={i} className="text-xs p-3 border-b border-gray-100 last:border-0 text-gray-600 font-mono">{sub.email}</div>
                      ))}
                      {(!selectedSegment.subscribers || selectedSegment.subscribers.length === 0) && (
                        <div className="text-xs p-4 text-red-600 bg-red-50 font-bold rounded-xl">⚠️ Ce segment n&apos;a aucun abonné. Les emails ne seront pas envoyés.</div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* STEP 3 — Design */}
            {step === 3 && (
              <div className="space-y-6">
                <div className="flex justify-between items-center">
                  <h2 className="text-xl font-bold text-gray-900">Design de l&apos;Email</h2>
                  <div className="flex items-center gap-4">
                    <button onClick={() => fetchData("templates", setTemplates)} className="text-sm font-bold text-gray-500 hover:text-gray-800 transition-colors">🔄 Actualiser</button>
                    <a href="/templates" target="_blank" rel="noopener noreferrer" className="text-sm font-bold text-blue-600 hover:text-blue-800 transition-colors">+ Studio IA ↗</a>
                  </div>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-5 max-h-[380px] overflow-y-auto pr-2">
                  {templates.map(tmpl => (
                    <div key={tmpl.id} onClick={() => setTemplateId(tmpl.id)}
                      className={`group relative border rounded-xl overflow-hidden cursor-pointer transition-all h-48 ${templateId === tmpl.id ? "border-blue-500 ring-2 ring-blue-100 shadow-sm" : "border-gray-200 hover:border-gray-300 hover:shadow-sm"}`}>
                      <div className="w-full h-[140px] bg-white overflow-hidden relative pointer-events-none">
                        <iframe srcDoc={tmpl.htmlContent} className="w-[400%] h-[400%] origin-top-left scale-[0.25] absolute top-0 left-0 border-0" tabIndex="-1" />
                      </div>
                      <div className={`p-3 border-t border-gray-100 text-xs font-bold text-center truncate transition-colors ${templateId === tmpl.id ? "bg-blue-50 text-blue-800" : "bg-gray-50 text-gray-700 group-hover:bg-gray-100"}`}>
                        {tmpl.name}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* STEP 4 — Schedule */}
            {step === 4 && (
              <div className="space-y-6 pb-24">
                <h2 className="text-xl font-bold text-gray-900">Planification de l&apos;Envoi</h2>

                <div className="grid grid-cols-2 gap-4">
                  {/* Card: Immediately */}
                  <label className={`flex items-start gap-4 p-5 border-2 rounded-xl cursor-pointer transition-all ${launchType === "now" ? "border-blue-500 bg-blue-50 ring-1 ring-blue-300 shadow-sm" : "border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50"}`}>
                    <input type="radio" name="launchType" value="now" checked={launchType === "now"} onChange={() => setLaunchType("now")} className="hidden" />
                    <div className={`mt-0.5 flex-shrink-0 h-5 w-5 rounded-full border-2 flex items-center justify-center transition-colors ${launchType === "now" ? "border-blue-500 bg-blue-500" : "border-gray-300"}`}>
                      {launchType === "now" && <div className="h-2 w-2 rounded-full bg-white" />}
                    </div>
                    <div>
                      <p className="font-semibold text-gray-900 text-sm">Envoyer Immédiatement</p>
                      <p className="text-xs text-gray-500 mt-1">L&apos;envoi démarre dès confirmation.</p>
                    </div>
                  </label>

                  {/* Card: Schedule later */}
                  <label className={`flex items-start gap-4 p-5 border-2 rounded-xl cursor-pointer transition-all ${launchType === "later" ? "border-blue-500 bg-blue-50 ring-1 ring-blue-300 shadow-sm" : "border-gray-200 bg-white hover:border-gray-300 hover:bg-gray-50"}`}>
                    <input type="radio" name="launchType" value="later" checked={launchType === "later"} onChange={() => setLaunchType("later")} className="hidden" />
                    <div className={`mt-0.5 flex-shrink-0 h-5 w-5 rounded-full border-2 flex items-center justify-center transition-colors ${launchType === "later" ? "border-blue-500 bg-blue-500" : "border-gray-300"}`}>
                      {launchType === "later" && <div className="h-2 w-2 rounded-full bg-white" />}
                    </div>
                    <div>
                      <p className="font-semibold text-gray-900 text-sm">Planifier pour plus tard</p>
                      <p className="text-xs text-gray-500 mt-1">Choisissez une date et heure d&apos;envoi.</p>
                    </div>
                  </label>
                </div>

                {launchType === "later" && (
                  <div className="mt-4 p-5 bg-slate-50 border border-slate-200 rounded-xl">
                    <label className="block text-sm font-semibold text-slate-700 mb-3">Sélectionner la Date et l&apos;Heure</label>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Date Picker */}
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                          <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                          </svg>
                        </div>
                        <input
                          type="date"
                          value={scheduleDate}
                          onChange={(e) => setScheduleDate(e.target.value)}
                          className="block w-full pl-10 pr-3 py-2.5 bg-white border border-slate-200 rounded-lg text-slate-900 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow outline-none shadow-sm"
                        />
                      </div>

                      {/* Time Picker */}
                      <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                          <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                        </div>
                        <input
                          type="time"
                          value={scheduleTime}
                          onChange={(e) => setScheduleTime(e.target.value)}
                          className="block w-full pl-10 pr-3 py-2.5 bg-white border border-slate-200 rounded-lg text-slate-900 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow outline-none shadow-sm"
                        />
                      </div>
                    </div>

                    <p className="mt-3 text-xs text-slate-500">
                      Heure locale du navigateur. La campagne sera envoyée automatiquement à l&apos;heure sélectionnée.
                    </p>
                  </div>
                )}

                {message && (
                  <div className={`text-sm font-bold text-center p-4 rounded-xl border ${message.includes("❌") ? "bg-red-50 text-red-700 border-red-200" : "bg-blue-50 text-blue-800 border-blue-200"}`}>
                    {message}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Wizard Footer Navigation */}
          <div className="p-6 bg-gray-50 border-t border-gray-100 flex justify-between items-center">
            {step > 1
              ? <button onClick={() => setStep(step - 1)} className="px-5 py-2.5 text-sm font-bold text-gray-600 hover:bg-gray-200 hover:text-gray-900 rounded-lg transition-colors">← Précédent</button>
              : <div />}
            {step < 4 ? (
              <button onClick={() => setStep(step + 1)}
                disabled={(step === 1 && (!name || !subject)) || (step === 2 && !segmentId) || (step === 3 && !templateId)}
                className="px-8 py-2.5 bg-gray-900 hover:bg-black text-white text-sm font-bold rounded-lg shadow-sm disabled:opacity-40 transition-colors">
                Continuer →
              </button>
            ) : (
              <button onClick={handleLaunch} disabled={isLaunching}
                className="px-8 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-lg uppercase tracking-wider shadow-md hover:shadow-lg disabled:opacity-50 transition-all">
                {isLaunching ? "Initialisation..." : launchType === "now" ? "Lancer Maintenant 🚀" : "Confirmer la Planification 🗓️"}
              </button>
            )}
          </div>
        </div>
      </div>

      {/* ── Full-width Campaigns Data Table ── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="font-bold text-gray-900 text-lg">Campagnes Récentes</h2>
          <span className="text-xs bg-blue-50 text-blue-700 border border-blue-100 px-2.5 py-1 rounded-full font-bold">{campaigns.length}</span>
        </div>

        {campaigns.length === 0 ? (
          <div className="p-12 text-center text-gray-400 text-sm">
            <p className="text-4xl mb-3">📭</p>
            <p className="font-medium">Aucune campagne lancée pour le moment.</p>
          </div>
        ) : (
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider border-b border-gray-100">
                <th className="px-6 py-3 font-semibold">Nom</th>
                <th className="px-6 py-3 font-semibold">Sujet</th>
                <th className="px-6 py-3 font-semibold">Statut</th>
                <th className="px-6 py-3 font-semibold">Date</th>
                <th className="px-6 py-3 font-semibold text-center">Envoyés</th>
                <th className="px-6 py-3 font-semibold text-center">Ouvertures</th>
                <th className="px-6 py-3 font-semibold text-center">Clics</th>
                <th className="px-6 py-3 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-sm">
              {campaigns.map(camp => (
                <CampaignTableRow key={camp.id} camp={camp} userEmail={session.user.email} onDelete={handleDelete} />
              ))}
            </tbody>
          </table>
        )}
      </div>

    </div>
  );
}

/* ── Row sub-component: fetches its own stats independently ── */
function CampaignTableRow({ camp, userEmail, onDelete }) {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    fetch(`http://localhost:8080/api/campaigns/${camp.id}/stats`, { headers: { "X-User-Email": userEmail } })
      .then(r => r.ok ? r.json() : null)
      .then(d => d && setStats(d))
      .catch(() => {});
  }, [camp.id, userEmail]);

  const statusStyles = {
    SENT:      "bg-green-100 text-green-700",
    SCHEDULED: "bg-yellow-100 text-yellow-700",
    SENDING:   "bg-blue-100 text-blue-700",
    FAILED:    "bg-red-100 text-red-700",
    DRAFT:     "bg-gray-100 text-gray-600",
  };

  const Dash = () => <span className="text-gray-300 animate-pulse">—</span>;

  return (
    <tr className="hover:bg-gray-50 transition group">
      <td className="px-6 py-4 font-semibold text-gray-900 max-w-[180px] truncate">{camp.name}</td>
      <td className="px-6 py-4 text-gray-500 max-w-[200px] truncate">{camp.subject}</td>
      <td className="px-6 py-4">
        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wide ${statusStyles[camp.status] || "bg-gray-100 text-gray-600"}`}>
          {camp.status}
        </span>
      </td>
      <td className="px-6 py-4 text-gray-500 text-xs whitespace-nowrap">
        {camp.status === "SCHEDULED"
          ? new Date(camp.scheduledAt).toLocaleString("fr-FR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })
          : new Date(camp.createdAt).toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" })}
      </td>
      <td className="px-6 py-4 text-center font-bold text-gray-700">{stats ? stats.totalSent : <Dash />}</td>
      <td className="px-6 py-4 text-center font-bold text-blue-600">{stats ? stats.uniqueOpens : <Dash />}</td>
      <td className="px-6 py-4 text-center font-bold text-green-600">{stats ? stats.uniqueClicks : <Dash />}</td>
      <td className="px-6 py-4 text-right">
        <button onClick={() => onDelete(camp.id)}
          className="text-gray-400 hover:text-red-600 transition duration-150 cursor-pointer p-1.5 rounded-lg hover:bg-red-50"
          title="Supprimer">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </td>
    </tr>
  );
}

export default function CampaignsPage() {
  return <SessionProvider><CampaignsContent /></SessionProvider>;
}