import { Pool } from 'pg'
import { randomToken, tokenHash } from '../src/crypto.js'

const [buildLabel, ttlArg = '300'] = process.argv.slice(2)
const url = process.env.DATABASE_URL
const pepper = process.env.TOKEN_PEPPER
if (!url || !pepper || pepper.length < 32 || !buildLabel) throw new Error('Usage: DATABASE_URL=... TOKEN_PEPPER=... pnpm issue-bootstrap <build-label> [ttl-seconds]')
const ttl = Number(ttlArg)
if (!Number.isInteger(ttl) || ttl < 30 || ttl > 3600) throw new Error('TTL must be 30..3600 seconds')
const token = randomToken()
const pool = new Pool({ connectionString: url })
try {
  await pool.query(`INSERT INTO bootstrap_tokens (token_hash, expires_at, build_label) VALUES ($1, now() + ($2 * interval '1 second'), $3)`, [tokenHash(token, pepper), ttl, buildLabel])
  process.stdout.write(token)
} finally { await pool.end() }
