import { WebPlugin } from '@capacitor/core';

import type { ArVrPluginPlugin } from './definitions';

export class ArVrPluginWeb extends WebPlugin implements ArVrPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
