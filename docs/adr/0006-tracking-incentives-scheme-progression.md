# 0006 - Tracking progression in the Incentives Scheme

Date 2026-06-16

## Status

✅ Accepted

## Context

The Incentives Scheme is an initiative that rewards people on unpaid work requirements for good behaviour by offering the
opportunity to earn reductions in the time needed to fulfil the requirement.

To do this, it is necessary to have some way of keeping track of someone's progress within the scheme.

## Options
There are two broad approaches that can be taken: **explicitly tracked** or **computed on request**.

### Explicit tracking
This approach uses separate records, either within the Community Payback database or in NDelius, to keep track of
people's Incentives Scheme data.

Examples of data that would need to be recorded include:
- someone's eligibility
- whether they have qualified
- the awarded discount

### Computing on request
This approach uses data that already exists, where available, to derive information about the Incentives Scheme.

## Decision

The information contained within appointments and adjustments within Delius is already enough to be able to fully
determine a person's progress within the scheme. This means that it is unnecessary to make an explicit record of
qualifying events, which eliminates a possible source of conflicting information in the event that an appointment or
adjustment is updated outside of the Community Payback service.

As the Incentives Scheme is quite a complex process, its implementation will be split up into multiple focused parts,
such as:
- Common configuration
- Eligibility
- Event gathering
- Progression calculation
- Discount calculation
- Submitting time discounts to NDelius

These parts are not intended to be consumed by other parts of the service directly; instead, there will be an
`IncentivesSchemeService` which is intended to be the main interface for the Incentives Scheme.

In order to determine someone's progress within the Incentives Scheme, events (appointments and adjustments) will be
collected in chronological order. Each event will then be assessed to determine whether it is disqualifying, qualifying,
or neither:
- a disqualifying event is not counted towards a person's progress, and disqualifies them from continuing in the
  Incentives Scheme.
- a qualifying event is counted towards a person's progress as normal.
- an event that is neither is not counted towards a person's progress, but does not prevent them from making further
  progress in the Incentives Scheme. This can be used to gracefully handle appointments that do not have an  outcome,
  preventing them from being preemptively credited before they are resolved.

Once the events have been assessed, two pieces of information will be produced: the person's status in the scheme, and
the total amount of time they have worked that qualifies for the Incentives Scheme. This information can be used to
calculate the current and projected discounts.

Further information about how these calculations are made can be found in
[Incentives Scheme Calculations](/docs/incentives-scheme/calculations.md).

## Consequences

The first consequence of this decision is that there is no explicit record of someone's progress through the Incentives
Scheme, which makes it difficult for users of other services to see this information. However, it is likely that
Community Payback will be the owners of Incentives Scheme data. It is also an issue that can be mitigated with solutions
such as emitting events, providing access to the API to other services, or populating NDelius with non-authoritative
records containing this data.

Secondly, there may be a performance cost associated with performing this calculation on request, rather than returning
a precomputed result. However, the number of events is likely to be in the dozens per person, so in practice this is
likely to be acceptably small.

As mentioned above, one of the positive consequences is that because the Incentives Scheme data is fully derived from
existing data, it is impossible to create an inconsistent state by updating events outside of the Community Payback
service. If an appointment or adjustment are updated, these changes are automatically considered as soon as Incentives
Scheme data is next requested.

As a result, any apparent conflicts have one of three causes:
- The source data, recorded in NDelius, is incorrect and needs to be updated
- The user has misinterpreted the meaning of the data, and the guidance on the service may need clarification
- The calculation itself is incorrect, and needs to be fixed

In all three cases, correcting the issue immediately fixes all affected data without needing to perform any cleanup on
associated records.
