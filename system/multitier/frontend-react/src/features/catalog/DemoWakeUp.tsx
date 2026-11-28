import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Cloud, Database, Info, KeyRound, Laptop, Server, type LucideIcon } from 'lucide-react';
import { authServerWarmUp } from '../../auth/user-manager';
import { probeSystem, type ProbedSystem, type ProbeResult } from '../../services/status-service';

type ServiceState = 'waking' | 'ready' | 'down';
/** A link in the diagram: idle until traffic can reach it, then waking (requests in flight), then ready. */
type LinkState = 'idle' | 'waking' | 'ready';

const STATUS_KEY = {
  waking: 'wakeUp.statusWaking',
  ready: 'wakeUp.statusReady',
  down: 'wakeUp.statusDown',
} as const;

/** How often a part that has not answered yet is asked again, and how long until it counts as not responding. */
const POLL_INTERVAL_MS = 2000;
const DOWN_AFTER_MS = 60_000;

/**
 * Reports when the auth server answers the warm-up ping the app sends on load (authServerWarmUp).
 * Undefined when authentication is disabled.
 */
function useAuthServerState(): ServiceState | undefined {
  const [state, setState] = useState<ServiceState | undefined>(authServerWarmUp ? 'waking' : undefined);

  useEffect(() => {
    if (!authServerWarmUp) return;
    let ignore = false;
    authServerWarmUp.then(
      () => !ignore && setState('ready'),
      () => !ignore && setState('down'),
    );
    return () => {
      ignore = true;
    };
  }, []);

  return state;
}

/** Asks the backend about one part until it reports UP; the first ask is also what wakes that part. */
function useProbe(system: ProbedSystem): ProbeResult {
  const [result, setResult] = useState<ProbeResult>({ reached: false, up: false });

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    const ask = async () => {
      const next = await probeSystem(system, controller.signal);
      if (controller.signal.aborted) return;
      setResult(next);
      if (!next.up) timer = setTimeout(() => void ask(), POLL_INTERVAL_MS);
    };
    void ask();
    return () => {
      controller.abort();
      clearTimeout(timer);
    };
  }, [system]);

  return result;
}

function stateOf(up: boolean, elapsedMs: number): ServiceState {
  if (up) return 'ready';
  return elapsedMs >= DOWN_AFTER_MS ? 'down' : 'waking';
}

function linkOf(reachable: boolean, ready: boolean): LinkState {
  if (!reachable) return 'idle';
  return ready ? 'ready' : 'waking';
}

interface NodeProps {
  icon: LucideIcon;
  name: string;
  /** None for the visitor's own browser, which is always there. */
  state?: ServiceState;
  area?: string;
  compact?: boolean;
}

function DiagramNode({ icon: Icon, name, state, area, compact = false }: Readonly<NodeProps>) {
  const { t } = useTranslation('catalog');
  const stateClass = state ? `wake-up-node-${state}` : 'wake-up-node-you';
  return (
    <div
      className={`wake-up-node ${stateClass}${compact ? ' wake-up-node-compact' : ''}`}
      style={area ? { gridArea: area } : undefined}
    >
      <Icon size={compact ? 18 : 24} aria-hidden="true" />
      <span className="wake-up-node-text">
        <span className="wake-up-node-name">{name}</span>
        {state && (
          <span className="wake-up-node-status">
            <span className="wake-up-node-dot" aria-hidden="true" />
            {t(STATUS_KEY[state])}
          </span>
        )}
      </span>
    </div>
  );
}

function DiagramLink({ state, area, vertical = false }: Readonly<{ state: LinkState; area: string; vertical?: boolean }>) {
  return (
    <span
      className={`wake-up-link wake-up-link-${area} wake-up-link-${state} ${vertical ? 'wake-up-link-v' : 'wake-up-link-h'}`}
      style={{ gridArea: area }}
      aria-hidden="true"
    />
  );
}

/**
 * The catalog's loading state, shown from the first moment as a dialog over the shop: the demo deployment
 * scales to zero, so a first visit waits on a cold start. The diagram shows only what it observes: the backend
 * reports on itself and the database, and on each external system (asking wakes them early); the auth server is
 * probed on its own. The catalog's arrival closes the dialog.
 */
export function DemoWakeUp() {
  const { t } = useTranslation('catalog');
  const [elapsedMs, setElapsedMs] = useState(0);
  const authState = useAuthServerState();
  const backend = useProbe('backend');
  const erp = useProbe('erp');
  const tax = useProbe('tax');
  const clock = useProbe('clock');

  useEffect(() => {
    const startedAt = Date.now();
    const timer = setInterval(() => setElapsedMs(Date.now() - startedAt), 1000);
    return () => clearInterval(timer);
  }, []);

  const backendReached = backend.reached || erp.reached || tax.reached || clock.reached;
  const externalsReady = erp.up && tax.up && clock.up;

  return (
    <div className="wake-up-overlay">
      <section className="wake-up" role="dialog" aria-modal="true" aria-label={t('wakeUp.aria')}>
        <div className="wake-up-head">
          <div className="spinner-border text-primary wake-up-spinner" aria-hidden="true" />
          <div>
            <h2 className="wake-up-title">{t('wakeUp.title')}</h2>
            <p className="wake-up-sub" role="status">
              {t('wakeUp.waiting', { seconds: Math.floor(elapsedMs / 1000) })}
            </p>
          </div>
        </div>

        <p className="wake-up-note" aria-label={t('wakeUp.noteAria')}>
          <Info size={20} aria-hidden="true" className="wake-up-note-icon" />
          <span>{t('wakeUp.note')}</span>
        </p>

        <div
          className={`wake-up-arch${authState ? '' : ' wake-up-arch-no-auth'}`}
          role="group"
          aria-label={t('wakeUp.diagramAria')}
        >
          <DiagramNode icon={Laptop} name={t('wakeUp.serviceBrowser')} area="browser" />
          <DiagramLink state={linkOf(true, backendReached)} area="to-backend" />
          <DiagramNode icon={Server} name={t('wakeUp.serviceBackend')} state={stateOf(backendReached, elapsedMs)} area="backend" />
          <DiagramLink state={linkOf(backendReached, externalsReady)} area="to-ext" />
          <div className="wake-up-ext" style={{ gridArea: 'ext' }} role="group" aria-label={t('wakeUp.externalLabel')}>
            <span className="wake-up-ext-label" aria-hidden="true">
              {t('wakeUp.externalLabel')}
            </span>
            <DiagramNode icon={Cloud} name={t('wakeUp.serviceErp')} state={stateOf(erp.up, elapsedMs)} compact />
            <DiagramNode icon={Cloud} name={t('wakeUp.serviceTax')} state={stateOf(tax.up, elapsedMs)} compact />
            <DiagramNode icon={Cloud} name={t('wakeUp.serviceClock')} state={stateOf(clock.up, elapsedMs)} compact />
          </div>
          {authState && <DiagramLink state={linkOf(true, authState === 'ready')} area="to-auth" vertical />}
          {authState && <DiagramNode icon={KeyRound} name={t('wakeUp.serviceAuth')} state={authState} area="auth" />}
          <DiagramLink state={linkOf(backendReached, backend.up)} area="to-db" vertical />
          <DiagramNode icon={Database} name={t('wakeUp.serviceDatabase')} state={stateOf(backend.up, elapsedMs)} area="db" />
        </div>

        <div className="wake-up-track" aria-hidden="true">
          <div className="wake-up-bar" />
        </div>
        <p className="wake-up-hint">{t('wakeUp.hint')}</p>
        <p className="wake-up-credit">{t('wakeUp.credit')}</p>
      </section>
    </div>
  );
}
