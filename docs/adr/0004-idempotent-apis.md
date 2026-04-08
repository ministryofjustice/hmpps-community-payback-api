# 0004 - API Idempotency 

Date 2025-09-25

## Status

✅ Accepted

## Context

There are several reasons why the API may receive repeated duplicate calls, potentially concurrently:

1. The user submits a form multiple times. Adding debounce to the form submit button can reduce the likelihood of this, but not eliminate it.
2. The user uses back/forward buttons and inadvertently submits a request
3. The UI receives an error from the API and automatically retries, despite the API receiving and successfully processing the request (e.g. network issue)

Without adequate protection in the API these subsequent requests could result in unexpected outcomes, be it by producing multiple events, persisting multiple JPA entities or NDelius entities.

This issue only applies to certain HTTP methods because others (e.g. GET, PUT) are naturally idempotent

## Options

We should implicitly or explicitly determine that a duplicate request has already been dealt with, and return the same response to the UI that would have been returned for the handled request (i.e. make the endpoint idempotent).

A few potential options are proposed to identify duplicate requests:

* Explicitly - Include a key that uniquely represents the logical request (an 'idempotency key'). This key could be random, or based upon some existing identity (e.g. persisted form ID, course completion ID) 
* Implicitly - Use persisted state to determine if the effects of handling the request have already been applied by a prior request

## Decision

* We will use an idempotency key header following the pattern [described on this page](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Idempotency-Key).
* Initially we will not capture a fingerprint for the request, and just identify repeated requests using the key alone
* Use of the key is optional, and some POST endpoints may not elect to use the key (if repeated requests have a negligible outcome)
* Use of the key will be indicated in the open api spec/swagger. The spec will advise what value should be used for the key
* A distributed lock will be acquired on the key during the request processing. Concurrent requests will wait on the lock release and then check if the key has successfully been used already before being processed
