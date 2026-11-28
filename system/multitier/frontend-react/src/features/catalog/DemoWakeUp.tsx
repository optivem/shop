import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Activity, Check, Cloud, FlaskConical, Info, Plug, ShieldCheck, Sparkles } from 'lucide-react';
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

const SPEC_DRIVEN_STEPS = [
  { titleKey: 'wakeUp.stepSpecTitle', detailKey: 'wakeUp.stepSpecDetail' },
  { titleKey: 'wakeUp.stepAcceptanceTitle', detailKey: 'wakeUp.stepAcceptanceDetail' },
  { titleKey: 'wakeUp.stepAiTitle', detailKey: 'wakeUp.stepAiDetail' },
  { titleKey: 'wakeUp.stepPipelineTitle', detailKey: 'wakeUp.stepPipelineDetail' },
] as const;

const HIGHLIGHTS = [
  { icon: Sparkles, titleKey: 'wakeUp.uxTitle', detailKey: 'wakeUp.uxDetail' },
  { icon: ShieldCheck, titleKey: 'wakeUp.securityTitle', detailKey: 'wakeUp.securityDetail' },
  { icon: Plug, titleKey: 'wakeUp.externalTitle', detailKey: 'wakeUp.externalDetail' },
  { icon: FlaskConical, titleKey: 'wakeUp.testingTitle', detailKey: 'wakeUp.testingDetail' },
  { icon: Activity, titleKey: 'wakeUp.observabilityTitle', detailKey: 'wakeUp.observabilityDetail' },
  { icon: Cloud, titleKey: 'wakeUp.cloudTitle', detailKey: 'wakeUp.cloudDetail' },
] as const;

const EXTERNAL_SYSTEMS = [
  { nameKey: 'wakeUp.extAuth', detailKey: 'wakeUp.extAuthDetail' },
  { nameKey: 'wakeUp.extErp', detailKey: 'wakeUp.extErpDetail' },
  { nameKey: 'wakeUp.extTax', detailKey: 'wakeUp.extTaxDetail' },
  { nameKey: 'wakeUp.extClock', detailKey: 'wakeUp.extClockDetail' },
] as const;

const PIPELINE_STAGES = [
  'wakeUp.stageCommit',
  'wakeUp.stageAcceptance',
  'wakeUp.stageQa',
  'wakeUp.stageQaSignOff',
  'wakeUp.stageProduction',
] as const;

const TECH_STACK = [
  'TypeScript',
  'React 19',
  'Vite',
  'Bootstrap 5',
  'Keycloak',
  'Java / Spring Boot',
  '.NET',
  'NestJS',
  'PostgreSQL',
  'Docker',
  'GitHub Actions',
  'ATDD + TDD',
];

/**
 * The catalog's loading state, shown from the first moment: the demo deployment scales to zero, so a first
 * visit waits on a cold start, and the card says so and shows how the shop is built while the services wake up. It shows nothing it cannot observe: the backend and ERP stay "Waking up"
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

        <hr className="wake-up-divider" />

        <div className="wake-up-behind">
          <h3 className="wake-up-eyebrow">{t('wakeUp.behind')}</h3>

          <div>
            <h4 className="wake-up-label">{t('wakeUp.howBuilt')}</h4>
            <ol className="wake-up-steps">
              {SPEC_DRIVEN_STEPS.map((step) => (
                <li key={step.titleKey}>
                  <strong>{t(step.titleKey)}</strong>
                  {t(step.detailKey)}
                </li>
              ))}
            </ol>
          </div>

          <div>
            <h4 className="wake-up-label">{t('wakeUp.highlights')}</h4>
            <ul className="wake-up-highlights">
              {HIGHLIGHTS.map(({ icon: Icon, titleKey, detailKey }) => (
                <li key={titleKey}>
                  <Icon size={20} aria-hidden="true" className="wake-up-highlight-icon" />
                  <strong>{t(titleKey)}</strong>
                  {t(detailKey)}
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="wake-up-label">{t('wakeUp.architecture')}</h4>
            <div className="wake-up-arch">
              <div className="wake-up-box">
                {t('wakeUp.frontend')}<small>React 19 + Vite</small>
              </div>
              <div className="wake-up-arrow" aria-hidden="true" />
              <div className="wake-up-box wake-up-box-main">
                {t('wakeUp.backend')}<small>{t('wakeUp.backendStack')}</small>
              </div>
              <div className="wake-up-arrow" aria-hidden="true" />
              <div className="wake-up-box">
                {t('wakeUp.database')}<small>PostgreSQL</small>
              </div>
              <p className="wake-up-ext-label">{t('wakeUp.externalLabel')}</p>
              <div className="wake-up-ext">
                {EXTERNAL_SYSTEMS.map((system) => (
                  <div key={system.nameKey} className="wake-up-box">
                    {t(system.nameKey)}
                    <small>{t(system.detailKey)}</small>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div>
            <h4 className="wake-up-label">{t('wakeUp.pipeline')}</h4>
            <ol className="wake-up-pipeline">
              {PIPELINE_STAGES.map((stage) => (
                <li key={stage}>{t(stage)}</li>
              ))}
            </ol>
            <p className="wake-up-detail">
              {t('wakeUp.pipelineDetail')}
            </p>
          </div>

          <div>
            <h4 className="wake-up-label">{t('wakeUp.techStack')}</h4>
            <ul className="wake-up-chips">
              {TECH_STACK.map((tech) => (
                <li key={tech}>{tech}</li>
              ))}
            </ul>
            <p className="wake-up-detail">{t('wakeUp.techStackNote')}</p>
          </div>
        </div>
      </section>
    </div>
  );
}
