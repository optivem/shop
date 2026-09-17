import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import * as fs from 'node:fs';
import { envOrDefault, nonEmptyOr } from '../src/testkit/common/fallback.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export interface TestConfig {
  myShop: {
    frontendUrl: string;
    backendApiUrl: string;
  };
  externalSystems: {
    erp: {
      url: string;
    };
    clock: {
      url: string;
    };
    tax: {
      url: string;
    };
  };
}

export interface ConfigOverrides {
  externalSystemMode?: string;
}

export function loadConfiguration(overrides?: ConfigOverrides): TestConfig {
  const environment = envOrDefault('ENVIRONMENT', 'local').toLowerCase();
  const externalSystemMode = nonEmptyOr(overrides?.externalSystemMode, envOrDefault('EXTERNAL_SYSTEM_MODE', 'real').toLowerCase());

  const configFileName = `test-config-${environment}-${externalSystemMode}.json`;
  const configPath = join(__dirname, configFileName);

  if (!fs.existsSync(configPath)) {
    throw new Error(`Configuration file not found: ${configPath}`);
  }

  const configContent = fs.readFileSync(configPath, 'utf-8');
  const config = JSON.parse(configContent) as TestConfig;

  // Env var overrides let the cross-lang verification workflow point this
  // test process at a different language's SUT without touching the JSON files.
  // Suffix matches externalSystemMode so stub/real suites in one tests.yaml
  // run can each get their own URL set.
  const suffix = '_' + externalSystemMode.toUpperCase();
  config.myShop.frontendUrl = getEnvVarOrDefault('MYSHOP_UI_BASE_URL' + suffix, config.myShop.frontendUrl);
  config.myShop.backendApiUrl = getEnvVarOrDefault('MYSHOP_API_BASE_URL' + suffix, config.myShop.backendApiUrl);
  config.externalSystems.erp.url = getEnvVarOrDefault('ERP_API_BASE_URL' + suffix, config.externalSystems.erp.url);
  config.externalSystems.clock.url = getEnvVarOrDefault('CLOCK_API_BASE_URL' + suffix, config.externalSystems.clock.url);
  config.externalSystems.tax.url = getEnvVarOrDefault('TAX_API_BASE_URL' + suffix, config.externalSystems.tax.url);

  return config;
}

function getEnvVarOrDefault(envVarName: string, fileValue: string): string {
  const envValue = process.env[envVarName];
  return envValue && envValue.trim() !== '' ? envValue : fileValue;
}
