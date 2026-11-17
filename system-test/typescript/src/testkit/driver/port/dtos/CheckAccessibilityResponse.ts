/** The accessibility check of the page that is open. */
export interface CheckAccessibilityResponse {
  /** The serious and critical axe violations, one readable line each: rule, impact, help and the offending elements. */
  violations: string[];
}
