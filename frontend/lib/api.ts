import type { AdminReservationOut, ReservationOut, SlotOut } from "./types";

const BASE = process.env.NEXT_PUBLIC_API_URL ?? "";

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    cache: "no-store",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!res.ok) {
    let message = "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
    try { message = (await res.json()).message ?? message; } catch {}
    throw new ApiError(res.status, message);
  }
  return (res.status === 204 ? undefined : await res.json()) as T;
}

export const getSlots = (date: string) => request<SlotOut[]>(`/api/slots?date=${date}`);
export const createReservation = (body: { date: string; hour: number; name: string; phone: string }) =>
  request<ReservationOut>("/api/reservations", { method: "POST", body: JSON.stringify(body) });
export const findReservations = (phone: string) =>
  request<ReservationOut[]>(`/api/reservations?phone=${encodeURIComponent(phone)}`);
export const cancelReservation = (id: number, phone: string) =>
  request<void>(`/api/reservations/${id}?phone=${encodeURIComponent(phone)}`, { method: "DELETE" });
export const adminLogin = (username: string, password: string) =>
  request<{ username: string }>("/api/admin/login", { method: "POST", body: JSON.stringify({ username, password }) });
export const adminMe = () => request<{ username: string }>("/api/admin/me");
export const adminLogout = () => request<void>("/api/admin/logout", { method: "POST" });
export const adminReservations = (date: string) => request<AdminReservationOut[]>(`/api/admin/reservations?date=${date}`);
