import React from 'react';
import { getBadgeClass } from '../../utils/statusMapper';

export default function AppBadge({ value, label }) {
  return <span className={getBadgeClass(value)}>{label || value || '—'}</span>;
}
