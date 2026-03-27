import React from 'react';
import { getBadgeClass } from '../../utils/statusMapper';

export default function AppBadge({ value }) {
  return <span className={getBadgeClass(value)}>{value || '—'}</span>;
}
