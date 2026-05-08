# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

사용자의 메모를 Claude AI로 인사이트·태그·질문으로 변환하는 Spring Boot 4.0.2 백엔드. PostgreSQL(pgvector) + Google OAuth2 + JWT 인증.

## 주요 명령어

```bash
./gradlew bootRun     # 실행
./gradlew test        # 전체 테스트
./gradlew test --tests "com.dnd.ahaive.domain.insight.service.InsightServiceTest"
./gradlew build       # 빌드 + 테스트
```

환경변수는 프로젝트 루트 `.env`에 설정 (`DB_DEV_*`, `ANTHROPIC_API_KEY`, `JWT_SECRET`, `GOOGLE_CLIENT_*`). 테스트는 H2 인메모리 + `application-test.yaml` 프로파일.

## 아키텍처

**패키지**: `domain/` (도메인별 로직) · `global/` (예외·보안·설정) · `infra/claude/` (AI 클라이언트)

**도메인 내부 구조**: `controller/` · `service/` · `dto/{request,response}/` · `entity/` · `repository/` · `exception/`

**인사이트 생성 플로우** (핵심):
```
InsightService (오케스트레이터)
  → InsightAiService: CompletableFuture로 병렬 AI 호출
      (TitleAiClient / PieceAiClient / TagAiClient / QuestionAiClient)
  → InsightCreationService: 트랜잭션 저장 (Insight → Piece → Tag → Question)
```

**AI 인프라** (`infra/claude/`): `ClaudeAiClient`(Spring AI 래퍼) · `ClaudeAiPrompt`(프롬프트 팩토리) · AI 기능별 클라이언트. 실패 시 `AiCallException` / `AiResponseParseException`.

**인증**: Google OAuth2 로그인 → JWT 발급 → `JwtTokenFilter`로 매 요청 검증. 컨트롤러에서 `@AuthenticationPrincipal CustomUserDetails`로 사용자 접근. `InsightValidator`로 소유권 검증.

**API 응답**: 모든 엔드포인트는 `ResponseDTO`(localDateTime, responseCode, statusCode, message, data)로 표준화. 에러는 `ErrorCode` enum + `GlobalExceptionHandler`.

**주요 엔티티**:
- `Insight` — `initThought`, `title`, 조회수, trash flag. InsightPiece·InsightTag와 1:N
- `InsightPiece` — `createdType`: INIT(AI생성) / SELF(사용자) / ANSWER(답변 전환)
- `Question` — status: `WAITING → COMPLETED → ARCHIVED`. Answer와 1:1
- `User` — `userUuid`, `role`(GUEST/MEMBER). `createGuest()` / `createMember()` 정적 팩토리

## 객체지향 설계 원칙

- **엔티티에 행동을 둔다**: 상태 변경은 의미 있는 메서드로 표현한다. 서비스에서 필드를 직접 조작하지 않는다 (`insight.setTrash(true)` 대신 `insight.moveToTrash()`).
- **Setter 금지**: 엔티티에 `@Setter`를 사용하지 않는다. 상태 전이 조건 검증은 엔티티 내부에서 처리한다.
- **생성자 보호**: `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + `private @Builder`로 외부 직접 생성을 막고, 정적 팩토리 메서드(`from()`, `of()`, `createXxx()`)로만 생성한다.
- **도메인 예외 사용**: 비즈니스 예외는 반드시 `CustomException`을 상속한 도메인 예외를 던진다. JPA `EntityNotFoundException` 같은 프레임워크 예외를 서비스 레이어에서 직접 던지지 않는다.
- **서비스 단일 책임**: 기존 서비스에 메서드를 계속 추가하기 전에 책임 분리를 먼저 검토한다 (오케스트레이터 / 저장 / AI 호출 / 검증 역할 분리 참고).
- **로직 위치 원칙**: 자신의 필드에 대한 판단은 엔티티에, 여러 엔티티를 조합하는 로직은 서비스에, 엔티티→응답 변환은 DTO의 정적 팩토리 메서드에 둔다. 컨트롤러에 비즈니스 로직을 두지 않는다.

## Git 컨벤션

- 브랜치: `feature-*`, `fix-*`, `refactor/*`
- 커밋 접두사: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`
- PR 대상: `develop` (main은 릴리즈 전용)
