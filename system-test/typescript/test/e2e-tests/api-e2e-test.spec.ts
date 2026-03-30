describe('API E2E Test', () => {
  it('getTodos_shouldReturnTodoWithExpectedFormat', async () => {
    // DISCLAIMER: This is an example of a badly written test
    // which unfortunately simulates real-life software test projects.
    // This is the starting point for our Starter exercises.

    // Arrange
    const request = 'http://localhost:8080/api/todos/4';

    // Act
    const response = await fetch(request, {
      method: 'GET'
    });

    // Assert
    expect(response.status).toBe(200);

    const responseBody = await response.text();

    // Verify JSON structure contains expected fields
    expect(responseBody).toContain('"userId"');
    expect(responseBody).toContain('"id"');
    expect(responseBody).toContain('"title"');
    expect(responseBody).toContain('"completed"');

    // Verify the specific todo has id 4
    expect(responseBody).toMatch(/"id":\s*4/);
  });
});
