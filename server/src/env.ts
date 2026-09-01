import { z } from 'zod'

const schema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().positive().default(3001),
  DATABASE_URL: z.string().min(1),
  TOKEN_PEPPER: z.string().min(32),
  PROTOCOL_VERSION: z.literal('1').default('1'),
})

export type Env = z.infer<typeof schema>
export const parseEnv = (input: NodeJS.ProcessEnv = process.env): Env => schema.parse(input)
