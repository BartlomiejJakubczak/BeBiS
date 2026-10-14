# BeBiS — "Be Best in Slot"

Spring Boot backend that logs a player in through Battle.net OAuth, syncs their **World of Warcraft Classic (Era, EU)**
characters, equipment and talents from the Blizzard API, scores items for the character's class/spec, and finds gear
upgrades per equipment slot.

## Stack

- Java 21, Spring Boot 3.5, Maven (use the wrapper: `./mvnw` / `mvnw.cmd`)
- PostgreSQL 16 (Flyway migrations, `ddl-auto: validate`), Redis 7 (Spring Cache)
- Spring Security OAuth2 client (two Blizzard registrations: `blizzard-service` = client credentials, `blizzard-user` =
  authorization code, scope `wow.profile`)
- Resilience4j (retry + circuit breaker around Blizzard fetchers), virtual threads, `@Async`
- Lombok is present but used sparingly; most classes use explicit constructors
- Tests: JUnit 5, Mockito, AssertJ, Testcontainers (Postgres + Redis), WireMock, Awaitility

## Running

```bash
docker compose up -d          # postgres:5432, redis:6379, pgadmin:5050
./mvnw spring-boot:run        # needs BLIZZARD_CLIENT_ID and BLIZZARD_CLIENT_SECRET env vars
./mvnw test                   # unit tests only (surefire: *Test)
./mvnw verify                 # unit + integration tests (failsafe: *IT); Docker must be running for Testcontainers
```

The local JDK is 25, so Lombok must stay declared in `maven-compiler-plugin`'s `annotationProcessorPaths` (JDK 23+
no longer discovers annotation processors from the classpath).

## Package layout (`com.bebis.BeBiS`)

Packages are organized by feature, and each one has the same internal layout:

- root of the feature package: services, mappers, orchestrators, synchronizers, fetchers
- `domain/`: immutable-ish domain models (records, sealed hierarchies) plus `domain/exception/`
- `jpa/`: entities, repositories, entity factories
- `dto/`: internal sync data carriers
- `event/` and `event/listener/`: Spring application events

| Package                    | Responsibility                                                                                                                                          |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `integration.blizzard`     | `BlizzardServiceClient` / `BlizzardUserClient` (RestClient) and raw API response DTOs                                                                   |
| `profile`                  | Account summary, character selection, talents/specs. `CharacterProfileOrchestrator` is the facade used by `ProfileController`                           |
| `equipment`                | Equipped gear sync and mapping (`Equipment`, `EquippedItem`, `Equipment.Slot`)                                                                          |
| `item`                     | Item fetching, mapping and persistence. `Item` is a sealed hierarchy: `Weapon`, `Armor`, `EquippableItem`. Identity is `ItemKey(baseId, suffixId)`      |
| `engine.power`             | Item scoring: `PowerCalculatorEngine` delegates to `AttributeScoreCalculator` and `WeaponScoreCalculator`, using stat weight registries per `ClassSpec` |
| `engine.upgrade`           | Upgrade finding (see below)                                                                                                                             |
| `config`, `audit`, `tools` | Spring config, auditing, small static helpers (`EnumTools`, `MapperTools`)                                                                              |

## Upgrade engine flow

1. `CharacterProfileOrchestrator.getCharacterInfo` builds `CharacterInfo` and publishes `CharacterSelectedEvent`.
2. `CharacterSelectedListener` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) calls
   `UpgradeFinderService.findUpgradesFor`.
3. `UpgradeFinderService` fans out to three orchestrators (each `@Async`):
    - `SingleSlotUpgradeOrchestrator`: isolated slots (head, neck, ..., ranged)
    - `DualSlotUpgradeOrchestrator`: trinkets and rings, evaluated as pairs and respecting unique-equipped
    - `HandSlotUpgradeOrchestrator`: main hand and off hand combinations, constrained by `ClassHandSlotRegistry`
4. Each orchestrator gets an item pool from `ItemPoolProvider`, then runs its `*UpgradeFinder` (all extend the abstract
   `UpgradeFinder`). Results are a sealed `UpgradeResult` that `UpgradeResultEvaluator` validates.
5. `UpgradeEventPublisher` maps each `UpgradeResult` to a sealed `UpgradeEvent` and publishes it.

Scoring sorts by score descending, then by `baseId` so results are deterministic. Keep that tie-breaker when you add new
sorting.

## Conventions

- **Encapsulation**: keep classes package-private and `final` wherever possible. Only the feature's entry points are
  `public`. Integration tests live in the same package as the class under test so they can reach package-private types.
- **Constructor injection** with explicit constructors, never field `@Autowired` in production code.
- **Sealed types + pattern-matching `switch`** for closed hierarchies (`Item`, `UpgradeResult`, `UpgradeEvent`). Prefer
  exhaustive switches over `instanceof` chains.
- **Optionals over nulls** for absent values (e.g. `Optional<EquippedItem>`, `Optional<WowTalents>`, `StatType`
  lookups).
- **Mapping**: Blizzard DTO → (`*SyncData`) → entity → domain. Mappers validate and throw domain exceptions (
  `InvalidItemException`, `InvalidCharacterException`, ...). Services skip invalid items rather than failing the whole
  batch.
- **Naming**: `*Orchestrator` coordinates async work for a feature, `*Finder` holds pure logic, `*Synchronizer`
  reconciles API data with the DB, `*Fetcher` wraps Blizzard calls with resilience, `*Registry` holds declarative lookup
  tables.
- **Schema changes** go into a new Flyway migration in `src/main/resources/db/migration/` (`V2__...sql`, ...). Never
  edit `V1__Initial_Schema.sql`.

## Testing conventions

- `*Test` = unit test (Mockito, `@ExtendWith(MockitoExtension.class)`). `*IT` = Spring/Testcontainers integration test.
- Base classes in `src/test/java/com/bebis/BeBiS/base/` (`BaseContainerTest` is sealed):
    - `BaseFullStackTest`: `@SpringBootTest`, transactional, Blizzard clients replaced with `@MockitoBean`, caches
      cleared after each test
    - `BaseNonTransactionalFullstackTest`: for async and event-driven ITs. Tear down manually (e.g.
      `TRUNCATE TABLE items CASCADE`)
    - `BasePersistenceTest`: `@DataJpaTest` against real Postgres
    - `BaseWiremockTest` / `BaseResilienceTest`: Blizzard HTTP and resilience behavior (`application-wiremock.yml`, port
        8089)
    - `BaseAsyncListenerTest`: listener tests; `@Import` the listener under test
- Build test data with the builders (`EquippableItemBuilder`, `WeaponBuilder`, `ArmorBuilder`, `ItemResponseBuilder`,
  `EquipmentResponseBuilder`, `WowCharacterBuilder`), not hand-made objects. `ProfileTestData` is legacy and should
  eventually become a builder.
- Use the `// given / // when / // then` structure, AssertJ assertions, and `@ParameterizedTest` + `@MethodSource` for
  case matrices. Use Awaitility for async assertions.
- Method naming: `shouldDoX_WhenCondition` (e.g. `shouldSignalUpgradeFoundResult_EmptyEquippedItem`).

## Domain notes (WoW Classic)

- `ClassSpec` is `CLASS_SPEC`. Characters with no clear spec fall back to `CLASS_NONE`. The active spec is derived from
  the talent tree; ties throw `TalentTreeTieInException`.
- Items with random suffixes ("of the Bear") share a `baseId` and differ by `suffixId`.
- Unique-equipped items can't occupy both ring or trinket slots.
- Shirt and tabard are not part of `Equipment.Slot`.

## Working with me (Claude)

- The developer cares about the learning process that comes with project development. Never provide code as an answer
  upfront, unless directly asked.
- When asked for code, inspect if the given problem presents itself with a good learning opportunity, like for example
  something that the developer hasn't seen yet.
  If that's the case, mention that once before presenting the solution.
- Treat yourself as a senior developer guiding me through project development, performing code reviews and providing
  deep architectural insights adhering to the best possible coding practices and architectural conventions currently on
  the market.
- Don't commit unless asked. Commit messages are lowercase, past-tense summaries of what changed (see `git log`).
