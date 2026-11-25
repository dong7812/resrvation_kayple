# backend — FastAPI 규칙

## 스택
Python 3.12, FastAPI, SQLAlchemy **2.0**, Pydantic **v2**, SQLite

## 구조
```
backend/
├─ main.py          # 앱 생성, CORS, 라우터 등록
├─ database.py      # engine, SessionLocal, Base, get_db
├─ models.py        # SQLAlchemy 모델
├─ schemas.py       # Pydantic 스키마
├─ routers/<도메인>.py
└─ tests/           # test-writer 전용
```
기능이 1~2개뿐이면 `routers/` 없이 `main.py`에 둬도 된다. 3개 이상이면 분리한다.

## database.py
- `DATABASE_URL = "sqlite:///./app.db"`, SQLite면 `connect_args={"check_same_thread": False}`
- `class Base(DeclarativeBase)`
- `get_db()`는 `yield` 의존성으로 세션을 열고 닫는다 (테스트에서 override 하기 위함)
- 테이블 생성은 `lifespan`에서 `Base.metadata.create_all(bind=engine)`

## SQLAlchemy 2.0 스타일만
- 모델: `Mapped[...]`, `mapped_column(...)`
- 조회: `db.scalars(select(Model)...).all()`, 단건 `db.get(Model, id)`
- 금지: `db.query(...)` (1.x 스타일)

## Pydantic v2
- 스키마 분리: `XxxCreate` / `XxxUpdate` (전부 Optional) / `XxxOut`
- `XxxOut`: `model_config = ConfigDict(from_attributes=True)`
- 수정 시 `body.model_dump(exclude_unset=True)`
- 입력 검증은 `Field(min_length=..., ge=...)`로 스키마에서 처리

## REST 규칙
| 동작 | Method / Path | 성공 |
|---|---|---|
| 목록 | `GET /items` | 200 |
| 단건 | `GET /items/{id}` | 200 |
| 생성 | `POST /items` | 201 |
| 수정 | `PATCH /items/{id}` | 200 |
| 삭제 | `DELETE /items/{id}` | 204 |

## 에러
- `HTTPException(status_code=..., detail="...")`
- 없음 404 / 비즈니스 규칙 위반 400 / 중복·충돌 409 / 입력 형식 422 (Pydantic 자동)
- `detail`은 화면에 그대로 보여줄 수 있는 문장으로

## CORS
`allow_origins=["http://localhost:3000"]`, `allow_methods=["*"]`, `allow_headers=["*"]`
