# Hardens a stage realm for production (see Dockerfile.cloud): only the SPA client, no test users,
# no localhost URLs, and none of the test realm's weak settings. prod-realm-check.jq enforces this.
def no_localhost: map(select(contains("localhost") | not));

del(.users, .passwordPolicy)
| .sslRequired = "external"
| .clients |= map(
    select(.clientId == "shop-frontend")
    | .redirectUris |= no_localhost
    | .webOrigins |= no_localhost
    | .attributes["post.logout.redirect.uris"] |= (split("##") | no_localhost | join("##")))
