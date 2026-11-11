# Cloud Run Keycloak, shared by every variant and stage. The realms carry every variant's deployed
# URLs, because --import-realm skips a realm that already exists, so URLs added later would never
# take effect. A new realm in the import dir is imported, so a stage that needs URLs gets its own realm:
#   shop       acceptance, with the password-grant test client and test users
#   shop-qa    QA, same clients and users
#   shop-prod  production: SPA client only, no test users, checked below so it can't regress
# Import files must be named <realm>-realm.json.
#
# The URLs hold ${SYSTEM} and ${RUN_APP_SUFFIX} placeholders, which Keycloak resolves from the
# container's environment when it imports the realms. So the image has no project details in it:
# one public image (ghcr.io/<owner>/<repo>/shop-keycloak) serves every project and scaffold.
# Runtime env: SYSTEM, RUN_APP_SUFFIX (<project-number>.<region>.run.app); see deploy-keycloak.
# Build context: docker/keycloak. Local compose keeps using the stock image with shop-realm.json.
FROM alpine:3.24 AS realm
RUN apk add --no-cache jq
COPY shop-realm.json redirects.jq acceptance-realm.jq stage-realm.jq prod-realm.jq prod-realm-check.jq /
# Single quotes: the placeholders go into the realm files literally.
RUN jq -L / --arg system '${SYSTEM}' --arg suffix '${RUN_APP_SUFFIX}' \
      -f /acceptance-realm.jq /shop-realm.json > /realm.json
RUN jq -L / --arg realm shop-qa --arg stage qa --arg system '${SYSTEM}' --arg suffix '${RUN_APP_SUFFIX}' \
      -f /stage-realm.jq /shop-realm.json > /realm-qa.json
RUN jq -L / --arg realm shop-prod --arg stage production --arg system '${SYSTEM}' --arg suffix '${RUN_APP_SUFFIX}' \
      -f /stage-realm.jq /shop-realm.json | jq -f /prod-realm.jq > /realm-prod.json \
    && jq -e -f /prod-realm-check.jq /realm-prod.json

FROM quay.io/keycloak/keycloak:26.8 AS builder
ENV KC_DB=postgres \
    KC_HEALTH_ENABLED=true
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY --from=realm /realm.json /opt/keycloak/data/import/shop-realm.json
COPY --from=realm /realm-qa.json /opt/keycloak/data/import/shop-qa-realm.json
COPY --from=realm /realm-prod.json /opt/keycloak/data/import/shop-prod-realm.json
ENV KC_DB=postgres \
    KC_HTTP_ENABLED=true \
    KC_PROXY_HEADERS=xforwarded \
    KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true
EXPOSE 8080
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm"]
