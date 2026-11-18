import { createContext, useContext } from 'react';

export const ParishRevisionContext = createContext(0);
export function useParishRevision() {
  return useContext(ParishRevisionContext);
}
