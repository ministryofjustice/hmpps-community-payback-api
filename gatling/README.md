# Load testing tool for the API

This is a subproject in side the Community Payback API. Attempts to fold this in to the main repo is more difficult than one would suspect There are a number of dependency issues that make it difficult.

## How to use this tool

 * Get a JWT from the API
 * Run the load test

### Getting the JWT

We need to get past the auth on the API, the easiest way to do this is to turn on request header logging and grab ut from the logs. We're going to assume that you are using the [cp-stack](../tools/cp-stack/README.md) to run the API.

First, ensure that the header request logging is turned on in [application-localdev.yml](../src/main/resources/application-localdev.yml). Make sure that the `include-headers` property is set to `true`.

```yaml
logging:
  level:
    org.springframework.web.filter.CommonsRequestLoggingFilter: DEBUG
  request:
    include-headers: true
```

Start the API and UI:

```shell
cp-stack start --local-api --local-ui
```

Go to the UI, log in as `community.paybackdevs` and trigger any API request. Usernames and passwords are in 1Password.

Open the [tilt console](http://localhost:10350/r/api-local/overview) and look for your request. Copy the `Authorization` header value, it will start with `authorization:"Bearer eyJra...` you will want the token, ```eyJ...n~950...XXX```. Stash that somewhere safe.

Run the simulations and pass in the header value.

./gradlew gatlingRun --simulation uk.gov.justice.digital.hmpps.communitypayback.simulations.ProviderSimulation -Djwt=eyJra...REDACTED...CUA
