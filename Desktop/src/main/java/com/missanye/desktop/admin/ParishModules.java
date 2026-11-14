package com.missanye.desktop.admin;

import java.util.*;

/** Native forms mirror the web forms and backend request DTOs, without exposing API field names. */
public final class ParishModules {
    private ParishModules(){}
    public enum Kind { TEXT, MULTILINE, INTEGER, MONEY, BOOLEAN, DATE, TIME, CHOICE, DAYS, TYPE, PASSWORD, BANK }
    public record Field(String key,String label,Kind kind,boolean required,List<String> options){
        public Field(String key,String label,Kind kind,boolean required,String... options){this(key,label,kind,required,List.of(options));}
    }
    public record Module(String id,String title,String read,String write,String createPath,String updatePath,boolean singleton,boolean removable,List<Field> fields){@Override public String toString(){return title;}}
    public static final List<String> DAYS=List.of("LUNDI","MARDI","MERCREDI","JEUDI","VENDREDI","SAMEDI","DIMANCHE");
    private static Field f(String key,String label,Kind kind,boolean required,String... options){return new Field(key,label,kind,required,options);}
    public static List<Module> all(){return List.of(
        new Module("schedules","Horaires des célébrations","schedule:read","schedule:manage","horaires","horaires",false,true,List.of(
            f("jourSemaine","Jour de la semaine",Kind.CHOICE,false,DAYS.toArray(String[]::new)),f("heureCelebration","Heure de célébration",Kind.TIME,true),f("libelle","Libellé",Kind.TEXT,false),f("dateSpecifique","Date ponctuelle (facultative)",Kind.DATE,false),f("uniqueSurParoisse","Messe unique pour cette date",Kind.BOOLEAN,false),f("natureHonoraire","Honoraire pour une date ponctuelle",Kind.CHOICE,false,"NORMALE","DOMINICALE","SPECIALE"),f("isActive","Actif",Kind.BOOLEAN,true))),
        new Module("types","Types de demandes","request-type:read","request-type:manage","type-demandes","type-demandes",false,true,List.of(
            f("libelle","Libellé",Kind.TEXT,true),f("description","Description",Kind.MULTILINE,false),f("typeDemandeEnum","Catégorie",Kind.CHOICE,true,"EUCHARISTIE","SACRAMENT","SACRAMENTAUX"),f("delaiMinimumHeures","Délai minimum en heures",Kind.INTEGER,true),f("joursCelebrationAutorises","Jours de célébration autorisés",Kind.DAYS,true),f("isActive","Actif",Kind.BOOLEAN,true))),
        new Module("prices","Forfaits et honoraires","pricing:read","pricing:manage","forfait-tarifs","forfait-tarifs",false,true,List.of(
            f("typeDemandePublicId","Type de demande",Kind.TYPE,true),f("codeForfait","Code du forfait",Kind.TEXT,false),f("nomForfait","Nom du forfait",Kind.TEXT,true),f("natureForfait","Nature de l’honoraire",Kind.CHOICE,true,"NORMALE","DOMINICALE","SPECIALE"),f("montantForfait","Montant (XOF)",Kind.MONEY,true),f("nombreJour","Durée en jours",Kind.INTEGER,false),f("nombreCelebration","Nombre de célébrations",Kind.INTEGER,true),f("joursCelebrationAutorises","Jours autorisés par le type",Kind.DAYS,true),f("heurePersonnalise","Autoriser une heure personnalisée",Kind.BOOLEAN,true),f("libelle","Description",Kind.MULTILINE,false),f("isActive","Actif",Kind.BOOLEAN,true))),
        new Module("choirs","Chorales","choir:read","choir:manage","paroisses/{parish}/chorales","chorales",false,true,List.of(f("nom","Nom de la chorale",Kind.TEXT,true),f("description","Description",Kind.MULTILINE,false))),
        new Module("team","Équipe paroissiale","user:manage","user:manage","admin/users","admin/users",false,true,List.of(
            f("nom","Nom",Kind.TEXT,true),f("prenom","Prénoms",Kind.TEXT,true),f("username","Identifiant",Kind.TEXT,true),f("email","E-mail",Kind.TEXT,false),f("telephone","Téléphone",Kind.TEXT,false),f("password","Mot de passe initial (création)",Kind.PASSWORD,false),f("role","Rôle applicatif",Kind.CHOICE,true,"SECRETAIRE","CURE","COMPTABLE_LOCAL","ADMIN"),f("roleParoisse","Fonction dans la paroisse",Kind.CHOICE,true,"ADMIN","GESTIONNAIRE","SECRETAIRE","CONSULTATION"),f("isActive","Compte actif",Kind.BOOLEAN,true))),
        new Module("parish","Coordonnées, banque et reçu","parish:read","parish-settings:manage",null,"paroisses/{parish}/coordonnees",true,false,List.of(
            f("email","E-mail de la paroisse",Kind.TEXT,false),f("telephone","Téléphone",Kind.TEXT,false),f("nomBanque","Banque",Kind.BANK,false,"Ecobank Togo","Orabank Togo","Banque Atlantique Togo","Union Togolaise de Banque (UTB)","Bank of Africa — Togo (BOA)","Banque Internationale pour l’Afrique au Togo (BIA)","BSIC — Togo","Coris Bank International — Togo","International Business Bank Togo (IB Bank)","Société Interafricaine de Banque (SIAB)","SUNU Bank Togo","NSIA Banque (succursale du Togo)","Société Générale (succursale du Togo)","Banque de Développement du Mali (succ. Togo)"),f("titulaireCompte","Titulaire du compte",Kind.TEXT,false),f("ibanOrRib","RIB / numéro de compte",Kind.TEXT,false))),
        new Module("profile","Mon profil","profile:read","profile:read",null,"admin/profile",true,false,List.of(f("nom","Nom",Kind.TEXT,true),f("prenom","Prénoms",Kind.TEXT,true),f("email","E-mail",Kind.TEXT,false),f("telephone","Téléphone",Kind.TEXT,false))),
        new Module("password","Mon mot de passe","profile:read","profile:read",null,"admin/profile/password",true,false,List.of(f("currentPassword","Mot de passe actuel",Kind.PASSWORD,true),f("newPassword","Nouveau mot de passe",Kind.PASSWORD,true),f("confirmPassword","Confirmer le nouveau mot de passe",Kind.PASSWORD,true))),
        new Module("celebrations","Célébrations du jour","dashboard:view","celebration-schedule:manage",null,null,false,false,List.of()),
        new Module("programme","Programme du jour","schedule:read","schedule:manage",null,null,false,false,List.of()),
        new Module("archives","Demandes archivées","demand:audit",null,null,null,false,false,List.of()),
        new Module("invoices","Factures","invoice:read",null,null,null,false,false,List.of()),
        new Module("payments","Paiements","payment:read",null,null,null,false,false,List.of()),
        new Module("cash","Journal de caisse","treasury:read",null,null,null,false,false,List.of()),
        new Module("account","Compte de trésorerie","treasury:read",null,null,null,true,false,List.of()),
        new Module("transfers","Reversements","treasury:read","treasury:manage","comptabilite/reversements",null,false,false,List.of(f("montant","Montant demandé (XOF)",Kind.INTEGER,true),f("motif","Motif",Kind.MULTILINE,false)))
    );}
    public static String label(String value){return switch(value){case "EUCHARISTIE"->"Eucharistie";case "SACRAMENT"->"Sacrement";case "SACRAMENTAUX"->"Sacramentaux";case "NORMALE"->"Ordinaire";case "DOMINICALE"->"Dominicale";case "SPECIALE"->"Spéciale";case "SECRETAIRE"->"Secrétaire";case "CURE"->"Curé";case "COMPTABLE_LOCAL"->"Comptable local";case "ADMIN"->"Administrateur";case "GESTIONNAIRE"->"Gestionnaire";case "CONSULTATION"->"Consultation";default->value.isEmpty()?"—":value.substring(0,1)+value.substring(1).toLowerCase(Locale.FRANCE);};}
}
