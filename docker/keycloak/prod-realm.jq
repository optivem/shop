# Hardens a stage realm for production (see Dockerfile): only the SPA client and the monolith's
# confidential login client, no test users, no localhost URLs, no client secret in the image, and
# none of the test realm's weak settings. prod-realm-check.jq enforces this.
# Keycloak generates a random shop-monolith secret on import; deploy-keycloak then sets the real one
# from Secret Manager (${SYSTEM}-monolith-client-secret), so the secret never reaches the image or git.
# Brute-force detection (B-PRG 4.2) is set here, not in shop-realm.json: on Keycloak 26 it locks a user
# out (user_temporarily_disabled, with no failed login recorded) when the same account signs in many
# times at once, which is what the parallel system tests do with customer1 and admin1.
def no_localhost: map(select(contains("localhost") | not));

del(.users, .passwordPolicy)
| .sslRequired = "external"
| . + {
    bruteForceProtected: true,
    permanentLockout: false,
    failureFactor: 10,
    waitIncrementSeconds: 60,
    maxFailureWaitSeconds: 900,
    maxDeltaTimeSeconds: 43200,
    quickLoginCheckMilliSeconds: 1000,
    minimumQuickLoginWaitSeconds: 60 }
| .clients |= map(
    select(.clientId == "shop-frontend" or .clientId == "shop-monolith")
    | del(.secret)
    | .redirectUris |= no_localhost
    | .webOrigins |= ((. // []) | no_localhost)
    | .attributes["post.logout.redirect.uris"] |= (split("##") | no_localhost | join("##")))
