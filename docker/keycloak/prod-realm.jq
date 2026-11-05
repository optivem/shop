# Hardens a stage realm for production (see Dockerfile): only the SPA client and the monolith's
# confidential login client, no test users, no localhost URLs, no client secret in the image, and
# none of the test realm's weak settings. prod-realm-check.jq enforces this.
# Keycloak generates a random shop-monolith secret on import; deploy-keycloak then sets the real one
# from Secret Manager (${SYSTEM}-monolith-client-secret), so the secret never reaches the image or git.
def no_localhost: map(select(contains("localhost") | not));

del(.users, .passwordPolicy)
| .sslRequired = "external"
| .clients |= map(
    select(.clientId == "shop-frontend" or .clientId == "shop-monolith")
    | del(.secret)
    | .redirectUris |= no_localhost
    | .webOrigins |= ((. // []) | no_localhost)
    | .attributes["post.logout.redirect.uris"] |= (split("##") | no_localhost | join("##")))
