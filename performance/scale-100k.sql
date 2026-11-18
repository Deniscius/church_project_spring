\set ON_ERROR_STOP on
DO $$ BEGIN
 IF current_database()<>'missanye_perf' OR NOT EXISTS(SELECT 1 FROM perf_fixture WHERE marker='isolated-synthetic-only') THEN RAISE EXCEPTION 'Wrong database'; END IF;
 IF (SELECT count(*) FROM demande WHERE code_suivie LIKE 'PERF-%')<>20000 THEN RAISE EXCEPTION 'Scaling requires the initial 20000-row fixture'; END IF;
END $$;
INSERT INTO demande(public_id,code_suivie,paroisse_id,type_demande_id,forfait_tarif_id,horaire_id,type_paiement_id,nom_fidele,prenom_fidele,tel_fidele,email_fidele,intention,montant,statut_demande,statut_paiement,statut_validation,created_at,updated_at)
SELECT md5('perf-demand-'||n||'-'||g)::uuid,'PERF-'||n||'-'||g,p.id,t.id,f.id,h.id,pay.id,'Fictif','Fidele '||g,'+22890000000','fidele@example.test','Intention fictive de performance',5000,
CASE WHEN g%4=0 THEN 'EN_ATTENTE' ELSE 'VALIDEE' END,CASE WHEN g%3=0 THEN 'PAYE' ELSE 'NON_PAYE' END,CASE WHEN g%4=0 THEN 'EN_ATTENTE' ELSE 'VALIDEE' END,
now()-(g*interval '5 minutes'),now()-(g*interval '5 minutes')
FROM generate_series(1,10) n CROSS JOIN generate_series(2001,10000) g
JOIN paroisse p ON p.public_id=md5('perf-parish-'||n)::uuid
JOIN type_demande t ON t.public_id=md5('perf-type-'||n)::uuid
JOIN forfait_tarif f ON f.public_id=md5('perf-price-'||n)::uuid
JOIN horaire h ON h.public_id=md5('perf-schedule-'||n||'-LUNDI')::uuid
CROSS JOIN LATERAL (SELECT id FROM type_paiement WHERE mode='ESPECES' AND NOT status_del LIMIT 1) pay;
INSERT INTO demande_date(public_id,demande_id,date_celebration,ordre,horaire_id,created_at,updated_at)
SELECT gen_random_uuid(),d.id,current_date+(d.id%60)::integer-30,1,d.horaire_id,d.created_at,d.updated_at FROM demande d WHERE code_suivie LIKE 'PERF-%' ON CONFLICT(demande_id,ordre) DO NOTHING;
INSERT INTO facture(public_id,demande_id,ref_facture,montant,statut_paiement,created_at,updated_at)
SELECT gen_random_uuid(),id,'FAC-'||code_suivie,5000,statut_paiement,created_at,updated_at FROM demande WHERE code_suivie LIKE 'PERF-%' ON CONFLICT(demande_id) DO NOTHING;
ANALYZE;
SELECT pg_stat_statements_reset();
