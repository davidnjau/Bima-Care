export const serviceTypes = [
  'Outpatient',
  'Inpatient',
  'Pharmacy',
  'Laboratory & Diagnostics',
  'Dental',
  'Optical',
]

export interface MockTransaction {
  reference: string
  type: 'Pre-Auth' | 'Claim'
  member: string
  service: string
  amount: string
  date: string
  status: string
}

export const transactions: MockTransaction[] = [
  {
    reference: 'PA-2024-000001',
    type: 'Pre-Auth',
    member: 'James Kamau',
    service: 'Inpatient',
    amount: 'Ksh 350,000',
    date: '28/04/2024',
    status: 'approved',
  },
  {
    reference: 'CLM-2024-000003',
    type: 'Claim',
    member: 'Grace Kamau',
    service: 'Laboratory',
    amount: 'Ksh 15,000',
    date: '20/04/2024',
    status: 'pending',
  },
  {
    reference: 'CLM-2024-000002',
    type: 'Claim',
    member: 'James Kamau',
    service: 'Pharmacy',
    amount: 'Ksh 3,200',
    date: '02/04/2024',
    status: 'approved',
  },
  {
    reference: 'CLM-2024-000001',
    type: 'Claim',
    member: 'James Kamau',
    service: 'Outpatient',
    amount: 'Ksh 8,500',
    date: '15/03/2024',
    status: 'approved',
  },
]

export const transactionSummary = {
  totalClaims: 3,
  approvedClaims: 2,
  totalValue: 'Ksh 26,700',
}
