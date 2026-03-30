import { NextRequest, NextResponse } from "next/server";

const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1000;
const TIMEOUT_MS = 30000;
const BASE_URL =
  process.env.TODOS_API_BASE_URL || "https://jsonplaceholder.typicode.com";

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;

  for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

      const response = await fetch(`${BASE_URL}/todos/${id}`, {
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        if (response.status === 404) {
          return NextResponse.json(
            { error: "Todo not found" },
            { status: 404 }
          );
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const todo = await response.json();

      if (!todo) {
        return NextResponse.json(
          { error: "Todo not found" },
          { status: 404 }
        );
      }

      return NextResponse.json(todo);
    } catch (error: unknown) {
      if (attempt === MAX_RETRIES - 1) {
        const message = error instanceof Error ? error.message : String(error);
        return NextResponse.json(
          {
            error: `External API is unavailable after ${MAX_RETRIES} attempts: ${message}`,
          },
          { status: 503 }
        );
      }

      await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS));
    }
  }

  return NextResponse.json(
    { error: "Unexpected error" },
    { status: 500 }
  );
}
