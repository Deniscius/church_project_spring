package com.eyram.dev.church_project_spring.security.jwt;

import com.eyram.dev.church_project_spring.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class AuthCookieServiceTest {
    private AuthCookieService service(boolean secure,String site){return new AuthCookieService(new JwtProperties("test-secret-for-cookie-encryption","api","web",3600000,"MS_AT",secure,site,"/",true));}
    @Test void crossSiteCookieAndItsDeletionAreSecureHttpOnlyAndPartitioned(){
        var service=service(true,"None");var response=new MockHttpServletResponse();service.writeAccessCookie(response,"signed-token");String cookie=response.getHeaders("Set-Cookie").get(1);assertThat(response.getHeaders("Set-Cookie").get(0)).contains("Max-Age=0").doesNotContain("Partitioned");assertThat(cookie).contains("Secure","HttpOnly","SameSite=None","Partitioned").doesNotContain("signed-token");
        var cleared=new MockHttpServletResponse();service.clearAccessCookie(cleared);assertThat(cleared.getHeader("Set-Cookie")).contains("Partitioned","Max-Age=0","SameSite=None");
    }
    @Test void sameSiteAndLocalDevelopmentKeepOrdinaryCookies(){
        for(var service:java.util.List.of(service(false,"Lax"),service(true,"Lax"))){var response=new MockHttpServletResponse();service.writeAccessCookie(response,"token");assertThat(response.getHeader("Set-Cookie")).doesNotContain("Partitioned");}
    }
}
