SELECT json_build_object(
 'marker',(SELECT marker FROM perf_fixture),
 'password','PerfOnly-2026!',
 'users',(SELECT json_agg(json_build_object(
   'username','perf-user-'||n,
   'parish',p.public_id,
   'cursor',coalesce(c.revision,0),
   'payload',json_build_object(
     'intention','Intention fictive de performance','nomFidele','Performance','prenomFidele','Fictif','telFidele','+22890000000','emailFidele','perf@example.test',
     'dateDebut',(date_trunc('week',current_date+interval '28 days'))::date,
     'joursConsecutifs',false,'paroissePublicId',p.public_id,
     'typeDemandePublicId',md5('perf-type-'||((n-1)%10+1))::uuid,
     'forfaitTarifPublicId',md5('perf-price-'||((n-1)%10+1))::uuid,
     'horairePublicId',md5('perf-schedule-'||((n-1)%10+1)||'-LUNDI')::uuid,
     'typePaiementPublicId',(SELECT public_id FROM type_paiement WHERE mode='ESPECES' AND NOT status_del LIMIT 1)
   ))) FROM generate_series(1,20) n
      JOIN paroisse p ON p.public_id=md5('perf-parish-'||((n-1)%10+1))::uuid
      LEFT JOIN desktop_sync_clock c ON c.parish_id=p.id)
);
