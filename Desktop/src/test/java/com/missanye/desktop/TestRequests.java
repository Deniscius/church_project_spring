package com.missanye.desktop;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class TestRequests {
    static final String PARISH="731c7ab4-2fae-4d78-a17b-0ff7653f4501";
    private TestRequests(){}
    static ObjectNode complete(){return new ObjectMapper().createObjectNode().put("intention","Une intention de messe complète").put("telFidele","+22890123456").put("dateDebut","2027-01-10").put("paroissePublicId",PARISH).put("typeDemandePublicId","731c7ab4-2fae-4d78-a17b-0ff7653f4502").put("forfaitTarifPublicId","731c7ab4-2fae-4d78-a17b-0ff7653f4503").put("typePaiementPublicId","731c7ab4-2fae-4d78-a17b-0ff7653f4504").put("horairePublicId","731c7ab4-2fae-4d78-a17b-0ff7653f4505");}
}
