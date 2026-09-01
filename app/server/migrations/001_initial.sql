CREATE TABLE IF NOT EXISTS bootstrap_tokens (
  token_hash bytea PRIMARY KEY,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  build_label text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS devices (
  id uuid PRIMARY KEY,
  credential_hash bytea NOT NULL UNIQUE,
  credential_version integer NOT NULL DEFAULT 1,
  revoked_at timestamptz,
  last_seen_at timestamptz,
  app_version text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reset_grants (
  grant_hash bytea PRIMARY KEY,
  device_id uuid NOT NULL REFERENCES devices(id),
  credential_version integer NOT NULL,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS reset_grants_device_idx ON reset_grants(device_id);
