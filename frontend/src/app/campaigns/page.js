"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function CampaignsContent() {
  const { data: session, status } = useSession();
  
  // --- STATE MACHINE ---
  const [step, setStep] = useState(1);
  const [campaigns, setCampaigns] = useState([]);
  const [segments, setSegments] = useState([]);
  const [templates, setTemplates] = useState([]);
  
  // --- CAMPAIGN DATA ---
  const [name, setName] = useState("");
  const [subject, setSubject] = useState("");
  const [segmentId, setSegmentId] = useState("");
  const [templateId, setTemplateId] = useState("");
  
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
      setStep(1); setName(""); setSubject(""); setSegmentId(""); setTemplateId(""); setMessage("");
  };
  const handleLaunch = async () => {
    if (!segmentId || !templateId || !name || !subject) { 
        setMessage("❌ Missing required fields."); return; 
    }
    setIsLaunching(true); setMessage("🚀 Igniting payload via Resend...");

    try {
      const res = await fetch("http://localhost:8080/api/campaigns/launch", {
        method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ name, subject, segmentId, templateId }),
      });
      
      if (res.ok) {
        const data = await res.json();
        setMessage(`✅ ${data.message}`);
        fetchData("campaigns", setCampaigns);
        setTimeout(() => { resetWizard(); }, 3000);
      } else {
        // RESILIENCY UPGRADE: Read as raw text first so HTML error pages don't crash the app
        const text = await res.text();
        try {
            const err = JSON.parse(text);
            setMessage(`❌ Error: ${err.message}`);
        } catch (e) {
            setMessage(`❌ Server Error ${res.status}: Endpoint blocked or crashed.`);
        }
      }
    } catch (error) { 
        setMessage(`❌ Network Error: Is the backend running?`); 
    } 
    finally { setIsLaunching(false); }
  };



  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Log in.</p>;

  return (
    <div className="max-w-5xl mx-auto p-4 md:p-8 mt-10">
      
      {/* Header & Progress Bar */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800 mb-4">Campaign Studio</h1>
        <div className="flex items-center justify-between bg-white p-4 rounded shadow border border-gray-100">
            {['Setup', 'Audience', 'Design', 'Review'].map((label, index) => {
                const stepNumber = index + 1;
                return (
                    <div key={label} className={`flex flex-col items-center flex-1 ${stepNumber !== 4 ? 'border-r border-gray-100' : ''}`}>
                        <div className={`h-8 w-8 rounded-full flex items-center justify-center font-bold text-sm mb-2 transition-colors ${step >= stepNumber ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-500'}`}>
                            {stepNumber}
                        </div>
                        <span className={`text-xs font-bold uppercase tracking-wider ${step >= stepNumber ? 'text-blue-800' : 'text-gray-400'}`}>{label}</span>
                    </div>
                );
            })}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        
        {/* Wizard Form Area */}
        <div className="md:col-span-2 bg-white rounded shadow border border-gray-100 min-h-[400px] flex flex-col">
            <div className="p-6 border-b border-gray-100 flex-grow">
                
                {/* STEP 1: SETUP */}
                {step === 1 && (
                    <div className="space-y-4 animate-fadeIn">
                        <h2 className="text-xl font-bold text-gray-800">Campaign Details</h2>
                        <p className="text-sm text-gray-500 mb-4">Give your campaign a name and a compelling subject line.</p>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">Campaign Name (Internal)</label>
                            <input type="text" value={name} onChange={(e) => setName(e.target.value)} className="w-full p-3 border rounded bg-gray-50 focus:bg-white" placeholder="e.g., Spring 2026 Promo" />
                        </div>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">Email Subject Line</label>
                            <input type="text" value={subject} onChange={(e) => setSubject(e.target.value)} className="w-full p-3 border rounded bg-gray-50 focus:bg-white" placeholder="You won't want to miss this..." />
                        </div>
                    </div>
                )}

                {/* STEP 2: AUDIENCE */}
                {step === 2 && (
                    <div className="space-y-4 animate-fadeIn">
                        <h2 className="text-xl font-bold text-gray-800">Target Audience</h2>
                        <p className="text-sm text-gray-500 mb-4">Who is receiving this campaign?</p>
                        
                        <div className="bg-purple-50 p-4 rounded border border-purple-100 mb-4 flex justify-between items-center">
                            <div>
                                <h3 className="font-bold text-purple-800 text-sm">✨ AI Segment Suggestion</h3>
                                <p className="text-xs text-purple-600">Let AI analyze your campaign name to suggest an audience.</p>
                            </div>
                            <button disabled className="bg-purple-600 text-white px-3 py-1.5 rounded text-xs font-bold opacity-50 cursor-not-allowed">Auto-Select (Soon)</button>
                        </div>

                        <label className="block text-sm font-bold text-gray-700 mb-1">Select Existing Segment</label>
                        <div className="grid grid-cols-1 gap-2">
                            {segments.map(seg => (
                                <div key={seg.id} onClick={() => setSegmentId(seg.id)} className={`p-4 border rounded cursor-pointer transition-all ${segmentId === seg.id ? 'border-blue-500 bg-blue-50 shadow-sm' : 'hover:bg-gray-50'}`}>
                                    <div className="font-bold text-gray-800">{seg.name}</div>
                                    <div className="text-xs text-gray-500 mt-1">{seg.description || "Pre-calculated subscriber list"}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* STEP 3: DESIGN */}
                {step === 3 && (
                    <div className="space-y-4 animate-fadeIn">
                        <h2 className="text-xl font-bold text-gray-800">Email Content</h2>
                        <p className="text-sm text-gray-500 mb-4">What are we sending them?</p>
                        
                        <div className="bg-purple-50 p-4 rounded border border-purple-100 mb-4 flex justify-between items-center">
                            <div>
                                <h3 className="font-bold text-purple-800 text-sm">✨ AI Template Generation</h3>
                                <p className="text-xs text-purple-600">Generate a custom template based on campaign name & segment.</p>
                            </div>
                            <button disabled className="bg-purple-600 text-white px-3 py-1.5 rounded text-xs font-bold opacity-50 cursor-not-allowed">Auto-Generate (Soon)</button>
                        </div>

                        <label className="block text-sm font-bold text-gray-700 mb-1">Select Existing Template</label>
                        <div className="grid grid-cols-1 gap-2 max-h-[250px] overflow-y-auto pr-2 custom-scrollbar">
                            {templates.map(tmpl => (
                                <div key={tmpl.id} onClick={() => setTemplateId(tmpl.id)} className={`p-4 border rounded cursor-pointer transition-all ${templateId === tmpl.id ? 'border-blue-500 bg-blue-50 shadow-sm' : 'hover:bg-gray-50'}`}>
                                    <div className="font-bold text-gray-800">{tmpl.name}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* STEP 4: REVIEW */}
                {step === 4 && (
                    <div className="space-y-4 animate-fadeIn">
                        <h2 className="text-xl font-bold text-gray-800">Review & Launch</h2>
                        <div className="bg-gray-50 p-6 rounded border space-y-3">
                            <div className="flex justify-between border-b pb-2">
                                <span className="text-gray-500 font-bold text-sm">Campaign Name</span>
                                <span className="text-gray-800 font-medium">{name}</span>
                            </div>
                            <div className="flex justify-between border-b pb-2">
                                <span className="text-gray-500 font-bold text-sm">Subject Line</span>
                                <span className="text-gray-800 font-medium">{subject}</span>
                            </div>
                            <div className="flex justify-between border-b pb-2">
                                <span className="text-gray-500 font-bold text-sm">Target Segment</span>
                                <span className="text-blue-600 font-bold">{segments.find(s => s.id === segmentId)?.name}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-500 font-bold text-sm">Template Design</span>
                                <span className="text-blue-600 font-bold">{templates.find(t => t.id === templateId)?.name}</span>
                            </div>
                        </div>
                        {message && <div className="text-sm font-bold text-center mt-4 p-3 rounded bg-blue-50 text-blue-800 border border-blue-200">{message}</div>}
                    </div>
                )}
            </div>

            {/* Navigation Footer */}
            <div className="p-4 bg-gray-50 border-t border-gray-200 flex justify-between items-center rounded-b">
                {step > 1 ? (
                    <button onClick={() => setStep(step - 1)} className="px-4 py-2 text-sm font-bold text-gray-600 hover:bg-gray-200 rounded transition-colors">← Back</button>
                ) : <div></div>}

                {step < 4 ? (
                    <button 
                        onClick={() => setStep(step + 1)} 
                        disabled={(step === 1 && (!name || !subject)) || (step === 2 && !segmentId) || (step === 3 && !templateId)}
                        className="px-6 py-2 bg-gray-800 hover:bg-black text-white text-sm font-bold rounded disabled:opacity-50 transition-colors"
                    >
                        Continue →
                    </button>
                ) : (
                    <button onClick={handleLaunch} disabled={isLaunching} className="px-8 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded uppercase tracking-wider shadow-lg disabled:opacity-50 transition-all transform hover:scale-105 active:scale-95">
                        {isLaunching ? "Initiating..." : "Launch Now 🚀"}
                    </button>
                )}
            </div>
        </div>

        {/* History Column */}
        <div className="md:col-span-1 bg-white rounded shadow border border-gray-100 overflow-hidden h-fit">
            <div className="p-4 bg-gray-50 border-b font-bold text-gray-700 flex justify-between items-center">
                <span>Recent Launches</span>
                <span className="text-xs bg-gray-200 text-gray-600 px-2 py-1 rounded-full">{campaigns.length}</span>
            </div>
            <div className="max-h-[500px] overflow-y-auto custom-scrollbar">
                {campaigns.length === 0 ? (
                    <p className="p-6 text-center text-gray-500 text-sm font-medium">No campaigns launched yet.</p>
                ) : (
                    campaigns.slice(0, 5).map(camp => (
                        <div key={camp.id} className="p-4 border-b border-gray-50 hover:bg-gray-50 transition-colors">
                            <div className="font-bold text-gray-800 text-sm">{camp.name}</div>
                            <div className="text-xs text-gray-500 mb-2 truncate">{camp.subject}</div>
                            <div className="flex justify-between items-center">
                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${camp.status === 'SENT' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                                    {camp.status}
                                </span>
                                <span className="text-gray-400 text-[10px]">
                                    {new Date(camp.createdAt).toLocaleDateString()}
                                </span>
                            </div>
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