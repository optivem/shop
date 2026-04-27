# Language Equivalents

Use this table when implementing any ATDD phase. All concepts are language-agnostic; this file shows the concrete syntax for each supported language.

## TODO Stubs

| Concept | Java | .NET (C#) | TypeScript |
|---------|------|-----------|------------|
| DSL stub | `throw new UnsupportedOperationException("TODO: DSL")` | `throw new NotImplementedException("TODO: DSL")` | `throw new Error("TODO: DSL")` |
| Driver stub | `throw new UnsupportedOperationException("TODO: Driver")` | `throw new NotImplementedException("TODO: Driver")` | `throw new Error("TODO: Driver")` |

## Test Disabling

| Operation | Java | .NET (C#) | TypeScript |
|-----------|------|-----------|------------|
| Disable a single test | `@Disabled("reason")` | `[Fact(Skip = "reason")]` or `[Theory(Skip = "reason")]` | `test.skip(true, "reason")` |
| Re-enable a test | Remove `@Disabled` | Remove `Skip = "reason"` | Remove `test.skip(...)` |

## String Field Types

"String fields" means the nullable string type in the target language:

| Language | String field type | Example |
|----------|------------------|---------|
| Java | `String` | `private String sku;` |
| .NET (C#) | `string?` | `public string? Sku { get; set; }` |
| TypeScript | `Optional<string>` | `private sku: Optional<string>;` |

## DTO Boilerplate

| Language | Request DTOs | Response DTOs |
|----------|-------------|---------------|
| Java | Lombok: `@Data @Builder @NoArgsConstructor @AllArgsConstructor` | Same |
| .NET (C#) | Auto-properties: `public string? Field { get; set; }` | `required` modifier for non-nullable IDs: `public required string Id { get; set; }` |
| TypeScript | `interface` with optional fields: `field?: Optional<string>` | `interface` with required fields: `field: string` |

## Test File Naming

| Language | Positive test file | Negative test file |
|----------|-------------------|-------------------|
| Java | `<UseCase>PositiveTest.java` | `<UseCase>NegativeTest.java` |
| .NET (C#) | `<UseCase>PositiveTest.cs` | `<UseCase>NegativeTest.cs` |
| TypeScript | `<UseCase>Positive.spec.ts` | `<UseCase>Negative.spec.ts` |

## Awaitable ShouldSucceed

The terminal `ShouldSucceed()` / `shouldSucceed()` assertion is awaitable in .NET and TypeScript:

| Language | Mechanism |
|----------|-----------|
| Java | Synchronous — no `await` needed |
| .NET (C#) | `IThenSuccess` implements `GetAwaiter()` — use `await ...ShouldSucceed()` |
| TypeScript | `ThenSuccessPort extends PromiseLike<void>` — use `await ...shouldSucceed()` |
