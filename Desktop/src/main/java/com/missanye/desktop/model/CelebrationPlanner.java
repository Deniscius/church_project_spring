package com.missanye.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

/** Local mirror of the public calendar and DemandeSchedulingPolicy; central availability remains authoritative. */
public final class CelebrationPlanner {
    private static final List<String> DAYS=List.of("LUNDI","MARDI","MERCREDI","JEUDI","VENDREDI","SAMEDI","DIMANCHE");
    private final Clock clock;
    public CelebrationPlanner(){this(Clock.system(ZoneId.of("Africa/Lome")));}
    public CelebrationPlanner(Clock clock){this.clock=clock;}
    public static boolean customAllowed(JsonNode type,JsonNode plan){return ("SPECIALE".equals(plan.path("natureForfait").asText(""))||"SPECIALE".equals(type.path("typeDemandeEnum").asText("")))&&plan.path("heurePersonnalise").asBoolean();}
    private Set<String> days(JsonNode node){var set=new LinkedHashSet<String>();for(var day:node.path("joursCelebrationAutorises"))if(DAYS.contains(day.asText("")))set.add(day.asText(""));return set;}
    public Set<String> allowedDays(JsonNode type,JsonNode plan){var t=days(type);var p=days(plan);if(p.isEmpty())return t.isEmpty()?new LinkedHashSet<>(DAYS):t;if(!t.isEmpty()){p.retainAll(t);if(p.isEmpty())throw new IllegalArgumentException("Le forfait et le type de demande n'ont aucun jour autorisé en commun.");}return p;}
    private String day(LocalDate date){return DAYS.get(date.getDayOfWeek().getValue()-1);}
    private boolean active(JsonNode schedule){return schedule.path("isActive").asBoolean(true)&&!schedule.path("statusDel").asBoolean();}
    private List<JsonNode> programme(JsonNode schedules,LocalDate date){
        var weekly=new ArrayList<JsonNode>();var dated=new ArrayList<JsonNode>();var unique=new ArrayList<JsonNode>();
        for(var s:schedules)if(active(s)){
            String specific=s.path("dateSpecifique").asText("");if(specific.equals(date.toString())){dated.add(s);if(s.path("uniqueSurParoisse").asBoolean())unique.add(s);}
            else if(specific.isBlank()&&(s.path("jourSemaine").asText("").isBlank()||day(date).equals(s.path("jourSemaine").asText(""))))weekly.add(s);
        }
        if(!unique.isEmpty())return unique;weekly.addAll(dated);return weekly;
    }
    public List<JsonNode> schedules(JsonNode all,LocalDate date,JsonNode type,JsonNode plan){
        if(date==null)return List.of();var choices=programme(all,date);var allowed=allowedDays(type,plan);
        choices.removeIf(s->(plan.path("nombreCelebration").asInt(1)!=30&&!s.path("uniqueSurParoisse").asBoolean()&&!allowed.contains(day(date)))||(!s.path("natureHonoraire").asText("").isBlank()&&!s.path("natureHonoraire").asText("").equals(plan.path("natureForfait").asText("")))||!future(date,s.path("heureCelebration").asText(""),type));
        choices.sort(Comparator.comparing(s->s.path("heureCelebration").asText("")));return choices;
    }
    public LocalDate earliestDate(JsonNode type){return LocalDateTime.now(clock).plusHours(type.path("delaiMinimumHeures").asInt(24)).toLocalDate();}
    public boolean allowedStart(JsonNode type,JsonNode plan,JsonNode all,LocalDate date){return !date.isBefore(earliestDate(type))&&(plan.path("nombreCelebration").asInt(1)==30||allowedDays(type,plan).contains(day(date))||(plan.path("nombreCelebration").asInt(1)==1&&programme(all,date).stream().anyMatch(s->s.path("uniqueSurParoisse").asBoolean())));}
    private boolean future(LocalDate date,String time,JsonNode type){try{return !LocalDateTime.of(date,LocalTime.parse(time)).isBefore(LocalDateTime.now(clock).plusHours(type.path("delaiMinimumHeures").asInt(24)));}catch(Exception unknown){return false;}}
    public List<LocalDate> dates(LocalDate start,JsonNode type,JsonNode plan){
        if(start==null)throw new IllegalArgumentException("Choisissez la première date de célébration.");int count=plan.path("nombreCelebration").asInt(1);if(count<1||count>30)throw new IllegalArgumentException("Le forfait doit prévoir entre 1 et 30 célébrations.");
        var allowed=allowedDays(type,plan);if(count!=30&&!allowed.contains(day(start)))throw new IllegalArgumentException("Le jour choisi n'est pas autorisé par ce forfait : "+String.join(", ",allowed)+".");
        var result=new ArrayList<LocalDate>();for(LocalDate d=start;result.size()<count&&!d.isAfter(start.plusYears(2));d=d.plusDays(1))if(count==30||allowed.contains(day(d)))result.add(d);
        int window=count==30?30:plan.path("nombreJour").asInt(count);if(result.size()!=count||(window>0&&result.get(count-1).isAfter(start.plusDays(window-1))))throw new IllegalArgumentException("Les célébrations dépassent la période de "+window+" jour(s) du forfait.");return result;
    }
    public JsonNode preferredPlan(JsonNode plans,JsonNode current,JsonNode all,LocalDate date){
        if(current.isMissingNode()||current.path("natureForfait").asText("").isBlank()||date==null||current.path("nombreCelebration").asInt(1)>1)return current;
        String nature=date.getDayOfWeek()==DayOfWeek.SUNDAY?"DOMINICALE":"NORMALE";for(var s:programme(all,date))if(!s.path("natureHonoraire").asText("").isBlank()){nature=s.path("natureHonoraire").asText("");break;}
        if("SPECIALE".equals(current.path("natureForfait").asText(""))&&programme(all,date).stream().noneMatch(s->!s.path("natureHonoraire").asText("").isBlank()))return current;
        if(nature.equals(current.path("natureForfait").asText("")))return current;
        for(var p:plans)if(active(p)&&nature.equals(p.path("natureForfait").asText(""))&&p.path("nombreCelebration").asInt(1)==1)return p;return current;
    }
    public void validate(JsonNode request,JsonNode type,JsonNode plan,JsonNode all){
        boolean custom=customAllowed(type,plan);var dates=new TreeSet<LocalDate>();for(var d:request.path("datesCelebration"))dates.add(LocalDate.parse(d.asText("")));if(dates.isEmpty())dates.add(LocalDate.parse(request.path("dateDebut").asText("")));
        var allowed=allowedDays(type,plan);int count=plan.path("nombreCelebration").asInt(1);if(dates.size()!=count)throw new IllegalArgumentException("Le forfait exige "+count+" date(s).");
        int window=count==30?30:plan.path("nombreJour").asInt(count);if(window>0&&dates.last().isAfter(dates.first().plusDays(window-1)))throw new IllegalArgumentException("Les dates dépassent la durée du forfait.");
        if(count==30&&!new ArrayList<>(dates).equals(dates(dates.first(),type,plan)))throw new IllegalArgumentException("Une trentaine doit comporter 30 jours consécutifs.");
        for(var date:dates){
            JsonNode slot=request;for(var candidate:request.path("celebrationSlots"))if(date.toString().equals(candidate.path("date").asText(""))){slot=candidate;break;}
            String time=slot.path("heurePersonnalisee").asText("");var choices=schedules(all,date,type,plan);
            if(!time.isBlank()){
                if(!slot.path("horairePublicId").asText("").isBlank())throw new IllegalArgumentException("Choisissez soit un horaire paroissial, soit une heure personnalisée.");
                if(!custom)throw new IllegalArgumentException("L'heure personnalisée est réservée aux demandes spéciales dont le forfait l'autorise.");
                if(programme(all,date).stream().anyMatch(s->s.path("uniqueSurParoisse").asBoolean()))throw new IllegalArgumentException("Une messe unique impose l'horaire paroissial pour le "+ScreenFormats.date(date.toString())+".");
                if(count!=30&&!allowed.contains(day(date)))throw new IllegalArgumentException("Le jour de célébration n'est pas autorisé.");
                if(!future(date,time,type))throw new IllegalArgumentException("Respectez le délai minimum de "+type.path("delaiMinimumHeures").asInt(24)+" heure(s) avant la célébration.");
            }else{String id=slot.path("horairePublicId").asText("");if(choices.stream().noneMatch(s->id.equals(s.path("publicId").asText(""))))throw new IllegalArgumentException("Aucun horaire compatible sélectionné pour le "+ScreenFormats.date(date.toString())+". Vérifiez le jour, le forfait et le délai minimum.");}
        }
    }
}
