import { WebPlugin } from '@capacitor/core';

import type { ArVrPluginPlugin } from './definitions';

export class ArVrPluginWeb extends WebPlugin implements ArVrPluginPlugin {
  async startSession(options: import('./definitions').ArVrSessionOptions): Promise<void> {
    console.log('startSession not supported on web', options);
  }

  async stopSession(): Promise<void> {
    console.log('stopSession not supported on web');
  }

  async toggleVRMode(options: { enable: boolean }): Promise<void> {
    console.log('toggleVRMode not supported on web', options);
  }
}
