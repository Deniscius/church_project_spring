package com.missanye.desktop.api;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

public final class ApiClient {
    private final URI base;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;
    public ApiClient(String address) {
        base = URI.create(address.endsWith("/") ? address : address + "/");
        String host = base.getHost();
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
        if (host == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                || !("https".equals(base.getScheme()) || (local && "http".equals(base.getScheme()))))
            throw new IllegalArgumentException("Utilisez HTTPS (HTTP autorisé uniquement en local).");
        http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private JsonNode request(String path, String body) throws Exception {return request(path,body,Duration.ofSeconds(30));}
    private JsonNode request(String path,String body,Duration timeout)throws Exception {
        var b = HttpRequest.newBuilder(base.resolve(path)).timeout(timeout)
                .header("Accept", "application/json").header("X-Requested-With", "XMLHttpRequest");
        if (body == null) b.GET(); else b.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        var response = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
            String message="";
            try { message=json.readTree(response.body()).path("message").asText(""); } catch(Exception ignored) {}
            throw new ApiException(response.statusCode(), message);
        }
        return json.readTree(response.body());
    }
    public JsonNode login(String username, String password) throws Exception {
        clear();
        try {
            request("auth/login-multi-tenant", json.writeValueAsString(Map.of("username", username, "password", password)),Duration.ofMinutes(2));
            try{return request("auth/me", null);}catch(ApiException rejected){
                if(rejected.status()==401||rejected.status()==403)throw new IllegalArgumentException("Vos identifiants ont été acceptés, mais le serveur n'a pas confirmé la session du desktop. Vérifiez la configuration des cookies du backend et l'adresse du serveur.");
                throw rejected;
            }
        } catch (ApiException refused) {
            clear();if(refused.status()==401)throw new ApiException("Identifiant ou mot de passe refusé par ce serveur. Utilisez les mêmes identifiants que sur le web et vérifiez le serveur sélectionné.",401);throw refused;
        } catch (Exception e) { clear(); throw e; }
    }
    public void logout() throws Exception {
        try { request("auth/logout", "{}"); } finally { clear(); }
    }
    public String baseUrl() { return base.toString(); }
    public JsonNode get(String path) throws Exception { return request(path,null); }
    public JsonNode post(String path, JsonNode body) throws Exception { return request(path,json.writeValueAsString(body)); }
    public byte[] pdf(String code) throws Exception {
        var request=HttpRequest.newBuilder(base.resolve("demandes/code/"+URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8)+"/recu.pdf"))
                .timeout(Duration.ofSeconds(30)).header("Accept","application/pdf").GET().build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofByteArray());
        if(response.statusCode()!=200) throw new ApiException(response.statusCode());
        byte[] bytes=response.body();
        if(bytes.length<5 || !new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) throw new java.io.IOException("Réponse PDF invalide");
        return bytes;
    }
    public void clear() { cookies.getCookieStore().removeAll(); }
    public static final class ApiException extends Exception {
        private final int status;
        public ApiException(int status) { this(status, ""); }
        public ApiException(int status, String detail) { super((status == 401 ? "Connexion refusée ou session expirée." : status == 403 ? "Vous ne disposez pas des droits nécessaires." : status == 429 ? "Trop de tentatives. Veuillez patienter avant de réessayer." : status >= 500 ? "Le service est momentanément indisponible. Votre travail est conservé." : "Cette opération n'a pas pu être confirmée.") + (detail.isBlank() || status >= 500 || status == 401 || status == 403 ? "" : " " + detail)); this.status = status; }
        private ApiException(String message,int status){super(message);this.status=status;}
        public int status() { return status; }
    }
}
