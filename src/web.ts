import { WebPlugin } from '@capacitor/core';

import type { ArVrPluginPlugin, ArAvailabilityResult } from './definitions';

export class ArVrPluginWeb extends WebPlugin implements ArVrPluginPlugin {
  async checkAvailability(): Promise<ArAvailabilityResult> {
    return {
      available: false,
      status: 'unsupported_device',
      message: 'AR is not supported in the web browser. Please use a native device.',
    };
  }

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
