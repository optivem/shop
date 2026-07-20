// Pact-backed Backend Stub Driver — the "latest" test-kit's low-level plumbing.
//
// It absorbs the raw PactV3 ceremony (construction, addInteraction, and the
// executeTest mock-server lifecycle) so the Backend Stub DSL and the specs above
// it never touch Pact directly. The pact is written into the canonical contracts/
// dir, resolved from the package root via process.cwd() — so a spec's folder depth
// (component/latest, integration/legacy, …) is irrelevant; every consumer spec
// merges idempotently into the one frontend-backend.json.
//
// PactV3 is callback-shaped: executeTest(cb) boots the mock server, runs cb, then
// verifies that every staged interaction was actually exercised. Left as-is, that
// callback has to wrap the spec body — which is exactly the plumbing a spec should
// never see. This driver inverts it: backendUrl() starts executeTest lazily and
// parks inside the callback on a deferred, handing the spec just a URL; finish()
// releases the deferred at teardown and awaits the run, so Pact's verification
// failure still fails the owning test.
import path from 'node:path';
import { PactV3 } from '@pact-foundation/pact';
import type { V3Interaction } from '@pact-foundation/pact';
import type { BackendStubDriver } from '../port/backend-stub-driver';

export class PactBackendStubDriver implements BackendStubDriver {
  private provider = newProvider();
  private staged = 0;
  private url?: Promise<string>;
  private run?: Promise<void>;
  private release?: () => void;
  private failure?: unknown;

  stub(interaction: V3Interaction): void {
    if (this.url) {
      // Pact stages every interaction before its mock server boots, so a stub()
      // after the first frontend gesture would silently never be served. Fail loudly.
      throw new Error(
        `Backend interactions must be staged before the first frontend gesture — ` +
          `"${interaction.uponReceiving}" was staged after the backend had already started. ` +
          `Move the backend.* arrange lines above the frontend.* act lines.`,
      );
    }
    this.provider.addInteraction(interaction);
    this.staged += 1;
  }

  // Boot the stubbed backend on first use and hand back its URL. The executeTest
  // callback is held open on a deferred until finish() releases it.
  //
  // Staging nothing is a claim, not an oversight: the frontend rejects this input on
  // its own and never reaches the network. Booting Pact anyway would add an
  // interaction-less run to the shared pact file, so instead we hand back a dead
  // address — if the frontend does call out, the connection fails and the test says so.
  backendUrl(): Promise<string> {
    if (this.staged === 0) {
      return Promise.resolve(UNREACHABLE_BACKEND);
    }
    if (!this.url) {
      this.url = new Promise<string>((resolveUrl, rejectUrl) => {
        this.run = this.provider
          .executeTest(async (mockServer) => {
            resolveUrl(mockServer.url);
            await new Promise<void>((release) => {
              this.release = release;
            });
          })
          .then(
            () => undefined,
            (error: unknown) => {
              // Verification failure (or a mock-server boot failure, in which case
              // the URL was never resolved). finish() rethrows it on the owning test.
              this.failure = error;
              rejectUrl(error);
            },
          );
      });
    }
    return this.url;
  }

  // Teardown: release the executeTest callback, await Pact's verification of every
  // staged interaction, and rethrow whatever it found. Then clear state for the
  // next test.
  async finish(): Promise<void> {
    const stagedButNeverExercised = this.staged > 0 && !this.url;

    this.release?.();
    await this.run;

    const failure = this.failure;
    this.reset();

    if (failure) {
      throw failure;
    }
    if (stagedButNeverExercised) {
      throw new Error(
        'The backend was stubbed but never called — the test staged interactions and made no frontend gesture.',
      );
    }
  }

  // A fresh provider per test: PactV3 keeps its staged interactions internally and
  // exposes no way to drop them, so a test that stubs without acting cannot leak
  // into the next one.
  reset(): void {
    this.provider = newProvider();
    this.staged = 0;
    this.url = undefined;
    this.run = undefined;
    this.release = undefined;
    this.failure = undefined;
  }
}

// Port 1 is privileged and never bound — any request here fails to connect immediately.
const UNREACHABLE_BACKEND = 'http://127.0.0.1:1';

function newProvider(): PactV3 {
  return new PactV3({
    consumer: 'frontend',
    provider: 'backend',
    dir: path.resolve(process.cwd(), '../../../contracts'),
  });
}
