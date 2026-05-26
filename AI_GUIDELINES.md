# AI Guidelines

## Convention
- Commit: `<type>(<scope>): <subject>` (single-line, Korean subject)
- Use only user-provided branches
- Java 17 / 4 spaces
- Lombok: `@Getter @Builder @RequiredArgsConstructor @Slf4j`
- Forbidden: `@Data @AllArgsConstructor @Setter(entity)`

## Architecture
- `controller -> service -> repository -> domain -> common`
- No cross-service imports
- Domain = pure POJO

## Engineering
- No `SELECT *`
- Use `#{}` only
- Prevent N+1
- No broad/swallowed exceptions
- Use `BusinessException + ErrorCode`
- Mask sensitive logs + `traceId`
- No external API in transactions
- No cache without TTL
- No unpaged `findAll`

## Finance
- Money = `BIGINT`
- Rate = `INT` (bps)
- No hard delete
- Keep status history
- Encrypt/mask sensitive data
- Date = `CHAR(8)` (`YYYYMMDD`)
- Datetime = `TIMESTAMPTZ(3)`

## Workflow
- Read before write
- Show affected files before large changes
- Keep changes scoped to one domain/purpose
- Prefer root-cause fixes over workaround patches
- Confirm destructive changes
- Approve if changing >5 files
- Verify all services if `common` changes
- Report if unverified

## Forbidden
- No prod DB access
- No direct push to `main`
- No `.env` commits
- No AI traces

## Hard Stop
- Security risk
- Insufficient context
- Blindly generating >200 LOC

## Context Efficiency
- Read only relevant symbols/ranges
- Prefer minimal diff-based edits
- Avoid re-reading unchanged files
- Run targeted tests only
- Avoid full test/build logs
- Prefer minimal output (`--quiet`)
- Avoid `--debug` and full stack traces unless required
- Remove WHAT comments
- Keep WHY comments only
- Move long explanations to docs
- Keep API descriptions minimal
- Remove generated comments

## Interaction Efficiency
- Batch related work in one turn
- Prefer direct implementation/results
- Minimize unnecessary back-and-forth