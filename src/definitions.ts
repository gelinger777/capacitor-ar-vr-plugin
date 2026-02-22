import { PluginListenerHandle } from '@capacitor/core';

export interface Poi {
  id: string;
  lat: number;
  lng: number;
  label: string;
  url: string;
  image?: string;     // URL or asset path for thumbnail image
}

export interface ArVrSessionOptions {
  pois: Poi[];
}

export interface ArAvailabilityResult {
  available: boolean;
  status: 'supported' | 'unsupported_device' | 'not_installed' | 'outdated' | 'unknown';
  message: string;
}

export interface ArVrPluginPlugin {
  checkAvailability(): Promise<ArAvailabilityResult>;
  startSession(options: ArVrSessionOptions): Promise<void>;
  stopSession(): Promise<void>;

  addListener(
    eventName: 'onObjectSelected',
    listenerFunc: (data: { id: string; url: string; label: string }) => void,
  ): Promise<PluginListenerHandle>;
}
