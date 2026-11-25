# AGENTS.md — 이동규의 작업 방식

이 파일은 **지도**다. 세부 규칙은 `docs/`에 있고, 지금 하는 작업에 맞는 문서만 읽는다.
과제별 내용(요구사항, 데이터 모델, API 명세)은 `CLAUDE.md`에 있다.

## 문서 지도

| 지금 하는 작업 | 먼저 읽을 문서 |
|---|---|
| 작업 순서, 병렬 테스트, 마무리 | `docs/workflow.md` |
| 백엔드 코드 | `docs/backend.md` |
| 프론트 코드 | `docs/frontend.md` + `frontend/AGENTS.md` |
| 테스트 | `docs/testing.md` |

## 핵심 규칙

1. **동작하는 최소 버전 먼저.** DB → API → 화면 한 줄기를 끝까지 연결한 뒤 확장한다.
2. **기능 하나씩.** 구현 전에 수정할 파일과 변경 내용을 3줄 이내로 먼저 말한다.
3. **변경하면 실행해서 확인한다.** 확인 방법(URL 또는 명령)을 함께 알려준다.
4. **추측하지 않는다.** 요구사항이 모호하면 가정을 명시하거나 묻는다. 요청하지 않은 기능을 늘리지 않는다.
5. **새 패키지는 이유를 먼저 말한다.**
6. **테스트 실패를 숨기거나, 테스트를 구현에 맞춰 고치지 않는다.**
7. **기능 단위로 커밋한다.** `feat:` `fix:` `test:` `chore:` `docs:`

## 역할 분담

| 담당 | 수정 가능 범위 |
|---|---|
| 메인 에이전트 | `backend/` (tests 제외), `frontend/` |
| `test-writer` 서브에이전트 | `backend/tests/` 만 |

## 완료 기준 (기능 하나)

- [ ] Swagger(`/docs`)에서 정상 응답
- [ ] `test-writer` 테스트 통과
- [ ] 화면에서 동작 (생성 → 목록 반영)
- [ ] 잘못된 입력에 에러 메시지 표시
- [ ] 커밋

## 명령

- backend: `cd backend && .venv/bin/uvicorn main:app --reload --port 8000`
- frontend: `cd frontend && npm run dev`
- 테스트: `cd backend && .venv/bin/pytest -q`
- 전체 검증: `/verify`

## 자동 검사 (hook)

파일을 수정하면 자동으로 검사가 돈다 (`backend/*.py` → ruff, `frontend/*.ts(x)` → eslint).
hook이 에러를 돌려주면 다음 작업으로 넘어가기 전에 먼저 고친다.
