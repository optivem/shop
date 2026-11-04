# Turns shop-realm.json into the acceptance realm, with every variant's Cloud Run URLs (see Dockerfile).
# Inputs: --arg system <system>, --arg suffix <project-number>.<region>.run.app (the Dockerfile passes
# the ${SYSTEM} / ${RUN_APP_SUFFIX} placeholders, which Keycloak resolves at import)
# Service names must match the cloud workflows.
include "redirects";

def urls(service): [{lang: ("java", "dotnet", "typescript"), stack: ("real", "stub")} | "https://\(service)-\($suffix)"];

urls("\($system)-multitier-\(.lang)-\(.stack)-frontend") as $multitier
| urls("\($system)-monolith-\(.lang)-\(.stack)") as $monolith
| .clients |= map(
    if .clientId == "shop-frontend" then add_redirects($multitier) | .webOrigins = $multitier + .webOrigins
    elif .clientId == "shop-monolith" then add_redirects($monolith)
    else . end)
