import { readFile } from 'node:fs/promises'
import { Pool } from 'pg'
import { afterAll, beforeAll, describe, expect, it } from 'vitest'
import { randomToken, tokenHash } from '../src/crypto.js'
import { PostgresProvisionStore } from '../src/postgres-store.js'

const enabled = Boolean(process.env.TEST_DATABASE_URL)
const suite = enabled ? describe : describe.skip
suite('Postgres atomic consumption', () => {
  const pool = new Pool({ connectionString: process.env.TEST_DATABASE_URL })
  const pepper = 'integration-test-pepper-at-least-32-characters'
  const store = new PostgresProvisionStore(pool, pepper)
  beforeAll(async () => { await pool.query(await readFile(new URL('../migrations/001_initial.sql', import.meta.url), 'utf8')) })
  afterAll(async () => { await pool.end() })

  it('allows exactly one winner under concurrent replay', async () => {
    const token = randomToken()
    await pool.query(`INSERT INTO bootstrap_tokens(token_hash, expires_at, build_label) VALUES($1, now() + interval '1 minute', 'test')`, [tokenHash(token, pepper)])
    const results = await Promise.all(Array.from({ length: 8 }, () => store.provision({ kind: 'bootstrap', token, appVersion: 'test' })))
    expect(results.filter(Boolean)).toHaveLength(1)
  })

  it('rejects expired bootstrap tokens', async () => {
    const token = randomToken()
    await pool.query(`INSERT INTO bootstrap_tokens(token_hash, expires_at, build_label) VALUES($1, now() - interval '1 second', 'test')`, [tokenHash(token, pepper)])
    expect(await store.provision({ kind: 'bootstrap', token, appVersion: 'test' })).toBeNull()
  })
})
