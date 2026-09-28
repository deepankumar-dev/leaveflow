package com.hackathon.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The production configuration (no "demo" profile): no sample data, first-run admin from the environment, forced
 * password change, people managed by HR, lockout, and demo endpoints absent. Tests share one database and run in order.
 */
@SpringBootTest(properties = {
        "leave.jwt.secret=real-mode-test-secret-0123456789-abcdefghij",
        "leave.bootstrap.admin-email=Admin@Company.test",
        "leave.bootstrap.admin-password=Initial-Pass-123",
        "leave.bcrypt-cost=4",
        "spring.datasource.url=jdbc:h2:mem:realtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RealModeTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    static final String NEW_PASS = "Better-Pass-456";
    static String adminToken;
    static long teamId;
    static long managerId;
    static long employeeId;

    private ResultActions call(String token, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b,
                               String body) throws Exception {
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(b);
    }

    private JsonNode login(String email, String password) throws Exception {
        String res = call(null, post("/api/auth/login"), "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    /** Signs in with a temporary password and sets a real one; returns the token to use afterwards. */
    private String activate(String email, String temp) throws Exception {
        String token = login(email, temp).get("token").asText();
        String res = call(token, post("/api/auth/change-password"),
                "{\"currentPassword\":\"" + temp + "\",\"newPassword\":\"" + NEW_PASS + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private static String person(String name, String email, String role, Long team, Long manager, String pw) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"role\":\"" + role + "\",\"teamId\":" + team
                + ",\"managerId\":" + manager + ",\"joinDate\":\"2024-01-15\",\"password\":\"" + pw + "\"}";
    }

    @Test @Order(1)
    @DisplayName("not a demo: login page config says so, no sample users, demo endpoints do not exist")
    void notDemo() throws Exception {
        call(null, get("/api/public/config"), null).andExpect(status().isOk()).andExpect(jsonPath("$.demoMode").value(false));
        call(null, post("/api/auth/login"), "{\"email\":\"asha@leave.demo\",\"password\":\"Password@123\"}")
                .andExpect(status().isUnauthorized());
        call(null, get("/h2-console/"), null).andExpect(status().is4xxClientError());
    }

    @Test @Order(2)
    @DisplayName("the first admin must change the password before anything else works")
    void forcedPasswordChange() throws Exception {
        JsonNode first = login("admin@company.test", "Initial-Pass-123");
        assertThat(first.get("user").get("mustChangePassword").asBoolean()).isTrue();
        String token = first.get("token").asText();

        call(token, get("/api/leaves/mine"), null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        call(token, get("/api/auth/me"), null).andExpect(status().isOk());

        call(token, post("/api/auth/change-password"), "{\"currentPassword\":\"nope\",\"newPassword\":\"" + NEW_PASS + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        call(token, post("/api/auth/change-password"), "{\"currentPassword\":\"Initial-Pass-123\",\"newPassword\":\"short1\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
        call(token, post("/api/auth/change-password"), "{\"currentPassword\":\"Initial-Pass-123\",\"newPassword\":\"onlyletterspassword\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));

        adminToken = activate("admin@company.test", "Initial-Pass-123");
        call(adminToken, get("/api/leaves/mine"), null).andExpect(status().isOk());
        call(null, post("/api/auth/login"), "{\"email\":\"admin@company.test\",\"password\":\"Initial-Pass-123\"}")
                .andExpect(status().isUnauthorized());          // the old password no longer works
    }

    @Test @Order(3)
    @DisplayName("HR creates a team, a manager and an employee; invalid setups are refused")
    void adminCreatesPeople() throws Exception {
        call(adminToken, post("/api/demo/reset-seed"), null).andExpect(status().isNotFound());   // demo endpoints do not exist
        String team = call(adminToken, post("/api/admin/teams"), "{\"name\":\"Platform\"}").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        teamId = json.readTree(team).get("id").asLong();
        call(adminToken, post("/api/admin/teams"), "{\"name\":\"platform\"}").andExpect(status().isConflict());

        String m = call(adminToken, post("/api/admin/users"), person("Mona Manager", "mona@company.test", "MANAGER", teamId, null, "Temp-Pass-111"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true)).andReturn().getResponse().getContentAsString();
        managerId = json.readTree(m).get("id").asLong();

        call(adminToken, post("/api/admin/users"), person("No Boss", "noboss@company.test", "EMPLOYEE", teamId, null, "Temp-Pass-111"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MANAGER_REQUIRED"));
        call(adminToken, post("/api/admin/users"), person("Mona Two", "MONA@company.test", "MANAGER", teamId, null, "Temp-Pass-111"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        call(adminToken, post("/api/admin/users"), person("Weak", "weak@company.test", "MANAGER", teamId, null, "weak"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));

        String e = call(adminToken, post("/api/admin/users"), person("Eli Employee", "eli@company.test", "EMPLOYEE", teamId, managerId, "Temp-Pass-222"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.managerName").value("Mona Manager")).andReturn().getResponse().getContentAsString();
        employeeId = json.readTree(e).get("id").asLong();
    }

    @Test @Order(4)
    @DisplayName("a real request end to end: employee applies, manager approves, HR approves, balance is used")
    void fullApproval() throws Exception {
        String eli = activate("eli@company.test", "Temp-Pass-222");
        String mona = activate("mona@company.test", "Temp-Pass-111");

        // Types are created by the bootstrap; a full-year joiner has the full annual quota for a future year.
        String applied = call(eli, post("/api/leaves"), "{\"leaveTypeCode\":\"ANNUAL\",\"fromDate\":\"2027-03-01\",\"toDate\":\"2027-03-03\",\"reason\":\"trip\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_MANAGER"))
                .andExpect(jsonPath("$.days").value(3.0)).andReturn().getResponse().getContentAsString();
        long id = json.readTree(applied).get("id").asLong();

        call(eli, post("/api/leaves/" + id + "/approve"), null).andExpect(status().isForbidden());   // not your own
        call(mona, post("/api/leaves/" + id + "/approve"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_HR"));
        call(adminToken, post("/api/leaves/" + id + "/approve"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        String bal = call(eli, get("/api/balances/me"), null).andReturn().getResponse().getContentAsString();
        assertThat(bal).contains("\"leaveTypeCode\":\"SICK\"");
        call(eli, get("/api/admin/users"), null).andExpect(status().isForbidden());
    }

    @Test @Order(5)
    @DisplayName("guards: cannot lock yourself out, cannot orphan a team, deactivated people cannot sign in")
    void guards() throws Exception {
        long adminId = json.readTree(call(adminToken, get("/api/auth/me"), null).andReturn().getResponse().getContentAsString()).get("id").asLong();
        String adminBody = person("Administrator", "admin@company.test", "HR", null, null, "x").replace("\"active\"", "");
        call(adminToken, put("/api/admin/users/" + adminId), adminBody.substring(0, adminBody.length() - 1) + ",\"active\":false}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_CHANGE"));

        String mona = person("Mona Manager", "mona@company.test", "MANAGER", teamId, null, "x");
        call(adminToken, put("/api/admin/users/" + managerId), mona.substring(0, mona.length() - 1) + ",\"active\":false}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HAS_REPORTS"));

        String eli = person("Eli Employee", "eli@company.test", "EMPLOYEE", teamId, managerId, "x");
        call(adminToken, put("/api/admin/users/" + employeeId), eli.substring(0, eli.length() - 1) + ",\"active\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        call(null, post("/api/auth/login"), "{\"email\":\"eli@company.test\",\"password\":\"" + NEW_PASS + "\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(6)
    @DisplayName("HR resets a password: the person must change it again at next sign-in")
    void resetPassword() throws Exception {
        call(adminToken, post("/api/admin/users/" + managerId + "/reset-password"), "{\"password\":\"Reset-Pass-789\"}").andExpect(status().isOk());
        JsonNode again = login("mona@company.test", "Reset-Pass-789");
        assertThat(again.get("user").get("mustChangePassword").asBoolean()).isTrue();
    }

    @Test @Order(7)
    @DisplayName("repeated failed sign-ins lock the account, even for the right password, and do not reveal if it exists")
    void lockout() throws Exception {
        for (int i = 0; i < 5; i++) {
            call(null, post("/api/auth/login"), "{\"email\":\"ghost@company.test\",\"password\":\"wrong-" + i + "\"}")
                    .andExpect(status().isUnauthorized());
        }
        call(null, post("/api/auth/login"), "{\"email\":\"ghost@company.test\",\"password\":\"wrong\"}")
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
        // a real account locks the same way (a successful sign-in first clears earlier failures)
        login("admin@company.test", NEW_PASS);
        for (int i = 0; i < 5; i++) {
            call(null, post("/api/auth/login"), "{\"email\":\"admin@company.test\",\"password\":\"wrong-" + i + "\"}")
                    .andExpect(status().isUnauthorized());
        }
        call(null, post("/api/auth/login"), "{\"email\":\"admin@company.test\",\"password\":\"" + NEW_PASS + "\"}")
                .andExpect(status().isTooManyRequests());
    }
}
