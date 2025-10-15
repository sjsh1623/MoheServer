# Repository Guidelines

## Project Structure & Module Organization
- Keep feature code in `src/main/java/com/mohe/spring`, moving controller → service → repository per `PROJECT_STRUCTURE.md`.
- Park DTOs under `dto`; services own business rules, repositories stay persistence-only.
- Add batch jobs to `src/main/java/com/mohe/spring/batch/{job,reader,processor,writer}` and reuse the job name across folders.
- Configuration and security live in `config` and `security`; YAML, SQL, and static assets live in `src/main/resources`.
- Mirror package paths under `src/test/java/com/mohe/spring`, and share fixtures or Spring profiles from `src/test/resources`.

## Build, Test, and Development Commands
- `./gradlew bootRun` starts the Spring API; pair it with `docker-compose up postgres -d` to launch PostgreSQL.
- `./gradlew build` compiles, runs verification, and emits the fat JAR under `build/libs/`.
- `./gradlew test` executes the JUnit 5 suite; add `clean` if the Gradle cache stalls.
- `docker-compose up --build` rebuilds the stack from `docker-compose.yml`; `docker-compose down` stops services.
- `./run-with-env.sh ./gradlew <task>` sources `.env` before running Gradle so secrets stay out of your shell history.

## Coding Style & Naming Conventions
- Target Java 21, four-space indentation, UTF-8 encoding, and avoid wildcard imports.
- Keep controllers thin, route logic through services, and leave repositories data-only.
- Name classes using established patterns (`PlaceController`, `PlaceService`, `PlaceRepository`, `CreatePlaceRequest`, `PlaceResponse`).
- Expose DTOs instead of entities in API layers, and run `./gradlew build` before pushing to catch `-Xlint` warnings.

## Testing Guidelines
- Use JUnit 5 with Spring Boot test slices and Testcontainers for PostgreSQL work.
- Name unit tests `{Subject}Test` and integration tests `{Subject}IntegrationTest`, mirroring source packages.
- Configure database credentials via `src/test/resources/application-test.yml` and store stubs in `src/test/resources`.
- Cover happy and failure paths, especially for validation, security, and external integrations.

## Commit & Pull Request Guidelines
- Follow `<type>: <imperative summary>` (e.g., `refactor: streamline batch reader`, `feat(place): add region caching`).
- Bundle schema or configuration changes with the code that requires them.
- Link PRs to the tracking issue, describe functional impact, and list verification steps (`./gradlew test`, `docker-compose up --build`).
- Request review after automation passes and attach screenshots or logs when UX or integration behavior shifts.

## Environment & Configuration
- Keep secrets in `.env`; run commands through `run-with-env.sh` to source them safely.
- Document new keys in `README.md` and provide sane defaults via `application-*.yml`.
- Guard optional providers (OpenAI, Naver, Google) with `@ConditionalOnProperty` and disable them in tests by default.
