"use client";
import { useSession } from "next-auth/react";
import { useState, useEffect } from "react";
import Link from "next/link";

export default function DashboardPage() {
  const { data: session } = useSession();
  
  const [data, setData] = useState({
    totalSubs: 0,
    totalCamps: 0,
    recentCamps: [],
    recentSubs: [],
    topSubs: [],
    totalOpens: 0,
    totalClicks: 0,
    openRate: "0%",
    clickRate: "0%",
    loading: true
  });

  useEffect(() => {
    if (!session?.user?.email) return;

    const fetchRealData = async () => {
      try {
        const headers = { 'X-User-Email': session.user.email };
        
        // 1. Fetch all raw data
        const [subsRes, campsRes, topSubsRes] = await Promise.all([
          fetch('http://localhost:8080/api/subscribers', { headers }),
          fetch('http://localhost:8080/api/campaigns', { headers }),
          fetch('http://localhost:8080/api/subscribers/top-engaged', { headers })
        ]);
        
        const subs = await subsRes.json();
        const camps = await campsRes.json();
        const topSubs = topSubsRes.ok ? await topSubsRes.json() : [];

        // 2. Fetch stats for all campaigns to calculate real global averages
        const statsPromises = camps.map(c => 
          fetch(`http://localhost:8080/api/campaigns/${c.id}/stats`, { headers })
            .then(res => res.ok ? res.json() : { totalSent: 0, uniqueOpens: 0, uniqueClicks: 0 })
        );
        const allStats = await Promise.all(statsPromises);

        let totalSent = 0;
        let totalOpens = 0;
        let totalClicks = 0;

        // Combine stats with campaign data for the table
        const campaignsWithStats = camps.map((camp, index) => {
          const s = allStats[index];
          totalSent += s.totalSent || 0;
          totalOpens += s.uniqueOpens || 0;
          totalClicks += s.uniqueClicks || 0;
          
          return {
            ...camp,
            opens: s.totalSent > 0 ? Math.round((s.uniqueOpens / s.totalSent) * 100) + '%' : '0%',
            clicks: s.uniqueOpens > 0 ? Math.round((s.uniqueClicks / s.uniqueOpens) * 100) + '%' : '0%'
          };
        });

        const openRateCalc = totalSent > 0 ? Math.round((totalOpens / totalSent) * 100) : 0;
        const clickRateCalc = totalOpens > 0 ? Math.round((totalClicks / totalOpens) * 100) : 0;

        setData({
          totalSubs: subs.length || 0,
          totalCamps: camps.length || 0,
          recentCamps: campaignsWithStats.slice(0, 4),
          recentSubs: subs.slice(0, 3), // Replaced mock "Top Engaged" with real Recent Subscribers
          topSubs: topSubs,
          totalOpens: totalOpens,
          totalClicks: totalClicks,
          openRate: `${openRateCalc}%`,
          clickRate: `${clickRateCalc}%`,
          loading: false
        });

      } catch (error) {
        console.error("Error fetching real dashboard data:", error);
        setData(prev => ({ ...prev, loading: false }));
      }
    };

    fetchRealData();
  }, [session]);

  const handleDelete = async (id) => {
    if (window.confirm("Êtes-vous sûr de vouloir supprimer cette campagne définitivement ?")) {
      try {
        const res = await fetch(`http://localhost:8080/api/campaigns/${id}`, {
          method: 'DELETE',
          headers: { 'X-User-Email': session?.user?.email }
        });
        if (res.ok) {
          setData(prev => ({
            ...prev,
            recentCamps: prev.recentCamps.filter(c => c.id !== id),
            totalCamps: prev.totalCamps - 1
          }));
        } else {
          const err = await res.json();
          alert(`Erreur: ${err.message}`);
        }
      } catch (error) {
        console.error(error);
      }
    }
  };

  if (data.loading) {
    return <div className="flex items-center justify-center h-full text-gray-500">Chargement des données en temps réel...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto pb-12">
      {/* Header */}
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-black text-gray-900 tracking-tight">Vue d'ensemble</h1>
          <p className="text-gray-500 mt-1">Content de vous revoir, {session?.user?.name?.split(' ')[0] || 'M23'}. Voici vos statistiques réelles.</p>
        </div>
        <Link href="/campaigns" className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg font-medium text-sm shadow-sm transition flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path></svg>
          Nouvelle Campagne
        </Link>
      </div>

      {/* Top KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <h3 className="text-sm font-semibold text-gray-500">Abonnés Totaux</h3>
            <span className="p-2 bg-blue-50 text-blue-600 rounded-lg"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg></span>
          </div>
          <div className="mt-4">
            <p className="text-3xl font-black text-gray-900">{data.totalSubs}</p>
          </div>
        </div>
        
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <h3 className="text-sm font-semibold text-gray-500">Campagnes</h3>
            <span className="p-2 bg-indigo-50 text-indigo-600 rounded-lg"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path></svg></span>
          </div>
          <div className="mt-4">
            <p className="text-3xl font-black text-gray-900">{data.totalCamps}</p>
          </div>
        </div>
        
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <h3 className="text-sm font-semibold text-gray-500">Ouvertures Totales</h3>
            <span className="p-2 bg-sky-50 text-sky-600 rounded-lg"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg></span>
          </div>
          <div className="mt-4 flex items-end justify-between">
            <p className="text-3xl font-black text-gray-900">{data.totalOpens}</p>
            <p className="text-sm text-sky-600 font-bold bg-sky-50 px-2 py-1 rounded-md">{data.openRate} Taux</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <h3 className="text-sm font-semibold text-gray-500">Clics Totaux</h3>
            <span className="p-2 bg-emerald-50 text-emerald-600 rounded-lg"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"></path></svg></span>
          </div>
          <div className="mt-4 flex items-end justify-between">
            <p className="text-3xl font-black text-gray-900">{data.totalClicks}</p>
            <p className="text-sm text-emerald-600 font-bold bg-emerald-50 px-2 py-1 rounded-md">{data.clickRate} Taux</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Table: Recent Campaigns */}
        <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="p-6 border-b border-gray-100 flex justify-between items-center">
            <h2 className="text-lg font-bold text-gray-900">Campagnes Récentes</h2>
            <Link href="/campaigns" className="text-sm text-blue-600 font-medium hover:underline">Voir tout</Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                  <th className="p-4 font-semibold">Nom de la Campagne</th>
                  <th className="p-4 font-semibold">Statut</th>
                  <th className="p-4 font-semibold text-right">Ouvertures</th>
                  <th className="p-4 font-semibold text-right">Clics</th>
                  <th className="p-4 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {data.recentCamps.length === 0 ? (
                   <tr><td colSpan="5" className="p-4 text-center text-gray-500">Aucune campagne trouvée.</td></tr>
                ) : (
                  data.recentCamps.map((camp) => (
                    <tr key={camp.id} className="hover:bg-gray-50 transition">
                      <td className="p-4 font-bold text-gray-900">{camp.name}</td>
                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                          camp.status === 'SENT' ? 'bg-green-100 text-green-700' : 
                          camp.status === 'SENDING' ? 'bg-blue-100 text-blue-700' : 
                          'bg-gray-100 text-gray-600'
                        }`}>
                          {camp.status}
                        </span>
                      </td>
                      <td className="p-4 text-right font-semibold text-gray-900">{camp.opens}</td>
                      <td className="p-4 text-right font-semibold text-green-600">{camp.clicks}</td>
                      <td className="p-4 text-right flex justify-end">
                        <button onClick={() => handleDelete(camp.id)} className="text-gray-400 hover:text-red-600 transition p-1 rounded-md hover:bg-red-50">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Sidebar Panel: Abonnés VIP */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 flex flex-col">
          <div className="p-6 border-b border-gray-100">
            <h2 className="text-lg font-bold text-gray-900">Abonnés VIP (Top Engagés)</h2>
            <p className="text-xs text-gray-500 mt-1">Vos abonnés les plus actifs et engagés</p>
          </div>
          <div className="p-6 flex-1">
            {data.topSubs.length === 0 ? (
              <div className="text-sm text-gray-500 text-center py-4">Aucun abonné VIP trouvé.</div>
            ) : (
              <div className="space-y-4">
                {data.topSubs.map((sub, i) => (
                  <div key={i} className="flex justify-between items-center text-sm border-b border-gray-50 pb-3 last:border-0">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-600 flex items-center justify-center font-bold">
                        {sub.email.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <div className="text-gray-900 font-medium truncate max-w-[150px]">{sub.email}</div>
                        <div className="text-[10px] text-purple-600 bg-purple-50 px-2 py-0.5 mt-1 rounded-full inline-block font-bold">
                          {sub.interactions} Interactions
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
            <div className="mt-6 pt-4 border-t border-gray-100 text-center">
               <Link href="/subscribers" className="text-sm text-blue-600 font-medium hover:underline">Gérer tous les abonnés</Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}