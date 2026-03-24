"use client";
import { signIn, SessionProvider, useSession } from "next-auth/react";

function LoginContent() {
  const { data: session } = useSession();

  if (session) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen">
        <h1 className="text-2xl font-bold">Bienvenue, {session.user.name}</h1>
        <p>{session.user.email}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <button 
        onClick={() => signIn('google')} 
        className="px-6 py-3 bg-blue-600 text-white font-bold rounded"
      >
        Continuer avec Google SSO
      </button>
    </div>
  );
}

export default function Page() {
  return (
    <SessionProvider>
      <LoginContent />
    </SessionProvider>
  );
}