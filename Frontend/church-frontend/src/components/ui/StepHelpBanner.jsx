import React from 'react';
import HelpTip from './HelpTip';

/**
 * Bandeau d’aide pour une étape de parcours (demande, inscription…).
 */
export default function StepHelpBanner({ title = 'Conseil', text }) {
  if (!text) return null;
  return (
    <aside className="step-help-banner" role="note">
      <div className="step-help-banner-head">
        <HelpTip text={text} label={title} />
        <strong>{title}</strong>
      </div>
      <p>{text}</p>
    </aside>
  );
}
