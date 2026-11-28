import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Check, Info } from 'lucide-react';
import { authServerWarmUp } from '../../auth/user-manager';

/** The services the first catalog request wakes: the backend, which asks the ERP for the products. */
type ServiceState = 'waking' | 'ready' | 'down';

const STATUS_KEY = {
  waking: 'wakeUp.statusWaking',
  ready: 'wakeUp.statusReady',
  down: 'wakeUp.statusDown',
} as const;

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

/**
 * The catalog's loading state, shown from the first moment: the demo deployment scales to zero, so a first
 * visit waits on a cold start, and the card says so while the services wake up. It shows nothing it cannot observe: the backend and ERP stay "Waking up"
 * until the catalog arrives (then the catalog replaces the card); the auth server is probed on its own.
 */
export function DemoWakeUp() {
  const { t } = useTranslation('catalog');
  const [elapsedMs, setElapsedMs] = useState(0);
  const authState = useAuthServerState();

  useEffect(() => {
    const startedAt = Date.now();
    const timer = setInterval(() => setElapsedMs(Date.now() - startedAt), 1000);
    return () => clearInterval(timer);
  }, []);

  const services: { name: string; state: ServiceState }[] = [
    { name: t('wakeUp.serviceBackend'), state: 'waking' },
    { name: t('wakeUp.serviceErp'), state: 'waking' },
  ];
  if (authState) services.push({ name: t('wakeUp.serviceAuth'), state: authState });

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

        <ul className="wake-up-services">
          {services.map((service) => (
            <li key={service.name} className={`wake-up-service-${service.state}`}>
              <span className="wake-up-service-dot" aria-hidden="true">
                {service.state === 'ready' && <Check size={14} strokeWidth={3} />}
              </span>
              <span>{service.name}</span>
              <span className="wake-up-service-status">{t(STATUS_KEY[service.state])}</span>
            </li>
          ))}
        </ul>
        <div className="wake-up-track" aria-hidden="true">
          <div className="wake-up-bar" />
        </div>
        <p className="wake-up-hint">{t('wakeUp.hint')}</p>
        <p className="wake-up-credit">{t('wakeUp.credit')}</p>
      </section>
    </div>
  );
}
