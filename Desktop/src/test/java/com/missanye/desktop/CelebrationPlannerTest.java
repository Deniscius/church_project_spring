package com.missanye.desktop;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.missanye.desktop.model.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class CelebrationPlannerTest {
    private final ObjectMapper json=new ObjectMapper();private final CelebrationPlanner planner=new CelebrationPlanner(Clock.fixed(Instant.parse("2026-12-01T06:00:00Z"),ZoneId.of("Africa/Lome")));
    private ObjectNode plan(){return json.createObjectNode().put("publicId","plan").put("natureForfait","NORMALE").put("nombreCelebration",1).put("nombreJour",1);}
    private ObjectNode schedule(String id,String day,String hour){return json.createObjectNode().put("publicId",id).put("jourSemaine",day).put("heureCelebration",hour);}
    @Test void programmeFiltersWeekdayAndOneOffUniqueOverridesWeeklySchedule(){
        var all=json.createArrayNode().add(schedule("mon","LUNDI","08:00").putNull("dateSpecifique").putNull("natureHonoraire")).add(schedule("tue","MARDI","06:30"));var type=json.createObjectNode();var plan=plan();var date=LocalDate.parse("2027-01-11");
        assertEquals("mon",planner.schedules(all,date,type,plan).get(0).path("publicId").asText());
        all.add(schedule("solemn","LUNDI","09:00").put("dateSpecifique",date.toString()).put("uniqueSurParoisse",true));assertEquals(1,planner.schedules(all,date,type,plan).size());assertEquals("solemn",planner.schedules(all,date,type,plan).get(0).path("publicId").asText());
    }
    @Test void seriesRespectsAllowedDaysAndWindowWhileThirtyUsesConsecutiveDates(){
        var plan=plan().put("nombreCelebration",3).put("nombreJour",5);plan.putArray("joursCelebrationAutorises").add("VENDREDI").add("LUNDI").add("MARDI");var type=json.createObjectNode();
        assertEquals(java.util.List.of(LocalDate.parse("2027-01-15"),LocalDate.parse("2027-01-18"),LocalDate.parse("2027-01-19")),planner.dates(LocalDate.parse("2027-01-15"),type,plan));
        plan.put("nombreJour",3);assertThrows(IllegalArgumentException.class,()->planner.dates(LocalDate.parse("2027-01-15"),type,plan));plan.put("nombreCelebration",30).put("nombreJour",30);assertEquals(LocalDate.parse("2027-02-13"),planner.dates(LocalDate.parse("2027-01-15"),type,plan).get(29));
    }
    @Test void customHourRequiresSpecialAuthorizedTariffAndCannotBypassUniqueMass(){
        var type=json.createObjectNode();var plan=plan().put("heurePersonnalise",true);var request=json.createObjectNode().put("dateDebut","2027-01-11").put("heurePersonnalisee","10:00");request.putArray("datesCelebration").add("2027-01-11");var all=json.createArrayNode();
        assertFalse(CelebrationPlanner.customAllowed(type,plan));assertThrows(IllegalArgumentException.class,()->planner.validate(request,type,plan,all));plan.put("natureForfait","SPECIALE");planner.validate(request,type,plan,all);
        all.add(schedule("unique","LUNDI","08:00").put("dateSpecifique","2027-01-11").put("uniqueSurParoisse",true));assertThrows(IllegalArgumentException.class,()->planner.validate(request,type,plan,all));
    }
    @Test void incompatibleDaysAndMinimumNoticeAreRejectedAndSundayTariffIsSelected(){
        var type=json.createObjectNode().put("delaiMinimumHeures",24);type.putArray("joursCelebrationAutorises").add("LUNDI");var plan=plan();plan.putArray("joursCelebrationAutorises").add("MARDI");assertThrows(IllegalArgumentException.class,()->planner.allowedDays(type,plan));
        var normal=plan();var sunday=plan().put("publicId","sunday").put("natureForfait","DOMINICALE");var all=json.createArrayNode().add(schedule("early","MARDI","08:00"));
        assertTrue(planner.schedules(all,LocalDate.parse("2026-12-01"),json.createObjectNode().put("delaiMinimumHeures",24),normal).isEmpty());assertEquals("sunday",planner.preferredPlan(json.createArrayNode().add(normal).add(sunday),normal,all,LocalDate.parse("2027-01-10")).path("publicId").asText());
    }
    @Test void phoneLookupAndDefaultNameMatchBusinessPresentation(){
        var demand=json.createObjectNode().put("telFidele","+22890123456");assertTrue(ScreenFormats.matchesDemand(demand,"90 12 34 56"));assertTrue(ScreenFormats.matchesDemand(demand,"+228 90-12-34-56"));assertFalse(ScreenFormats.matchesDemand(demand,"99999999"));assertEquals("Un(e) chrétien(ne)",ScreenFormats.faithful(demand.putNull("nomFidele").putNull("prenomFidele")));
    }
}
