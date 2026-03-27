export function required(value) {
  return value !== undefined && value !== null && String(value).trim() !== '';
}

export function email(value) {
  return /^[^
\s@]+@[^
\s@]+\.[^
\s@]+$/.test(String(value || '').trim());
}
