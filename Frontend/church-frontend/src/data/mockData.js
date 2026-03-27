export const mockUser = {
  id: 12,
  firstName: 'Jean',
  lastName: 'Kossi',
  username: 'jkossi',
  role: 'SECRETAIRE',
};

export const mockParish = {
  id: 3,
  name: 'Paroisse Saint Joseph',
  city: 'Lomé',
  email: 'saintjoseph@example.com',
  phone: '+228 90 00 00 00',
};

export const mockStats = {
  requests: 148,
  pendingRequests: 17,
  validatedRequests: 112,
  invoices: 121,
  paymentsToVerify: 8,
};

export const mockRequests = [
  {
    id: 1,
    trackingCode: 'MS-2026-0001',
    applicant: 'Kokou Adjei',
    parish: 'Paroisse Saint Joseph',
    requestType: 'Messe d’action de grâce',
    requestStatus: 'EN_ATTENTE',
    validationStatus: 'EN_ATTENTE',
    paymentStatus: 'EN_ATTENTE',
    amount: 5000,
    createdAt: '2026-03-20',
  },
  {
    id: 2,
    trackingCode: 'MS-2026-0002',
    applicant: 'Ama Mensah',
    parish: 'Paroisse Saint Joseph',
    requestType: 'Messe de requiem',
    requestStatus: 'VALIDEE',
    validationStatus: 'VALIDEE',
    paymentStatus: 'PAYE',
    amount: 15000,
    createdAt: '2026-03-18',
  },
  {
    id: 3,
    trackingCode: 'MS-2026-0003',
    applicant: 'Yao Gnakade',
    parish: 'Paroisse Saint Joseph',
    requestType: 'Messe spéciale famille',
    requestStatus: 'TRAITEE',
    validationStatus: 'VALIDEE',
    paymentStatus: 'A_VERIFIER',
    amount: 25000,
    createdAt: '2026-03-17',
  },
];

export const mockPayments = [
  {
    id: 1,
    trackingCode: 'MS-2026-0002',
    applicant: 'Ama Mensah',
    amount: 15000,
    type: 'TMONEY',
    transactionId: 'TX-92011',
    status: 'PAYE',
    paidAt: '2026-03-18',
  },
  {
    id: 2,
    trackingCode: 'MS-2026-0003',
    applicant: 'Yao Gnakade',
    amount: 25000,
    type: 'FLOOZ',
    transactionId: 'TX-92012',
    status: 'A_VERIFIER',
    paidAt: '2026-03-17',
  },
];

export const mockInvoices = [
  {
    id: 1,
    number: 'FAC-2026-0101',
    trackingCode: 'MS-2026-0002',
    applicant: 'Ama Mensah',
    amount: 15000,
    status: 'PAYE',
    issuedAt: '2026-03-18',
  },
  {
    id: 2,
    number: 'FAC-2026-0102',
    trackingCode: 'MS-2026-0003',
    applicant: 'Yao Gnakade',
    amount: 25000,
    status: 'EN_ATTENTE',
    issuedAt: '2026-03-17',
  },
];

export const mockSchedules = [
  { id: 1, label: 'Messe du matin', day: 'LUNDI', hour: '06:30', active: 'ACTIVE' },
  { id: 2, label: 'Messe du soir', day: 'JEUDI', hour: '18:00', active: 'ACTIVE' },
];

export const mockRequestTypes = [
  { id: 1, label: 'Messe d’action de grâce', category: 'EUCHARISTIE', active: 'ACTIVE' },
  { id: 2, label: 'Messe de requiem', category: 'SACRAMENTAL', active: 'ACTIVE' },
];

export const mockPricing = [
  { id: 1, label: 'Forfait simple', amount: 5000, celebrations: 1, customHour: false, active: 'ACTIVE' },
  { id: 2, label: 'Forfait famille', amount: 15000, celebrations: 3, customHour: true, active: 'ACTIVE' },
];

export const mockParishes = [
  { id: 1, name: 'Saint Joseph', city: 'Lomé', email: 'sj@example.com', phone: '+228 90 00 00 01', active: 'ACTIVE' },
  { id: 2, name: 'Sainte Rita', city: 'Lomé', email: 'sr@example.com', phone: '+228 90 00 00 02', active: 'ACTIVE' },
];

export const mockUsers = [
  { id: 1, firstName: 'Jean', lastName: 'Kossi', username: 'jkossi', role: 'SECRETAIRE', active: 'ACTIVE' },
  { id: 2, firstName: 'Mawuli', lastName: 'Seddoh', username: 'mseddoh', role: 'ADMIN', active: 'ACTIVE' },
];

export const mockAccesses = [
  { id: 1, user: 'Jean Kossi', parish: 'Saint Joseph', role: 'SECRETAIRE', active: 'ACTIVE' },
  { id: 2, user: 'Mawuli Seddoh', parish: 'Sainte Rita', role: 'ADMIN', active: 'ACTIVE' },
];

export const mockLocalities = [
  { id: 1, label: 'Lomé Centre' },
  { id: 2, label: 'Agoè' },
  { id: 3, label: 'Adidogomé' },
];

export const mockPaymentTypes = [
  { id: 1, label: 'TMONEY' },
  { id: 2, label: 'FLOOZ' },
  { id: 3, label: 'ESPECES' },
];
