# Adds every variant's Cloud Run URLs to the realm (see Dockerfile.cloud).
# Inputs: --arg system <system>, --arg suffix <project-number>.<region>.run.app
# Service names must match the cloud workflows.
def urls(service): [{lang: ("java", "dotnet", "typescript"), stack: ("real", "stub")} | "https://\(service)-\($suffix)"];

def add_redirects($urls):
  ($urls | map(. + "/*")) as $patterns
  | .redirectUris = $patterns + .redirectUris
  | .attributes["post.logout.redirect.uris"] = ($patterns + [.attributes["post.logout.redirect.uris"]] | join("##"));

urls("\($system)-multitier-\(.lang)-\(.stack)-frontend") as $multitier
| urls("\($system)-monolith-\(.lang)-\(.stack)") as $monolith
| .clients |= map(
    if .clientId == "shop-frontend" then add_redirects($multitier) | .webOrigins = $multitier + .webOrigins
    elif .clientId == "shop-monolith" then add_redirects($monolith)
    else . end)
