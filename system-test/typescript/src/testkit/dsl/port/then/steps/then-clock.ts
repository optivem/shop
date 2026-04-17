export interface ThenClock extends PromiseLike<void> {
  hasTime(time?: string): this;
}
