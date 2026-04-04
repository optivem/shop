# Channel Mode: Static vs Dynamic Phase Channel Resolution

## Problem

UI tests are slow because Given (setup) and Then (verification) steps run through the browser, even though only the When (action) step actually tests UI behavior.

## Solution: Channel Mode

A new configuration concept (following the `ExternalSystemMode` pattern) with two values:

- **DYNAMIC** (default) -- current behavior, all phases use the test's annotated channel
- **STATIC** -- Given/Then always use API (fast), When uses the annotated channel

### Example: PlaceOrder test with `@Channel(UI)`

| Phase | DYNAMIC mode | STATIC mode |
|-------|-------------|-------------|
| Given (setup products, coupons) | UI (browser) | API (HTTP) |
| When (place order) | UI (browser) | UI (browser) |
| Then (verify order status) | UI (browser) | API (HTTP) |

In STATIC mode, the browser is only active during the When step. Setup and verification happen via fast API calls.

## Configuration

Two settings control the behavior:

1. **channelMode** -- selects DYNAMIC or STATIC mode
2. **staticChannel** -- when mode is STATIC, specifies which channel to use for Given/Then phases (e.g. `API`)

```
# Java
-DchannelMode=static -DstaticChannel=API

# .NET / TypeScript
CHANNEL_MODE=static
STATIC_CHANNEL=API
```

When `channelMode=dynamic` (default), `staticChannel` is ignored -- all phases use the test's annotated channel.

When `channelMode=static`, `staticChannel` is **required** -- it defines the fast channel used for Given/Then. Typically this is `API`, but it's configurable rather than hardcoded so the framework stays flexible.

## Architecture

### New Enum: ChannelMode

```
enum ChannelMode { DYNAMIC, STATIC }
```

Lives in `dsl-port` alongside `ExternalSystemMode`.

### Dual Shop in UseCaseDsl

UseCaseDsl receives TWO shop driver suppliers:

- **shopDriverSupplier** -- for Given/Then (setup + verification). In STATIC mode this creates a driver for the static channel (e.g. API). In DYNAMIC mode, same as action supplier.
- **actionShopDriverSupplier** -- for When (the action under test). Always creates a driver for the annotated channel.

Two methods expose them:
- `shop()` -- used by Given steps and Then verification
- `actionShop()` -- used by When steps only

```java
public ShopDsl shop() { /* lazy from shopDriverSupplier */ }
public ShopDsl actionShop() {
    if (actionShopDriverSupplier == shopDriverSupplier) return shop();  // DYNAMIC: reuse
    /* lazy from actionShopDriverSupplier */  // STATIC: separate driver
}
```

This solves the When-Then boundary naturally: `BaseWhenStep.then()` passes the same `app` to `ThenResultImpl`. When steps call `app.actionShop()` (annotated channel), Then steps call `app.shop()` (static channel). Same object, different methods.

### Configuration Wiring

Following the existing `getFixedExternalSystemMode()` pattern:

1. `PropertyLoader.getChannelMode()` reads `channelMode` system property (defaults to DYNAMIC)
2. `PropertyLoader.getStaticChannel()` reads `staticChannel` system property (required when STATIC)
3. `BaseConfigurableTest.createUseCaseDsl()` builds both suppliers based on mode

## Files to Modify (per language)

### New Files
- `dsl-port/ChannelMode` -- enum definition

### Modified Files
- `PropertyLoader` -- read channelMode + staticChannel from system properties
- `UseCaseDsl` -- add second shop supplier + `actionShop()` method
- `BaseConfigurableTest` -- build both suppliers based on mode
- `WhenPlaceOrderImpl` -- use `app.actionShop()` instead of `app.shop()`
- `WhenViewOrderImpl` -- use `app.actionShop()` instead of `app.shop()`

### No Changes Needed
- Test files (no test code changes)
- Driver implementations (ShopUiDriver, ShopApiDriver)
- Driver port interfaces (ShopDriver)
- ERP/Tax/Clock drivers (already channel-independent)

## Verification

1. `channelMode=dynamic` (or unset) -- all existing tests pass identically
2. `channelMode=static` with UI tests -- Given/Then use API, When uses UI
3. `channelMode=static` with API-only tests -- behaves identically (API everywhere)
4. No browser launched for Given/Then in STATIC mode
5. Shared state (order numbers, aliases) flows correctly: API Given -> UI When -> API Then
