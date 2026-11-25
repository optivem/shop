#!/usr/bin/env bash
# Applies sign-up (UC-013 Create Customer) from docker/keycloak/shop-realm.json to a realm that already
# exists: --import-realm never updates one, so the live acceptance realm only gets it from here.
# Usage: reconcile-sign-up.sh <keycloak-base-url> <realm> <realm-file>; ADMIN_TOKEN holds a master admin token.
# Idempotent and create-or-update only. The order matters: Verify Profile goes off before the country
# becomes required, or every existing user without a country would be sent to the profile form at sign-in.
# The parallel acceptance variants run this at the same moment, so each write is retried on 409.
set -euo pipefail

kc="$1"
realm="$2"
realm_file="$3"
: "${ADMIN_TOKEN:?ADMIN_TOKEN must hold a master admin token}"
[[ "$realm" != "shop-prod" ]] || { echo "::error::Sign-up stays off in shop-prod; refusing to reconcile it"; exit 1; }

admin="${kc}/admin/realms/${realm}"
auth=(-H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json")

# Sends a write and retries a 409 (concurrent update); any other non-2xx fails with the response body.
write() {
  local method="$1" url="$2" data="$3" what="$4" body code attempt
  body="$(mktemp)"
  for attempt in 1 2 3 4 5; do
    code="$(curl -sS -o "$body" -w '%{http_code}' "${auth[@]}" -X "$method" "$url" -d "$data")"
    [[ "$code" == 409 && "$attempt" -lt 5 ]] || break
    echo "${what} returned 409 (concurrent update); retrying (${attempt}/5)."
    sleep $((attempt * 2 + RANDOM % 3))
  done
  [[ "$code" == 2* ]] || { echo "::error::${what} in ${realm} returned HTTP ${code}: $(head -c 2000 "$body")"; exit 1; }
}

# 1. Verify Profile off: users the tests create have no country, and must still sign in without a profile form.
action="$(curl -fsS "${auth[@]}" "${admin}/authentication/required-actions/VERIFY_PROFILE")"
write PUT "${admin}/authentication/required-actions/VERIFY_PROFILE" "$(jq -c '.enabled = false' <<< "$action")" "Turning off Verify Profile"
echo "Verify Profile is off in ${realm}."

# 2. The user profile with the required shipping country.
profile="$(jq -r '.components["org.keycloak.userprofile.UserProfileProvider"][0].config["kc.user.profile.config"][0]' "$realm_file")"
[[ "$(jq -r '[.attributes[].name] | index("country") != null' <<< "$profile")" == true ]] \
  || { echo "::error::${realm_file} has no country attribute in its user profile"; exit 1; }
write PUT "${admin}/users/profile" "$profile" "Setting the user profile"
echo "User profile of ${realm} has: $(jq -c '[.attributes[].name]' <<< "$profile")."

# 3. New users are customers: CUSTOMER in the realm's default role.
default_role_id="$(curl -fsS "${auth[@]}" "$admin" | jq -r '.defaultRole.id // empty')"
[[ -n "$default_role_id" ]] || { echo "::error::Realm ${realm} has no default role"; exit 1; }
customer="$(curl -fsS "${auth[@]}" "${admin}/roles/CUSTOMER")"
if curl -fsS "${auth[@]}" "${admin}/roles-by-id/${default_role_id}/composites" | jq -e 'any(.[]; .name == "CUSTOMER")' > /dev/null; then
  echo "The default role of ${realm} already holds CUSTOMER."
else
  write POST "${admin}/roles-by-id/${default_role_id}/composites" "[${customer}]" "Adding CUSTOMER to the default role"
  echo "The default role of ${realm} now holds CUSTOMER."
fi

# 4. The country claim on both login clients: the monolith reads it from the ID token, the backends from the access token.
for client_id in shop-frontend shop-monolith; do
  mapper="$(jq -c --arg c "$client_id" '.clients[] | select(.clientId == $c) | .protocolMappers[] | select(.name == "country")' "$realm_file")"
  [[ -n "$mapper" ]] || { echo "::error::${realm_file} has no country mapper on ${client_id}"; exit 1; }
  id="$(curl -fsS "${auth[@]}" "${admin}/clients?clientId=${client_id}" | jq -r '.[0].id // empty')"
  [[ -n "$id" ]] || { echo "::error::${client_id} is missing from realm ${realm}"; exit 1; }
  models="${admin}/clients/${id}/protocol-mappers/models"
  existing="$(curl -fsS "${auth[@]}" "$models" | jq -r '.[] | select(.name == "country") | .id')"
  if [[ -n "$existing" ]]; then
    write PUT "${models}/${existing}" "$(jq -c --arg id "$existing" '.id = $id' <<< "$mapper")" "Updating the country mapper of ${client_id}"
  else
    # Another variant may create it between the lookup and the POST: a 409 then means it is there.
    code="$(curl -sS -o /dev/null -w '%{http_code}' "${auth[@]}" -X POST "$models" -d "$mapper")"
    [[ "$code" == 201 || "$code" == 409 ]] || { echo "::error::Adding the country mapper to ${client_id} in ${realm} returned HTTP ${code}"; exit 1; }
  fi
  echo "${client_id} (${realm}) carries the country claim."
done

# 5. Last, so a visitor can only sign up once everything above is in place.
write PUT "$admin" '{"registrationAllowed": true}' "Turning on registration"
echo "Sign-up is on in ${realm}."
