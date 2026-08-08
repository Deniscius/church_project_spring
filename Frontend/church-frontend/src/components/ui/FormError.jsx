import AppAlert from './AppAlert';

/**
 * Bandeau d’erreur de formulaire avec ancre de scroll / focus.
 */
export default function FormError({ error, errorRef, title = 'À corriger' }) {
  if (!error) return null;

  const messages = Array.isArray(error) ? error.filter(Boolean) : [error];
  if (!messages.length) return null;

  return (
    <div
      ref={errorRef}
      tabIndex={-1}
      className="form-error-banner"
      data-form-error
      role="alert"
    >
      <AppAlert variant="danger">
        {messages.length === 1 ? (
          messages[0]
        ) : (
          <>
            <strong>{title}</strong>
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
