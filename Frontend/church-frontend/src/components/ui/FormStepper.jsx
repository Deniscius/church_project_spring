import React from 'react';

/**
 * Indicateur d'étapes (cercles 1, 2, 3…) pour les formulaires guidés.
 */
export default function FormStepper({ steps, currentStep, onStepClick }) {
  return (
    <nav className="form-stepper" aria-label="Étapes du formulaire">
      <ol className="form-stepper-list">
        {steps.map((step, index) => {
          const number = index + 1;
          const status =
            number < currentStep ? 'done' : number === currentStep ? 'current' : 'todo';
          const clickable = typeof onStepClick === 'function' && number < currentStep;
          return (
            <li key={step.id || number} className={`form-stepper-item is-${status}`}>
              {clickable ? (
                <button
                  type="button"
                  className="form-stepper-button"
                  onClick={() => onStepClick(number)}
                  aria-current={status === 'current' ? 'step' : undefined}
                >
                  <span className="form-stepper-circle" aria-hidden="true">
                    {status === 'done' ? '✓' : number}
                  </span>
                  <span className="form-stepper-label">{step.label}</span>
                </button>
              ) : (
                <div
                  className="form-stepper-button"
                  aria-current={status === 'current' ? 'step' : undefined}
                >
                  <span className="form-stepper-circle" aria-hidden="true">
                    {status === 'done' ? '✓' : number}
                  </span>
                  <span className="form-stepper-label">{step.label}</span>
                </div>
              )}
              {index < steps.length - 1 ? (
                <span className="form-stepper-connector" aria-hidden="true" />
              ) : null}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
