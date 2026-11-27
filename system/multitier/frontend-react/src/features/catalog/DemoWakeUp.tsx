import { useEffect, useState } from 'react';
import { Info } from 'lucide-react';
import { LoadingSpinner } from '../../components';
import { DEMO_SITE_NOTE } from '../../utils/messages';

/** How long the catalog may load before the plain spinner turns into the demo site's wake-up card. */
const WAKE_UP_DELAY_MS = 3000;

/** The services the first catalog request wakes: the backend, which asks the ERP for the products. */
const WAKING_SERVICES = ['Shop backend', 'Product catalog (ERP)'];

const SPEC_DRIVEN_STEPS = [
  { title: 'Spec', detail: 'Each feature starts as acceptance criteria in plain business language.' },
  { title: 'Acceptance tests', detail: 'The spec becomes executable acceptance tests that fail first.' },
  { title: 'AI implements', detail: 'AI agents write the code test-first (ATDD + TDD) until the tests pass.' },
  { title: 'Pipeline verifies', detail: 'Nothing ships until every test is green in the pipeline.' },
];

const EXTERNAL_SYSTEMS = [
  { name: 'ERP', detail: 'products & prices' },
  { name: 'Tax', detail: 'tax rates' },
  { name: 'Clock', detail: 'current time' },
];

const PIPELINE_STAGES = ['Commit', 'Acceptance', 'QA', 'QA sign-off', 'Production'];

const TECH_STACK = [
  'TypeScript',
  'React 19',
  'Vite',
  'Bootstrap 5',
  'Java / Spring Boot',
  '.NET',
  'NestJS',
  'PostgreSQL',
  'Docker',
  'GitHub Actions',
  'ATDD + TDD',
];

/**
 * The catalog's loading state. A fast load shows the usual spinner; a slow one (the demo deployment scales
 * to zero, so a first visit waits on a cold start) turns into a card that says so and shows how the shop is
 * built while the services wake up. It shows nothing it cannot observe: the services stay "Waking up" until
 * the catalog arrives, and then the catalog replaces the card.
 */
export function DemoWakeUp() {
  const [elapsedMs, setElapsedMs] = useState(0);

  useEffect(() => {
    const startedAt = Date.now();
    const timer = setInterval(() => setElapsedMs(Date.now() - startedAt), 1000);
    return () => clearInterval(timer);
  }, []);

  if (elapsedMs < WAKE_UP_DELAY_MS) return <LoadingSpinner message="Loading products..." />;

  return (
    <section className="wake-up" aria-label="Demo Site Wake Up">
      <div className="wake-up-head">
        <div className="spinner-border text-primary wake-up-spinner" aria-hidden="true" />
        <div>
          <h2 className="wake-up-title">Waking up the demo shop…</h2>
          <p className="wake-up-sub" role="status">
            Waiting for {Math.floor(elapsedMs / 1000)} s. The first visit can take up to a minute.
          </p>
        </div>
      </div>

      <p className="wake-up-note" aria-label="Slow Loading Hint">
        <Info size={20} aria-hidden="true" className="wake-up-note-icon" />
        <span>{DEMO_SITE_NOTE}</span>
      </p>

      <ul className="wake-up-services">
        {WAKING_SERVICES.map((service) => (
          <li key={service}>
            <span className="wake-up-service-dot" aria-hidden="true" />
            <span>{service}</span>
            <span className="wake-up-service-status">Waking up…</span>
          </li>
        ))}
      </ul>
      <div className="wake-up-track" aria-hidden="true">
        <div className="wake-up-bar" />
      </div>
      <p className="wake-up-hint">The shop opens by itself when everything is ready.</p>

      <hr className="wake-up-divider" />

      <div className="wake-up-behind">
        <h3 className="wake-up-eyebrow">While you wait: what's behind this demo</h3>

        <div>
          <h4 className="wake-up-label">How it's built: Spec-Driven Development with AI</h4>
          <ol className="wake-up-steps">
            {SPEC_DRIVEN_STEPS.map((step) => (
              <li key={step.title}>
                <strong>{step.title}</strong>
                {step.detail}
              </li>
            ))}
          </ol>
        </div>

        <div>
          <h4 className="wake-up-label">Architecture</h4>
          <div className="wake-up-arch">
            <div className="wake-up-box">
              Frontend<small>React 19 + Vite</small>
            </div>
            <div className="wake-up-arrow" aria-hidden="true" />
            <div className="wake-up-box wake-up-box-main">
              Backend<small>Java, .NET or TypeScript</small>
            </div>
            <div className="wake-up-arrow" aria-hidden="true" />
            <div className="wake-up-box">
              Database<small>PostgreSQL</small>
            </div>
            <p className="wake-up-ext-label">The backend calls three external systems</p>
            <div className="wake-up-ext">
              {EXTERNAL_SYSTEMS.map((system) => (
                <div key={system.name} className="wake-up-box">
                  {system.name}
                  <small>{system.detail}</small>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div>
          <h4 className="wake-up-label">Delivery pipeline</h4>
          <ol className="wake-up-pipeline">
            {PIPELINE_STAGES.map((stage) => (
              <li key={stage}>{stage}</li>
            ))}
          </ol>
          <p className="wake-up-detail">
            Every change goes through GitHub Actions: unit and component tests at commit, then acceptance, contract
            and end-to-end tests against the deployed system before it reaches production.
          </p>
        </div>

        <div>
          <h4 className="wake-up-label">Tech stack</h4>
          <ul className="wake-up-chips">
            {TECH_STACK.map((tech) => (
              <li key={tech}>{tech}</li>
            ))}
          </ul>
          <p className="wake-up-detail">The same shop is built three times: in Java, .NET and TypeScript.</p>
        </div>
      </div>
    </section>
  );
}
