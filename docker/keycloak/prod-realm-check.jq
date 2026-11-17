# Run with jq -e: fails the image build if the production realm regresses.
# directAccessGrantsEnabled must be explicitly false, because Keycloak treats a missing value as true.
# Brute-force detection (B-PRG 4.2) is the login rate limit: no backend sees a password, Keycloak does.
# shop-monolith is confidential, but its secret is set at deploy time (deploy-keycloak), never in the file.
.realm == "shop-prod"
and ((.users // []) == [])
and (.passwordPolicy == null)
and (.bruteForceProtected == true and .permanentLockout == false)
and ([.clients[].clientId] | sort) == ["shop-frontend", "shop-monolith"]
and all(.clients[];
      .directAccessGrantsEnabled == false
      and .serviceAccountsEnabled == false
      and has("secret") == false)
and (tostring | contains("localhost") | not)
