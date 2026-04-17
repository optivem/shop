import { UseCaseContext as LegacyUseCaseContext } from '../use-case-context.js';

/**
 * Re-export the existing `UseCaseContext` at the shared location that
 * matches the Java layout (`dsl/core/shared/UseCaseContext`). The
 * implementation continues to live at `dsl/core/use-case-context.ts` to
 * avoid churning every current caller.
 */
export { LegacyUseCaseContext as UseCaseContext };
