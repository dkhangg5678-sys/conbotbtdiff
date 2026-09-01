import { readFile } from 'node:fs/promises'
import { Pool } from 'pg'

const url = process.env.DATABASE_URL
if (!url) throw new Error('DATABASE_URL is required')
const pool = new Pool({ connectionString: url, ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : undefined })
const sql = await readFile(new URL('../migrations/001_initial.sql', import.meta.url), 'utf8')
try { await pool.query(sql) } finally { await pool.end() }
