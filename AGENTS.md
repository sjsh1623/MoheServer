# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/mohe/spring` follows the layered flow in `PROJECT_STRUCTURE.md`; route features `controller` → `service` → `repository` and park DTOs under `dto`.
- Place batch readers/processors/writers in `batch/{job,reader,processor,writer}`; reuse the same folder names when adding jobs.
- Configuration and security code belong in `config` and `security`; put YAML, SQL, and static assets in `src/main/resources`.
- Mirror production packages inside `src/test/java` and share fixtures or Spring profiles from `src/test/resources` such as `application-test.yml`.

## Build, Test, and Development Commands
- `./gradlew bootRun` starts the API; pair it with `docker-compose up postgres -d` when you need PostgreSQL.
- `./gradlew build` runs compilation plus verification and emits the fat JAR in `build/libs/`.
- `./gradlew test` executes the JUnit 5 suite (Spring Boot, Testcontainers); add `./gradlew clean` if the cache misbehaves.
- `docker-compose up --build` recreates the full stack described in `docker-compose.yml`; `docker-compose down` stops the services.
- `./run-with-env.sh` sources `.env` before invoking Gradle, keeping secrets out of your shell history.

## Coding Style & Naming Conventions
- Target Java 21 with four-space indentation and UTF-8 files; avoid wildcard imports.
- Name layers `{Domain}Controller`, `{Domain}Service`, `{Entity}Repository`, `{Action}Request`, `{Domain}Response` to match existing packages.
- Keep controllers thin, concentrate rules in services, let repositories stay data-only, and surface DTOs instead of entities.
- Run `./gradlew build` before pushing to respect the `-Xlint` warnings wired into `build.gradle`.

## Testing Guidelines
- Use JUnit 5 and Spring Boot test slices; name classes `{Subject}Test` or `{Subject}IntegrationTest` and mirror package paths under `src/test/java/com/mohe/spring/...`.
- Reach for Testcontainers when PostgreSQL logic is involved, configure credentials in `application-test.yml`, and stub external APIs with fixtures in `src/test/resources`.
- Cover happy paths plus failure branches so security, validation, and exception flows stay guarded.

## Commit & Pull Request Guidelines
- Follow the history pattern `<type>: <imperative summary>` (e.g., `refactor: streamline batch reader`); add scoped prefixes like `feat(place):` as needed.
- Keep commits focused and bundle schema or configuration updates that the change depends on.
- In PRs, link the tracking issue, explain the impact, list verification steps (`./gradlew test`, `docker-compose up`), and request review only after automation passes.

## Environment & Configuration
- Keep secrets in `.env`; `run-with-env.sh` reads the file for local runs. Never commit credentials.
- Document new keys in `README.md`, supply defaults via `application-*.yml`, and guard optional integrations (OpenAI, Naver, Google) with `@ConditionalOnProperty`.
