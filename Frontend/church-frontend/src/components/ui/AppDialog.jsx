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
  const busyRef = useRef(busy);
  const cancelRef = useRef(onCancel);
  const resolvedVariant = danger ? 'danger' : variant;

  useEffect(() => {
    busyRef.current = busy;
    cancelRef.current = onCancel;
  }, [busy, onCancel]);

  useEffect(() => {
    if (!open) return undefined;
    const previous = document.activeElement;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const focusableSelector =
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
    const focusId = window.requestAnimationFrame(() => {
      const node = dialogRef.current;
      node?.querySelector(focusableSelector)?.focus();
      if (node && !node.contains(document.activeElement)) node.focus();
    });

    const onKey = (event) => {
      if (event.key === 'Escape' && !busyRef.current) {
        cancelRef.current?.();
        return;
      }
      if (event.key !== 'Tab') return;

      const focusable = [...(dialogRef.current?.querySelectorAll(focusableSelector) || [])];
      if (!focusable.length) {
        event.preventDefault();
        dialogRef.current?.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', onKey);
    return () => {
      window.cancelAnimationFrame(focusId);
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prevOverflow;
      if (previous instanceof HTMLElement) previous.focus();
    };
  }, [open]);

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
        tabIndex={-1}
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
