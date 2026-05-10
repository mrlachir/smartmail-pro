import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function Sidebar() {
  const pathname = usePathname();

  const navItems = [
    { name: 'Dashboard', path: '/' },
    { name: 'Campaigns', path: '/campaigns' },
    { name: 'Audiences', path: '/segments' },
    { name: 'Templates', path: '/templates' },
  ];

  return (
    <div className="w-64 h-screen bg-gray-900 text-white flex flex-col fixed left-0 top-0">
      <div className="p-6 text-2xl font-bold border-b border-gray-800">
        SmartMail Pro
      </div>
      <nav className="flex-1 p-4 space-y-2">
        {navItems.map((item) => {
          const isActive = pathname === item.path || pathname?.startsWith(item.path + '/');
          return (
            <Link
              key={item.name}
              href={item.path}
              className={`block px-4 py-3 rounded-lg transition-colors ${
                isActive 
                  ? 'bg-blue-600 text-white font-medium' 
                  : 'text-gray-400 hover:bg-gray-800 hover:text-white'
              }`}
            >
              {item.name}
            </Link>
          );
        })}
      </nav>
      <div className="p-4 border-t border-gray-800 text-xs text-gray-500 text-center">
        Logged in as M23
      </div>
    </div>
  );
}