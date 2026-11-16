-- Durable correlation: webhook transaction events do not contain checkout metadata.
CREATE TABLE saspay_checkout (
 id uuid PRIMARY KEY, purpose varchar(20) NOT NULL CHECK(purpose IN ('DEMANDE','ABONNEMENT')),
 target_id uuid NOT NULL, amount integer NOT NULL CHECK(amount>0), platform_fee integer NOT NULL DEFAULT 0,
 remote_id uuid UNIQUE, transaction_id uuid UNIQUE, payment_url varchar(500),
 state varchar(20) NOT NULL DEFAULT 'CREATING', error_message varchar(250),
 charged numeric(18,2), net_amount numeric(18,2), provider_fee numeric(18,2),
 created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, checked_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX saspay_checkout_target ON saspay_checkout(purpose,target_id,created_at DESC);
CREATE INDEX saspay_checkout_pending ON saspay_checkout(checked_at) WHERE state IN ('PENDING','UNCERTAIN');
CREATE TABLE saspay_webhook_event (
 transaction_id uuid NOT NULL, event_type varchar(60) NOT NULL, received_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(transaction_id,event_type)
);
ALTER TABLE details_paiement ADD COLUMN provider_charged numeric(18,2), ADD COLUMN provider_net numeric(18,2), ADD COLUMN provider_fee numeric(18,2);
