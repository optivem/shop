# Glossary

## Interface Change

An **interface change** is any modification to a public contract between layers. This includes:

- Adding, removing, or renaming interface methods
- Changing method signatures (parameters, return types)
- Adding, removing, or renaming fields in request or response DTOs associated with those methods

This definition applies uniformly to DSL port interfaces, Driver port interfaces, and external system interfaces.

**Why it matters for the ATDD pipeline:**
- A DSL interface change → update DSL port and implementation
- A Driver interface change → update driver port and adapters
- An external system interface change (any change under `driver-port/.../external/`) → triggers the contract test subprocess (see `contract-tests.md`)
