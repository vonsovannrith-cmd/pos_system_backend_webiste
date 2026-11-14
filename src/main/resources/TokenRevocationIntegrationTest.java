//package com.example.pos_system;
//
//
//import com.example.pos_system.dto.user.request.CreateUserRequest;
//import com.example.pos_system.dto.user.request.UpdateUserRequest;
//import com.example.pos_system.dto.user.response.UserAdminResponse;
//import com.example.pos_system.model.entity.enums.Role;
//import com.example.pos_system.model.service.user.UserService;
//import com.example.pos_system.security.JwtService;
//import jakarta.servlet.http.Cookie;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//class TokenRevocationIntegrationTest extends IntegrationTestBase {
//
//    private static final String PASSWORD = "Pass1234";
//
//    @Autowired MockMvc mvc;
//    @Autowired
//    UserService userService;
//    @Autowired
//    JwtService jwtService;
//    @Autowired StringRedisTemplate redis;
//
//    // ---------- helpers ----------
//
//    private UserAdminResponse newUser() {
//        String email = "rv" + UUID.randomUUID().toString().substring(0, 8) + "@pos.com";
//        return userService.create(new CreateUserRequest("Rev Test", email, PASSWORD, Role.CASHIER));
//    }
//
//    private Cookie login(String email, String password) throws Exception {
//        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
//                .andExpect(status().isOk()).andReturn();
//        return r.getResponse().getCookie("POS_AUTH");
//    }
//
//    private Cookie xsrf() throws Exception {
//        return mvc.perform(get("/api/health")).andReturn().getResponse().getCookie("XSRF-TOKEN");
//    }
//
//    private void me(Cookie auth, int expectedStatus) throws Exception {
//        mvc.perform(get("/api/auth/me").cookie(auth)).andExpect(status().is(expectedStatus));
//    }
//
//    // ---------- tests ----------
//
//    @Test
//    void logoutRevokesThatTokenAndStoresItInRedis() throws Exception {
//        UserAdminResponse u = newUser();
//        Cookie auth = login(u.email(), PASSWORD);
//        me(auth, 200);
//
//        Cookie xsrf = xsrf();
//        mvc.perform(post("/api/auth/logout").cookie(auth, xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
//                .andExpect(status().isNoContent());
//
//        me(auth, 401);   // Cookie ចាស់ដដែល ឥឡូវអស់សុពលភាព
//
//        String jti = jwtService.parse(auth.getValue()).getId();
//        assertThat(redis.hasKey("revoked:" + jti)).isTrue();
//        assertThat(redis.getExpire("revoked:" + jti)).isPositive();   // មាន TTL: នឹងលុបខ្លួនឯង
//    }
//
//    @Test
//    void logoutOfOneDeviceKeepsTheOtherDeviceLoggedIn() throws Exception {
//        UserAdminResponse u = newUser();
//        Cookie register1 = login(u.email(), PASSWORD);
//        Cookie tablet = login(u.email(), PASSWORD);
//
//        Cookie xsrf = xsrf();
//        mvc.perform(post("/api/auth/logout").cookie(register1, xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
//                .andExpect(status().isNoContent());
//
//        me(register1, 401);
//        me(tablet, 200);
//    }
//
//    @Test
//    void changingPasswordKillsOtherSessionsButKeepsTheCurrentOne() throws Exception {
//        UserAdminResponse u = newUser();
//        Cookie current = login(u.email(), PASSWORD);
//        Cookie other = login(u.email(), PASSWORD);
//
//        Cookie xsrf = xsrf();
//        MvcResult r = mvc.perform(post("/api/auth/change-password").cookie(current, xsrf)
//                        .header("X-XSRF-TOKEN", xsrf.getValue())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewPass5678\"}"))
//                .andExpect(status().isNoContent()).andReturn();
//
//        Cookie fresh = r.getResponse().getCookie("POS_AUTH");
//        assertThat(fresh).isNotNull();
//        assertThat(fresh.getValue()).isNotEqualTo(current.getValue());
//
//        me(other, 401);      // Session ដទៃ ត្រូវដកចេញ
//        me(current, 401);    // Token ចាស់នៃ Session នេះ ក៏អស់សុពលភាព
//        me(fresh, 200);      // Cookie ថ្មី នៅ Login ដដែល
//    }
//
//    @Test
//    void deactivatedUserLosesAccessAndOldTokensStayDeadAfterReactivation() throws Exception {
//        UserAdminResponse u = newUser();
//        Cookie old = login(u.email(), PASSWORD);
//        me(old, 200);
//
//        userService.update(u.id(), new UpdateUserRequest("Rev Test", Role.CASHIER, false), "admin@pos.com");
//        me(old, 401);
//
//        userService.update(u.id(), new UpdateUserRequest("Rev Test", Role.CASHIER, true), "admin@pos.com");
//        me(old, 401);                                   // មិន "រស់ឡើងវិញ" ✅
//        me(login(u.email(), PASSWORD), 200);            // Login ថ្មីដំណើរការ
//    }
//
//    @Test
//    void loginRateLimitIsStoredInRedis() throws Exception {
//        UserAdminResponse u = newUser();
//
//        for (int i = 0; i < 5; i++) {
//            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
//                            .content("{\"email\":\"" + u.email() + "\",\"password\":\"Wrong1234\"}"))
//                    .andExpect(status().isUnauthorized());
//        }
//
//        // ដងទី ៦ ទោះ Password ត្រូវ ក៏ត្រូវបានបិទ
//        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"email\":\"" + u.email() + "\",\"password\":\"" + PASSWORD + "\"}"))
//                .andExpect(status().isTooManyRequests());
//
//        assertThat(redis.keys("login:fail:*")).isNotEmpty();
//    }
//}
