# frontend — Next.js 규칙

## 스택
Next.js 16 (App Router), React 19, TypeScript, Tailwind

## 먼저 할 일
Next 코드를 쓰기 전에 `frontend/AGENTS.md`의 지시대로 `node_modules/next/dist/docs/`에서 해당 기능 문서를 확인한다.
학습 데이터의 예전 Next 문법을 그대로 쓰지 않는다.

## 구조
```
frontend/
├─ app/
│  ├─ page.tsx            # 메인 화면
│  └─ <도메인>/page.tsx    # 필요할 때만 추가
├─ components/            # 화면 조각 (폼, 목록 아이템)
└─ lib/
   ├─ api.ts              # API 호출은 여기서만
   └─ types.ts            # 백엔드 XxxOut 과 같은 이름의 타입
```

## API 호출
- 모든 호출은 `lib/api.ts`의 함수로. 컴포넌트에서 `fetch` 직접 호출 금지.
- 주소는 `process.env.NEXT_PUBLIC_API_URL`
- 응답이 `ok`가 아니면 백엔드 `detail`을 담아 `Error`를 던진다.
- `cache: "no-store"`

## 화면
- 상태·이벤트가 있는 컴포넌트는 `"use client"`
- 모든 데이터 화면은 **로딩 / 에러 / 빈 목록** 3가지 상태를 표시한다.
- 생성·수정·삭제 후에는 목록을 다시 불러와 반영한다.
- 에러는 `detail` 문장을 화면에 그대로 보여준다.

## 스타일
- 기능이 먼저. Tailwind 기본 유틸리티로 최소한만.
- UI 라이브러리 추가 금지 (필요하면 이유를 먼저 말한다).
