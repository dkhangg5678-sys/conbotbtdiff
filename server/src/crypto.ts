import { createHmac, randomBytes, timingSafeEqual } from 'node:crypto'

export const randomToken = () => randomBytes(32).toString('base64url')
export const tokenHash = (token: string, pepper: string) =>
  createHmac('sha256', pepper).update(token, 'utf8').digest()

export const constantTimeEqual = (a: Buffer, b: Buffer) =>
  a.length === b.length && timingSafeEqual(a, b)
