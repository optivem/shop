describe('API Smoke Test', () => {
  it('echo_shouldReturn200OK', async () => {
    // DISCLAIMER: This is an example of a badly written test
    // which unfortunately simulates real-life software test projects.
    // This is the starting point for our Starter exercises.

    const response = await fetch('http://localhost:8080/api/echo', {
      method: 'GET'
    });

    expect(response.status).toBe(200);
  });
});
