\set ON_ERROR_STOP on
DO $$ BEGIN IF current_database()<>'missanye_perf' THEN RAISE EXCEPTION 'Only the isolated missanye_perf database is allowed'; END IF; END $$;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE TABLE perf_fixture(marker text PRIMARY KEY CHECK(marker='isolated-synthetic-only'));
INSERT INTO perf_fixture VALUES('isolated-synthetic-only');
INSERT INTO users(public_id,nom,prenom,username,password,email,role,is_global,is_active,created_at,updated_at)
VALUES(md5('perf-observer')::uuid,'Performance','Observer','perf-observer',crypt('PerfOnly-2026!',gen_salt('bf',10)),'observer@example.test','SUPER_ADMIN',true,true,now(),now());
INSERT INTO paroisse(public_id,nom,adresse,email,doyenne_id,statut_tenant,is_active,subscription_expires_at,created_at,updated_at)
SELECT md5('perf-parish-'||n)::uuid,'PERF Paroisse '||n,'Fictive','parish'||n||'@example.test',(SELECT id FROM doyenne ORDER BY id LIMIT 1),'ACTIVE',true,now()+interval '1 year',now(),now()
FROM generate_series(1,10) n;
INSERT INTO users(public_id,nom,prenom,username,password,email,role,is_global,is_active,created_at,updated_at)
SELECT md5('perf-user-'||n)::uuid,'Performance','Agent '||n,'perf-user-'||n,crypt('PerfOnly-2026!',gen_salt('bf',10)),'agent'||n||'@example.test','SECRETAIRE',false,true,now(),now()
FROM generate_series(1,20) n;
INSERT INTO paroisse_access(public_id,user_id,paroisse_id,role_paroisse,active,created_at,updated_at)
SELECT gen_random_uuid(),u.id,p.id,'SECRETAIRE',true,now(),now()
FROM generate_series(1,20) n JOIN users u ON u.username='perf-user-'||n JOIN paroisse p ON p.public_id=md5('perf-parish-'||((n-1)%10+1))::uuid;
INSERT INTO type_demande(public_id,paroisse_id,type_principal,libelle,delai_minimum_heures,is_active,created_at,updated_at)
SELECT md5('perf-type-'||n)::uuid,p.id,'EUCHARISTIE','Messe performance',24,true,now(),now()
FROM generate_series(1,10) n JOIN paroisse p ON p.public_id=md5('perf-parish-'||n)::uuid;
INSERT INTO forfait_tarif(public_id,type_demande_id,code_forfait,nom_forfait,nature_forfait,montant_forfait,nombre_celebration,nombre_jour,is_active,created_at,updated_at)
SELECT md5('perf-price-'||n)::uuid,t.id,'PERF-'||n,'Une célébration','NORMALE',5000,1,1,true,now(),now()
FROM generate_series(1,10) n JOIN type_demande t ON t.public_id=md5('perf-type-'||n)::uuid;
INSERT INTO horaire(public_id,paroisse_id,jour_semaine,heure_celebration,libelle,is_active,created_at,updated_at)
SELECT md5('perf-schedule-'||n||'-'||day)::uuid,p.id,day,'09:00:00','Messe fictive',true,now(),now()
FROM generate_series(1,10) n JOIN paroisse p ON p.public_id=md5('perf-parish-'||n)::uuid CROSS JOIN unnest(ARRAY['LUNDI','MARDI','MERCREDI','JEUDI','VENDREDI','SAMEDI','DIMANCHE']) day;
INSERT INTO demande(public_id,code_suivie,paroisse_id,type_demande_id,forfait_tarif_id,horaire_id,type_paiement_id,nom_fidele,prenom_fidele,tel_fidele,email_fidele,intention,montant,statut_demande,statut_paiement,statut_validation,created_at,updated_at)
SELECT md5('perf-demand-'||n||'-'||g)::uuid,'PERF-'||n||'-'||g,p.id,t.id,f.id,h.id,pay.id,'Fictif','Fidele '||g,'+22890000000','fidele@example.test','Intention fictive de performance',5000,
CASE WHEN g%4=0 THEN 'EN_ATTENTE' ELSE 'VALIDEE' END,CASE WHEN g%3=0 THEN 'PAYE' ELSE 'NON_PAYE' END,CASE WHEN g%4=0 THEN 'EN_ATTENTE' ELSE 'VALIDEE' END,
now()-(g*interval '5 minutes'),now()-(g*interval '5 minutes')
FROM generate_series(1,10) n CROSS JOIN generate_series(1,2000) g
JOIN paroisse p ON p.public_id=md5('perf-parish-'||n)::uuid
JOIN type_demande t ON t.public_id=md5('perf-type-'||n)::uuid
JOIN forfait_tarif f ON f.public_id=md5('perf-price-'||n)::uuid
JOIN horaire h ON h.public_id=md5('perf-schedule-'||n||'-LUNDI')::uuid
CROSS JOIN LATERAL (SELECT id FROM type_paiement WHERE mode='ESPECES' AND NOT status_del LIMIT 1) pay;
INSERT INTO demande_date(public_id,demande_id,date_celebration,ordre,horaire_id,created_at,updated_at)
SELECT gen_random_uuid(),d.id,current_date+(d.id%60)::integer-30,1,d.horaire_id,d.created_at,d.updated_at FROM demande d WHERE code_suivie LIKE 'PERF-%';
INSERT INTO facture(public_id,demande_id,ref_facture,montant,statut_paiement,created_at,updated_at)
SELECT gen_random_uuid(),id,'FAC-'||code_suivie,5000,statut_paiement,created_at,updated_at FROM demande WHERE code_suivie LIKE 'PERF-%';
ANALYZE;
SELECT pg_stat_statements_reset();
