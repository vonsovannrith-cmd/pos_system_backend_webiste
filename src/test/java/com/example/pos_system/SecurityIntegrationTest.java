package com.example.pos_system;

import com.example.pos_system.dto.user.request.CreateUserRequest;
import com.example.pos_system.model.entity.enums.Role;
import com.example.pos_system.model.service.user.UserService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired
    UserService userService;

    private static final String ADMIN_LOGIN = "{\"email\":\"admin@pos.com\",\"password\":\"Admin@12345\"}";

    private MvcResult login(String json) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String categoryJson() {
        return "{\"name\":\"cat-" + UUID.randomUUID().toString().substring(0, 8) + "\",\"description\":null}";
    }

    /** Mimic browser behavior: Make any GET request to retrieve the XSRF-TOKEN cookie */
    private Cookie xsrfCookie() throws Exception {
        Cookie xsrf = mvc.perform(get("/api/health")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        return xsrf;
    }

    @Test
    void protectedEndpointRejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginSetsHardenedHttpOnlyCookie() throws Exception {
        String setCookie = login(ADMIN_LOGIN).getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(h -> h.startsWith("POS_AUTH=")).findFirst().orElseThrow();

        assertThat(setCookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api");
    }

    @Test
    void cookieAuthenticatedWriteRequiresCsrfToken() throws Exception {
        Cookie auth = login(ADMIN_LOGIN).getResponse().getCookie("POS_AUTH");
        assertThat(auth).isNotNull();

        // 1) Auth Cookie present, but no CSRF token → 403
        mvc.perform(post("/api/categories").cookie(auth)
                        .contentType(MediaType.APPLICATION_JSON).content(categoryJson()))
                .andExpect(status().isForbidden());


        // 2) Cookie + Header X-XSRF-TOKEN match → 201
        Cookie xsrf = xsrfCookie();
        mvc.perform(post("/api/categories").cookie(auth, xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(categoryJson()))
                .andExpect(status().isCreated());

        // 3) Incorrect header → 403
        mvc.perform(post("/api/categories").cookie(auth, xsrf).header("X-XSRF-TOKEN", "forged")
                        .contentType(MediaType.APPLICATION_JSON).content(categoryJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void bearerTokenWorksWithoutCsrf() throws Exception {
        String token = JsonPath.read(login(ADMIN_LOGIN).getResponse().getContentAsString(), "$.token");

        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(categoryJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidBearerHeaderIsNotRescuedByValidCookie() throws Exception {
        Cookie auth = login(ADMIN_LOGIN).getResponse().getCookie("POS_AUTH");

        mvc.perform(get("/api/auth/me").cookie(auth).header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExpiresTheCookie() throws Exception {
        Cookie auth = login(ADMIN_LOGIN).getResponse().getCookie("POS_AUTH");
        Cookie xsrf = xsrfCookie();

        MvcResult result = mvc.perform(post("/api/auth/logout").cookie(auth, xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(h -> assertThat(h).startsWith("POS_AUTH=").contains("Max-Age=0"));
    }

    @Test
    void cashierCannotUseAdminApi() throws Exception {
        String email = "c" + UUID.randomUUID().toString().substring(0, 8) + "@pos.com";
        userService.create(new CreateUserRequest("Test Cashier", email, "Pass1234", Role.CASHIER));

        String token = JsonPath.read(login(
                "{\"email\":\"" + email + "\",\"password\":\"Pass1234\"}")
                .getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}