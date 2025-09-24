# 0001 - Wrap list responses in a JSON Object

Date 2025-09-24

## Status

✅ Accepted

## Context

It is common for a number of endpoints to return a list of objects. Either an array can be returned as the response, or it can be wrapped within a JSON root object.

_Array Response_
```json
[
  {
    "firstName" : "John",
    "lastName" : "Smith"
  },
  {
    "firstName" : "Sue",
    "lastName" : "Storm"
  }
]
```

_Wrapped in a JSON Object_
```json
{
  "persons" :
  [
    {
      "firstName" : "John",
      "lastName" : "Smith"
    },
    {
      "firstName" : "Sue",
      "lastName" : "Storm"
    }
  ]
}
```

## Decision

We have decided that all responses will be wrapped in a JSON object.

This makes our API easier to use; Consumers do not need to build cases to support a response sometimes being an Array, and other times being a JSON object.

It also gives us the ability to add more fields alongside the array if required (e.g. paging meta data), since consumers can ignore new fields if they don't use them, instead of it being a breaking change.

## Consequences

1. Consideration will need to be taken when building controllers to respond with the correct type.
2. When responding with only a list of data. The data will need to be wrapped into a JSON object.