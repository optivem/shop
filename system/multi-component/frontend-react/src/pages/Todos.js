import React, { useState } from 'react';
import './Todos.css';

function Todos() {
  const [todoId, setTodoId] = useState('');
  const [todo, setTodo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchTodo = async () => {
    if (!todoId) {
      alert('Please enter a Todo ID');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`http://localhost:8080/api/todos/${todoId}`);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setTodo(data);
    } catch (err) {
      setError(err.message);
      setTodo(null);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      fetchTodo();
    }
  };

  return (
    <div className="container todos">
      <h1>Todo Fetcher</h1>

      <div className="form-group">
        <label htmlFor="todoId">Enter Todo ID:</label>
        <input
          type="text"
          id="todoId"
          placeholder="Enter Todo ID"
          value={todoId}
          onChange={(e) => setTodoId(e.target.value)}
          onKeyPress={handleKeyPress}
        />
        <button id="fetchTodo" onClick={fetchTodo}>
          Fetch Todo
        </button>
      </div>

      {loading && (
        <div id="todoResult" className="todo-result">
          Loading...
        </div>
      )}

      {error && (
        <div id="todoResult" className="todo-result error">
          <p>Error: {error}</p>
        </div>
      )}

      {todo && !loading && !error && (
        <div id="todoResult" className="todo-result">
          <h3>Todo Details:</h3>
          <p><strong>User ID:</strong> {todo.userId}</p>
          <p><strong>ID:</strong> {todo.id}</p>
          <p><strong>Title:</strong> {todo.title}</p>
          <p><strong>Completed:</strong> {todo.completed ? 'Yes' : 'No'}</p>
        </div>
      )}
    </div>
  );
}

export default Todos;