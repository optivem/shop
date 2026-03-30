export default function Home() {
  const timestamp = new Date().toISOString();

  return (
    <main>
      <h1>Welcome</h1>
      <p>This page was server-side rendered at {timestamp}.</p>
    </main>
  );
}
