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
