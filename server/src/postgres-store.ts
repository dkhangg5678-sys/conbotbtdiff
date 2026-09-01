import { randomUUID } from 'node:crypto'
import { Pool, type PoolClient } from 'pg'
import { randomToken, tokenHash } from './crypto.js'
import type { ProvisionInput, ProvisionResult, ProvisionStore } from './types.js'

export class PostgresProvisionStore implements ProvisionStore {
  constructor(private readonly pool: Pool, private readonly pepper: string) {}

  private async transaction<T>(work: (client: PoolClient) => Promise<T>): Promise<T> {
    const client = await this.pool.connect()
    try {
      await client.query('BEGIN')
      const result = await work(client)
      await client.query('COMMIT')
      return result
    } catch (error) {
      await client.query('ROLLBACK')
      throw error
    } finally {
      client.release()
    }
  }

  async provision(input: ProvisionInput): Promise<ProvisionResult | null> {
    return this.transaction(async (client) => {
      let deviceId: string = randomUUID()
      if (input.kind === 'bootstrap') {
        const consumed = await client.query(
          `UPDATE bootstrap_tokens SET consumed_at = now()
           WHERE token_hash = $1 AND consumed_at IS NULL AND expires_at > now()
           RETURNING token_hash`,
          [tokenHash(input.token, this.pepper)],
        )
        if (consumed.rowCount !== 1) return null
      } else {
        const consumed = await client.query<{ device_id: string; credential_version: number }>(
          `UPDATE reset_grants AS grants SET consumed_at = now()
           FROM devices
           WHERE grants.grant_hash = $1
             AND grants.device_id = $2::uuid
             AND grants.consumed_at IS NULL
             AND grants.expires_at > now()
             AND devices.id = grants.device_id
             AND devices.revoked_at IS NULL
             AND devices.credential_version = grants.credential_version
           RETURNING grants.device_id, grants.credential_version`,
          [tokenHash(input.grant, this.pepper), input.deviceId],
        )
        if (consumed.rowCount !== 1) return null
        deviceId = input.deviceId
      }

      const credential = randomToken()
      const credentialHash = tokenHash(credential, this.pepper)
      if (input.kind === 'reset') {
        const rotated = await client.query(
          `UPDATE devices SET credential_hash = $1, credential_version = credential_version + 1,
             revoked_at = NULL, app_version = $2, updated_at = now()
           WHERE id = $3::uuid AND revoked_at IS NULL`,
          [credentialHash, input.appVersion, deviceId],
        )
        if (rotated.rowCount !== 1) throw new Error('Device rotation failed')
      } else {
        await client.query(
          `INSERT INTO devices (id, credential_hash, app_version) VALUES ($1::uuid, $2, $3)`,
          [deviceId, credentialHash, input.appVersion],
        )
      }
      return { deviceId, credential }
    })
  }

  async authenticate(deviceId: string, credential: string): Promise<boolean> {
    const result = await this.pool.query(
      `SELECT 1 FROM devices WHERE id = $1::uuid AND credential_hash = $2 AND revoked_at IS NULL`,
      [deviceId, tokenHash(credential, this.pepper)],
    )
    return result.rowCount === 1
  }

  async issueResetGrant(deviceId: string, credential: string): Promise<string | null> {
    const grant = randomToken()
    const inserted = await this.pool.query(
      `INSERT INTO reset_grants (grant_hash, device_id, credential_version, expires_at)
       SELECT $1, id, credential_version, now() + interval '2 minutes'
       FROM devices WHERE id = $2::uuid AND credential_hash = $3 AND revoked_at IS NULL
       RETURNING grant_hash`,
      [tokenHash(grant, this.pepper), deviceId, tokenHash(credential, this.pepper)],
    )
    return inserted.rowCount === 1 ? grant : null
  }

  async heartbeat(deviceId: string, appVersion: string): Promise<void> {
    await this.pool.query(
      `UPDATE devices SET last_seen_at = now(), app_version = $2, updated_at = now() WHERE id = $1::uuid`,
      [deviceId, appVersion],
    )
  }
}
