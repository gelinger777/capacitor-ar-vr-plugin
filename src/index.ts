import { registerPlugin } from '@capacitor/core';

import type { ArVrPluginPlugin } from './definitions';

const ArVrPlugin = registerPlugin<ArVrPluginPlugin>('ArVrPlugin', {
  web: () => import('./web').then((m) => new m.ArVrPluginWeb()),
});

export * from './definitions';
export { ArVrPlugin };
