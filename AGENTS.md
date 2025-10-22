# Repository Guidelines

## Project Structure & Module Organization
Source lives under `src/main/java/com/mohe/spring`, following controller → service → repository layers per package. DTOs belong in `dto`, configuration in `config`, security in `security`, and Spring Batch jobs under `batch/{job,reader,processor,writer}` using a shared job name. Place integration resources (YAML, SQL, static assets) in `src/main/resources`. Tests mirror the main tree at `src/test/java/com/mohe/spring`, with shared fixtures and Spring profiles in `src/test/resources`.

## Build, Test, and Development Commands
Use `./gradlew build` for compilation, lint warnings, and the fat JAR in `build/libs/`. Run `./gradlew test` (or `./run-with-env.sh ./gradlew test`) to execute the JUnit 5 suite. Start the API locally with `./gradlew bootRun`, and pair it with `docker-compose up postgres -d` to launch Postgres. Rebuild the stack via `docker-compose up --build`; shut it down with `docker-compose down`.

## Coding Style & Naming Conventions
Target Java 21 with four-space indentation, UTF-8 encoding, and explicit imports. Keep controllers thin, delegating business rules to services and persistence logic to repositories. Name types following established patterns such as `PlaceController`, `PlaceService`, `CreatePlaceRequest`, and `PlaceResponse`. Run `./gradlew build` before pushing to surface `-Xlint` issues early.

## Testing Guidelines
Tests use JUnit 5 and Spring Boot test slices; leverage Testcontainers for PostgreSQL interactions. Name unit tests `{Subject}Test` and integration tests `{Subject}IntegrationTest`, aligning packages with production code. Configure database credentials in `src/test/resources/application-test.yml`, and store reusable stubs alongside. Cover both success paths and expected failures for validation, security, and external integrations.

## Commit & Pull Request Guidelines
Compose commits as `<type>: <imperative summary>` (e.g., `feat(place): add region caching`). Bundle schema or config changes with the code that needs them. Pull requests should link tracking issues, describe functional impact, list verification steps (e.g., `./gradlew test`, `docker-compose up --build`), and include screenshots or logs for UX or integration shifts. Request review only after automation passes.

## Security & Configuration Tips
Keep secrets in `.env` and run Gradle through `./run-with-env.sh` to avoid leaking credentials. Document new configuration keys in `README.md`, supply sane defaults via `application-*.yml`, and guard optional providers (OpenAI, Naver, Google) with `@ConditionalOnProperty`; disable them in tests by default.
