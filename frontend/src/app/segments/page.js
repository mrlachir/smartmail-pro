"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function SegmentsContent() {
  const { data: session, status } = useSession();
  const [segments, setSegments] = useState([]);
  
  // Form State
  const [editingId, setEditingId] = useState(null); // NEW: Tracks if we are editing
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [rules, setRules] = useState([{ column: "status", operator: "=", value: "" }]);
  const [message, setMessage] = useState("");

  const [availableColumns, setAvailableColumns] = useState(["status"]);
  const [evaluatedSubscribers, setEvaluatedSubscribers] = useState(null);
  const [viewingSegmentName, setViewingSegmentName] = useState("");

  const fetchSegments = async () => {
    try {
      const res = await fetch("http://localhost:8080/api/segments");
      if (res.ok) setSegments(await res.json());
    } catch (error) {
      console.error("Failed to fetch segments");
    }
  };

  const fetchAttributes = async () => {
    try {
      const res = await fetch("http://localhost:8080/api/subscribers/attributes");
      if (res.ok) {
        const data = await res.json();
        setAvailableColumns(["status", ...data]);
      }
    } catch (error) {
      console.error("Failed to fetch attributes");
    }
  };

  useEffect(() => {
    fetchSegments();
    fetchAttributes();
  }, []);

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

  // NEW: Loads segment data into the form
  const handleEdit = (segment) => {
    setEditingId(segment.id);
    setName(segment.name);
    setDescription(segment.description || "");
    
    try {
        const parsedRules = JSON.parse(segment.rules);
        // Handle both legacy (single object) and new (array) formats
        if (Array.isArray(parsedRules)) {
            setRules(parsedRules);
        } else {
            setRules([parsedRules]);
        }
    } catch (e) {
        setRules([{ column: "status", operator: "=", value: "" }]);
    }
    
    // Scroll to top to see the form
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setMessage("");
  };

  // NEW: Cancels edit mode and clears the form
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

    // Determine the URL and Method based on editing mode
    const url = editingId 
        ? `http://localhost:8080/api/segments/${editingId}` 
        : "http://localhost:8080/api/segments";
    const method = editingId ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, description, rules: JSON.stringify(rules) }),
      });

      if (res.ok) {
        setMessage(`✅ Segment ${editingId ? 'updated' : 'saved'} successfully.`);
        cancelEdit(); // Reset form
        fetchSegments(); // Refresh table
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
      await fetch(`http://localhost:8080/api/segments/${id}`, { method: "DELETE" });
      if (editingId === id) cancelEdit(); // Clear form if they delete what they are editing
      fetchSegments();
      setEvaluatedSubscribers(null);
    } catch (error) {
      console.error("Error deleting segment");
    }
  };

  const handleRunRule = async (segment) => {
    setViewingSegmentName(segment.name);
    try {
      const res = await fetch(`http://localhost:8080/api/segments/${segment.id}/subscribers`);
      if (res.ok) {
        setEvaluatedSubscribers(await res.json());
      }
    } catch (error) {
      console.error("Error running segment rules");
    }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Please log in first.</p>;

  return (
    <div className="max-w-7xl mx-auto p-8 mt-10 grid grid-cols-1 lg:grid-cols-2 gap-8 text-black">
      
      {/* Left Column */}
      <div>
        <h1 className="text-2xl font-bold mb-6 bg-white p-4 rounded shadow">Segment Builder</h1>
        
        <div className={`mb-8 p-6 rounded shadow ${editingId ? 'bg-yellow-50 border-2 border-yellow-400' : 'bg-white'}`}>
          <div className="flex justify-between items-center border-b pb-2 mb-4">
            <h2 className="text-lg font-semibold">
                {editingId ? "Edit Segment" : "Create Dynamic Segment"}
            </h2>
            {editingId && (
                <button onClick={cancelEdit} className="text-sm font-bold text-gray-500 hover:text-gray-800">
                    Cancel Edit
                </button>
            )}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Segment Name</label>
              <input type="text" required value={name} onChange={(e) => setName(e.target.value)} className="w-full p-2 border rounded" placeholder="e.g., Active Berlin Users > 25" />
            </div>
            
            <div className="border p-4 bg-gray-50 rounded space-y-3">
                <div className="flex justify-between items-center border-b pb-2">
                    <span className="font-semibold text-gray-700">Conditions (AND)</span>
                    <button type="button" onClick={handleAddRule} className="text-sm bg-blue-100 text-blue-700 px-3 py-1 rounded hover:bg-blue-200 font-bold">
                        + Add Condition
                    </button>
                </div>

                {rules.map((rule, index) => (
                    <div key={index} className="flex gap-2 items-center bg-white p-2 border rounded shadow-sm">
                        <select 
                            value={rule.column} 
                            onChange={(e) => handleRuleChange(index, "column", e.target.value)} 
                            className="flex-1 p-2 border rounded bg-white text-sm"
                        >
                            {availableColumns.map((col) => (
                                <option key={col} value={col}>{col}</option>
                            ))}
                        </select>
                        
                        <select 
                            value={rule.operator} 
                            onChange={(e) => handleRuleChange(index, "operator", e.target.value)} 
                            className="w-20 p-2 border rounded bg-white font-mono text-center text-sm"
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
                            className="flex-1 p-2 border rounded text-sm" 
                            placeholder="Value" 
                        />
                        
                        {rules.length > 1 && (
                            <button 
                                type="button" 
                                onClick={() => handleRemoveRule(index)}
                                className="text-red-500 hover:text-red-700 px-2 font-bold"
                            >
                                ✕
                            </button>
                        )}
                    </div>
                ))}
            </div>

            <button type="submit" className={`w-full py-3 font-bold rounded shadow text-white ${editingId ? 'bg-yellow-600 hover:bg-yellow-700' : 'bg-blue-600 hover:bg-blue-700'}`}>
                {editingId ? "Update Segment" : "Save Multi-Condition Segment"}
            </button>
          </form>
          {message && <p className="mt-3 text-sm font-medium text-center">{message}</p>}
        </div>

        <div className="bg-white rounded shadow overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-gray-100 border-b">
              <tr>
                <th className="p-3">Name</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {segments.length === 0 ? (
                <tr><td colSpan="2" className="p-4 text-center text-gray-500">No segments created yet.</td></tr>
              ) : (
                segments.map(seg => (
                  <tr key={seg.id} className="border-b hover:bg-gray-50">
                    <td className="p-3 font-medium">
                      {seg.name} <br/>
                      <div className="text-xs text-gray-400 font-mono mt-1 space-y-1">
                        {Array.isArray(JSON.parse(seg.rules)) 
                            ? JSON.parse(seg.rules).map((r, i) => (
                                <div key={i}>AND {r.column} {r.operator} {r.value}</div>
                              ))
                            : <span>Legacy single rule format</span>
                        }
                      </div>
                    </td>
                    <td className="p-3 text-right space-x-3 align-top">
                      <button onClick={() => handleRunRule(seg)} className="text-green-600 font-bold hover:underline">Run</button>
                      <button onClick={() => handleEdit(seg)} className="text-blue-600 font-bold hover:underline">Edit</button>
                      <button onClick={() => handleDelete(seg.id)} className="text-red-500 hover:underline">Delete</button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Right Column */}
      <div>
        <h1 className="text-2xl font-bold mb-6 bg-white p-4 rounded shadow">Engine Results</h1>
        <div className="bg-white rounded shadow p-6 min-h-[400px]">
          {!evaluatedSubscribers ? (
            <div className="h-full flex flex-col items-center justify-center text-gray-400 mt-20">
              <p>Click "Run" to query the database.</p>
            </div>
          ) : (
             <div>
              <h2 className="text-lg font-semibold mb-2 text-green-700">
                Matches for: {viewingSegmentName}
              </h2>
              <p className="text-sm text-gray-500 mb-4">Found {evaluatedSubscribers.length} subscribers.</p>
              
              <div className="max-h-[600px] overflow-y-auto border rounded">
                <table className="w-full text-sm text-left">
                  <thead className="bg-gray-50 border-b sticky top-0">
                    <tr>
                      <th className="p-2">Email</th>
                      <th className="p-2">Custom Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {evaluatedSubscribers.length === 0 ? (
                      <tr><td colSpan="2" className="p-4 text-center text-gray-500">No matching subscribers.</td></tr>
                    ) : (
                      evaluatedSubscribers.map(sub => (
                        <tr key={sub.id} className="border-b">
                          <td className="p-2 font-medium">{sub.email}</td>
                          <td className="p-2 text-xs text-gray-600">
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
  );
}

export default function SegmentsPage() {
  return (
    <SessionProvider>
      <SegmentsContent />
    </SessionProvider>
  );
}