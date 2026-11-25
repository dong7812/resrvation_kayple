"use client";

import { useCallback, useEffect, useState } from "react";
import { adminLogin, adminLogout, adminMe, adminReservations, ApiError } from "@/lib/api";
import { formatDate, formatHour, formatPhone, kstToday } from "@/lib/format";
import type { AdminReservationOut } from "@/lib/types";

/** "2026-11-25T21:28:32.79" → "2026.11.25 21:28" (서버가 KST로 저장) */
const fmtDateTime = (v: string) => v.slice(0, 16).replace("T", " ").replace(/-/g, ".");

export default function AdminPage() {
  const [authed, setAuthed] = useState<boolean | null>(null);
  useEffect(() => { adminMe().then(() => setAuthed(true)).catch(() => setAuthed(false)); }, []);
  if (authed === null) return <p className="muted">확인 중...</p>;
  return authed ? <AdminList onLogout={() => setAuthed(false)} /> : <Login onLogin={() => setAuthed(true)} />;
}

function Login({ onLogin }: { onLogin: () => void }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!username || !password) { setError("아이디와 비밀번호를 입력해 주세요."); return; }
    setLoading(true); setError("");
    try { await adminLogin(username, password); onLogin(); }
    catch (err) { setError((err as Error).message); }
    finally { setLoading(false); }
  }

  return (
    <>
      <h1>관리자 로그인</h1>
      <form className="card" onSubmit={submit}>
        <label htmlFor="u">아이디</label>
        <input id="u" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
        <label htmlFor="p">비밀번호</label>
        <input id="p" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
        {error && <div className="alert">{error}</div>}
        <button className="primary full" disabled={loading}>{loading ? "로그인 중..." : "로그인"}</button>
      </form>
    </>
  );
}

function AdminList({ onLogout }: { onLogout: () => void }) {
  const dates = Array.from({ length: 7 }, (_, i) => kstToday(i));
  const [date, setDate] = useState(dates[0]);
  const [list, setList] = useState<AdminReservationOut[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState<"ALL" | "BOOKED" | "CANCELLED">("ALL");
  const booked = list.filter((r) => r.status === "BOOKED").length;
  const shown = filter === "ALL" ? list : list.filter((r) => r.status === filter);

  const load = useCallback(async (d: string) => {
    setLoading(true); setError("");
    try { setList(await adminReservations(d)); }
    catch (e) {
      if (e instanceof ApiError && e.status === 401) { onLogout(); return; } // 세션 만료
      setError((e as Error).message);
    } finally { setLoading(false); }
  }, [onLogout]);

  useEffect(() => { load(date); }, [date, load]);

  return (
    <>
      <div className="row">
        <h1>예약 목록</h1>
        <button onClick={async () => { await adminLogout().catch(() => {}); onLogout(); }}>로그아웃</button>
      </div>
      <div className="card">
        <label htmlFor="d">날짜</label>
        <input id="d" type="date" value={date} onChange={(e) => e.target.value && setDate(e.target.value)} />
      </div>
      <div className="card admin-wide">
        <h2>{formatDate(date)} · 예약됨 {booked}건 · 취소됨 {list.length - booked}건</h2>
        <div className="tabs">
          {(["ALL", "BOOKED", "CANCELLED"] as const).map((f) => (
            <button key={f} className={filter === f ? "active" : ""} onClick={() => setFilter(f)}>
              {f === "ALL" ? "전체" : f === "BOOKED" ? "예약됨" : "취소됨"}
            </button>
          ))}
        </div>
        {loading ? <p className="muted">불러오는 중...</p>
          : error ? <div className="alert">{error}</div>
          : shown.length === 0 ? <p className="muted">{filter === "CANCELLED" ? "취소된 예약이 없습니다." : "예약이 없습니다."}</p>
          : (
            <div className="table-wrap"><table>
              <thead><tr><th>상담 시간</th><th>이름</th><th>연락처</th><th>상태</th><th>예약 일시</th></tr></thead>
              <tbody>
                {shown.map((r) => (
                  <tr key={`${r.status}-${r.id}`} className={r.status === "CANCELLED" ? "cancelled" : ""}>
                    <td>{formatHour(r.hour)}</td><td>{r.name}</td><td>{formatPhone(r.phone)}</td>
                    <td>
                      <span className={`badge ${r.status === "BOOKED" ? "booked" : "cancel"}`}>
                        {r.status === "BOOKED" ? "예약됨" : "취소됨"}
                      </span>
                      {r.cancelledAt && <small className="muted block">{fmtDateTime(r.cancelledAt)} 취소</small>}
                    </td>
                    <td className="nowrap">{fmtDateTime(r.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table></div>
          )}
      </div>
    </>
  );
}
