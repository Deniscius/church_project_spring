import React from 'react';

const paths = {
  dashboard: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  requests: <><path d="M9 5h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H9" /><path d="M5 3h8v4H5z" /><path d="M3 5h2v16H3z" /><path d="M9 12h8M9 16h6" /></>,
  celebrations: <><path d="M4 4h16v16H4z" /><path d="M8 8h8M8 12h8M8 16h5" /><path d="M16 2v4M8 2v4" /></>,
  payments: <><rect x="2.5" y="5" width="19" height="14" rx="2" /><path d="M2.5 10h19M7 15h3" /></>,
  invoices: <><path d="M6 2h9l4 4v16l-3-2-4 2-4-2-3 2V3a1 1 0 0 1 1-1z" /><path d="M14 2v5h5M8 11h8M8 15h6" /></>,
  schedules: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M8 3v4M16 3v4M3 10h18" /><path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01" /></>,
  types: <><path d="M4 4h16v5H4zM4 15h7v5H4zM15 15h5v5h-5z" /><path d="M12 9v3M7.5 12h10M7.5 12v3M17.5 12v3" /></>,
  pricing: <><path d="M20 13 13 20l-9-9V4h7z" /><circle cx="8.5" cy="8.5" r="1.2" /></>,
  team: <><circle cx="9" cy="8" r="4" /><path d="M2 21a7 7 0 0 1 14 0M16 4a4 4 0 0 1 0 8M18 14a6 6 0 0 1 4 6" /></>,
  profile: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
  parishes: <><path d="M12 2v20M8 6h8M5 10h14l-2 12H7z" /><path d="M9 14h6M9 18h6" /></>,
  users: <><circle cx="8" cy="8" r="4" /><circle cx="17" cy="9" r="3" /><path d="M1.5 21a6.5 6.5 0 0 1 13 0M14 15a5 5 0 0 1 8 4" /></>,
  access: <><rect x="3" y="11" width="18" height="10" rx="2" /><path d="M7 11V8a5 5 0 0 1 10 0v3M12 15v2" /></>,
  deaneries: <><path d="M3 21h18M5 21V8l7-5 7 5v13M9 21v-6h6v6" /></>,
  paymentTypes: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 9h18M7 14h4M16 14h1" /></>,
  logout: <><path d="M10 17l5-5-5-5M15 12H3" /><path d="M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5" /></>,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  close: <path d="m6 6 12 12M18 6 6 18" />,
  back: <path d="m15 18-6-6 6-6" />,
};

export default function AppIcon({ name, size = 20, className = '' }) {
  return (
    <svg
      className={`app-icon ${className}`.trim()}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {paths[name] || paths.dashboard}
    </svg>
  );
}
