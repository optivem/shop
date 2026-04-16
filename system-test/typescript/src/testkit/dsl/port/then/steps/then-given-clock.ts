export interface ThenGivenClock extends PromiseLike<void> {
  hasTime(time?: string): this;
}
