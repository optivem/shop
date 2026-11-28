# 2026-11-28 01:52:09 UTC — Language switcher: English / Serbian / German / French (frontend-react)

## TL;DR

**Why:** The demo shop is English-only. Serbian, German and French options make it relevant to retailers in those markets and show i18n done properly, without touching the backend.
**End result:** `system/multitier/frontend-react` has an EN / СР / DE / FR language switcher in the header. Every piece of UI text, plus money and date formatting, comes from translation files. English stays the default, so all existing e2e and unit tests pass unchanged.

## Outcomes

- A visitor can switch between English (`en`), Serbian Cyrillic (`sr-Cyrl-RS`), German (`de-DE`) and French (`fr-FR`) from the store header, and the whole UI changes without a page reload.
- The choice survives a reload (stored per browser). First visit defaults to English.
- No hardcoded user-facing strings remain in components. All text lives in `en` / `sr` / `de` / `fr` message files with identical key sets, and a unit test fails if any language is missing a key.
- Prices and dates are formatted for the active locale (e.g. `1.234,56` / `28.11.2026.` in Serbian and German, `1 234,56` / `28/11/2026` in French).
- `<html lang>` follows the active language (accessibility).
- e2e selectors and English text assertions are untouched. System tests run in English and stay green.

## ▶ Next executable step (resume here)

Steps 1–8 are done but not committed. The full gate is green: `tsc -b`, `eslint .`, the unit tests, and `npm run build`. What's left is Step 9's manual part: check every page in each language at phone width, then run the multitier sample system tests (the user must OK them first). After that, commit via `/commit`.

**Handover notes (2026-11-28):**
- Added `i18next` 27.2.2 and `react-i18next` 18.0.1 (exact pins). `npm install` reported audit warnings that haven't been reviewed.
- Deleted `src/utils/messages.ts` and the `QUANTITY_ERROR` constant: nothing used them any more.
- Serbian writes SKU as `Шифра (SKU)`, because the Cyrillic test rejects Latin-only Serbian strings.
- These stay in English on purpose: backend order statuses (PLACED, CANCELLED), product names and categories, API error `detail` text, and the MyShop logo.
- The e2e drivers find dropdowns by exact `aria-label`, so the new language select can't collide with them.
- To test locally: the user's dev server on `:5173` (hard refresh), or `gh optivem system start` with `gh-optivem-multitier-typescript.yaml`, then `:3311` (real) or `:3312` (stub).

## Steps

- [ ] Step 9: Verify. Automated gate done. Remaining: a quick manual check of every page in each language at phone width (German strings run longest and are the most likely to break buttons, tabs and table headers); multitier sample system tests per CLAUDE.md (ask before running locally).

## Conventions (for every string-extraction unit)

Steps 1–3 built the infrastructure in `system/multitier/frontend-react/src/i18n/`. Every extraction unit follows these rules:

- **Namespaces and ownership.** There are four namespaces: `common`, `catalog`, `checkout` and `account`. Each one has a file per language at `src/i18n/locales/<en|sr|de|fr>/<ns>.ts`. A unit edits only its own namespace's four files and its own component files. `en/<ns>.ts` is the source; `sr`/`de`/`fr` are typed `Messages<typeof en>`, so a missing or extra key is a compile error.
- **Calling translations.** In components, use `const { t } = useTranslation('<ns>')`. In non-React code, use `import { i18n } from '<rel>/i18n'` and call `i18n.t('<ns>:key')` at call time, never in a module-level constant. Group keys by component (`productCard.addToCart`).
- **English stays byte-identical.** Every English string is copied exactly from today's source, including `aria-label`s, because the e2e and unit tests assert on that text. `data-testid`, `name`, `id` and `className` never change.
- **Backend data stays untranslated:** product names, categories, ERP/API error `detail` messages.
- **Interpolation** uses `{{name}}`. **Plurals:** define all four of `_one`, `_few`, `_many` and `_other` in every language, so key parity holds.
- **Formatting.** Money goes through `formatMoney`/`formatCents` (`utils/money.ts`). Replace `toLocaleString('en-US', …)` with `formatDateTime` and `` `${(x*100).toFixed(2)}%` `` with `formatPercent` (`i18n/format.ts`). A component that shows formatted values but has no `t` calls uses `useFormat()`, so it re-renders when the language changes.
- **Translation style.** Serbian is in Cyrillic (`sr-Cyrl-RS`). Use the formal "you" everywhere: `Ви`, `Sie`, `vous`. Use natural e-commerce wording, not word-for-word translation. Put a non-breaking space before French `? ! : ;`.
- **Gate:** `npx tsc -b`, `npx eslint <touched files>`, `npx vitest run src/test/unit`. The existing tests must pass unchanged.

## Decisions

- **Library:** `react-i18next` (+ `i18next`). It's the standard choice and handles plurals, interpolation and typed keys.
- **Backend-sourced text:** product names, categories and API error messages from the backend/ERP stay untranslated. This keeps the work frontend-only.
- **Currency:** one currency for all languages. Only number and date formatting follow the locale, because the backend has no currency conversion.
- **Scope:** `system/multitier/frontend-react` only. The monolith UIs (Java/.NET/TS) are out of scope.

## Before starting

- The working tree has uncommitted edits in `user-manager.ts`, `DemoWakeUp.tsx`, `Catalog.tsx`, and the wake-up unit test. They are still uncommitted and mixed in with the i18n changes, so separate them at commit time.
