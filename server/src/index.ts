import { Pool } from 'pg'
import { buildApp } from './app.js'
import { parseEnv } from './env.js'
import { PostgresProvisionStore } from './postgres-store.js'

const env = parseEnv()
const pool = new Pool({ connectionString: env.DATABASE_URL, ssl: env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : undefined })
const app = await buildApp(new PostgresProvisionStore(pool, env.TOKEN_PEPPER), env.PROTOCOL_VERSION)

const shutdown = async () => { await app.close(); await pool.end() }
process.on('SIGTERM', shutdown)
process.on('SIGINT', shutdown)
await app.listen({ host: '0.0.0.0', port: env.PORT })
