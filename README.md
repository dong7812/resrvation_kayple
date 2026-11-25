# 1:1 상담 예약 시스템

상담실 1개, 1시간 단위 상담 예약 서비스. 한 시간대에는 예약 1건만 받는다.

- 사용자: https://ldg.recruit.kayple.com/
- 관리자: https://ldg.recruit.kayple.com/admin
- API: https://ldg.recruit.kayple.com/api/…

| 구분 | 기술 |
|---|---|
| 웹 | Next.js 15 (App Router, 정적 export), React 19, TypeScript |
| 백엔드 | Spring Boot 3.4 (Java 21), Spring Data JPA, Bean Validation |
| DB | MySQL 8.0 (테스트는 H2 MySQL 모드) |
| 배포 | nginx (HTTPS, 경로 분리) + systemd |

## 실행 방법 (로컬)

```bash
# 백엔드 (MySQL 필요)
cd backend
DB_PASSWORD=... ADMIN_PASSWORD=... ./mvnw spring-boot:run      # 127.0.0.1:8080

# 프론트 (개발 서버에서 API 주소 지정)
cd frontend && npm ci
NEXT_PUBLIC_API_URL=http://localhost:8080 npm run dev          # localhost:3000

# 테스트
cd backend && ./mvnw test                                       # 47개
```

## 배포 방법

```
브라우저 ─HTTPS─> nginx(443)
                   ├─ /api/*        → proxy 127.0.0.1:8080 (Spring Boot, systemd: booking)
                   └─ /, /admin     → /var/www/booking (Next.js 정적 export)
```

서버에서 저장소를 받은 뒤 1회:

```bash
git clone <this repo> ~/booking && cd ~/booking
DB_PASSWORD='<~/README-server.txt의 값>' ADMIN_PASSWORD='<관리자 비밀번호>' bash deploy/deploy.sh
```

- 비밀번호는 코드/GitHub에 없다. `/etc/booking.env`(chmod 600)에만 저장되고 systemd가 환경변수로 주입한다.
- 이후 재배포는 `git pull && bash deploy/deploy.sh`.
- 8080은 `127.0.0.1`에만 바인딩해 외부에서 직접 접근할 수 없다.

## API

| Method | Path | 성공 | 에러 |
|---|---|---|---|
| GET | `/api/slots?date=YYYY-MM-DD` | 200 `[{hour, available, past}]` | 400 7일 범위 밖 |
| POST | `/api/reservations` `{date,hour,name,phone}` | 201 | 400 입력 오류·지난 시간, **409 이미 예약된 시간** |
| GET | `/api/reservations?phone=` | 200 (빈 배열 가능) | 400 |
| DELETE | `/api/reservations/{id}?phone=` | 204 (멱등) | 400 지난 예약 |
| POST | `/api/admin/login` | 200 + 세션 쿠키 | 401 |
| POST | `/api/admin/logout` | 204 | |
| GET | `/api/admin/me`, `/api/admin/reservations?date=` | 200 | **401 비로그인** |

에러 응답은 `{"message": "화면에 그대로 보여줄 문장"}`.

## 한 것

- [x] 날짜(오늘~7일)·시간(10·11·13~17시) 선택, 예약된 칸은 '마감', 지난 시간은 보이지만 클릭 불가
- [x] 이름·연락처 입력 검증 (화면 + 서버 이중 검증), 필드별 오류 메시지
- [x] 서버 측 중복 방지: **DB UNIQUE(slot_date, slot_hour) + INSERT 한 번** → 충돌 시 409
- [x] 입력 중 다른 사람이 먼저 예약하면 "이미 예약된 시간입니다. 다른 시간을 선택해 주세요." 표시 후 슬롯 갱신
- [x] 연락처로 내 예약 조회 (`010-1234-5678`, `010-123-4567`, 하이픈 유무 모두 같은 결과), 확인 모달 후 취소
- [x] 관리자 로그인(세션, HttpOnly 쿠키) → 날짜별 예약 목록. 비로그인 시 관리자 API 401, 세션 만료 시 로그인 화면으로
- [x] 테스트 47개: 동시 10건 → 1건 성공·9건 409, 입력 검증·주입 문자열, 조회·취소 멱등, 관리자 인증
- [x] nginx 경로 분리 배포, HTTPS

## 보안 점검

| 항목 | 상태 | 조치 |
|---|---|---|
| 비밀정보 노출 | 안전 | 관리자·DB 비밀번호, pem 키는 저장소 전체 히스토리에 없음 (`git log -p` 검색으로 확인). 서버 `/etc/booking.env`(600)에만 존재. `.gitignore`에 `*.pem`, `.env` |
| SQL 인젝션 | 방어 | JPA 파라미터 바인딩만 사용(문자열 연결 쿼리 없음), 이름 화이트리스트·연락처 정규식·타입 바인딩 → 400 (테스트 포함) |
| XSS | 방어 | 이름 화이트리스트 + React 자동 이스케이프, `X-Content-Type-Options: nosniff` |
| 관리자 API 무단 접근 | 방어 | 세션 없으면 `/api/admin/**` 401 (테스트 포함) |
| 무차별 대입(로그인) | 완화 | nginx 로그인 IP당 분당 5회 → 429, 상수시간 비교 |
| 세션 고정 / CSRF | 방어 | 로그인 시 세션 재발급, 쿠키 HttpOnly·Secure·SameSite=Strict |
| 슬롯 독점(자동 예약 폭주) | 완화 | nginx API IP당 초당 10회 제한. 근본 대책(본인 인증)은 미구현 |
| 내부 정보 노출 | 방어 | 에러 응답 스택 트레이스 숨김, `server_tokens off`, 8080은 127.0.0.1 바인딩 |
| 클릭재킹 / 다운그레이드 | 방어 | `X-Frame-Options: DENY`, HSTS, http→https 301 |
| 남의 예약 취소 | 방어 | id+연락처가 모두 맞아야 삭제 (테스트 포함) |
| **연락처만으로 조회** | **남은 위험** | 남의 연락처를 알면 그 사람의 예약(이름·시간)을 볼 수 있음 → 작업 지시 2번 |

## 못 한 것 / 단순화한 것

- 시안의 디자인 디테일은 맞추지 않았다 (기능 우선, 평가 대상 아님).
- 로그인 제한은 nginx IP 기준만 있다 (계정 잠금은 없음).
- 예약자 본인 확인이 연락처뿐이다 (SMS 인증 없음). 연락처를 아는 사람은 조회·취소 가능.
- 프론트 E2E 테스트 없음 (백엔드 API 테스트만).
- DB 마이그레이션 도구(Flyway) 대신 `ddl-auto=update` 사용.

## 설계 결정

### 중복 방지: 비관적 락 대신 UNIQUE 제약
| | 비관적 락 `SELECT … FOR UPDATE` | **UNIQUE + INSERT (선택)** |
|---|---|---|
| 빈 칸 처리 | 잠글 행이 없음 → InnoDB 갭 락 → 동시 INSERT 시 데드락 위험. 쓰려면 슬롯 행을 미리 만들어야 함 | 행이 없어도 DB가 원자적으로 판정 |
| 쿼리 | 조회 + 변경 2번, 트랜잭션 유지 | INSERT 1번 |
| 동시 요청 | 대기(락 경합) | 즉시 거절 → 409 |

상담실이 하나라 충돌 시 "기다리기"보다 "바로 다른 시간 안내"가 사용자 경험에도 맞다.

### 그 밖의 엣지 케이스
- 시간대: 서버/JVM이 UTC여도 오늘·지난 시간 판단이 어긋나지 않게 Asia/Seoul로 고정 (`Clock` 빈 주입 → 테스트에서 시각 고정).
- 7일 범위 밖, 12시 같은 잘못된 시간, 지난 시간을 API로 직접 보내면 400.
- 주입 방어: JPA 파라미터 바인딩만 사용, 이름은 한글·영문·공백 화이트리스트, 연락처 정규식, 날짜·시간은 타입 바인딩.
- 취소는 `DELETE … WHERE id=? AND phone=?` 하나로 처리. 이미 없거나 연락처가 다르면 아무것도 지우지 않고 204 (멱등, 남의 예약 존재 여부 노출 안 함). 지난 예약은 취소 불가.
- 취소는 행 삭제 → 같은 칸을 다시 예약 가능.
- 더블 클릭: 요청 중 버튼 비활성화 + 서버 UNIQUE가 최종 방어.
- 한 사람의 여러 칸 예약은 요구사항에 제한이 없어 허용.

## AI가 만든 코드에서 직접 판단해 고친 부분

1. **중복 방지 방식**: AI 초안 논의에서 비관적 락이 후보였으나, 예약이 없는 칸에는 잠글 행이 없어 MySQL에서 갭 락·데드락이 날 수 있다고 판단해 UNIQUE 제약 + 409로 정했다.
2. **날짜가 하루 밀리는 버그**: 초안은 `@PostConstruct`에서 `TimeZone.setDefault(Asia/Seoul)`를 호출했다. JDBC 드라이버가 이미 UTC로 초기화된 뒤라 11/26 예약이 11/25로 저장·응답되는 것을 병렬 테스트 에이전트가 잡았고, `main()`에서 JDBC 초기화 전에 고정하고 테스트 JVM도 `-Duser.timezone=Asia/Seoul`로 맞췄다. (테스트를 구현에 맞추지 않고 구현을 고침)
3. **연락처 형식**: `010-000-0000`(구형 3자리 국번)도 받도록 정규식을 `^010-?\d{3,4}-?\d{4}$`로 하고, 저장은 숫자만 해 하이픈 유무와 관계없이 조회되게 했다.
4. **취소 멱등성**: id 단독 삭제는 남의 예약을 지울 수 있어 id+연락처를 함께 조건으로 걸고, 반복 호출에도 같은 결과(204)가 나오게 했다.
5. **주입 방어**: API를 직접 호출하는 경우를 고려해 이름 화이트리스트와 타입 바인딩 400 처리를 추가했다.
6. **예약어 컬럼**: `date`, `hour`는 MySQL 키워드라 `slot_date`, `slot_hour`로 바꿨다.

## 팀원에게 넘길 작업 지시 3개

1. **관리자 로그인 무차별 대입 방어**
   `POST /api/admin/login`에 IP 기준 5회 실패 시 10분 잠금을 추가해 주세요. 잠금 중에는 429와 "잠시 후 다시 시도해 주세요."를 반환하고, 6번째 시도가 429인지 확인하는 테스트를 `AdminAuthTest`에 추가해 주세요.
2. **예약자 본인 확인 강화**
   지금은 연락처만 알면 조회·취소가 됩니다. 예약 시 4자리 비밀번호(또는 예약 번호)를 함께 받아 해시로 저장하고, 조회·취소 API에 그 값을 요구하도록 바꿔 주세요. 틀린 값으로 취소하면 삭제되지 않는 테스트가 필요합니다.
3. **스키마 관리와 시안 반영**
   `ddl-auto=update`를 Flyway `V1__init.sql`(UNIQUE 제약 포함)로 바꾸고 운영은 `validate`로 설정해 주세요. 이어서 화면 시안 10개(예약 2단계, 오류 상태, 완료, 빈 결과, 취소 확인, 관리자 로그인 오류/목록)에 맞춰 스타일을 정리해 주세요.

## AI 활용

### 진행 순서
1. **요구사항 분석 먼저**: 구현 전에 `CLAUDE.md`(필수/선택, 데이터 모델, API 명세, 엣지케이스 가정)를 AI가 작성하고, 직접 검토해 엣지케이스(지난 시간 비활성, 연락처 2형식, 취소 멱등, 중복 방지 방식 트레이드오프, 입력 중 선점 시 경고, 주입 방어)를 추가·확정했다.
2. **하네스**: `AGENTS.md`(지도) + `docs/`(규칙) + `CLAUDE.md`(명세)를 기준으로 구현.

### 병렬 단위 테스트 (메인 에이전트 + test-writer 에이전트)
```
메인 에이전트                          test-writer 에이전트 (백그라운드)
─────────────                          ───────────────────────────────
백엔드 API 구현·컴파일 ───────────────▶ 명세(CLAUDE.md) 기준 JUnit 작성·실행
프론트 화면 구현 (기다리지 않음)         ├ ReservationApiTest / ConcurrencyTest / AdminAuthTest
배포 스크립트 작성                       └ 1차 보고: 39 passed / 2 failed
     ◀──────────── 실패 원인 보고: "11/26 예약이 11/25로 응답" (구현 문제)
시간대 버그 수정 (테스트는 그대로)
이름 화이트리스트 추가 → 스펙 변경 전달 ▶ 주입 입력 테스트 6개 추가
     ◀──────────── 2차 보고: 47 passed / 0 failed
```
- test-writer는 `backend/src/test/`만 수정할 수 있고, **테스트를 구현에 맞춰 고치지 않는다**는 규칙으로 돌렸다. 그래서 실패를 구현 버그로 보고했고, 테스트가 아니라 구현을 고쳤다.
- 동시성 테스트는 스레드 10개가 `CountDownLatch`로 동시에 같은 칸을 예약 → 정확히 1건 성공·9건 409·DB 1건을 검증한다.
- 메인 에이전트는 테스트를 기다리지 않고 화면·배포를 진행해 1시간 안에 구현·테스트·배포를 마쳤다.

### 배포
클라우드 세션에서는 서버 SSH가 막혀 있어, 로컬 Mac의 원격 세션이 pem 키로 서버에 접속해 `deploy/deploy.sh`를 실행하고 실주소(`/`, `/admin`, `/api`, 401, 로그인)를 검증했다.
