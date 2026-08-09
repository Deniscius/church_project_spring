import AppAlert from './AppAlert';

/**
 * Bandeau d’erreur de formulaire avec ancre de scroll / focus.
 * Accepte une string, un Error, ou un tableau de messages.
 */
export default function FormError({ error, errorRef, title = 'À corriger' }) {
  if (!error) return null;

  const messages = (Array.isArray(error) ? error : [error])
    .flatMap((item) => {
      if (!item) return [];
      if (typeof item === 'string') {
        return item.split(/\s*;\s*/).map((s) => s.trim()).filter(Boolean);
      }
      if (item instanceof Error) {
        const fromProp = Array.isArray(item.messages) ? item.messages : null;
        if (fromProp?.length) return fromProp;
        return item.message ? [item.message] : [];
      }
      return [String(item)];
    })
    .filter(Boolean);

  if (!messages.length) return null;

  return (
    <div
      ref={errorRef}
      tabIndex={-1}
      className="form-error-banner"
      data-form-error
      role="alert"
      aria-live="assertive"
    >
      <AppAlert variant="danger">
        {messages.length === 1 ? (
          <>
            <strong className="form-error-title">{title}</strong>
            <p className="form-error-single">{messages[0]}</p>
          </>
        ) : (
          <>
            <strong className="form-error-title">
              {title} ({messages.length})
            </strong>
            <ul className="form-error-list">
              {messages.map((msg) => (
                <li key={msg}>{msg}</li>
              ))}
            </ul>
          </>
        )}
      </AppAlert>
    </div>
  );
}
