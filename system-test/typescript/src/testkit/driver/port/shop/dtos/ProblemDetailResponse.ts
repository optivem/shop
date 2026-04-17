export interface ProblemDetailResponse {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  errors?: { field: string; message: string; code?: string; rejectedValue?: string }[];
}
