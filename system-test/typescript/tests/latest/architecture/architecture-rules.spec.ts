import { test, expect } from '@playwright/test';
import { Project, Scope } from 'ts-morph';

/**
 * Which DSL/driver structural rules can be enforced mechanically? Four
 * representative rules (A1/A2/A7/A10), one per feasibility tier, checked against
 * the test-kit source via ts-morph's AST.
 */

const ROOT = process.cwd().replace(/\\/g, '/');

const project = new Project({
  tsConfigFilePath: `${ROOT}/tsconfig.json`,
  skipAddingFilesFromTsConfig: true,
});
project.addSourceFilesAtPaths(`${ROOT}/src/testkit/**/*.ts`);

const sourceFiles = project.getSourceFiles();

function pathOf(filePath: string): string {
  return filePath.replace(/\\/g, '/');
}

// A1 — request DTOs in the driver port expose only string members.
function findA1Violations(): string[] {
  const violations: string[] = [];
  const dtoFiles = sourceFiles.filter((sf) => pathOf(sf.getFilePath()).includes('/driver/port/dtos/'));

  for (const sf of dtoFiles) {
    for (const iface of sf.getInterfaces()) {
      if (!iface.getName().endsWith('Request')) continue;
      for (const prop of iface.getProperties()) {
        const typeText = prop.getTypeNode()?.getText() ?? prop.getType().getText();
        const nonString = typeText
          .split('|')
          .map((s) => s.trim())
          .filter((s) => s !== 'null' && s !== 'undefined')
          .filter((s) => s !== 'string');
        if (nonString.length > 0) {
          violations.push(`${iface.getName()}.${prop.getName()}: ${typeText}`);
        }
      }
    }
  }

  return violations;
}

test('A1 - request DTOs in driver/port/dtos declare only string members', () => {
  const violations = findA1Violations();
  expect(violations, `Request DTOs in driver/port/dtos must declare only string members: ${violations.join('; ')}`).toEqual([]);
});

// A2 — verification public methods are fluent (own type) or terminal (void); never getters.
function findA2Violations(): string[] {
  const violations: string[] = [];
  const coreFiles = sourceFiles.filter((sf) => pathOf(sf.getFilePath()).includes('/dsl/core/'));

  for (const sf of coreFiles) {
    for (const cls of sf.getClasses()) {
      const className = cls.getName();
      if (!className?.endsWith('Verification')) continue;
      for (const method of cls.getMethods()) {
        const scope = method.getScope();
        if (scope === Scope.Protected || scope === Scope.Private) continue;
        const ret = method.getReturnTypeNode()?.getText() ?? method.getReturnType().getText();
        const ok = ret === 'this' || ret === className || ret === 'void';
        if (!ok) {
          violations.push(`${className}.${method.getName()} -> ${ret}`);
        }
      }
    }
  }

  return violations;
}

test('A2 - verification public methods are fluent or void', () => {
  const violations = findA2Violations();
  expect(violations, `Verification public methods must return their own type (fluent) or void (terminal): ${violations.join('; ')}`).toEqual([]);
});

// A7 — DSL core declares no own *Request/*Response; it reuses driver/port/dtos.
function findA7Violations(): string[] {
  const violations: string[] = [];
  const coreFiles = sourceFiles.filter((sf) => pathOf(sf.getFilePath()).includes('/dsl/core/'));

  for (const sf of coreFiles) {
    const declared = [
      ...sf.getInterfaces().map((d) => d.getName()),
      ...sf.getClasses().map((d) => d.getName()),
      ...sf.getTypeAliases().map((d) => d.getName()),
      ...sf.getEnums().map((d) => d.getName()),
    ].filter((n): n is string => !!n);

    for (const name of declared) {
      if (name.endsWith('Request') || name.endsWith('Response')) {
        violations.push(`${sf.getBaseName()}: ${name}`);
      }
    }
  }

  return violations;
}

test('A7 - dsl/core declares no own *Request/*Response DTOs', () => {
  const violations = findA7Violations();
  expect(violations, `DSL core must not declare its own *Request/*Response DTOs: ${violations.join('; ')}`).toEqual([]);
});

// A10 — every MyShopDriver operation takes a single *Request and returns Result<*Response, …> (strict).
function findA10Violations(): { found: boolean; violations: string[] } {
  const violations: string[] = [];
  let found = false;

  for (const sf of sourceFiles) {
    const iface = sf.getInterface('MyShopDriver');
    if (!iface) continue;
    found = true;

    for (const method of iface.getMethods()) {
      const params = method.getParameters();
      const paramType = params[0]?.getTypeNode()?.getText() ?? '';
      if (params.length !== 1 || !paramType.endsWith('Request')) {
        violations.push(`${method.getName()} must take exactly one *Request parameter (was [${params.map((p) => p.getTypeNode()?.getText()).join(', ')}])`);
      }

      const ret = method.getReturnTypeNode()?.getText() ?? method.getReturnType().getText();
      const firstTypeArg = /Result<\s*([A-Za-z0-9_]+)/.exec(ret)?.[1] ?? '';
      if (!firstTypeArg.endsWith('Response')) {
        violations.push(`${method.getName()} must return Result<*Response, …> (was ${ret})`);
      }
    }
  }

  return { found, violations };
}

test('A10 - every MyShopDriver method takes a *Request and returns a *Response', () => {
  const { found, violations } = findA10Violations();
  expect(found, 'MyShopDriver interface not found').toBe(true);
  expect(violations, `Every MyShopDriver method must take a single *Request and return Result<*Response, …>: ${violations.join('; ')}`).toEqual([]);
});
