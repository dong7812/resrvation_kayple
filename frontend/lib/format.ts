export const NAME_REGEX = /^[가-힣a-zA-Z ]+$/;
export const PHONE_REGEX = /^010-?\d{3,4}-?\d{4}$/;

export function kstToday(offset = 0): string {
  const now = new Date(Date.now() + 9 * 3600 * 1000 + offset * 86400 * 1000);
  return now.toISOString().slice(0, 10);
}

export function formatDate(date: string): string {
  const w = ["일", "월", "화", "수", "목", "금", "토"][new Date(`${date}T12:00:00Z`).getUTCDay()];
  const [, m, day] = date.split("-");
  return `${Number(m)}월 ${Number(day)}일 (${w})`;
}

export const formatHour = (h: number) => `${String(h).padStart(2, "0")}:00 - ${String(h + 1).padStart(2, "0")}:00`;

export function formatPhone(p: string): string {
  return p.length === 11 ? `${p.slice(0, 3)}-${p.slice(3, 7)}-${p.slice(7)}` : `${p.slice(0, 3)}-${p.slice(3, 6)}-${p.slice(6)}`;
}
