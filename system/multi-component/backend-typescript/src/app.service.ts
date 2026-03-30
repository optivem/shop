import { Injectable, HttpException, HttpStatus } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class AppService {
  private readonly maxRetries = 3;
  private readonly retryDelayMs = 1000;
  private readonly timeoutMs = 30000;

  constructor(private configService: ConfigService) {}

  getHello(): string {
    return 'Hello World!';
  }

  getHomePageHtml(): string {
    return `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Starter</title>
</head>
<body>
    <h1>Hello World!</h1>
    <p>Welcome to the Starter - TypeScript</p>
    <nav>
        <a href="/todos">Todo Manager</a>
    </nav>
</body>
</html>
    `;
  }

  getEcho(): string {
    return 'Echo response';
  }

  async getTodo(id: number): Promise<any> {
    const baseUrl = this.configService.get<string>(
      'TODOS_API_BASE_URL',
      'https://jsonplaceholder.typicode.com',
    );

    for (let attempt = 0; attempt < this.maxRetries; attempt++) {
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), this.timeoutMs);

        const response = await fetch(`${baseUrl}/todos/${id}`, {
          signal: controller.signal,
        });

        clearTimeout(timeoutId);

        if (!response.ok) {
          if (response.status === 404) {
            throw new HttpException('Todo not found', HttpStatus.NOT_FOUND);
          }
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const todo: unknown = await response.json();

        if (!todo) {
          throw new HttpException('Todo not found', HttpStatus.NOT_FOUND);
        }

        return todo;
      } catch (error: unknown) {
        if (
          error instanceof HttpException &&
          error.getStatus() === (HttpStatus.NOT_FOUND as number)
        ) {
          throw error;
        }

        if (attempt === this.maxRetries - 1) {
          const message =
            error instanceof Error ? error.message : String(error);
          throw new HttpException(
            `External API is unavailable after ${this.maxRetries} attempts: ${message}`,
            HttpStatus.SERVICE_UNAVAILABLE,
          );
        }

        await new Promise((resolve) => setTimeout(resolve, this.retryDelayMs));
      }
    }

    throw new HttpException(
      'Unexpected error',
      HttpStatus.INTERNAL_SERVER_ERROR,
    );
  }

  getTodosPageHtml(): string {
    return `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Todo Fetcher</title>
</head>
<body>
    <h1>Todo Fetcher</h1>
    <div>
        <label for="todoId">Todo ID:</label>
        <input type="text" id="todoId" placeholder="Enter todo ID">
        <button id="fetchTodo">Fetch Todo</button>
    </div>
    <div id="todoResult"></div>

    <script>
        document.getElementById('fetchTodo').addEventListener('click', async function() {
            const todoId = document.getElementById('todoId').value;
            const resultDiv = document.getElementById('todoResult');

            if (!todoId) {
                resultDiv.textContent = 'Please enter a todo ID';
                return;
            }

            try {
                const response = await fetch('/api/todos/' + todoId);
                const todo = await response.json();

                resultDiv.innerHTML = \`
                    <h3>Todo Details:</h3>
                    <p><strong>User ID:</strong> \${todo.userId}</p>
                    <p><strong>ID:</strong> \${todo.id}</p>
                    <p><strong>Title:</strong> \${todo.title}</p>
                    <p><strong>Completed:</strong> \${todo.completed ? 'Yes' : 'No'}</p>
                \`;
            } catch (error) {
                resultDiv.textContent = 'Error fetching todo: ' + error.message;
            }
        });
    </script>
</body>
</html>
    `;
  }
}
