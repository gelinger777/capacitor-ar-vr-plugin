import { PluginListenerHandle } from '@capacitor/core';

export interface Poi {
  id: string;
  lat: number;
  lng: number;
  label: string;
  url: string;
}

export interface ArVrSessionOptions {
  pois: Poi[];
}

export interface ArVrPluginPlugin {
  startSession(options: ArVrSessionOptions): Promise<void>;
  stopSession(): Promise<void>;
  toggleVRMode(options: { enable: boolean }): Promise<void>;

  addListener(
    eventName: 'onObjectSelected',
    listenerFunc: (data: { id: string; url: string }) => void,
  ): Promise<PluginListenerHandle>;
}
