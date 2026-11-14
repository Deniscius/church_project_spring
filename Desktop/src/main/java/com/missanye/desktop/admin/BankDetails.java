package com.missanye.desktop.admin;

import java.math.BigInteger;
import java.util.*;

/** Same IBAN/BBAN and bank consistency rules as the web treasury form. */
public final class BankDetails {
    private BankDetails(){}
    public static final Map<String,String> CODES=Map.of("Ecobank Togo","00906","Orabank Togo","00904","Banque Atlantique Togo","00801","Union Togolaise de Banque (UTB)","00301");
    public static String validate(String raw,String bank){
        String value=raw.toUpperCase(Locale.ROOT).replaceAll("[\\s\\p{Z}._-]","");String code=null;
        if(value.matches("TG\\d{2}TG\\d{22}")){
            String moved=value.substring(4)+value.substring(0,4);var numeric=new StringBuilder();for(char c:moved.toCharArray())numeric.append(Character.isDigit(c)?String.valueOf(c):String.valueOf(c-'A'+10));
            if(!new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).equals(BigInteger.ONE))throw new IllegalArgumentException("La clé de contrôle de l’IBAN est incorrecte.");code=value.substring(6,11);
        }else if(value.matches("TG\\d{22}")){code=value.substring(2,7);}
        else if(value.matches("[A-Z0-9]{23}")){
            var converted=new StringBuilder();for(char c:value.substring(0,21).toCharArray()){int digit=c<='9'?c-'0':c<='I'?c-'A'+1:c<='R'?c-'J'+1:c-'S'+2;converted.append(digit);}
            String s=converted.toString();int expected=97-(89*new BigInteger(s.substring(0,5)).mod(BigInteger.valueOf(97)).intValue()+15*new BigInteger(s.substring(5,10)).mod(BigInteger.valueOf(97)).intValue()+3*new BigInteger(s.substring(10)).mod(BigInteger.valueOf(97)).intValue())%97;
            if(Integer.parseInt(value.substring(21))!=expected)throw new IllegalArgumentException("La clé du RIB est incorrecte.");
        }else throw new IllegalArgumentException("Utilisez un IBAN Togo (28 caractères) ou un RIB domestique Togo (24 caractères).");
        String expected=CODES.entrySet().stream().filter(e->e.getKey().equalsIgnoreCase(bank.strip())).map(Map.Entry::getValue).findFirst().orElse(null);
        if(expected!=null&&(code==null||!expected.equals(code)))throw new IllegalArgumentException("Le RIB ne correspond pas à la banque sélectionnée.");return value;
    }
}
