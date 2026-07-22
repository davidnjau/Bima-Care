export interface MockPolicy {
  name: string
  number: string
  type: string
  premium: string
  members: number
  period: string
  status: string
}

export const policies: MockPolicy[] = [
  {
    name: 'Family Comprehensive Plus',
    number: 'KIC-2024-FAM-001234',
    type: 'Family',
    premium: 'Ksh 85,000',
    members: 4,
    period: 'Jan 2024 – Dec 2024',
    status: 'active',
  },
  {
    name: 'Individual Premium',
    number: 'KIC-2024-IND-002001',
    type: 'Individual',
    premium: 'Ksh 45,000',
    members: 1,
    period: 'Jan 2024 – Dec 2024',
    status: 'active',
  },
  {
    name: 'Corporate Gold',
    number: 'KIC-2024-COR-003001',
    type: 'Corporate',
    premium: 'Ksh 2,500,000',
    members: 85,
    period: 'Jan 2024 – Dec 2024',
    status: 'active',
  },
  {
    name: 'Individual Basic',
    number: 'KIC-2024-IND-003012',
    type: 'Individual',
    premium: 'Ksh 25,000',
    members: 1,
    period: 'Jan 2024 – Dec 2024',
    status: 'suspended',
  },
  {
    name: 'Family Essential',
    number: 'KIC-2023-FAM-000456',
    type: 'Family',
    premium: 'Ksh 55,000',
    members: 3,
    period: 'Jan 2023 – Dec 2023',
    status: 'expired',
  },
]

export interface MockReport {
  title: string
  description: string
}

export const reports: MockReport[] = [
  {
    title: 'Claims Summary Report',
    description: 'Monthly claims overview with approval rates and amounts',
  },
  {
    title: 'Member Utilization Report',
    description: 'Member benefit usage and claims patterns',
  },
  {
    title: 'Provider Performance Report',
    description: 'Claims volume and costs by provider',
  },
  {
    title: 'Financial Summary',
    description: 'Premium collection vs claims payout analysis',
  },
]

export const claimsTrend = [
  { month: 'Jan', total: 120, approved: 95 },
  { month: 'Feb', total: 135, approved: 108 },
  { month: 'Mar', total: 150, approved: 122 },
  { month: 'Apr', total: 142, approved: 118 },
  { month: 'May', total: 160, approved: 130 },
  { month: 'Jun', total: 175, approved: 145 },
]

export const premiumTrend = [
  { month: 'Jan', amount: 8_200_000 },
  { month: 'Feb', amount: 9_100_000 },
  { month: 'Mar', amount: 10_400_000 },
  { month: 'Apr', amount: 11_000_000 },
  { month: 'May', amount: 12_600_000 },
  { month: 'Jun', amount: 13_800_000 },
]

export const topProviders = [
  { name: 'Nairobi Hospital', volume: 580 },
  { name: 'Karen Hospital', volume: 420 },
  { name: 'Aga Khan', volume: 360 },
  { name: 'MP Shah', volume: 290 },
  { name: "Gertrude's", volume: 210 },
]
