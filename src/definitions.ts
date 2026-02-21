export interface ArVrPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
