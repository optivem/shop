"use client";

import { useState } from "react";

export default function TodosPage() {
  const [todoId, setTodoId] = useState("");
  const [result, setResult] = useState<string | null>(null);

  async function fetchTodo() {
    if (!todoId) {
      setResult("Please enter a todo ID");
      return;
    }

    try {
      const response = await fetch(`/api/todos/${todoId}`);
      const todo = await response.json();

      setResult(
        `<h3>Todo Details:</h3>` +
          `<p><strong>User ID:</strong> ${todo.userId}</p>` +
          `<p><strong>ID:</strong> ${todo.id}</p>` +
          `<p><strong>Title:</strong> ${todo.title}</p>` +
          `<p><strong>Completed:</strong> ${todo.completed ? "Yes" : "No"}</p>`
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setResult(`Error fetching todo: ${message}`);
    }
  }

  return (
    <main>
      <h1>Todo Fetcher</h1>
      <div>
        <label htmlFor="todoId">Todo ID:</label>
        <input
          type="text"
          id="todoId"
          placeholder="Enter todo ID"
          value={todoId}
          onChange={(e) => setTodoId(e.target.value)}
        />
        <button id="fetchTodo" onClick={fetchTodo}>
          Fetch Todo
        </button>
      </div>
      {result && (
        <div id="todoResult" dangerouslySetInnerHTML={{ __html: result }} />
      )}
    </main>
  );
}
