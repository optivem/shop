export interface ThenSuccess extends PromiseLike<void> {
  and(): this;
}
