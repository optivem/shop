# Run with jq -e: fails the image build if the production realm regresses.
# directAccessGrantsEnabled must be explicitly false, because Keycloak treats a missing value as true.
.realm == "shop-prod"
and ((.users // []) == [])
and (.passwordPolicy == null)
and (.clients | length > 0)
and all(.clients[];
      .clientId != "shop-system-test"
      and .directAccessGrantsEnabled == false
      and .serviceAccountsEnabled == false
      and has("secret") == false)
and (tostring | contains("localhost") | not)
