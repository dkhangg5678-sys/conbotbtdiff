import Fastify from 'fastify'
import rateLimit from '@fastify/rate-limit'
import websocket from '@fastify/websocket'
import { z } from 'zod'
import type { ProvisionStore } from './types.js'

const provisionSchema = z.discriminatedUnion('kind', [
  z.object({ kind: z.literal('bootstrap'), token: z.string().min(32).max(256), appVersion: z.string().min(1).max(64) }),
  z.object({ kind: z.literal('reset'), grant: z.string().min(32).max(256), deviceId: z.string().uuid(), appVersion: z.string().min(1).max(64) }),
])
const authSchema = z.object({ deviceId: z.string().uuid(), credential: z.string().min(32).max(256) })

export async function buildApp(store: ProvisionStore, protocolVersion = '1') {
  const app = Fastify({
    logger: { redact: ['req.headers.authorization', 'req.headers.sec-websocket-protocol', 'req.body.token', 'req.body.grant', 'req.body.credential'] },
    bodyLimit: 4096,
    disableRequestLogging: true,
  })
  await app.register(rateLimit, { max: 20, timeWindow: '1 minute' })
  await app.register(websocket)

  app.addHook('onSend', async (_request, reply) => {
    reply.header('Cache-Control', 'no-store').header('X-Content-Type-Options', 'nosniff')
  })

  app.get('/health', async () => ({ ok: true }))
  app.get('/ready', async () => ({ ok: true, protocolVersion, provisioning: true }))

  app.post('/provision', { config: { rateLimit: { max: 5, timeWindow: '1 minute' } } }, async (request, reply) => {
    const parsed = provisionSchema.safeParse(request.body)
    if (!parsed.success) return reply.code(401).send({ error: 'Provisioning denied' })
    const result = await store.provision(parsed.data)
    if (!result) return reply.code(401).send({ error: 'Provisioning denied' })
    return reply.code(201).send({ ...result, protocolVersion })
  })

  app.post('/devices/reset-grant', { config: { rateLimit: { max: 3, timeWindow: '1 minute' } } }, async (request, reply) => {
    const parsed = authSchema.safeParse(request.body)
    if (!parsed.success) return reply.code(401).send({ error: 'Authorization denied' })
    const grant = await store.issueResetGrant(parsed.data.deviceId, parsed.data.credential)
    if (!grant) return reply.code(401).send({ error: 'Authorization denied' })
    return { grant, expiresInSeconds: 120 }
  })

  app.get('/ws', { websocket: true }, (socket, request) => {
    const authorization = request.headers.authorization
    const match = typeof authorization === 'string'
      ? /^Device ([0-9a-f-]{36})\.([A-Za-z0-9_-]{32,256})$/.exec(authorization)
      : null
    if (!match) return socket.close(1008, 'Authentication required')
    const [, deviceId, credential] = match
    void store.authenticate(deviceId, credential).then((valid) => {
      if (!valid) return socket.close(1008, 'Authentication denied')
      socket.on('message', (raw: { toString(): string }) => {
        try {
          const message = z.object({ type: z.literal('heartbeat'), appVersion: z.string().max(64) }).parse(JSON.parse(raw.toString()))
          void store.heartbeat(deviceId, message.appVersion)
        } catch {
          socket.close(1003, 'Invalid message')
        }
      })
      socket.send(JSON.stringify({ type: 'connected', protocolVersion }))
    }).catch(() => socket.close(1011, 'Service unavailable'))
  })
  return app
}
