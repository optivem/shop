import * as path from 'path';
import * as fs from 'fs';

export interface TestConfig {
  shop: {
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
  };
}

export interface ConfigOverrides {
  externalSystemMode?: string;
}

export function loadConfiguration(overrides?: ConfigOverrides): TestConfig {
  const environment = (process.env.ENVIRONMENT || 'local').toLowerCase();
  const externalSystemMode = overrides?.externalSystemMode || (process.env.EXTERNAL_SYSTEM_MODE || 'real').toLowerCase();

  const configFileName = `test-config-${environment}-${externalSystemMode}.json`;
  const configPath = path.join(__dirname, configFileName);

  if (!fs.existsSync(configPath)) {
    throw new Error(`Configuration file not found: ${configPath}`);
  }

  const configContent = fs.readFileSync(configPath, 'utf-8');
  return JSON.parse(configContent) as TestConfig;
}
