# Turns shop-realm.json into a stage's realm, with every variant's URLs for that stage (see Dockerfile.cloud).
# All variants, not only those deployed today: an imported realm is never updated, so URLs added later wouldn't apply.
# Inputs: --arg realm <realm name>, --arg stage <qa|production>, --arg system <system>,
#         --arg suffix <project-number>.<region>.run.app
# Service names must match _deploy-stage-cloud.yml.
include "redirects";

def urls(service): [{lang: ("java", "dotnet", "typescript")} | "https://\(service)-\($suffix)"];

urls("\($system)-multitier-\(.lang)-\($stage)-frontend") as $multitier
| urls("\($system)-monolith-\(.lang)-\($stage)") as $monolith
| .realm = $realm
| .clients |= map(
    if .clientId == "shop-frontend" then add_redirects($multitier) | .webOrigins = $multitier + .webOrigins
    elif .clientId == "shop-monolith" then add_redirects($monolith)
    else . end)
