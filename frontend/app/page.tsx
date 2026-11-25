"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError, cancelReservation, createReservation, findReservations, getSlots } from "@/lib/api";
import { formatDate, formatHour, formatPhone, kstToday, PHONE_REGEX } from "@/lib/format";
import type { ReservationOut, SlotOut } from "@/lib/types";

type Step = "select" | "form" | "done";

export default function Home() {
  const [tab, setTab] = useState<"book" | "mine">("book");
  return (
    <>
      <h1>1:1 상담 예약</h1>
      <div className="tabs">
        <button className={tab === "book" ? "active" : ""} onClick={() => setTab("book")}>예약하기</button>
        <button className={tab === "mine" ? "active" : ""} onClick={() => setTab("mine")}>내 예약 확인</button>
      </div>
      {tab === "book" ? <Booking /> : <MyReservations />}
    </>
  );
}

function Booking() {
  const dates = Array.from({ length: 7 }, (_, i) => kstToday(i));
  const [step, setStep] = useState<Step>("select");
  const [date, setDate] = useState(dates[0]);
  const [hour, setHour] = useState<number | null>(null);
  const [slots, setSlots] = useState<SlotOut[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [errors, setErrors] = useState<{ name?: string; phone?: string }>({});
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState<ReservationOut | null>(null);

  const load = useCallback(async (d: string) => {
    setLoading(true);
    setLoadError("");
    try {
      setSlots(await getSlots(d));
    } catch (e) {
      setLoadError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(date); }, [date, load]);

  function validate() {
    const next: { name?: string; phone?: string } = {};
    const n = name.trim();
    if (!n) next.name = "이름을 입력해 주세요.";
    else if (n.length > 20) next.name = "이름은 20자 이내로 입력해 주세요.";
    if (!phone.trim()) next.phone = "연락처를 입력해 주세요.";
    else if (!PHONE_REGEX.test(phone.trim())) next.phone = "연락처는 010-0000-0000 형식으로 입력해 주세요.";
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function submit() {
    if (hour === null || submitting || !validate()) return;
    setSubmitting(true);
    setSubmitError("");
    try {
      setDone(await createReservation({ date, hour, name: name.trim(), phone: phone.trim() }));
      setStep("done");
    } catch (e) {
      setSubmitError((e as Error).message);
      if (e instanceof ApiError && e.status === 409) load(date); // 다른 사람이 먼저 예약: 슬롯 갱신
    } finally {
      setSubmitting(false);
    }
  }

  function reset() {
    setStep("select"); setHour(null); setName(""); setPhone(""); setErrors({}); setSubmitError(""); setDone(null);
    load(date);
  }

  if (step === "done" && done) {
    return (
      <div className="card">
        <h2>예약이 완료되었습니다</h2>
        <p>{formatDate(done.date)} {formatHour(done.hour)}</p>
        <p className="muted">{done.name} · {formatPhone(done.phone)}</p>
        <p className="muted">예약 확인·취소는 &lsquo;내 예약 확인&rsquo;에서 연락처로 할 수 있습니다.</p>
        <button className="primary full" onClick={reset}>처음으로</button>
      </div>
    );
  }

  if (step === "form" && hour !== null) {
    return (
      <div className="card">
        <h2>예약 정보 입력</h2>
        <p className="muted">{formatDate(date)} {formatHour(hour)}</p>
        <label htmlFor="name">이름</label>
        <input id="name" className={errors.name ? "invalid" : ""} value={name} maxLength={30}
               onChange={(e) => setName(e.target.value)} placeholder="홍길동" />
        {errors.name && <div className="field-error">{errors.name}</div>}
        <label htmlFor="phone">연락처</label>
        <input id="phone" className={errors.phone ? "invalid" : ""} value={phone} inputMode="tel"
               onChange={(e) => setPhone(e.target.value)} placeholder="010-0000-0000" />
        {errors.phone && <div className="field-error">{errors.phone}</div>}
        {submitError && <div className="alert">{submitError}</div>}
        <div className="row" style={{ marginTop: 16 }}>
          <button onClick={() => { setStep("select"); setSubmitError(""); setHour(null); load(date); }}>이전</button>
          <button className="primary" style={{ flex: 1 }} disabled={submitting} onClick={submit}>
            {submitting ? "예약 중..." : "예약하기"}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>날짜 선택</h2>
      <div className="dates">
        {dates.map((d) => (
          <button key={d} className={d === date ? "active" : ""} onClick={() => { setDate(d); setHour(null); }}>
            {formatDate(d)}
          </button>
        ))}
      </div>
      <h2>시간 선택</h2>
      {loading ? <p className="muted">불러오는 중...</p>
        : loadError ? <div className="alert">{loadError}</div>
        : (
          <div className="slots">
            {slots.map((s) => (
              <button key={s.hour} className={`slot ${hour === s.hour ? "active" : ""}`} disabled={!s.available}
                      onClick={() => setHour(s.hour)}>
                {formatHour(s.hour)}
                <small>{s.past ? "지난 시간" : s.available ? "예약 가능" : "마감"}</small>
              </button>
            ))}
          </div>
        )}
      <button className="primary full" disabled={hour === null} onClick={() => setStep("form")}>다음</button>
    </div>
  );
}

function MyReservations() {
  const [phone, setPhone] = useState("");
  const [searched, setSearched] = useState<string | null>(null);
  const [list, setList] = useState<ReservationOut[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [target, setTarget] = useState<ReservationOut | null>(null);
  const [cancelling, setCancelling] = useState(false);

  async function search(p = phone) {
    if (!PHONE_REGEX.test(p.trim())) { setError("연락처는 010-0000-0000 형식으로 입력해 주세요."); return; }
    setLoading(true); setError("");
    try {
      setList(await findReservations(p.trim()));
      setSearched(p.trim());
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function confirmCancel() {
    if (!target || searched === null) return;
    setCancelling(true);
    try {
      await cancelReservation(target.id, searched);
      setTarget(null);
      await search(searched);
    } catch (e) {
      setError((e as Error).message);
      setTarget(null);
    } finally {
      setCancelling(false);
    }
  }

  return (
    <>
      <div className="card">
        <h2>내 예약 확인</h2>
        <label htmlFor="q">연락처</label>
        <input id="q" value={phone} inputMode="tel" placeholder="010-0000-0000"
               onChange={(e) => setPhone(e.target.value)} onKeyDown={(e) => e.key === "Enter" && search()} />
        {error && <div className="alert">{error}</div>}
        <button className="primary full" disabled={loading} onClick={() => search()}>{loading ? "조회 중..." : "조회하기"}</button>
      </div>
      {searched !== null && !loading && (
        list.length === 0 ? (
          <div className="card"><p className="muted">예약 내역이 없습니다.</p></div>
        ) : list.map((r) => (
          <div className="card row" key={r.id}>
            <div>
              <div>{formatDate(r.date)} {formatHour(r.hour)}</div>
              <div className="muted">{r.name} · {formatPhone(r.phone)}</div>
            </div>
            <button className="danger" onClick={() => setTarget(r)}>취소</button>
          </div>
        ))
      )}
      {target && (
        <div className="modal-bg">
          <div className="modal">
            <h2>예약을 취소할까요?</h2>
            <p>{formatDate(target.date)} {formatHour(target.hour)}</p>
            <p className="muted">취소하면 되돌릴 수 없습니다.</p>
            <div className="row">
              <button style={{ flex: 1 }} onClick={() => setTarget(null)}>닫기</button>
              <button className="danger" style={{ flex: 1 }} disabled={cancelling} onClick={confirmCancel}>예약 취소</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
