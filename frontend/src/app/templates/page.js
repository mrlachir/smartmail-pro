"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function TemplatesContent() {
    const [refineInstructions, setRefineInstructions] = useState("");
  const [isRefining, setIsRefining] = useState(false);


  const { data: session, status } = useSession();
  const [templates, setTemplates] = useState([]);

  const [editingId, setEditingId] = useState(null);
  const [name, setName] = useState("");
  const [htmlContent, setHtmlContent] = useState("");
  const [message, setMessage] = useState("");

  const [aiTopic, setAiTopic] = useState("");
  const [isAiLoading, setIsAiLoading] = useState(false);

  const fetchTemplates = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/templates", {
        headers: { "X-User-Email": session.user.email }
      });
      if (res.ok) setTemplates(await res.json());
    } catch (error) {
      console.error("Failed to fetch templates");
    }
  };

  useEffect(() => {
    if (session?.user?.email) fetchTemplates();
  }, [session]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("Saving...");

    const url = editingId 
        ? `http://localhost:8080/api/templates/${editingId}` 
        : "http://localhost:8080/api/templates";
    const method = editingId ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method: method,
        headers: { 
            "Content-Type": "application/json",
            "X-User-Email": session.user.email 
        },
        body: JSON.stringify({ name, htmlContent }),
      });

      if (res.ok) {
        setMessage(`✅ Template ${editingId ? 'updated' : 'saved'} successfully.`);
        cancelEdit(); 
        fetchTemplates(); 
      } else {
        const errData = await res.json();
        setMessage(`❌ Failed: ${errData.message}`);
      }
    } catch (error) {
      setMessage("❌ Error connecting to server.");
    }
  };

  const handleAiRefine = async () => {
    if (!refineInstructions.trim() || !htmlContent.trim()) return;
    
    setIsRefining(true);
    setMessage("🤖 Gemini is updating your code...");

    try {
        const res = await fetch("http://localhost:8080/api/ai/refine-template", {
            method: "POST",
            headers: { 
                "Content-Type": "application/json",
                "X-User-Email": session.user.email 
            },
            body: JSON.stringify({ currentHtml: htmlContent, instructions: refineInstructions })
        });

        const data = await res.json();
        if (res.ok) {
            setHtmlContent(data.html);
            setMessage("✅ Template updated successfully!");
            setRefineInstructions(""); // Clear input
        } else {
            setMessage(`❌ Refinement Error: ${data.message}`);
        }
    } catch (error) {
        setMessage("❌ Failed to connect to AI engine.");
    } finally {
        setIsRefining(false);
    }
  };

  const handleEdit = (tmpl) => {
    setEditingId(tmpl.id);
    setName(tmpl.name);
    setHtmlContent(tmpl.htmlContent);
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setMessage("");
  };

  const cancelEdit = () => {
    setEditingId(null);
    setName("");
    setHtmlContent("");
    setMessage("");
  };

  const handleDelete = async (id) => {
    if (!confirm("Are you sure you want to delete this template?")) return;
    try {
      await fetch(`http://localhost:8080/api/templates/${id}`, { 
          method: "DELETE",
          headers: { "X-User-Email": session.user.email } 
      });
      if (editingId === id) cancelEdit(); 
      fetchTemplates();
    } catch (error) {
      console.error("Error deleting template");
    }
  };

  const handleAiGenerate = async () => {
    if (!aiTopic.trim()) {
        setMessage("❌ Please enter a topic for the AI.");
        return;
    }
    setIsAiLoading(true);
    setMessage("🤖 Gemini is writing your HTML...");

    try {
        const res = await fetch("http://localhost:8080/api/ai/generate-template", {
            method: "POST",
            headers: { 
                "Content-Type": "application/json",
                "X-User-Email": session.user.email 
            },
            body: JSON.stringify({ topic: aiTopic })
        });

        const data = await res.json();
        if (res.ok) {
            setHtmlContent(data.html);
            setMessage("✅ AI Template generated! You can edit the code below.");
            setAiTopic(""); // Clear the input
        } else {
            setMessage(`❌ AI Error: ${data.message}`);
        }
    } catch (error) {
        setMessage("❌ Failed to connect to AI engine.");
    } finally {
        setIsAiLoading(false);
    }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Please log in first.</p>;

  return (
    <div className="max-w-[1400px] mx-auto p-8 mt-10 grid grid-cols-1 lg:grid-cols-2 gap-8 text-black">
      
      {/* Left Column: Code Editor & AI */}
      <div>
        <h1 className="text-2xl font-bold mb-6 bg-white p-4 rounded shadow">Template Studio</h1>
        
        {/* AI Generator Box */}
        <div className="mb-6 p-4 bg-purple-50 border-2 border-purple-200 rounded shadow-sm">
            <h2 className="font-bold text-purple-800 mb-2">✨ AI Template Designer</h2>
            <div className="flex gap-2">
                <input 
                    type="text" 
                    value={aiTopic} 
                    onChange={(e) => setAiTopic(e.target.value)} 
                    placeholder="e.g., A welcome email for a fitness app" 
                    className="flex-1 p-2 border rounded text-sm" 
                />
                <button 
                    onClick={handleAiGenerate} 
                    disabled={isAiLoading}
                    className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded font-bold shadow disabled:opacity-50 min-w-[140px]"
                >
                    {isAiLoading ? "Writing..." : "Generate Code"}
                </button>
            </div>
        </div>

        <div className={`mb-8 p-6 rounded shadow ${editingId ? 'bg-yellow-50 border-2 border-yellow-400' : 'bg-white'}`}>
          <div className="flex justify-between items-center border-b pb-2 mb-4">
            <h2 className="text-lg font-semibold">
                {editingId ? "Edit Template" : "Save Template"}
            </h2>
            {editingId && (
                <button onClick={cancelEdit} className="text-sm font-bold text-gray-500 hover:text-gray-800">
                    Cancel Edit
                </button>
            )}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Template Name</label>
              <input type="text" required value={name} onChange={(e) => setName(e.target.value)} className="w-full p-2 border rounded" placeholder="e.g., Fitness Welcome Series" />
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Raw HTML Code</label>
              <textarea 
                required 
                value={htmlContent} 
                onChange={(e) => setHtmlContent(e.target.value)} 
                className="w-full p-3 border rounded text-xs font-mono bg-gray-900 text-green-400 outline-none focus:ring-2 focus:ring-blue-500" 
                rows="14"
              ></textarea>
            </div>

            {/* THE UPGRADE: The Chatbot Refinement UI */}
            {htmlContent && (
                <div className="p-3 bg-blue-50 border-l-4 border-blue-500 rounded flex gap-2 items-center">
                    <span className="text-xl">💬</span>
                    <input 
                        type="text" 
                        value={refineInstructions} 
                        onChange={(e) => setRefineInstructions(e.target.value)} 
                        placeholder="e.g., Change the background to dark mode, add a footer..." 
                        className="flex-1 p-2 border rounded text-sm bg-white"
                        onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAiRefine())}
                    />
                    <button 
                        type="button"
                        onClick={handleAiRefine} 
                        disabled={isRefining || !refineInstructions.trim()}
                        className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded text-sm font-bold disabled:opacity-50"
                    >
                        {isRefining ? "Updating..." : "Ask AI to Edit"}
                    </button>
                </div>
            )}


            <button type="submit" className={`w-full py-3 font-bold rounded shadow text-white ${editingId ? 'bg-yellow-600 hover:bg-yellow-700' : 'bg-blue-600 hover:bg-blue-700'}`}>
                {editingId ? "Update Template" : "Save Template"}
            </button>
          </form>
          {message && <p className="mt-3 text-sm font-medium text-center">{message}</p>}
        </div>

        <div className="bg-white rounded shadow overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-gray-100 border-b">
              <tr>
                <th className="p-3">Saved Templates</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {templates.length === 0 ? (
                <tr><td colSpan="2" className="p-4 text-center text-gray-500">No templates saved yet.</td></tr>
              ) : (
                templates.map(tmpl => (
                  <tr key={tmpl.id} className="border-b hover:bg-gray-50">
                    <td className="p-3 font-medium">{tmpl.name}</td>
                    <td className="p-3 text-right space-x-3">
                      <button onClick={() => handleEdit(tmpl)} className="text-blue-600 font-bold hover:underline">Edit</button>
                      <button onClick={() => handleDelete(tmpl.id)} className="text-red-500 hover:underline">Delete</button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Right Column: Live Preview */}
      <div>
        <h1 className="text-2xl font-bold mb-6 bg-white p-4 rounded shadow">Live Rendering</h1>
        <div className="bg-white rounded shadow border-4 border-gray-100 overflow-hidden h-[850px]">
            {htmlContent ? (
                <iframe
                    srcDoc={htmlContent}
                    className="w-full h-full border-none bg-white"
                    title="Live Email Preview"
                    sandbox="allow-same-origin"
                />
            ) : (
                <div className="h-full flex flex-col items-center justify-center text-gray-400">
                    <p>Write or generate HTML to see the live rendering here.</p>
                </div>
            )}
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