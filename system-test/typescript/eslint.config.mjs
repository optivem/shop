import js from '@eslint/js';
import playwright from 'eslint-plugin-playwright';
import globals from 'globals';
import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';

export default defineConfig(
    {
        ignores: ['playwright-report/**', 'test-results/**', 'dist/**'],
    },
    js.configs.recommended,
    ...tseslint.configs.recommendedTypeChecked,
    ...tseslint.configs.stylisticTypeChecked,
    {
        ...playwright.configs['flat/recommended'],
        files: ['tests/**'],
        rules: {
            ...playwright.configs['flat/recommended'].rules,
            'playwright/expect-expect': [
                'warn',
                {
                    assertFunctionNames: ['shouldSucceed', 'shouldFail', 'shouldBeRunning'],
                    assertFunctionPatterns: ['^has[A-Z]'],
                },
            ],
        },
    },
    {
        languageOptions: {
            globals: globals.node,
            parserOptions: {
                projectService: true,
                tsconfigRootDir: import.meta.dirname,
            },
        },
        rules: {
            '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
        },
    },
    {
        files: ['**/*.{js,mjs,cjs}'],
        ...tseslint.configs.disableTypeChecked,
    },
);
