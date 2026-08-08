import React, { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import AppButton from './AppButton';

/**
 * Boîte de dialogue accessible (role=dialog, aria-modal).
 * Variantes : default | success | danger | info
 */
export default function AppDialog({
  open,
  title,
  children,
  confirmLabel = 'Confirmer',
  cancelLabel = 'Annuler',
  onConfirm,
  onCancel,
  danger = false,
  busy = false,
  hideCancel = false,
  hideConfirm = false,
  variant = 'default',
  size = 'md',
  promptLabel = null,
  promptValue = '',
  onPromptChange,
  promptPlaceholder = '',
  promptRows = 3,
}) {
  const titleId = useId();
  const descId = useId();
  const promptId = useId();
  const dialogRef = useRef(null);
  const resolvedVariant = danger ? 'danger' : variant;

  useEffect(() => {
    if (!open) return undefined;
    const previous = document.activeElement;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const node = dialogRef.current;
    const focusable = node?.querySelector(
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled])'
    );
    focusable?.focus();

    const onKey = (event) => {
      if (event.key === 'Escape' && !busy) onCancel?.();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prevOverflow;
      if (previous instanceof HTMLElement) previous.focus();
    };
  }, [open, busy, onCancel]);

  if (!open) return null;

  const panel = (
    <div
      className="dialog-backdrop"
      onClick={() => {
        if (!busy) onCancel?.();
      }}
    >
      <div
        ref={dialogRef}
        className={`dialog-panel dialog-panel--${size} dialog-panel--${resolvedVariant}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descId}
        onClick={(e) => e.stopPropagation()}
      >
        {resolvedVariant === 'success' ? (
          <div className="dialog-icon dialog-icon--success" aria-hidden="true">
            ✓
          </div>
        ) : null}
        <h2 id={titleId} className="dialog-title">
          {title}
        </h2>
        <div id={descId} className="dialog-body">
          {children}
          {promptLabel != null ? (
            <div className="form-field" style={{ marginTop: children ? 12 : 0 }}>
              <label htmlFor={promptId}>{promptLabel}</label>
              <textarea
                id={promptId}
                className="input"
                rows={promptRows}
                value={promptValue}
                placeholder={promptPlaceholder}
                disabled={busy}
                onChange={(e) => onPromptChange?.(e.target.value)}
              />
            </div>
          ) : null}
        </div>
        {hideCancel && hideConfirm ? null : (
          <div className="dialog-actions">
            {!hideCancel ? (
              <AppButton variant="secondary" onClick={onCancel} disabled={busy}>
                {cancelLabel}
              </AppButton>
            ) : null}
            {!hideConfirm ? (
              <AppButton
                variant={resolvedVariant === 'danger' ? 'danger' : 'primary'}
                onClick={onConfirm}
                disabled={busy}
                loading={busy}
              >
                {confirmLabel}
              </AppButton>
            ) : null}
          </div>
        )}
      </div>
    </div>
  );

  return createPortal(panel, document.body);
}
