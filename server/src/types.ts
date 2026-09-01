export type ProvisionInput =
  | { kind: 'bootstrap'; token: string; appVersion: string }
  | { kind: 'reset'; grant: string; deviceId: string; appVersion: string }

export type ProvisionResult = { deviceId: string; credential: string }

export interface ProvisionStore {
  provision(input: ProvisionInput): Promise<ProvisionResult | null>
  authenticate(deviceId: string, credential: string): Promise<boolean>
  issueResetGrant(deviceId: string, credential: string): Promise<string | null>
  heartbeat(deviceId: string, appVersion: string): Promise<void>
}
