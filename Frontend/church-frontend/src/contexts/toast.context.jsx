/* eslint-disable react-refresh/only-export-components */
import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
} from 'react';
import { createPortal } from 'react-dom';

const ToastContext = createContext(null);

let toastSeq = 0;

/**
 * Bulles flottantes (MessageBox) pour succès / erreur / info.
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef(new Map());

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
    const timer = timersRef.current.get(id);
    if (timer) {
      window.clearTimeout(timer);
      timersRef.current.delete(id);
    }
  }, []);

  const push = useCallback((variant, message, options = {}) => {
    const text = String(message || '').trim();
    if (!text) return null;
    const id = ++toastSeq;
    const duration = options.duration ?? (variant === 'danger' ? 6000 : 4000);
    setToasts((prev) => [...prev.slice(-4), { id, variant, message: text }]);
    if (duration > 0) {
      const timer = window.setTimeout(() => dismiss(id), duration);
      timersRef.current.set(id, timer);
    }
    return id;
  }, [dismiss]);

  const api = useMemo(
    () => ({
      push,
      success: (message, options) => push('success', message, options),
      error: (message, options) => push('danger', message, options),
      info: (message, options) => push('info', message, options),
      warning: (message, options) => push('warning', message, options),
      dismiss,
    }),
    [push, dismiss]
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      {createPortal(
        <div className="toast-host" aria-live="polite" aria-relevant="additions">
          {toasts.map((toast) => (
            <div
              key={toast.id}
              className={`toast-bubble toast-bubble--${toast.variant}`}
              role={toast.variant === 'danger' ? 'alert' : 'status'}
            >
              <p className="toast-bubble-message">{toast.message}</p>
              <button
                type="button"
                className="toast-bubble-close"
                aria-label="Fermer"
                onClick={() => dismiss(toast.id)}
              >
                ×
              </button>
            </div>
          ))}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast doit être utilisé dans ToastProvider');
  }
  return ctx;
}
