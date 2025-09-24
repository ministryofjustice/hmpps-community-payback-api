# 0002 - Organise code by features

Date 2025-09-24

## Status

✅ Accepted

## Context

There are many ways that code can be organised, wrt. package naming and hierarchy. Some common approaches:

1. By layer

Package code based upon the 'layer' of the architecture it represents

```
   controller
   dto
   service
   entity
   client
```

2. By feature

Package code based upon features

```
   common
   person
   project
```

## Decision

We have decided to package code by feature for the following reasons:

* It encourages cohesion and modularity of code. If a change is made that affects many top-level packages, it suggests groupings may not be correct
* Helps isolate changes to a particular part of the code (i.e. typically when modifying code in a feature package it's likely some other code in the same package will also need updating)
* As the code base grows it's easier to find specific code

Code can still be isolated by layers, but within the feature packages themselves

## Consequences

This approach will need monitoring as the features for the project are identified and elaborated. If the nature of the solution requires tight coupling, or the features are constrained by existing/upstream services and data models, it may be that a layered based approach may make more sense
