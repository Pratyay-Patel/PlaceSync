# CLAUDE.md — PlaceSync

Permanent instruction manual for Claude Code when working in this repository.
Read this file at the start of every session. It governs all behaviour.

---

## 1. Project Overview

PlaceSync is a production-grade, cloud-native SaaS platform that modernises campus placement and recruitment operations. It replaces spreadsheets and email chains with a unified, role-aware system for students, recruiters, and placement administrators.

**Learning goals:** Build end-to-end production experience across the full modern Java/React stack — Spring Boot 3, Kafka, S3, CI/CD, Docker, Nginx, OAuth2.

**Resume goals:** Demonstrate production-quality backend architecture, cloud integration, event-driven design, and full-stack delivery.

**Mindset:** Every decision should be defensible in a production code review. No shortcuts that would embarrass a professional engineer.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend framework | Spring Boot 3.3.6 (modular monolith) |
| Build tool | Maven |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Hibernate 6 / Spring Data JPA |
| Caching | Redis 7 |
| Messaging | Apache Kafka |
| Object storage | AWS S3 (SDK v2) |
| Security | Spring Security 6, JWT (jjwt 0.12), OAuth2 (Google) |
| Mapper layer | MapStruct |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Frontend | React 18, TypeScript 5, Vite 5 |
| UI library | Material UI v5 |
| HTTP client | Axios + TanStack Query |
| Frontend state | Zustand |
| Containerisation | Docker, Docker Compose |
| Reverse proxy | Nginx |
| CI/CD | GitHub Actions |
| Code quality | SonarLint (local), SonarCloud (CI) |
| Test frameworks | JUnit 5, Mockito, Testcontainers |

**Environment note:** Every `mvn` command must be prefixed with `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"` because the system default is JDK 8.

---

## 3. Source of Truth

The following documents are authoritative. Never contradict them without explicit user approval:

- `docs/IMPLEMENTATION_PLAN.md` — phased build-out, subphase acceptance criteria, status tracking
- `docs/SRS.md` — all functional and non-functional requirements
- `docs/ARCHITECTURE.md` — system architecture, module boundaries, infrastructure design
- `docs/DATABASE_DESIGN.md` — schema, table definitions, indexing strategy, constraints

If a decision conflicts with any of these documents, surface the conflict and ask before proceeding.

---

## 4. Development Philosophy

- **Maintainability over cleverness** — code that a future reader can understand without context
- **Consistency over novelty** — follow established patterns within this codebase
- **SOLID principles** — single responsibility, open/closed, dependency inversion throughout
- **Clean Architecture layers** — Controller → Service → Repository; never skip layers
- **Simplicity over premature optimisation** — optimise only when there is a measured problem
- **Production mindset** — security, logging, validation, and error handling are not optional extras

---

## 5. Implementation Workflow

This is the most important section. Follow it exactly for every numbered subphase in `IMPLEMENTATION_PLAN.md`.

**Before starting a subphase:**
- Re-read the relevant section of `IMPLEMENTATION_PLAN.md`.
- Read only the documentation needed for that subphase (token efficiency).
- Confirm the subphase scope matches the plan before writing any code.

**After completing a subphase:**

1. Run all relevant verification steps (`mvn clean verify`, smoke test if applicable).
2. Update `IMPLEMENTATION_PLAN.md` — mark the subphase complete, check off acceptance criteria.
3. Provide a completion summary:
   - Files created (with package paths)
   - Files modified (with what changed)
   - Dependencies added (groupId:artifactId:version)
   - Endpoints added (METHOD path → description)
   - Acceptance criteria satisfied
   - Verification performed
4. Suggest a Conventional Commit message.
5. State the branch status and confirm this is a good commit point.
6. **Stop. Wait for user confirmation before starting the next subphase.**

Never chain subphases. Never say "I'll continue with the next step." Stop and wait.

---

## 6. Git Workflow

**Claude never performs Git operations.** The user commits, pushes, merges, and manages branches manually.

Claude may recommend:
- Branch names (following `feat/<scope>`, `fix/<scope>`, `chore/<scope>` conventions)
- Conventional Commit messages (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`)
- When to raise a PR and what to describe in it
- When a commit point has been reached

Claude must never:
- Run `git commit`, `git push`, `git merge`, `git rebase`, `git reset`, or `git branch -D`
- Stage files with `git add`
- Amend commits
- Force-push anything

**`.gitignore` rule:** If any build artifact, IDE file, generated file, or binary is untracked in git status, add it to `.gitignore` immediately without being asked.

---

## 7. Coding Standards

**Dependency injection:** Constructor injection only. Never `@Autowired` on a field.

**Lombok:** Use `@RequiredArgsConstructor` on service and controller classes. Use `@Getter`, `@Setter`, `@Builder`, `@Data` on DTOs/entities where appropriate. Avoid Lombok where it obscures intent.

**Validation:** Apply Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, etc.) on all request DTOs. Use `@Valid` on all `@RequestBody` parameters. Use custom `ConstraintValidator` implementations for business-rule validations.

**DTO pattern:** Never expose JPA entities in API responses. Always map to a dedicated response DTO. Use MapStruct mappers — never manual mapping in service methods.

**Repository pattern:** Repositories are interfaces extending Spring Data repositories. No SQL in service classes.

**Service layer:** All business logic lives in `@Service` classes. Controllers delegate immediately to services and perform no logic of their own.

**Exception handling:** All exception-to-HTTP-status mapping goes through `GlobalExceptionHandler`. Never return error details from controllers directly. Never catch and swallow exceptions silently.

**Logging:** SLF4J only — `private static final Logger log = LoggerFactory.getLogger(ClassName.class)`. No `System.out.println`. Log at INFO for write operations, WARN for recoverable failures, ERROR with the exception object for unhandled cases.

**Package structure:** One package per module (`auth`, `user`, `recruiter`, `company`, `job`, `application`, `interview`, `notification`, `analytics`, `common`). Sub-packages: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`.

**Naming:** Classes are nouns. Methods are verbs. No abbreviations. Names should be self-documenting.

**PostgreSQL enums:** All entity fields mapped to PostgreSQL custom enum types must carry `@JdbcType(PostgreSQLEnumJdbcType.class)`. Never omit this annotation.

**Comments:** Default to writing no comments. Add one only when the WHY is non-obvious (a hidden constraint, a workaround, a subtle invariant). Never comment what the code does.

---

## 8. Testing Requirements

A subphase is not done if any of the following fail:

- `mvn clean verify` exits non-zero
- Any existing test regresses
- Acceptance criteria from `IMPLEMENTATION_PLAN.md` are unchecked

**Unit tests (Mockito):** Use `@ExtendWith(MockitoExtension.class)`. Mock all repository and external-service dependencies. Test both happy paths and all documented error paths. Name methods: `methodName_scenario_expectedBehaviour()`.

**Integration tests (Testcontainers):** Use real PostgreSQL via `@Testcontainers`. Do not use H2 for integration tests — PostgreSQL-specific features (custom ENUMs, constraints) will not be exercised.

**Security tests:** Every access-control rule must have a corresponding test (wrong role → 403, no token → 401, cross-user access → 403/404).

When implementing a feature, proactively suggest the corresponding tests even if the user has not asked for them.

---

## 9. Documentation Rules

- Update `IMPLEMENTATION_PLAN.md` after every subphase — status, acceptance criteria checkboxes, "What was built" note.
- Do not create new documentation files unless the user asks.
- Do not generate READMEs, ADRs, or design docs unprompted.
- Keep existing docs synchronised with implementation — if an endpoint changes, update the plan's endpoint table.
- Never duplicate content across documents. Reference the authoritative source instead.

---

## 10. Token Efficiency

This project is developed on a Claude Pro subscription. Conserve context aggressively.

- **Incremental implementation:** Implement one subphase at a time. Never look ahead.
- **Targeted reads:** Read only the files relevant to the current subphase. Do not re-read files already in context.
- **Surgical edits:** Use the `Edit` tool with precise `old_string`/`new_string` pairs rather than rewriting entire files.
- **Concise responses:** Summaries should be complete but not padded. One bullet per fact.
- **No speculative scaffolding:** Do not create files, stubs, or placeholder classes for future subphases.
- **No architectural repetition:** Do not restate architecture decisions that are already in `ARCHITECTURE.md`.

---

## 11. Things Claude Must Never Do

- **Never invent requirements.** If something is not in `SRS.md` or `IMPLEMENTATION_PLAN.md`, do not implement it.
- **Never silently change architecture.** If the correct implementation requires deviating from `ARCHITECTURE.md`, surface the conflict first.
- **Never skip verification.** `mvn clean verify` must pass before a subphase is declared complete.
- **Never ignore the implementation plan.** The subphase scope is defined in `IMPLEMENTATION_PLAN.md` — do not expand it.
- **Never rewrite large files unnecessarily.** Use targeted edits.
- **Never implement a future subphase or phase without explicit user approval.**
- **Never perform Git operations** (commit, push, merge, rebase, reset, branch delete).
- **Never mark a subphase complete** without running the build and checking the acceptance criteria.
- **Never continue to the next subphase** without waiting for user confirmation.
- **Never use `System.out.println`.** Use SLF4J logging.
- **Never expose JPA entities** in API response bodies.
- **Never add dependencies** to `pom.xml` that are not required by the current subphase.

---

## 12. Definition of Done

A subphase is complete **only** when all of the following are true:

- [ ] Implementation matches the scope defined in `IMPLEMENTATION_PLAN.md`
- [ ] `mvn clean verify` passes (with `JAVA_HOME` set to JDK 21)
- [ ] All acceptance criteria checkboxes for the subphase are satisfied
- [ ] No existing tests have regressed
- [ ] `IMPLEMENTATION_PLAN.md` is updated (status, acceptance criteria, "What was built")
- [ ] A completion summary has been provided (files, deps, endpoints, criteria)
- [ ] A Conventional Commit message has been suggested
- [ ] Claude has stopped and is waiting for user confirmation
