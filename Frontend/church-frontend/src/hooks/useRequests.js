import { useEffect, useState } from 'react';
import { useTenant } from './useTenant';
import { requestService } from '../services/request.service';
import { mapDemandeToRequestRow } from '../utils/apiMappers';

/** Données demandes pour la paroisse active (réutilisable par des écrans futurs). */
export function useRequests() {
  const { activeParish } = useTenant();
  const [data, setData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!activeParish?.id) {
      setData([]);
      setIsLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        setIsLoading(true);
        setError(null);
        const raw = await requestService.getByParish(activeParish.id);
        if (!cancelled) setData((raw || []).map(mapDemandeToRequestRow));
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erreur');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeParish?.id]);

  return { data, isLoading, error };
}
