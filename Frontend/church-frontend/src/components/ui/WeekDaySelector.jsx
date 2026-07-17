import React from 'react';
import { WEEK_DAY_LABELS, WEEK_DAYS } from '../../constants/enums';

export default function WeekDaySelector({
  value = [],
  onChange,
  disabled = false,
  availableDays = null,
  id = 'week-day-selector',
}) {
  const selected = new Set(value);
  const allowed = availableDays?.length ? new Set(availableDays) : null;

  const toggle = (day) => {
    if (disabled || (allowed && !allowed.has(day))) return;
    const next = new Set(selected);
    if (next.has(day)) next.delete(day);
    else next.add(day);
    onChange(WEEK_DAYS.filter((item) => next.has(item)));
  };

  return (
    <div className="week-day-selector" id={id} role="group" aria-label="Jours de célébration autorisés">
      {WEEK_DAYS.map((day) => {
        const active = selected.has(day);
        const isDisabled = disabled || (allowed != null && !allowed.has(day));
        return (
          <button
            key={day}
            type="button"
            className={`week-day-chip${active ? ' is-active' : ''}${isDisabled && !active ? ' is-disabled' : ''}`}
            aria-pressed={active}
            disabled={isDisabled}
            onClick={() => toggle(day)}
          >
            {WEEK_DAY_LABELS[day] || day}
          </button>
        );
      })}
    </div>
  );
}
