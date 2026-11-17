export interface ProblemDetailResponse {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  traceId?: string;
  errors?: { field: string; message: string; code?: string; rejectedValue?: string }[];
}
