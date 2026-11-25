# testing — 테스트 규칙

## 원칙
- 테스트의 기준은 **`CLAUDE.md`의 API 명세**다. 구현을 보고 "지금 동작하는 대로" 테스트를 맞추지 않는다.
- 구현 코드는 경로, 스키마 이름, import 경로를 확인하는 용도로만 읽는다.
- 테스트가 실패하면 구현을 고치지 않고 원인을 보고한다.
- 백엔드 API 테스트가 우선이다. 프론트는 시간이 남을 때 화면 흐름만 확인한다.

## 환경 — `backend/tests/conftest.py` (없으면 처음 한 번 만든다)
```python
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from database import Base, get_db
from main import app

engine = create_engine(
    "sqlite://",
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSession = sessionmaker(bind=engine, autoflush=False)


@pytest.fixture
def client():
    Base.metadata.create_all(bind=engine)

    def override_get_db():
        db = TestingSession()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
    Base.metadata.drop_all(bind=engine)
```
`backend/` 구조가 다르면 import 경로만 맞춘다. `backend/pytest.ini`에 `pythonpath = .` 가 설정되어 있다.

## 기능 하나당 테스트
1. 정상: 생성 201, 조회 200, 응답 필드·값 검증
2. 없는 리소스: 404
3. 잘못된 입력: 422 / 명세의 비즈니스 규칙 위반: 400·409
4. 상태 변화: 생성 후 목록에 반영, 삭제 후 조회 404

## 이름
- 파일: `backend/tests/test_<도메인>.py`
- 함수: `test_<동작>_<조건>_<기대결과>` (예: `test_create_item_missing_title_returns_422`)

## 실행
`cd backend && .venv/bin/pytest -q`

## test-writer 보고 형식
```
[test-writer] <기능명>
- 작성: tests/test_xxx.py (N개)
- 결과: N passed / M failed
- 실패 원인: 기대값 vs 실제값, 명세/구현 중 어느 쪽 문제인지
```
