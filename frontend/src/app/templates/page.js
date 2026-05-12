"use client";
import { useRouter } from "next/navigation";
import { useState, useEffect, useRef } from "react";
import { useSession, SessionProvider } from "next-auth/react";
import dynamic from "next/dynamic";

// SSR-safe: EmailEditor uses GrapesJS which requires `window`
const EmailEditor = dynamic(() => import("../../components/EmailEditor"), { ssr: false });

function TemplatesContent() {
  const router = useRouter();
  const { data: session, status } = useSession();

  // --- TEMPLATE STATES ---
  const [templates, setTemplates] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [name, setName] = useState("");
  const [htmlContent, setHtmlContent] = useState("");
  const [templateMessage, setTemplateMessage] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

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
  const editorRef = useRef(null); // ref to EmailEditor — exposes loadTemplate() via useImperativeHandle

  // --- DATA FETCHING ---
  const fetchTemplates = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/templates", { headers: { "X-User-Email": session.user.email } });
      if (res.ok) setTemplates(await res.json());
    } catch (error) { console.error("Failed to fetch templates"); }
  };

  const fetchMedia = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/media", { headers: { "X-User-Email": session.user.email } });
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
        method,
        headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ name, htmlContent }),
      });
      if (res.ok) {
        setTemplateMessage("✅ Template enregistré.");
        cancelEdit(); fetchTemplates();
      } else {
        const err = await res.json(); setTemplateMessage(`❌ Erreur: ${err.message}`);
      }
    } catch (error) { setTemplateMessage("❌ Erreur de connexion."); }
  };

  const handleEdit = (tmpl) => {
    // 1. Update the save-form fields
    setEditingId(tmpl.id);
    setName(tmpl.name);
    setHtmlContent(tmpl.htmlContent);
    setTemplateMessage("");

    // 2. Inject HTML directly into the GrapesJS canvas via the ref
    if (editorRef.current) {
      editorRef.current.loadTemplate(tmpl.htmlContent || "");
    }

    // 3. Scroll smoothly up to the editor
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const cancelEdit = () => { setEditingId(null); setName(""); setHtmlContent(""); setTemplateMessage(""); };

  const handleDelete = async (id) => {
    if (!confirm("Supprimer ce template ?")) return;
    await fetch(`http://localhost:8080/api/templates/${id}`, { method: "DELETE", headers: { "X-User-Email": session.user.email } });
    if (editingId === id) cancelEdit();
    fetchTemplates();
  };

  const handleTemplateAiGenerate = async () => {
    if (!aiTopic.trim()) return;
    setIsAiLoading(true); setTemplateMessage("🤖 Conception du template...");
    try {
      const res = await fetch("http://localhost:8080/api/ai/generate-template", {
        method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ topic: aiTopic, provider: aiTemplateProvider }),
      });
      const data = await res.json();
      if (res.ok) {
        setHtmlContent(data.html);
        setTemplateMessage("✅ Template généré par l'IA !");
        setAiTopic("");
        if (editor) editor.setComponents(data.html || "");
      }
      else { setTemplateMessage(`❌ Erreur: ${data.message}`); }
    } catch (error) { setTemplateMessage("❌ Connexion échouée."); }
    finally { setIsAiLoading(false); }
  };

  const handleTemplateAiRefine = async () => {
    if (!refineInstructions.trim() || !htmlContent.trim()) return;
    setIsRefining(true); setTemplateMessage("🤖 Mise à jour du canvas...");
    try {
      const res = await fetch("http://localhost:8080/api/ai/refine-template", {
        method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ currentHtml: htmlContent, instructions: refineInstructions, provider: aiTemplateProvider }),
      });
      const data = await res.json();
      if (res.ok) {
        setHtmlContent(data.html);
        setTemplateMessage("✅ Template mis à jour !");
        setRefineInstructions("");
        if (editor) editor.setComponents(data.html || "");
      }
      else { setTemplateMessage(`❌ Erreur: ${data.message}`); }
    } catch (error) { setTemplateMessage("❌ Connexion échouée."); }
    finally { setIsRefining(false); }
  };

  // --- MEDIA LOGIC ---
  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (file.size > 10 * 1024 * 1024) { setMediaMessage("❌ Fichier trop lourd (Max 10MB)."); return; }

    setIsMediaUploading(true); setMediaMessage("⏳ Téléversement...");
    const formData = new FormData();
    formData.append("file", file);
    // NOTE: Do NOT set Content-Type manually — fetch sets it automatically
    // with the correct multipart boundary when given a FormData body.

    try {
      const res = await fetch("http://localhost:8080/api/media/upload", {
        method: "POST",
        headers: { "X-User-Email": session.user.email },
        body: formData,
      });
      if (res.ok) {
        setMediaMessage("✅ Téléversé.");
        fetchMedia();
        if (fileInputRef.current) fileInputRef.current.value = "";
      } else {
        // Safely parse error — backend may return JSON or HTML (e.g. 413/500)
        const errText = await res.text();
        let errMsg = `Erreur serveur (${res.status})`;
        try { errMsg = JSON.parse(errText).message || errMsg; } catch (_) {}
        setMediaMessage(`❌ ${errMsg}`);
      }
    } catch (error) {
      // Network-level failure: backend not running or CORS blocked
      console.error("Upload network error:", error);
      setMediaMessage("❌ Serveur inaccessible. Le backend est-il démarré ?");
    } finally {
      setIsMediaUploading(false);
      setTimeout(() => setMediaMessage(""), 4000);
    }
  };


  const handleMediaAiGenerate = async () => {
    if (!aiMediaPrompt.trim()) return;
    setIsAiMediaGenerating(true); setMediaMessage("🎨 Génération de l'image...");
    try {
      const res = await fetch("http://localhost:8080/api/ai/generate-image", {
        method: "POST", headers: { "Content-Type": "application/json", "X-User-Email": session.user.email },
        body: JSON.stringify({ prompt: aiMediaPrompt, provider: aiMediaProvider }),
      });
      if (res.ok) { setMediaMessage("✅ Image enregistrée !"); setAiMediaPrompt(""); fetchMedia(); }
      else { const err = await res.json(); setMediaMessage(`❌ Erreur: ${err.message}`); }
    } catch (error) { setMediaMessage("❌ Erreur."); }
    finally { setIsAiMediaGenerating(false); setTimeout(() => setMediaMessage(""), 3000); }
  };

  // --- DERIVED STATE ---
  const filteredTemplates = templates.filter(t =>
    t.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (status === "loading") return <p className="p-8 text-gray-500">Chargement...</p>;
  if (!session) return <p className="p-8 text-red-500">Accès Refusé. Veuillez vous connecter.</p>;

  return (
    <div className="max-w-[1600px] mx-auto pb-12">

      {/* ── Page Header ── */}
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">Studio de Templates</h1>
          <p className="text-gray-500 mt-1">Concevez vos emails avec l'IA et gérez vos assets visuels.</p>
        </div>
        {templateMessage && (
          <span className={`text-sm font-bold px-4 py-2 rounded-full border ${templateMessage.includes("❌") ? "bg-red-50 text-red-600 border-red-200" : "bg-blue-50 text-blue-600 border-blue-100"}`}>
            {templateMessage}
          </span>
        )}
      </div>

      {/* ── Main Editor: 12-col grid ── */}
      <div className="grid grid-cols-12 gap-6 mb-10">

        {/* ── LEFT COLUMN (col-span-3): Controls ── */}
        <div className="col-span-12 xl:col-span-3 flex flex-col gap-5">

          {/* 1. Save Card */}
          <div className={`bg-white rounded-2xl shadow-sm border p-6 transition-colors ${editingId ? "border-yellow-300 bg-yellow-50" : "border-gray-100"}`}>
            <h2 className="text-sm font-bold text-gray-700 mb-4 uppercase tracking-wide">
              {editingId ? "✏️ Modifier le Template" : "💾 Enregistrer"}
            </h2>
            <form onSubmit={handleSubmit} className="flex flex-col gap-3">
              <div>
                <label className="block text-xs font-bold text-gray-600 mb-1">Nom du Template</label>
                <input
                  type="text" required value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full p-3 border border-gray-200 rounded-xl text-sm bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
                  placeholder="ex: Email de Bienvenue"
                />
              </div>
              <button
                type="submit"
                className={`w-full py-3 font-bold rounded-xl text-white text-sm transition shadow-sm ${editingId ? "bg-yellow-500 hover:bg-yellow-600" : "bg-green-600 hover:bg-green-700"}`}
              >
                {editingId ? "Mettre à jour" : "Enregistrer"}
              </button>
              {editingId && (
                <button type="button" onClick={cancelEdit} className="w-full py-2 text-sm font-bold text-gray-500 border border-gray-200 rounded-xl hover:bg-gray-100 transition">
                  Annuler l'édition
                </button>
              )}
            </form>
          </div>

          {/* 2. AI Template Card */}
          <div className="bg-gradient-to-b from-purple-50 to-white rounded-2xl shadow-sm border border-purple-100 p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-sm font-bold text-purple-800 uppercase tracking-wide">✨ IA Template</h2>
              <div className="flex bg-white rounded-lg border border-purple-200 p-0.5 text-[10px]">
                <button onClick={() => setAiTemplateProvider("groq")} className={`px-2.5 py-1 font-bold rounded-md transition-colors ${aiTemplateProvider === "groq" ? "bg-purple-100 text-purple-800 shadow-sm" : "text-gray-500"}`}>Groq</button>
                <button onClick={() => setAiTemplateProvider("gemini")} className={`px-2.5 py-1 font-bold rounded-md transition-colors ${aiTemplateProvider === "gemini" ? "bg-blue-100 text-blue-800 shadow-sm" : "text-gray-500"}`}>Gemini</button>
              </div>
            </div>
            <input
              type="text" value={aiTopic} onChange={(e) => setAiTopic(e.target.value)}
              placeholder="Ex: Promotion Black Friday..."
              className="w-full p-3 border border-purple-100 rounded-xl text-sm mb-3 focus:outline-none focus:ring-2 focus:ring-purple-400 bg-white"
            />
            <button onClick={handleTemplateAiGenerate} disabled={isAiLoading} className="w-full bg-purple-600 hover:bg-purple-700 text-white py-3 rounded-xl text-sm font-bold shadow-sm transition disabled:opacity-50">
              {isAiLoading ? "Génération..." : "✨ Créer le Template"}
            </button>

            <div className="mt-5 pt-5 border-t border-purple-100">
              <p className="text-xs font-bold text-gray-500 mb-2">Affiner le canvas actuel</p>
              <input
                type="text" value={refineInstructions} onChange={(e) => setRefineInstructions(e.target.value)}
                placeholder="Ex: Ajoute un bouton rouge..."
                className="w-full p-3 border border-purple-100 rounded-xl text-sm mb-3 focus:outline-none focus:ring-2 focus:ring-blue-400 disabled:opacity-50 bg-white"
                disabled={!htmlContent}
              />
              <button onClick={handleTemplateAiRefine} disabled={isRefining || !htmlContent} className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-xl text-sm font-bold shadow-sm transition disabled:opacity-50">
                {isRefining ? "Mise à jour..." : "🔧 Appliquer les Modifications"}
              </button>
            </div>
          </div>

          {/* 3. Assets Visuels Card */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
            <h2 className="text-sm font-bold text-gray-800 uppercase tracking-wide mb-1">🖼️ Assets Visuels</h2>
            <p className="text-xs text-gray-400 mb-4">Glissez-déposez dans le canevas.</p>
            {mediaMessage && <p className="text-xs font-bold text-blue-600 mb-3">{mediaMessage}</p>}

            {/* AI Generate + Upload controls */}
            <div className="flex flex-col gap-3 mb-4">
              <div className="flex bg-gray-50 rounded-lg border border-gray-200 p-1">
                <button onClick={() => setAiMediaProvider("pollinations")} className={`flex-1 py-1.5 text-xs font-bold rounded-md transition-colors ${aiMediaProvider === "pollinations" ? "bg-white shadow text-purple-700" : "text-gray-500"}`}>IA Gratuite</button>
                <button onClick={() => setAiMediaProvider("gemini")} className={`flex-1 py-1.5 text-xs font-bold rounded-md transition-colors ${aiMediaProvider === "gemini" ? "bg-white shadow text-blue-700" : "text-gray-500"}`}>Gemini</button>
              </div>
              <input
                type="text" value={aiMediaPrompt} onChange={(e) => setAiMediaPrompt(e.target.value)}
                placeholder="Décrire une image..."
                className="w-full p-2.5 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-gray-400"
              />
              <button onClick={handleMediaAiGenerate} disabled={isAiMediaGenerating || !aiMediaPrompt.trim()} className="w-full bg-gray-800 hover:bg-gray-900 text-white py-2.5 rounded-xl text-xs font-bold transition disabled:opacity-50">
                {isAiMediaGenerating ? "Génération..." : "Générer l'Image"}
              </button>
              <div className="flex items-center gap-2">
                <hr className="flex-1 border-gray-200" />
                <span className="text-xs text-gray-400">ou</span>
                <hr className="flex-1 border-gray-200" />
              </div>
              <input type="file" accept="image/*" className="hidden" ref={fileInputRef} onChange={handleFileUpload} />
              <button onClick={() => fileInputRef.current.click()} disabled={isMediaUploading} className="w-full bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 py-2.5 rounded-xl text-xs font-bold transition">
                {isMediaUploading ? "Téléversement..." : "📁 Téléverser une Image"}
              </button>
            </div>

            {/* Gallery grid */}
            <div className="grid grid-cols-3 gap-2 max-h-48 overflow-y-auto">
              {mediaList.length === 0 ? (
                <p className="col-span-3 text-xs text-gray-400 italic text-center py-3">Aucune image.</p>
              ) : (
                mediaList.map(media => (
                  <div key={media.id} className="cursor-grab active:cursor-grabbing group relative">
                    <img
                      src={media.fileUrl} alt={media.fileName}
                      className="w-full aspect-square object-cover border border-gray-100 group-hover:border-blue-400 rounded-lg transition-all shadow-sm"
                      draggable="true"
                      onDragStart={(e) => {
                        e.dataTransfer.setData("text/html", `<img src="${media.fileUrl}" style="max-width:100%; height:auto;" alt="asset"/>`);
                      }}
                    />
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* ── RIGHT COLUMN (col-span-9): GrapesJS Canvas ── */}
        <div className="col-span-12 xl:col-span-9">
          <div className="h-[700px] w-full border border-gray-200 rounded-2xl overflow-hidden bg-white shadow-sm">
            {/* SSR-disabled dynamic component — GrapesJS loads fully client-side */}
            <EmailEditor
              ref={editorRef}
              initialHtml={htmlContent}
              onHtmlChange={setHtmlContent}
            />
          </div>
          {/* ── Saved Templates Section ── */}
          <br />
      <br />
      <div>
        {/* Section Header + Search */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Vos Templates Enregistrés</h2>
            <p className="text-sm text-gray-500 mt-0.5">{filteredTemplates.length} template{filteredTemplates.length !== 1 ? "s" : ""} trouvé{filteredTemplates.length !== 1 ? "s" : ""}</p>
          </div>
          {/* Search Bar */}
          <div className="relative w-full md:w-72">
            <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Rechercher un template..."
              className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition"
            />
          </div>
        </div>
              
        {/* Template Cards Grid */}
        {filteredTemplates.length === 0 ? (
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-16 text-center">
            <svg className="w-12 h-12 text-gray-200 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <p className="text-gray-400 font-medium">
              {searchQuery ? `Aucun résultat pour "${searchQuery}"` : "Aucun template enregistré pour le moment."}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {filteredTemplates.map(tmpl => (
              <div key={tmpl.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden group hover:shadow-md hover:border-gray-200 transition-all flex flex-col">

                {/* Thumbnail Area */}
                <div className="relative bg-gradient-to-br from-gray-50 to-gray-100 h-44 flex items-center justify-center overflow-hidden border-b border-gray-100">
                  {/* Scaled-down live preview iframe */}
                  <div className="w-full h-full overflow-hidden relative pointer-events-none">
                    <iframe
                      srcDoc={tmpl.htmlContent}
                      className="w-[300%] h-[300%] origin-top-left border-0"
                      style={{ transform: "scale(0.333)", transformOrigin: "top left" }}
                      tabIndex="-1"
                      sandbox="allow-same-origin"
                      title={tmpl.name}
                    />
                  </div>
                  {/* Edit overlay on hover */}
                  <div className="absolute inset-0 bg-blue-600/0 group-hover:bg-blue-600/10 transition-colors flex items-center justify-center">
                    <button
                      onClick={() => handleEdit(tmpl)}
                      className="opacity-0 group-hover:opacity-100 transition-opacity bg-white text-blue-700 font-bold text-xs px-4 py-2 rounded-lg shadow-md border border-blue-100 hover:bg-blue-50"
                    >
                      ✏️ Ouvrir dans l'éditeur
                    </button>
                  </div>
                </div>

                {/* Card Footer */}
                <div className="p-4 flex flex-col gap-3 flex-1">
                  <div>
                    <h3 className="font-bold text-gray-900 text-sm truncate">{tmpl.name}</h3>
                    {tmpl.createdAt && (
                      <p className="text-xs text-gray-400 mt-0.5">
                        {new Date(tmpl.createdAt).toLocaleDateString("fr-FR", { day: "numeric", month: "short", year: "numeric" })}
                      </p>
                    )}
                  </div>
                  <div className="flex gap-2 mt-auto">
                    <button
                      onClick={() => handleEdit(tmpl)}
                      className="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-lg transition shadow-sm"
                    >
                      Modifier
                    </button>
                    <button
                      onClick={() => handleDelete(tmpl.id)}
                      className="py-2 px-3 bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold rounded-lg transition border border-red-100"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
        </div>
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