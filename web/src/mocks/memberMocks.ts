export interface MockClaim {
  provider: string
  date: string
  status: string
  type: string
  amount: string
  diagnosis: string
}

export const claims: MockClaim[] = [
  {
    provider: 'Nairobi Hospital',
    date: '15 Mar 2024',
    status: 'approved',
    type: 'Outpatient',
    amount: 'Ksh 8,500',
    diagnosis: 'Upper Respiratory Tract Infection',
  },
  {
    provider: 'Mediheal Pharmacy — Westlands',
    date: '02 Apr 2024',
    status: 'approved',
    type: 'Pharmacy',
    amount: 'Ksh 3,200',
    diagnosis: 'Prescription refill — Hypertension',
  },
  {
    provider: 'Karen Hospital',
    date: '10 Feb 2024',
    status: 'approved',
    type: 'Outpatient',
    amount: 'Ksh 5,500',
    diagnosis: 'Routine checkup',
  },
]

export interface MockDependent {
  name: string
  relationship: string
  gender: string
  dob: string
  age: number
  memberId: string
}

export const dependents: MockDependent[] = [
  {
    name: 'Grace Kamau',
    relationship: 'Spouse',
    gender: 'Female',
    dob: '22 Mar 1988',
    age: 38,
    memberId: 'KIC-MEM-2024-001235',
  },
  {
    name: 'Brian Kamau',
    relationship: 'Child',
    gender: 'Male',
    dob: '10 Sep 2015',
    age: 10,
    memberId: 'KIC-MEM-2024-001236',
  },
  {
    name: 'Faith Kamau',
    relationship: 'Child',
    gender: 'Female',
    dob: '05 Apr 2018',
    age: 8,
    memberId: 'KIC-MEM-2024-001237',
  },
]
