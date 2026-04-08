# 0004 - Avoiding Orphaned NDelius Entites 

Date 2026-04-02

## Status

✅ Accepted

## Context

The following discussions assume transaction boundaries have been correctly defined and configured in the API (that changes to the postgres database _will_ rollback on unexpected errors)

Entities are created and updated in NDelius synchronously. These updates are sent to NDelius before our local transaction has committed (e.g. to save the upstream ID in our database). It's possible that after updating NDelius our local transaction doesn't commit, meaning we have no record of the upstream NDelius Entity creation. The user may then retry the request potentially creating a duplicate entity in NDelius. Scenarios where a request fails and isn't retried also need to be considered, as these create orphaned NDelius entities.

Note that communicating synchronously with NDelius (as opposed to using asynchronous processing triggered by domain events, outbox pattern etc.) was an early architectural decision due to the nature of user interaction patterns and the need for updates to be reflected immediately. Use of asynchronous communicate _may_ help alleviate this issue because it naturally breaks the reliance on tracking what we have created in NDelius in our database. Until all appointments are created and managed in community payback this will be difficult.

## Options

We will pre-emptively check if the required entity already exists in NDelius before creating it. This can be achieved in as follows:

1. By matching on existing properties on the entity 
2. By using a discrete identifier that is deterministic for the request being processed (e.g. if creating an appointment, such a value could be managed using the NDelius appointment's external reference). 

Once an existing NDelius entity has been identified we can either update it, leave as is (if it's in the required state) or delete and recreate the entity.

To avoid orphaned entities (where the request isn't retried), we should add a compensating transaction that runs on failure/rollback of the request

## Decision

* We will use a discrete 'external reference' stored on NDelius Entities to handle these scenarios. Whilst existing properties may be sufficient it may be difficult, if not impossible, to state with 100% certainty an entity was created for a specific request.
* We will always raise an alert regardless of whether the issue has been dealt with so we can track and investigate such occurrences
* A compensating transaction will be configured on error/transaction rollback to remove any entities that have been created by NDelius in the thread of execution. If this fails an alert should be raised. Ideally this logic would apply to all endpoints, by monitoring NDelius entity creation in all threads
* If a delete endpoint does not yet exist we will alert to support manual investigation and intervention can be applied

For example:

* When creating an adjustment for a travel time task we can use the corresponding community payback appointment id for the external reference, or the travel time task id
* When creating an appointment for a course completion we can use the corresponding course completion id for the external reference 
