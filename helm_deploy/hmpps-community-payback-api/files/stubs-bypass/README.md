# Stubs Bypass

This configuration can be used to forward all requests to the actual upstream `community-payback-and-delius` endpoint

To use this locally update cp-stack/compose.yml to use this folder for wiremock configuration e.g.

```
    volumes:
      - "../../helm_deploy/hmpps-community-payback-api/files/stubs-bypass:/home/wiremock"
```