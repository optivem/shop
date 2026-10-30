export class ConcurrentModificationException extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ConcurrentModificationException';
  }
}
