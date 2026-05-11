"use client";
import { useSession, signIn, signOut } from "next-auth/react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import Sidebar from "./Sidebar";

export default function AppShell({ children }) {
  const { data: session, status } = useSession();

  if (status === "loading") {
    return <div className="min-h-screen flex items-center justify-center">Chargement...</div>;
  }

  // 🔴 If not logged in, show your exact SSO button
  if (!session) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center mb-8">
            <h1 className="text-3xl font-bold mb-2">SmartMail Pro</h1>
            <p className="text-gray-500">Connectez-vous pour accéder au tableau de bord</p>
        </div>
        <button 
          onClick={() => signIn('google')} 
          className="px-6 py-3 bg-blue-600 hover:bg-blue-700 transition text-white font-bold rounded shadow"
        >
          Continuer avec Google SSO
        </button>
      </div>
    );
  }

  // 🟢 If logged in, show Sidebar + Content
  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <Sidebar />

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto p-8">
        {children}
      </main>
    </div>
  );
}
