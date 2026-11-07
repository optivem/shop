/**
 * The ERP could not answer: it is unreachable, timed out, returned an error
 * status or a malformed response. A product the ERP reports as missing (404)
 * is not this; that is a validation failure.
 */
export class ErpUnavailableException extends Error {
  constructor(message: string, options?: ErrorOptions) {
    super(message, options);
    this.name = 'ErpUnavailableException';
  }
}
