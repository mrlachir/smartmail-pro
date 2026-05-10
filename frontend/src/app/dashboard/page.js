"use client";
import { useState, useEffect } from 'react';

export default function DashboardOverview() {
  const [stats, setStats] = useState({ sent: 0, opens: 0, clicks: 0 });
  const userEmail = "911lachi@gmail.com"; // Replace with your actual auth state later

  useEffect(() => {
    // Replace '24' with an actual recent Campaign ID to show live data
    fetch(`http://localhost:8080/api/campaigns/24/stats`, {
        headers: { "X-User-Email": userEmail }
    })
    .then(res => res.json())
    .then(data => {
        setStats({
            sent: data.totalSent || 0,
            opens: data.uniqueOpens || 0,
            clicks: data.uniqueClicks || 0
        });
    })
    .catch(err => console.error("Telemetry Offline", err));
  }, []);

  return (
    <div className="space-y-6">
      
      {/* TELEMETRY KPI CARDS */}
      <section>
        <h2 className="text-lg font-bold text-gray-700 mb-4 uppercase tracking-wide">Live Telemetry</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
            <p className="text-sm text-gray-500 uppercase font-semibold">Total Dispatched</p>
            <p className="text-4xl font-black text-gray-800 mt-2">{stats.sent}</p>
          </div>

          <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm border-t-4 border-t-blue-500">
            <p className="text-sm text-gray-500 uppercase font-semibold">Verified Opens</p>
            <p className="text-4xl font-black text-blue-600 mt-2">{stats.opens}</p>
          </div>

          <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm border-t-4 border-t-green-500">
            <p className="text-sm text-gray-500 uppercase font-semibold">Link Clicks</p>
            <p className="text-4xl font-black text-green-600 mt-2">{stats.clicks}</p>
          </div>

        </div>
      </section>

      {/* API STATUS SECTION */}
      <section className="mt-10">
        <h2 className="text-lg font-bold text-gray-700 mb-4 uppercase tracking-wide">System Endpoints</h2>
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <table className="min-w-full text-left text-sm text-gray-600">
            <thead className="bg-gray-50 border-b border-gray-200 text-gray-700 uppercase">
              <tr>
                <th className="px-6 py-4 font-semibold">Service</th>
                <th className="px-6 py-4 font-semibold">Endpoint</th>
                <th className="px-6 py-4 font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 font-mono text-xs">
              <tr>
                <td className="px-6 py-4 font-sans font-medium text-gray-800">Telemetry: Clicks</td>
                <td className="px-6 py-4">GET /api/track/click/&#123;cid&#125;/&#123;sid&#125;</td>
                <td className="px-6 py-4"><span className="bg-green-100 text-green-700 px-2 py-1 rounded">200 OK</span></td>
              </tr>
              <tr>
                <td className="px-6 py-4 font-sans font-medium text-gray-800">Campaign Dispatcher</td>
                <td className="px-6 py-4">POST /api/campaigns/launch</td>
                <td className="px-6 py-4"><span className="bg-green-100 text-green-700 px-2 py-1 rounded">200 OK</span></td>
              </tr>
              <tr>
                <td className="px-6 py-4 font-sans font-medium text-gray-800">AI Segment Builder</td>
                <td className="px-6 py-4">POST /api/segments/ai-generate</td>
                <td className="px-6 py-4"><span className="bg-green-100 text-green-700 px-2 py-1 rounded">200 OK</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

    </div>
  );
}