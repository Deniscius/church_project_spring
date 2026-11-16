export function formatCurrency(value, currency = 'XOF') {
  const amount = Number(value || 0);
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount);
}
