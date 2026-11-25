@AGENTS.md

> 이번 과제는 docs/의 기본 스택(FastAPI/SQLite)과 다르다: **Spring Boot + MySQL + Next.js**. 원칙(최소 버전 먼저, 기능 단위 커밋, 테스트는 명세 기준)은 그대로 따른다.

# 과제 요구사항

## 필수
- [ ] F1 슬롯 조회: 오늘~+6일(7일), 10·11·13·14·15·16·17시(7칸). 예약된 칸은 '마감'
- [ ] F2 예약: 이름·연락처 입력, 잘못된 입력은 오류 메시지
- [ ] F3 중복 방지: 같은 날짜·시간 1건만, **서버(DB)에서** 차단 → 화면에 오류 메시지
- [ ] F4 연락처로 내 예약 조회 + 취소(확인 단계)
- [ ] F5 관리자(/admin): 로그인(계정 1개, 비밀번호는 서버 환경변수) 후 날짜별 예약 목록. 비로그인 시 관리자 API 401
- [ ] F6 테스트: 중복 예약 방지(동시 요청 포함), 입력 검증, 관리자 인증
- [ ] F7 배포: https://ldg.recruit.kayple.com (/, /admin, /api 경로 분리)
- [ ] F8 제출: Public GitHub 저장소, README(실행·배포 / 한 것·못 한 것 / AI 코드 직접 수정 부분 / 팀원 작업 지시 3개)

## 선택 (40분 시점에 미완이면 포기)
- [ ] 시안 10개 화면에 맞춘 디자인 (평가 대상 아님)

## 가정 (명시)
- 시간대는 Asia/Seoul. 오늘 이미 지난 시간은 예약 불가(마감 표시)
- 연락처: 010으로 시작하는 휴대폰 번호, 하이픈 유무 무관 → 숫자만 저장
- 이름: 공백 제외 1~20자
- 취소 = 행 삭제 (같은 칸을 다시 예약 가능해야 하므로)
- 취소할 때 id + 연락처가 일치해야 함 (남의 예약 취소 방지)

# 데이터 모델
reservation
| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK auto | |
| date | DATE | |
| hour | INT | 10,11,13~17 |
| name | VARCHAR(20) | |
| phone | VARCHAR(11) | 숫자만 |
| created_at | DATETIME | |
| | **UNIQUE(date, hour)** | 중복 방지의 최종 보루 |

# API 명세 (모두 /api 접두사)
| Method | Path | 요청 | 응답 | 에러 |
|---|---|---|---|---|
| GET | /api/slots?date=YYYY-MM-DD | | 200 `[{hour, available}]` | 400 범위 밖 날짜 |
| POST | /api/reservations | `{date, hour, name, phone}` | 201 `{id,date,hour,name,phone}` | 400 입력 오류·지난 시간, **409 이미 예약된 시간** |
| GET | /api/reservations?phone= | | 200 `[...]` (빈 배열 가능) | 400 연락처 형식 |
| DELETE | /api/reservations/{id}?phone= | | 204 | 404 없음/연락처 불일치 |
| POST | /api/admin/login | `{username, password}` | 200 (세션 쿠키) | 401 |
| POST | /api/admin/logout | | 204 | |
| GET | /api/admin/reservations?date= | | 200 `[...]` | **401 비로그인** |

에러 응답: `{ "message": "화면에 그대로 보여줄 문장" }`

# 설계
- 중복 방지: 애플리케이션 사전 체크 + DB UNIQUE 제약. 동시 요청은 `DataIntegrityViolationException` → 409
- 관리자 인증: HttpSession + 인터셉터(`/api/admin/**`, login 제외). 계정은 `ADMIN_USERNAME`/`ADMIN_PASSWORD` 환경변수
- 프론트: Next.js 정적 export → nginx가 `/`·`/admin` 서빙, `/api` → Spring Boot(127.0.0.1:8080) 프록시. 같은 도메인이라 CORS 불필요
- 테스트: JUnit5 + MockMvc + H2(MySQL 모드). 동시성은 스레드 N개로 같은 칸 예약 → 1건만 성공
- 배포: 서버에서 빌드(JDK 21, Node 24), systemd로 jar 실행, DB 비밀번호·관리자 비밀번호는 `/etc/booking.env`
