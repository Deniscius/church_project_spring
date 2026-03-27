import { useState } from 'react';

export function useFilters(initialValue = {}) {
  const [filters, setFilters] = useState(initialValue);

  const updateFilter = (name, value) => {
    setFilters((previous) => ({ ...previous, [name]: value }));
  };

  const resetFilters = () => setFilters(initialValue);

  return {
    filters,
    updateFilter,
    resetFilters,
  };
}
