/** A product photo as the ERP serves it: the raw bytes and their content type (e.g. image/jpeg). */
export interface ErpProductImage {
  content: Buffer;
  contentType: string;
}
