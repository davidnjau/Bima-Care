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
