"use client";
import { useRouter } from "next/navigation";
import { useState, useEffect, useRef } from "react";
import { useSession, SessionProvider } from "next-auth/react";
import dynamic from "next/dynamic";

// Dynamically import the editor
const EmailEditor = dynamic(() => import("../../components/EmailEditor"), { ssr: false });

function TemplatesContent() {
  const router = useRouter(); // <-- 1. Add this line
  const { data: session, status } = useSession();
  
  // --- TEMPLATE STATES ---
  const [templates, setTemplates] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [name, setName] = useState("");
  const [htmlContent, setHtmlContent] = useState("");
  const [templateMessage, setTemplateMessage] = useState("");

  // --- TEMPLATE AI STATES ---
  const [aiTopic, setAiTopic] = useState("");
  const [refineInstructions, setRefineInstructions] = useState("");
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [isRefining, setIsRefining] = useState(false);
  const [aiTemplateProvider, setAiTemplateProvider] = useState("groq"); 

  // --- MEDIA & IMAGE AI STATES ---
  const [mediaList, setMediaList] = useState([]);
  const [isMediaUploading, setIsMediaUploading] = useState(false);
  const [mediaMessage, setMediaMessage] = useState("");
  const [aiMediaPrompt, setAiMediaPrompt] = useState("");
  const [aiMediaProvider, setAiMediaProvider] = useState("pollinations");
  const [isAiMediaGenerating, setIsAiMediaGenerating] = useState(false);
  const fileInputRef = useRef(null);

  // --- DATA FETCHING ---
  const fetchTemplates = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/templates", { headers: { "X-User-Email": session.user.email }});
      if (res.ok) setTemplates(await res.json());
    } catch (error) { console.error("Failed to fetch templates"); }
  };

  const fetchMedia = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/media", { headers: { "X-User-Email": session.user.email }});
      if (res.ok) setMediaList(await res.json());
    } catch (error) { console.error("Failed to fetch media"); }
  };

  useEffect(() => {
    if (session?.user?.email) {
        fetchTemplates();
        fetchMedia();
    }
  }, [session]);

  // --- TEMPLATE LOGIC ---
  const handleSubmit = async (e) => {
    e.preventDefault();
    setTemplateMessage("Saving...");
    const url = editingId ? `http://localhost:8080/api/templates/${editingId}` : "http://localhost:8080/api/templates";
    const method = editingId ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method: method,
        headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ name, htmlContent }),
      });
      if (res.ok) {
        setTemplateMessage(`✅ Template saved.`);
        cancelEdit(); fetchTemplates(); 
      } else {
        const err = await res.json(); setTemplateMessage(`❌ Failed: ${err.message}`);
      }
    } catch (error) { setTemplateMessage("❌ Error connecting to server."); }
  };

  const handleEdit = (tmpl) => {
    setEditingId(tmpl.id); setName(tmpl.name); setHtmlContent(tmpl.htmlContent);
    window.scrollTo({ top: 0, behavior: 'smooth' }); setTemplateMessage("");
  };
  const cancelEdit = () => { setEditingId(null); setName(""); setHtmlContent(""); setTemplateMessage(""); };
  const handleDelete = async (id) => {
    if (!confirm("Delete template?")) return;
    await fetch(`http://localhost:8080/api/templates/${id}`, { method: "DELETE", headers: { "X-User-Email": session.user.email } });
    if (editingId === id) cancelEdit(); fetchTemplates();
  };

  const handleTemplateAiGenerate = async () => {
    if (!aiTopic.trim()) return;
    setIsAiLoading(true); setTemplateMessage("🤖 Designing template...");
    try {
        const res = await fetch("http://localhost:8080/api/ai/generate-template", {
            method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
            body: JSON.stringify({ topic: aiTopic, provider: aiTemplateProvider })
        });
        const data = await res.json();
        if (res.ok) { setHtmlContent(data.html); setTemplateMessage("✅ AI Template generated!"); setAiTopic(""); } 
        else { setTemplateMessage(`❌ Error: ${data.message}`); }
    } catch (error) { setTemplateMessage("❌ Connection failed."); } 
    finally { setIsAiLoading(false); }
  };

  const handleTemplateAiRefine = async () => {
    if (!refineInstructions.trim() || !htmlContent.trim()) return;
    setIsRefining(true); setTemplateMessage("🤖 Updating canvas...");
    try {
        const res = await fetch("http://localhost:8080/api/ai/refine-template", {
            method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
            body: JSON.stringify({ currentHtml: htmlContent, instructions: refineInstructions, provider: aiTemplateProvider })
        });
        const data = await res.json();
        if (res.ok) { setHtmlContent(data.html); setTemplateMessage("✅ Template updated!"); setRefineInstructions(""); } 
        else { setTemplateMessage(`❌ Error: ${data.message}`); }
    } catch (error) { setTemplateMessage("❌ Connection failed."); } 
    finally { setIsRefining(false); }
  };

  // --- MEDIA LOGIC ---
  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { setMediaMessage("❌ File too large (Max 5MB)."); return; }

    setIsMediaUploading(true); setMediaMessage("⏳ Uploading...");
    const formData = new FormData(); formData.append("file", file);

    try {
      const res = await fetch("http://localhost:8080/api/media/upload", {
        method: "POST", headers: { "X-User-Email": session.user.email }, body: formData,
      });
      if (res.ok) { setMediaMessage("✅ Uploaded."); fetchMedia(); if (fileInputRef.current) fileInputRef.current.value = ""; } 
      else { const err = await res.json(); setMediaMessage(`❌ Failed: ${err.message}`); }
    } catch (error) { setMediaMessage("❌ Connection error."); } 
    finally { setIsMediaUploading(false); setTimeout(() => setMediaMessage(""), 3000); }
  };

  const handleMediaAiGenerate = async () => {
    if (!aiMediaPrompt.trim()) return;
    setIsAiMediaGenerating(true); setMediaMessage("🎨 Generating image...");
    try {
      const res = await fetch("http://localhost:8080/api/ai/generate-image", {
        method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ prompt: aiMediaPrompt, provider: aiMediaProvider })
      });
      if (res.ok) { setMediaMessage("✅ Image saved!"); setAiMediaPrompt(""); fetchMedia(); } 
      else { const err = await res.json(); setMediaMessage(`❌ Failed: ${err.message}`); }
    } catch (error) { setMediaMessage("❌ Error."); } 
    finally { setIsAiMediaGenerating(false); setTimeout(() => setMediaMessage(""), 3000); }
  };

  // --- RENDER ---
  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Log in.</p>;

  return (
    <div className="max-w-7xl mx-auto pb-12">
      {/* 1. Header */}
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">Studio de Templates</h1>
          <p className="text-gray-500 mt-1">Concevez vos emails avec l'IA et gérez vos assets visuels.</p>
        </div>
        {templateMessage && <span className="text-sm font-bold text-blue-600 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">{templateMessage}</span>}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-4 gap-8 mb-8">
        
        {/* 2. Left Column: Template AI & Saving */}
        <div className="xl:col-span-1 space-y-6">
            {/* Save Block */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                    <div>
                        <label className="block text-sm font-bold text-gray-700 mb-2">Nom du Template</label>
                        <input type="text" required value={name} onChange={(e) => setName(e.target.value)} className="w-full p-3 border border-gray-200 rounded-lg text-sm bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition" placeholder="ex: Email de Bienvenue" />
                    </div>
                    <div className="flex flex-col gap-2">
                        <button type="submit" className={`w-full py-3 font-medium rounded-lg text-white transition shadow-sm ${editingId ? 'bg-yellow-600 hover:bg-yellow-700' : 'bg-green-600 hover:bg-green-700'}`}>
                            {editingId ? "Mettre à jour" : "Enregistrer le Template"}
                        </button>
                        {editingId && <button type="button" onClick={cancelEdit} className="w-full px-3 py-2 text-sm font-bold text-gray-500 hover:bg-gray-100 border border-gray-200 rounded-lg transition">Annuler l'édition</button>}
                    </div>
                </form>
            </div>

            {/* Template AI Block */}
            <div className="bg-purple-50 p-6 rounded-2xl shadow-sm border border-purple-100">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="font-bold text-purple-800 text-sm">✨ IA Template</h2>
                    <div className="flex bg-white rounded border border-purple-200 p-0.5">
                        <button onClick={() => setAiTemplateProvider("groq")} className={`px-2 py-1 text-[10px] font-bold rounded transition-colors ${aiTemplateProvider === 'groq' ? 'bg-purple-100 text-purple-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>Groq</button>
                        <button onClick={() => setAiTemplateProvider("gemini")} className={`px-2 py-1 text-[10px] font-bold rounded transition-colors ${aiTemplateProvider === 'gemini' ? 'bg-blue-100 text-blue-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>Gemini</button>
                    </div>
                </div>
                <input type="text" value={aiTopic} onChange={(e) => setAiTopic(e.target.value)} placeholder="Sujet du design..." className="w-full p-3 border border-purple-200 rounded-lg text-sm mb-3 focus:outline-none focus:ring-2 focus:ring-purple-500" />
                <button onClick={handleTemplateAiGenerate} disabled={isAiLoading} className="w-full bg-purple-600 hover:bg-purple-700 text-white py-2.5 rounded-lg text-sm font-bold shadow-sm transition disabled:opacity-50">
                    {isAiLoading ? "Génération en cours..." : "Créer le Template"}
                </button>
                <div className="mt-4 border-t border-purple-200 pt-4">
                    <input type="text" value={refineInstructions} onChange={(e) => setRefineInstructions(e.target.value)} placeholder="Ajuster le design..." className="w-full p-3 border border-purple-200 rounded-lg text-sm mb-3 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50" disabled={!htmlContent} />
                    <button onClick={handleTemplateAiRefine} disabled={isRefining || !htmlContent} className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg text-sm font-bold shadow-sm transition disabled:opacity-50">
                        {isRefining ? "Mise à jour..." : "Appliquer les Modifications"}
                    </button>
                </div>
            </div>
        </div>

        {/* 3. Right Column: Media Manager & Canvas */}
        <div className="xl:col-span-3 flex flex-col gap-6">
            
            {/* The Unified Media Bar */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
                <div className="flex flex-col md:flex-row justify-between md:items-center gap-4 mb-4 pb-4 border-b border-gray-100">
                    <div>
                        <h2 className="font-bold text-gray-900">🖼️ Assets Visuels</h2>
                        <p className="text-xs text-gray-500 mt-1">Glissez-déposez les images directement dans le canevas.</p>
                        {mediaMessage && <p className="text-xs font-bold text-blue-600 mt-2">{mediaMessage}</p>}
                    </div>

                    {/* Media Generation & Upload Controls */}
                    <div className="flex flex-wrap items-center gap-3">
                        <div className="flex bg-gray-50 rounded-lg border border-gray-200 p-1">
                            <button onClick={() => setAiMediaProvider("pollinations")} className={`px-3 py-1.5 text-xs font-bold rounded-md transition-colors ${aiMediaProvider === 'pollinations' ? 'bg-white shadow text-purple-700' : 'text-gray-500 hover:text-gray-700'}`}>IA Gratuite</button>
                            <button onClick={() => setAiMediaProvider("gemini")} className={`px-3 py-1.5 text-xs font-bold rounded-md transition-colors ${aiMediaProvider === 'gemini' ? 'bg-white shadow text-blue-700' : 'text-gray-500 hover:text-gray-700'}`}>Gemini</button>
                        </div>
                        <input type="text" value={aiMediaPrompt} onChange={(e) => setAiMediaPrompt(e.target.value)} placeholder="Prompt de l'image..." className="w-48 p-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-gray-400" />
                        <button onClick={handleMediaAiGenerate} disabled={isAiMediaGenerating || !aiMediaPrompt.trim()} className="bg-gray-800 hover:bg-gray-900 text-white px-4 py-2 rounded-lg text-sm font-bold shadow-sm transition disabled:opacity-50">
                            {isAiMediaGenerating ? "..." : "Générer"}
                        </button>
                        <span className="text-gray-200 font-light text-xl">|</span>
                        <input type="file" accept="image/*" className="hidden" ref={fileInputRef} onChange={handleFileUpload} />
                        <button onClick={() => fileInputRef.current.click()} disabled={isMediaUploading} className="bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition px-4 py-2 rounded-lg text-sm font-bold">
                            {isMediaUploading ? "..." : "Téléverser"}
                        </button>
                    </div>
                </div>

                {/* Draggable Gallery Grid */}
                <div className="flex gap-4 overflow-x-auto pb-2 custom-scrollbar items-center">
                    {mediaList.length === 0 ? (
                        <p className="text-sm text-gray-400 italic py-4">Aucune image dans votre galerie.</p>
                    ) : (
                        mediaList.map(media => (
                            <div key={media.id} className="flex-shrink-0 cursor-grab active:cursor-grabbing">
                                <img 
                                    src={media.fileUrl} 
                                    alt={media.fileName} 
                                    className="h-24 w-24 object-cover border-2 border-gray-100 hover:border-blue-500 hover:opacity-80 rounded-xl bg-white transition-all shadow-sm"
                                    draggable="true"
                                    onDragStart={(e) => {
                                        e.dataTransfer.setData('text/html', `<img src="${media.fileUrl}" style="max-width:100%; height:auto;" alt="uploaded asset"/>`);
                                    }}
                                />
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* Drag and Drop Canvas Workspace */}
            <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white shadow-sm min-h-[600px]">
                <EmailEditor initialHtml={htmlContent} onHtmlChange={setHtmlContent} />
            </div>
        </div>
      </div>

      {/* 4. Saved Templates List */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-100">
            <h2 className="text-lg font-bold text-gray-900">Vos Templates Enregistrés</h2>
        </div>
        <table className="w-full text-left border-collapse">
          <tbody className="divide-y divide-gray-100 text-sm">
            {templates.length === 0 ? (
              <tr><td colSpan="2" className="p-8 text-center text-gray-500 font-medium">Aucun template enregistré pour le moment.</td></tr>
            ) : (
              templates.map(tmpl => (
                <tr key={tmpl.id} className="hover:bg-gray-50 transition">
                  <td className="p-4 font-bold text-gray-900">{tmpl.name}</td>
                  <td className="p-4 text-right">
                      <div className="flex justify-end gap-2">
                          <button onClick={() => handleEdit(tmpl)} className="text-blue-600 font-bold hover:underline text-xs bg-blue-50 px-3 py-1.5 rounded-lg transition">Modifier</button>
                          <button onClick={() => handleDelete(tmpl.id)} className="text-red-600 font-bold hover:underline text-xs bg-red-50 px-3 py-1.5 rounded-lg transition">Supprimer</button>
                      </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

    </div>
  );
}

export default function TemplatesPage() {
  return (
    <SessionProvider>
      <TemplatesContent />
    </SessionProvider>
  );
}