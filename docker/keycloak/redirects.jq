# Shared by the realm filters; include with jq -L.
def add_redirects($urls):
  ($urls | map(. + "/*")) as $patterns
  | .redirectUris = $patterns + .redirectUris
  | .attributes["post.logout.redirect.uris"] = ($patterns + [.attributes["post.logout.redirect.uris"]] | join("##"));
