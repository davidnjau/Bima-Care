const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

function formatYmd(year: number, month: number, day: number): string {
  return `${day} ${MONTHS[month - 1]} ${year}`
}

function formatHm(hour: number, minute: number): string {
  const period = hour >= 12 ? 'PM' : 'AM'
  const h12 = hour % 12 === 0 ? 12 : hour % 12
  return `${h12}:${String(minute).padStart(2, '0')} ${period}`
}

// Formats an ISO-8601 date or date-time string (with or without a timezone offset,
// with or without fractional seconds) into a readable form - purely by parsing the
// digits in the string, never via `new Date()`. These are calendar dates/wall-clock
// times as recorded by the backend, not universal instants, so reinterpreting them
// in the browser's local timezone would silently shift the displayed day.
// Returns null if the value doesn't look like a date, so callers can fall back to
// showing it unchanged.
export function formatDate(value: string | null | undefined): string | null {
  if (!value) return null

  const dateOnly = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (dateOnly) {
    const [, y, m, d] = dateOnly
    return formatYmd(Number(y), Number(m), Number(d))
  }

  const dateTime = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/)
  if (!dateTime) return null
  const [, y, m, d, h, min] = dateTime
  const datePart = formatYmd(Number(y), Number(m), Number(d))
  if (h === '00' && min === '00') return datePart
  return `${datePart}, ${formatHm(Number(h), Number(min))}`
}
