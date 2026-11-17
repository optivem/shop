// axe-core accessibility check for the default-run screen tests (FT Step 3).
//
// Runs axe on a rendered screen and returns the serious and critical violations,
// one readable line each, so a failing test names the rule and the offending element.
// `color-contrast` is off: jsdom computes no layout or styles, so axe cannot judge
// contrast here. Contrast is covered by Lighthouse's accessibility score
// (.github/workflows/lighthouse.yml) against the real rendered page.
import axe from 'axe-core';
import { vi } from 'vitest';

const BLOCKING_IMPACTS = new Set(['serious', 'critical']);

export async function seriousA11yViolations(container: Element): Promise<string[]> {
  // axe probes a canvas for icon fonts; jsdom has none and logs "Not implemented" for each probe.
  const getContext = vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
  let results: axe.AxeResults;
  try {
    results = await axe.run(container, {
      resultTypes: ['violations'],
      rules: { 'color-contrast': { enabled: false } },
    });
  } finally {
    getContext.mockRestore();
  }
  return results.violations
    .filter((violation) => violation.impact != null && BLOCKING_IMPACTS.has(violation.impact))
    .map((violation) => `${violation.id} (${violation.impact}): ${violation.help} at ${violation.nodes.map((node) => node.target.join(' ')).join(', ')}`);
}
