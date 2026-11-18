import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTenant } from '../../hooks/useTenant';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import { ParishRevisionContext } from '../../hooks/useParishRevision';
import { getApiBaseUrl } from '../../config/apiBaseUrl';
import { apiClient } from '../../services/http/apiClient';

/** One stream per active parish, shared by all mounted admin widgets. */
export default function ParishChangesProvider({ children }) {
  const { activeParish } = useTenant();
  const { isAuthenticated, user } = useAuth();
  const { has } = usePermissions();
  const canRead = has('demand:read');
  const parishId = activeParish?.id;
  const userId = user?.id;
  const queryClient = useQueryClient();
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    if (!isAuthenticated || !parishId || !canRead) return undefined;
    let stopped = false;
    let stream;
    let retry;
    let known;
    let polling = false;
    const abort = new AbortController();
    const accept = (value) => {
      if (stopped || !value || value === known) return;
      known = value;
      setRevision((previous) => previous + 1);
      // Also reconcile once on subscribe: a write may have raced with initial page loading.
      queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes(parishId) });
    };
    const fallback = async () => {
      if (stopped || polling || document.visibilityState === 'hidden' || navigator.onLine === false) return;
      polling = true;
      try {
        const data = await apiClient(`/desktop/paroisses/${parishId}/revision`, {}, { auth: true, signal: abort.signal });
        accept(data?.revision);
      } catch {
        // Keep the current screen and its draft through temporary outages.
      } finally { polling = false; }
    };
    const connect = () => {
      window.clearTimeout(retry);
      if (stopped || document.visibilityState === 'hidden' || navigator.onLine === false) return;
      if (typeof EventSource === 'undefined') { fallback(); return; }
      stream?.close();
      stream = new EventSource(`${getApiBaseUrl()}/desktop/paroisses/${parishId}/events`, { withCredentials: true });
      stream.addEventListener('revision', (event) => accept(event.data));
      stream.onerror = () => {
        stream?.close(); stream = null;
        fallback();
        retry = window.setTimeout(connect, 30_000);
      };
    };
    const resume = () => {
      stream?.close(); stream = null;
      window.clearTimeout(retry);
      connect();
    };
    connect();
    const timer = window.setInterval(() => { if (!stream) fallback(); }, 60_000);
    document.addEventListener('visibilitychange', resume);
    window.addEventListener('online', resume);
    window.addEventListener('offline', resume);
    return () => {
      stopped = true; abort.abort(); stream?.close();
      window.clearTimeout(retry); window.clearInterval(timer);
      document.removeEventListener('visibilitychange', resume);
      window.removeEventListener('online', resume);
      window.removeEventListener('offline', resume);
    };
  }, [isAuthenticated, userId, parishId, canRead, queryClient]);

  return <ParishRevisionContext.Provider value={revision}>{children}</ParishRevisionContext.Provider>;
}
