# Backend (.NET)

This is a sample backend API service written in C# using ASP.NET Core.

## Instructions

Open up the 'backend' folder

```shell
cd backend
```

Ensure you have .NET SDK 8.0 installed

```shell
dotnet --version
```

Build the application

```shell
dotnet build
```

Run the application

```shell
dotnet run
```

App should now be running on:
http://localhost:8081/

## Authentication

The API is an OAuth2 resource server: every endpoint except `GET /health` requires a Keycloak-issued bearer token (signature via JWKS, issuer, audience `shop-backend`, expiry). Admin-only: `/api/admin/**`, `GET`/`POST /api/coupons`, `POST /api/orders/{n}/deliver`. Configure with `AUTH_ISSUER_URI`, `AUTH_JWK_SET_URI` and `AUTH_AUDIENCE` (required). The backend tests mint their own signed tokens, so no identity provider is needed to run them.
