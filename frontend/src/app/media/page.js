"use client";
import { useState, useEffect, useRef } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function MediaContent() {
  const { data: session, status } = useSession();
  const [mediaList, setMediaList] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [message, setMessage] = useState("");
  const fileInputRef = useRef(null);



const [aiPrompt, setAiPrompt] = useState("");
  const [aiProvider, setAiProvider] = useState("pollinations"); // Default to Free
  const [isAiGenerating, setIsAiGenerating] = useState(false);
  
  const fetchMedia = async () => {
    if (!session?.user?.email) return;
    try {
      const res = await fetch("http://localhost:8080/api/media", {
        headers: { "X-User-Email": session.user.email }
      });
      if (res.ok) setMediaList(await res.json());
    } catch (error) {
      console.error("Failed to fetch media");
    }
  };

  useEffect(() => {
    if (session?.user?.email) fetchMedia();
  }, [session]);

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Strict size limit (5MB)
    if (file.size > 5 * 1024 * 1024) {
        setMessage("❌ File is too large. Maximum size is 5MB.");
        return;
    }

    setIsUploading(true);
    setMessage("⏳ Uploading...");

    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await fetch("http://localhost:8080/api/media/upload", {
        method: "POST",
        headers: { 
            "X-User-Email": session.user.email 
            // Note: Do NOT set Content-Type to application/json here. 
            // The browser automatically sets the correct multipart boundary for FormData.
        },
        body: formData,
      });

      if (res.ok) {
        setMessage("✅ Image uploaded successfully.");
        fetchMedia(); // Refresh gallery
        if (fileInputRef.current) fileInputRef.current.value = ""; // Reset input
      } else {
        const errData = await res.json();
        setMessage(`❌ Upload failed: ${errData.message}`);
      }
    } catch (error) {
      setMessage("❌ Error connecting to server.");
    } finally {
      setIsUploading(false);
    }
  };

  const copyToClipboard = (url) => {
    navigator.clipboard.writeText(url);
    setMessage("✅ URL copied to clipboard!");
    setTimeout(() => setMessage(""), 3000);
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Please log in first.</p>;
const handleAiGenerate = async () => {
    if (!aiPrompt.trim()) return;
    setIsAiGenerating(true);
    setMessage("🤖 Generating image...");

    try {
      const res = await fetch("http://localhost:8080/api/ai/generate-image", {
        method: "POST",
        headers: { 
            "Content-Type": "application/json",
            "X-User-Email": session.user.email 
        },
        // Send the selected provider
        body: JSON.stringify({ prompt: aiPrompt, provider: aiProvider })
      });

      if (res.ok) {
        setMessage("✅ AI Image generated and saved!");
        setAiPrompt("");
        fetchMedia(); // Refresh gallery to show the new image
      } else {
        const errData = await res.json();
        setMessage(`❌ Generation failed: ${errData.message}`);
      }
    } catch (error) {
      setMessage("❌ Error connecting to AI engine.");
    } finally {
      setIsAiGenerating(false);
    }
  };
  return (
    <div className="max-w-7xl mx-auto p-8 mt-10 text-black">
      <div className="flex justify-between items-center mb-8 bg-white p-6 rounded shadow">
        <div>
            <h1 className="text-2xl font-bold">Media Gallery</h1>
            <p className="text-gray-500 text-sm mt-1">Upload and manage images for your email templates.</p>
        </div>
        
        {/* Upload Button */}
        <div className="flex flex-col items-end gap-4">
            {message && <span className="text-sm font-bold text-blue-600">{message}</span>}
            
            <div className="flex items-center gap-2">
                
                {/* THE UPGRADE: Provider Toggle Switch */}
                <div className="flex bg-gray-100 rounded p-1 border">
                    <button
                        onClick={() => setAiProvider("pollinations")}
                        className={`px-3 py-1 text-xs font-bold rounded transition-colors ${aiProvider === 'pollinations' ? 'bg-white shadow text-purple-700' : 'text-gray-500 hover:text-gray-700'}`}
                    >
                        Free AI
                    </button>
                    <button
                        onClick={() => setAiProvider("gemini")}
                        className={`px-3 py-1 text-xs font-bold rounded transition-colors ${aiProvider === 'gemini' ? 'bg-white shadow text-blue-700' : 'text-gray-500 hover:text-gray-700'}`}
                    >
                        Google Gemini
                    </button>
                </div>

                <input 
                    type="text" 
                    value={aiPrompt} 
                    onChange={(e) => setAiPrompt(e.target.value)} 
                    placeholder="e.g., A minimalist laptop on a desk" 
                    className="w-64 p-2 border rounded text-sm outline-none focus:ring-2 focus:ring-purple-400" 
                    onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAiGenerate())}
                />
                
                {/* Button dynamically changes color based on provider */}
                <button 
                    onClick={handleAiGenerate}
                    disabled={isAiGenerating || !aiPrompt.trim()}
                    className={`text-white px-4 py-2 rounded font-bold shadow disabled:opacity-50 transition-colors ${aiProvider === 'gemini' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-purple-600 hover:bg-purple-700'}`}
                >
                    {isAiGenerating ? "Drawing..." : "✨ AI Create"}
                </button>

                <span className="text-gray-300 mx-2">|</span>

                <input 
                    type="file" 
                    accept="image/png, image/jpeg, image/gif, image/webp"
                    className="hidden" 
                    ref={fileInputRef}
                    onChange={handleFileUpload}
                />
                <button 
                    onClick={() => fileInputRef.current.click()}
                    disabled={isUploading}
                    className="bg-gray-800 hover:bg-gray-900 text-white px-4 py-2 rounded font-bold shadow disabled:opacity-50"
                >
                    {isUploading ? "..." : "Upload File"}
                </button>
            </div>
        </div>
      </div>

      {/* Gallery Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        {mediaList.length === 0 ? (
          <div className="col-span-full p-12 text-center bg-white rounded shadow text-gray-400">
            No images uploaded yet.
          </div>
        ) : (
          mediaList.map((media) => (
            <div key={media.id} className="bg-white rounded shadow-sm border overflow-hidden flex flex-col group relative">
                <div className="h-40 bg-gray-100 flex items-center justify-center p-2 relative">
                    <img 
                        src={media.fileUrl} 
                        alt={media.fileName} 
                        className="max-h-full max-w-full object-contain"
                    />
                    {/* Hover Overlay */}
                    <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-40 transition-all flex items-center justify-center opacity-0 group-hover:opacity-100">
                        <button 
                            onClick={() => copyToClipboard(media.fileUrl)}
                            className="bg-white text-black text-xs font-bold px-3 py-1 rounded shadow"
                        >
                            Copy URL
                        </button>
                    </div>
                </div>
                <div className="p-2 border-t bg-gray-50 text-xs truncate text-gray-600">
                    {media.fileName}
                </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default function MediaPage() {
  return (
    <SessionProvider>
      <MediaContent />
    </SessionProvider>
  );
}