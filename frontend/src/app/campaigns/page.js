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

    if (!stats) return <div className="text-xs text-gray-400 mt-2 animate-pulse">Loading telemetry...</div>;

    return (
        <div className="mt-4 grid grid-cols-3 gap-2 text-center text-xs border-t pt-3 border-gray-100">
            <div><span className="block text-lg font-bold text-gray-800">{stats.totalSent}</span><span className="text-gray-500 uppercase text-[10px] tracking-wider">Sent</span></div>
            <div><span className="block text-lg font-bold text-blue-600">{stats.uniqueOpens}</span><span className="text-gray-500 uppercase text-[10px] tracking-wider">Opens</span></div>
            <div><span className="block text-lg font-bold text-green-600">{stats.uniqueClicks}</span><span className="text-gray-500 uppercase text-[10px] tracking-wider">Clicks</span></div>
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

  const handleLaunch = async () => {
    if (!segmentId || !templateId || !name || !subject) return setMessage("❌ Missing required fields.");
    if (launchType === "later" && !scheduledAt) return setMessage("❌ Please select a date and time.");

    setIsLaunching(true); 
    setMessage(launchType === "later" ? "🗓️ Scheduling campaign..." : "🚀 Igniting payload via Resend...");

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
        try { const err = JSON.parse(text); setMessage(`❌ Error: ${err.message}`); } 
        catch (e) { setMessage(`❌ Server Error ${res.status}`); }
      }
    } catch (error) { setMessage(`❌ Network Error.`); } 
    finally { setIsLaunching(false); }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Log in.</p>;

  // Helper to find selected segment details
  const selectedSegment = segments.find(s => s.id === segmentId);

  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8 mt-10">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800 mb-4">Campaign Studio</h1>
        <div className="flex items-center justify-between bg-white p-4 rounded shadow border border-gray-100">
            {['Setup', 'Audience', 'Design', 'Schedule'].map((label, index) => {
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 bg-white rounded shadow border border-gray-100 min-h-[500px] flex flex-col">
            <div className="p-6 border-b border-gray-100 flex-grow">
                
                {/* STEP 1: SETUP */}
                {step === 1 && (
                    <div className="space-y-4 animate-fadeIn">
                        <h2 className="text-xl font-bold text-gray-800">Campaign Details</h2>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">Campaign Name</label>
                            <input type="text" value={name} onChange={(e) => setName(e.target.value)} className="w-full p-3 border rounded bg-gray-50 focus:bg-white" />
                        </div>
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">Subject Line</label>
                            <input type="text" value={subject} onChange={(e) => setSubject(e.target.value)} className="w-full p-3 border rounded bg-gray-50 focus:bg-white" />
                        </div>
                    </div>
                )}

                {/* STEP 2: AUDIENCE (LIST VIEW) */}
                {step === 2 && (
                    <div className="space-y-4 animate-fadeIn">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-xl font-bold text-gray-800">Target Audience</h2>
                            <div className="flex items-center gap-4">
                                <button onClick={() => fetchData("segments", setSegments)} className="text-sm font-bold text-gray-500 hover:text-gray-800 transition-colors">🔄 Refresh List</button>
                                <a href="/segments" target="_blank" rel="noopener noreferrer" className="text-sm font-bold text-blue-600 hover:text-blue-800">+ Create New Segment ↗</a>
                            </div>
                        </div>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {segments.map(seg => {
                                const count = seg.subscribers ? seg.subscribers.length : 0;
                                return (
                                <div key={seg.id} onClick={() => setSegmentId(seg.id)} className={`p-4 border rounded-lg cursor-pointer transition-all ${segmentId === seg.id ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200' : 'hover:bg-gray-50 border-gray-200'}`}>
                                    <h3 className="font-bold text-gray-800 text-lg">{seg.name}</h3>
                                    <div className="mt-2 inline-block bg-gray-200 text-gray-700 text-xs px-2 py-1 rounded-full font-bold">
                                        👤 {count} Subscribers
                                    </div>
                                </div>
                            )})}
                        </div>

                        {/* Expandable Subscriber List */}
                        {selectedSegment && (
                            <div className="mt-6 border-t pt-4">
                                <h4 className="text-sm font-bold text-gray-600 mb-2">Previewing recipients for: {selectedSegment.name}</h4>
                                <div className="bg-gray-50 rounded border max-h-[150px] overflow-y-auto">
                                    {(selectedSegment.subscribers || []).map((sub, i) => (
                                        <div key={i} className="text-xs p-2 border-b last:border-0 text-gray-700 font-mono">{sub.email}</div>
                                    ))}
                                    {(!selectedSegment.subscribers || selectedSegment.subscribers.length === 0) && (
                                        <div className="text-xs p-3 text-red-500 font-bold">⚠️ This segment has no subscribers. Emails will not send.</div>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* STEP 3: DESIGN (THUMBNAIL GRID) */}
                {step === 3 && (
                    <div className="space-y-4 animate-fadeIn">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-xl font-bold text-gray-800">Email Design</h2>
                            <div className="flex items-center gap-4">
                                <button onClick={() => fetchData("templates", setTemplates)} className="text-sm font-bold text-gray-500 hover:text-gray-800 transition-colors">🔄 Refresh List</button>
                                <a href="/templates" target="_blank" rel="noopener noreferrer" className="text-sm font-bold text-blue-600 hover:text-blue-800">+ Studio / AI Generator ↗</a>
                            </div>
                        </div>
                        
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                            {templates.map(tmpl => (
                                <div key={tmpl.id} onClick={() => setTemplateId(tmpl.id)} className={`group relative border rounded-lg overflow-hidden cursor-pointer transition-all h-48 ${templateId === tmpl.id ? 'border-blue-500 ring-2 ring-blue-200' : 'border-gray-200 hover:border-gray-400'}`}>
                                    {/* Thumbnail Iframe Hack */}
                                    <div className="w-full h-[140px] bg-white overflow-hidden relative pointer-events-none">
                                        <iframe srcDoc={tmpl.htmlContent} className="w-[400%] h-[400%] origin-top-left scale-[0.25] absolute top-0 left-0 border-0" tabIndex="-1" />
                                    </div>
                                    <div className={`p-2 border-t text-xs font-bold text-center truncate ${templateId === tmpl.id ? 'bg-blue-50 text-blue-800' : 'bg-gray-50 text-gray-700 group-hover:bg-gray-100'}`}>
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
                        <h2 className="text-xl font-bold text-gray-800">Delivery Schedule</h2>
                        
                        <div className="flex gap-4">
                            <label className={`flex-1 border p-4 rounded-lg cursor-pointer ${launchType === 'now' ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200' : 'hover:bg-gray-50'}`}>
                                <input type="radio" name="launchType" value="now" checked={launchType === 'now'} onChange={() => setLaunchType('now')} className="mr-2" />
                                <span className="font-bold text-gray-800">Blast Immediately</span>
                            </label>
                            <label className={`flex-1 border p-4 rounded-lg cursor-pointer ${launchType === 'later' ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200' : 'hover:bg-gray-50'}`}>
                                <input type="radio" name="launchType" value="later" checked={launchType === 'later'} onChange={() => setLaunchType('later')} className="mr-2" />
                                <span className="font-bold text-gray-800">Schedule for Later</span>
                            </label>
                        </div>

                        {launchType === 'later' && (
                            <div className="p-4 bg-gray-50 border rounded-lg animate-fadeIn">
                                <label className="block text-sm font-bold text-gray-700 mb-2">Select Release Date & Time</label>
                                <input type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} className="w-full p-3 border rounded bg-white" />
                            </div>
                        )}

                        {message && <div className="text-sm font-bold text-center p-3 rounded bg-blue-50 text-blue-800 border border-blue-200">{message}</div>}
                    </div>
                )}
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-200 flex justify-between items-center rounded-b">
                {step > 1 ? <button onClick={() => setStep(step - 1)} className="px-4 py-2 text-sm font-bold text-gray-600 hover:bg-gray-200 rounded">← Back</button> : <div></div>}
                {step < 4 ? (
                    <button onClick={() => setStep(step + 1)} disabled={(step === 1 && (!name || !subject)) || (step === 2 && !segmentId) || (step === 3 && !templateId)} className="px-6 py-2 bg-gray-800 hover:bg-black text-white text-sm font-bold rounded disabled:opacity-50">
                        Continue →
                    </button>
                ) : (
                    <button onClick={handleLaunch} disabled={isLaunching} className="px-8 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded uppercase tracking-wider shadow-lg disabled:opacity-50">
                        {isLaunching ? "Initiating..." : launchType === 'now' ? "Launch Now 🚀" : "Confirm Schedule 🗓️"}
                    </button>
                )}
            </div>
        </div>

        {/* History Column */}
        <div className="lg:col-span-1 bg-white rounded shadow border border-gray-100 overflow-hidden h-fit">
            <div className="p-4 bg-gray-50 border-b font-bold text-gray-700 flex justify-between items-center">
                <span>Recent Campaigns</span>
                <span className="text-xs bg-gray-200 text-gray-600 px-2 py-1 rounded-full">{campaigns.length}</span>
            </div>
            <div className="max-h-[600px] overflow-y-auto custom-scrollbar">
                {campaigns.length === 0 ? <p className="p-6 text-center text-gray-500 text-sm font-medium">No campaigns launched yet.</p> : (
                    campaigns.map(camp => (
                        <div key={camp.id} className="p-4 border-b border-gray-50 hover:bg-gray-50 transition-colors">
                            <div className="font-bold text-gray-800 text-sm">{camp.name}</div>
                            <div className="text-xs text-gray-500 mb-2 truncate">{camp.subject}</div>
                            <div className="flex justify-between items-center">
                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold 
                                    ${camp.status === 'SENT' ? 'bg-green-100 text-green-700' : 
                                      camp.status === 'SCHEDULED' ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'}`}>
                                    {camp.status}
                                </span>
                                <span className="text-gray-400 text-[10px]">
                                    {camp.status === 'SCHEDULED' ? new Date(camp.scheduledAt).toLocaleString() : new Date(camp.createdAt).toLocaleDateString()}
                                </span>
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