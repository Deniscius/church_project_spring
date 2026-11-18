CREATE TABLE mail_outbox (
 id uuid PRIMARY KEY,
 payload bytea,
 state varchar(12) NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','SENDING','SENT','DEAD')),
 attempts integer NOT NULL DEFAULT 0,
 available_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 lease_token uuid,
 lease_until timestamptz,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finished_at timestamptz,
 last_error varchar(100)
);
CREATE INDEX mail_outbox_ready ON mail_outbox(available_at) WHERE state='PENDING';
CREATE INDEX mail_outbox_lease ON mail_outbox(lease_until) WHERE state='SENDING';
