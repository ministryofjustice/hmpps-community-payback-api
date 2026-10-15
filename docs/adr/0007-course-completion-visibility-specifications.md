# 0007 - Compose course completion listing filters with JPA specifications

Date: 2026-10-14

## Status

✅ Accepted

## Context

The admin UI retrieves ETE course completions through `GET /admin/providers/{providerCode}/course-completions`. The listing needs to support rollout by office, with a different course completion start timestamp for each office. Developers must be able to configure any number of offices, enable restrictions independently for each deployment and bypass restrictions through a show-all setting.

The UI does not need to send an office query parameter to activate rollout restrictions. The API applies them automatically alongside the existing provider, optional PDU, requested offices, resolution status, course-failure handling, external reference and completion date range filters.

Before this change, `EteCourseCompletionEventEntityRepository.findAllWithFilters` used a single `@Query` containing JPQL. Optional filters were represented by clauses such as:

```sql
AND (:officesCount = 0 OR e.office IN :offices)
AND (CAST(:fromDate AS timestamp) IS NULL OR e.completionDateTime >= :fromDate)
```

This was a reasonable representation of a fixed set of filters. The new requirement adds a variable-length group of paired conditions:

```sql
AND (
  (e.office = :officeA AND e.completionDateTime >= :officeAStart)
  OR
  (e.office = :officeB AND e.completionDateTime >= :officeBStart)
  OR ...
)
```

An office list and a single shared date cannot express this requirement. Each date must remain associated with its own office. Independently checking membership in a list of offices and dates could match the wrong office/date combination.

Visibility must be applied in the database before paging and counting. Filtering a page after retrieval can leave it partly empty and leave `totalElements` and `totalPages` describing records that are not actually visible.

## Why a specification cannot simply be added to the existing query

Spring Data JPA has separate execution paths for declared queries and specification queries.

An `@Query` method executes the declared JPQL or SQL and binds its parameters. Spring Data specially recognises `Pageable` and `Sort` to apply pagination and sorting. That support does not make arbitrary query-building objects special method parameters. See [Spring Data query methods and special parameters](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html) and [declared JPA queries](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).

Adding a `Specification` or `CriteriaBuilder` parameter to an `@Query` method does **not** cause Spring Data to convert it into extra conditions and append them to the declared query. A bound value is also not a JPQL fragment: an office name can be bound to `:office`, but a dynamically constructed `OR` expression cannot be bound as if it were a string value representing executable query syntax.

The supported specification execution path is `JpaSpecificationExecutor`, which provides `findAll(Specification<T>, Pageable)`. It constructs a criteria query from the supplied specification and then applies pagination. See the [executor API](https://docs.spring.io/spring-data/jpa/reference/4.0/api/java/org/springframework/data/jpa/repository/JpaSpecificationExecutor.html).

This distinction is the reason for refactoring the existing listing filters, rather than retaining the annotation and adding one specification argument. Specifications are not inherently required for every dynamic query; a custom repository implementation is another possible solution. They are the approach chosen here for composing the existing filters with the new variable-length visibility group.

## Decision

Use Spring Data JPA specifications for the course completion listing query.

- Extend `EteCourseCompletionEventEntityRepository` with `JpaSpecificationExecutor<EteCourseCompletionEventEntity>` alongside `JpaRepository`.
- Keep `findAllWithFilters` as the repository entry point used by the service.
- Replace its annotated query with an implementation that composes named specifications and delegates to `findAll(specification, pageable)`.
- Define the individual predicates in `EteCourseCompletionEventSpecifications`, with equivalent JPQL comments to help readers relate the new code to the old query.
- Use deployment configuration from Helm to control show-all and office/date rules. Do not inspect environment names or Spring profiles in visibility logic.

This decision applies to the listing query. `findByCrnAndReceivedAt` and `findBlock` retain their existing `@Query` implementations because their fixed query structure does not need configurable office/date predicates.

## How the specifications work

### A specification describes a database predicate

A specification is a function that constructs a JPA Criteria API predicate. It does not load records or filter Kotlin objects in memory. Spring Data's [specification documentation](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html) describes this execution model.

For example, the implementation of `completedFrom` is:

```kotlin
fun completedFrom(from: OffsetDateTime?) = Specification<EteCourseCompletionEventEntity> { root, _, cb ->
  from?.let { cb.greaterThanOrEqualTo(root.get("completionDateTime"), it) }
}
```

In this implementation, `root` describes the completion entity and its property paths. The unused second argument is the criteria query. `cb` is the criteria builder, which creates comparisons and logical operations. The lambda contributes the equivalent of `e.completionDateTime >= :from` when a timestamp is supplied.

The timestamp remains a query value handled by JPA. Office names and timestamps are not concatenated into executable query strings.

### Optional conditions contribute no predicate

If `from` is null, `completedFrom` returns a null predicate. Spring Data's specification composition excludes that predicate. This preserves the old behaviour of ignoring an optional filter when its parameter is absent.

A null predicate does not mean `completionDateTime IS NULL`. Similarly, a null `pduId` means no additional PDU restriction; the mandatory provider filter remains effective.

`Specification.unrestricted()` also contributes no predicate. It is not an instruction to discard the other specifications. See the [Specification API](https://docs.spring.io/spring-data/jpa/reference/4.0/api/java/org/springframework/data/jpa/domain/Specification.html) for null-predicate and composition semantics.

### Existing request filters are combined with AND

`findAllWithFilters` calls `Specification.allOf` with the provider, PDU, requested offices, resolution, course-failure, reference, date and visibility specifications.

For this implementation, the resulting structure is:

```text
provider
AND optional PDU
AND optional requested offices
AND resolution selection
AND course-failure selection
AND optional external reference
AND optional completion start/end range
AND configured visibility
```

All contributing predicates must match. Optional specifications that contribute no predicate do not alter the remaining conditions.

The existing resolution semantics are retained: resolved and unresolved selection use a left join and test the resolution ID for non-null or null respectively. Selecting any resolution status adds no resolution predicate or join. The left join is needed to retain rows without a resolution when selecting unresolved completions.

The existing course-failure semantics are also retained: hide failures selects passed completions; show all adds no status predicate; only when maximum attempts are reached selects passed completions or records where `MOD(attempts, 3) = 0`. This refactoring does not redefine the attempt-count rule.

### Each office rule combines office and date with AND

For every configured office, `visibleOffices` builds:

```kotlin
offices(listOf(office.name)).and(completedFrom(requireNotNull(office.from)))
```

An Office A rule therefore means Office A **and** a completion timestamp at or after Office A's start. The office and date are inseparable parts of that rule.

The `from` field is validated as required in configuration. `requireNotNull` also prevents a directly constructed invalid rule from silently becoming an unrestricted date predicate.

### Office rules are combined with OR

`Specification.anyOf` combines the individual office rules. A completion needs to match one complete office/date pair, not every office.

For example, suppose Office A starts at `2026-10-01T00:00:00+01:00` and Office B at `2026-11-01T00:00:00Z`. A completion for Office B in October does not match Office A's earlier date, because the Office A branch also requires its office name to match.

The complete query is logically:

```sql
WHERE e.pdu.providerCode = :providerCode
  AND /* other requested filters */
  (
    (e.office = :officeA AND e.completionDateTime >= :officeAStart)
    OR
    (e.office = :officeB AND e.completionDateTime >= :officeBStart)
  )
```

This is illustrative JPQL, not a literal query string used at runtime. JPA generates SQL from the composed criteria. The grouping is important: a match for Office B must not bypass the provider or other requested filters.

### Show-all and an empty restricted office list are different

| Visibility input | Specification behaviour | Listing effect |
| --- | --- | --- |
| No visibility configuration passed | `unrestricted()` | No configured office/date predicate |
| `showAll = true` | `unrestricted()` | No configured office/date predicate |
| `showAll = false`, with office rules | OR of office/date pairs | Only matching configured completions |
| `showAll = false`, with no offices | `cb.disjunction()` | No matching completions |

`cb.disjunction()` with no operands produces an always-false predicate, logically equivalent to `WHERE 1 = 0`. The empty-list case is explicit: an empty configured allowlist must not be treated as unrestricted. In particular, an empty collection passed to specification composition is not a reliable way to express deny-all; the implementation deliberately returns false.

The requested-office filter has a different contract. `findAllWithFilters` passes no office predicate when the request has no offices. An explicit request filter still intersects the configured rules. Show-all bypasses configured visibility, not the caller's office selection or any other request filter.

### Paging, sorting and totals use the combined predicate

The repository passes the composed specification and `Pageable` to `JpaSpecificationExecutor.findAll`. The visible result set is established before pagination. Sorting and page limits apply to that set, and count queries, when needed, use the same specification to determine totals.

Do not execute one paged query per office and concatenate the results: that would need separate global sorting, duplicate handling and counting. The implementation produces one composed result query, plus a count query when required by page execution; it does not issue a separate listing query for every specification.

## Configuration and examples

Shared application and Helm configuration defaults to showing all. Production's Helm values set `COURSE_COMPLETIONS_VISIBILITY_SHOW_ALL` to `"false"`. Dev and preprod inherit `"true"` unless their environment values override it. Restricting additional deployments means configuring their Helm values, not adding environment checks to Kotlin.

An example under `generic-service.env` is:

```yaml
COURSE_COMPLETIONS_VISIBILITY_SHOW_ALL: "false"
COURSE_COMPLETIONS_VISIBILITY_OFFICES_0_NAME: "Office A"
COURSE_COMPLETIONS_VISIBILITY_OFFICES_0_FROM: "2026-10-01T00:00:00+01:00"
COURSE_COMPLETIONS_VISIBILITY_OFFICES_1_NAME: "Office B"
COURSE_COMPLETIONS_VISIBILITY_OFFICES_1_FROM: "2026-11-01T00:00:00Z"
```

Names must exactly match stored office names, including case. Cutoffs are inclusive and use course completion time, not event receipt time. Offsets identify precise instants: midnight at UTC+1 is the preceding day's 23:00 UTC. Duplicate rules for an office are ORed, effectively applying the earliest start for that office. Rules are not additionally scoped by PDU in this implementation; the normal provider/PDU filters still apply.

Production currently has no active office rules. With show-all false, its listing is therefore empty until rules are provided or show-all is enabled. Configuration changes require restart/redeployment.

See the [developer configuration guide](../course-completion-visibility.md) for additional examples and operational behaviour.
