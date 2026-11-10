// The tile colour of a brand: the same brand always gets the same colour.

// Dark enough for white text at >= 4.5:1 contrast.
const TILE_COLOURS = ['#1f4e9c', '#7a2e8e', '#0f6b5c', '#9c3d1f', '#3b4a5a', '#8a1c3f', '#2f5d1e', '#5b3a99'];

export function brandColour(brand: string): string {
  let hash = 0;
  for (const char of brand) {
    hash = (hash * 31 + (char.codePointAt(0) ?? 0)) % 2147483647;
  }
  return TILE_COLOURS[hash % TILE_COLOURS.length] ?? TILE_COLOURS[0]!;
}
