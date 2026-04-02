"use client";
import { useState, useEffect } from "react";
import { useSession, SessionProvider } from "next-auth/react";

function SubscribersContent() {
  const { data: session, status } = useSession();
  const [file, setFile] = useState(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [subscribers, setSubscribers] = useState([]);

  // Fetch subscribers on load
  const fetchSubscribers = async () => {
    try {
      const res = await fetch("http://localhost:8080/api/subscribers");
      if (res.ok) {
        const data = await res.json();
        setSubscribers(data);
      }
    } catch (error) {
      console.error("Failed to fetch subscribers", error);
    }
  };

  useEffect(() => {
    fetchSubscribers();
  }, []);

  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      setMessage("❌ Please select a file first.");
      return;
    }

    setLoading(true);
    setMessage("Uploading...");

    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await fetch("http://localhost:8080/api/subscribers/import", {
        method: "POST",
        body: formData,
      });

      const data = await res.json();

      if (res.ok) {
        setMessage("✅ " + data.message);
        setFile(null);
        document.getElementById("csvFileInput").value = "";
        fetchSubscribers(); // Refresh the table after upload
      } else {
        setMessage("❌ " + data.message);
      }
    } catch (error) {
      setMessage("❌ Error connecting to the server.");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm("Are you sure you want to delete this subscriber?")) return;
    
    try {
      const res = await fetch(`http://localhost:8080/api/subscribers/${id}`, {
        method: "DELETE",
      });
      
      if (res.ok) {
        fetchSubscribers(); // Refresh the table after deletion
      } else {
        alert("Failed to delete subscriber.");
      }
    } catch (error) {
      console.error("Error deleting", error);
    }
  };

  if (status === "loading") return <p className="p-8">Loading...</p>;
  if (!session) return <p className="p-8 text-red-500">Access Denied. Please log in first.</p>;

  return (
    <div className="max-w-4xl mx-auto p-8 mt-10 bg-white rounded shadow text-black">
      <h1 className="text-2xl font-bold mb-6">Subscriber Management</h1>
      
      {/* Upload Section */}
      <div className="mb-8 p-6 border-2 border-dashed border-gray-300 rounded text-center bg-gray-50">
        <h2 className="text-lg font-semibold mb-2">Import CSV Contacts</h2>
        <p className="text-sm text-gray-500 mb-4">Format Requirement: Email, FirstName, LastName</p>
        
        <form onSubmit={handleUpload} className="space-y-4 max-w-md mx-auto">
          <input 
            id="csvFileInput"
            type="file" 
            accept=".csv" 
            onChange={handleFileChange}
            className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
          />
          <button 
            type="submit" 
            disabled={loading}
            className="w-full py-2 px-4 bg-blue-600 text-white font-bold rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? "Importing..." : "Upload & Import"}
          </button>
        </form>
        {message && <p className="mt-4 font-medium">{message}</p>}
      </div>

      {/* Table Section */}
      <h2 className="text-xl font-bold mb-4">Subscriber List ({subscribers.length})</h2>
      <div className="overflow-x-auto">
        <table className="w-full border-collapse border border-gray-200 text-sm">
          <thead className="bg-gray-100">
            <tr>
              <th className="border border-gray-200 p-2 text-left">Email</th>
              <th className="border border-gray-200 p-2 text-left">First Name</th>
              <th className="border border-gray-200 p-2 text-left">Last Name</th>
              <th className="border border-gray-200 p-2 text-left">Status</th>
              <th className="border border-gray-200 p-2 text-center">Action</th>
            </tr>
          </thead>
          <tbody>
            {subscribers.length === 0 ? (
              <tr>
                <td colSpan="5" className="border border-gray-200 p-4 text-center text-gray-500">
                  No subscribers found. Upload a CSV to get started.
                </td>
              </tr>
            ) : (
              subscribers.map((sub) => (
                <tr key={sub.id} className="hover:bg-gray-50">
                  <td className="border border-gray-200 p-2">{sub.email}</td>
                  <td className="border border-gray-200 p-2">{sub.firstName || "-"}</td>
                  <td className="border border-gray-200 p-2">{sub.lastName || "-"}</td>
                  <td className="border border-gray-200 p-2">
                    <span className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium">
                      {sub.status}
                    </span>
                  </td>
                  <td className="border border-gray-200 p-2 text-center">
                    <button 
                      onClick={() => handleDelete(sub.id)}
                      className="text-red-600 hover:text-red-800 font-medium cursor-pointer"
                    >
                      Delete
                    </button>
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

export default function SubscribersPage() {
  return (
    <SessionProvider>
      <SubscribersContent />
    </SessionProvider>
  );
}