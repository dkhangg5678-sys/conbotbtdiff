import { randomUUID } from 'node:crypto'
import { afterEach, describe, expect, it } from 'vitest'
import { buildApp } from '../src/app.js'
import type { ProvisionInput, ProvisionResult, ProvisionStore } from '../src/types.js'

class FakeStore implements ProvisionStore {
  validToken = 'b'.repeat(43)
  expired = false
  used = false
  credential = 'c'.repeat(43)
  deviceId = randomUUID()
  resetGrant = 'r'.repeat(43)
  resetUsed = false

  async provision(input: ProvisionInput): Promise<ProvisionResult | null> {
    if (input.kind === 'bootstrap') {
      if (input.token !== this.validToken || this.expired || this.used) return null
      this.used = true
      return { deviceId: this.deviceId, credential: this.credential }
    }
    if (input.deviceId !== this.deviceId || input.grant !== this.resetGrant || this.resetUsed) return null
    this.resetUsed = true
    this.credential = 'n'.repeat(43)
    return { deviceId: this.deviceId, credential: this.credential }
  }
  async authenticate(id: string, credential: string) { return id === this.deviceId && credential === this.credential }
  async issueResetGrant(id: string, credential: string) { return await this.authenticate(id, credential) ? this.resetGrant : null }
  async heartbeat() {}
}

const apps: Array<Awaited<ReturnType<typeof buildApp>>> = []
afterEach(async () => { await Promise.all(apps.splice(0).map((app) => app.close())) })
async function fixture() { const store = new FakeStore(); const app = await buildApp(store); apps.push(app); return { app, store } }
const body = (token: string) => ({ kind: 'bootstrap', token, appVersion: '1.0.0' })

describe('/provision', () => {
  it('rejects a wrong token without revealing why', async () => {
    const { app } = await fixture()
    const response = await app.inject({ method: 'POST', url: '/provision', payload: body('x'.repeat(43)) })
    expect(response.statusCode).toBe(401)
    expect(response.json()).toEqual({ error: 'Provisioning denied' })
  })
  it('rejects an expired token', async () => {
    const { app, store } = await fixture(); store.expired = true
    expect((await app.inject({ method: 'POST', url: '/provision', payload: body(store.validToken) })).statusCode).toBe(401)
  })
  it('consumes a token once and rejects replay', async () => {
    const { app, store } = await fixture()
    expect((await app.inject({ method: 'POST', url: '/provision', payload: body(store.validToken) })).statusCode).toBe(201)
    expect((await app.inject({ method: 'POST', url: '/provision', payload: body(store.validToken) })).statusCode).toBe(401)
  })
  it('rotates only through an authenticated one-time reset grant', async () => {
    const { app, store } = await fixture()
    expect((await app.inject({ method: 'POST', url: '/devices/reset-grant', payload: { deviceId: store.deviceId, credential: 'wrong'.repeat(8) } })).statusCode).toBe(401)
    const grantResponse = await app.inject({ method: 'POST', url: '/devices/reset-grant', payload: { deviceId: store.deviceId, credential: store.credential } })
    const grant = grantResponse.json().grant
    const resetBody = { kind: 'reset', deviceId: store.deviceId, grant, appVersion: '1.0.0' }
    expect((await app.inject({ method: 'POST', url: '/provision', payload: resetBody })).statusCode).toBe(201)
    expect((await app.inject({ method: 'POST', url: '/provision', payload: resetBody })).statusCode).toBe(401)
  })
})
