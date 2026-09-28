package com.hackathon.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.leave.service.EscalationScheduler;
import com.hackathon.leave.service.SeedService;
import com.hackathon.leave.support.MutableClock;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end tests over HTTP against the fixed seed data (docs/SEED_SCENARIOS.md). The clock is a MutableClock that
 * starts at the seed's reference "now" (2026-10-12 10:00 IST); the seed is reloaded before every test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiFlowTest.TestClockConfig.class)
class ApiFlowTest {

    static final String SEED_NOW = "2026-10-12T10:00:00+05:30";

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(OffsetDateTime.parse(SEED_NOW).toInstant(), com.hackathon.leave.config.ClockConfig.ZONE);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired SeedService seed;
    @Autowired EscalationScheduler escalationJob;
    @Autowired Clock clock;

    @BeforeEach
    void reloadSeed() {
        ((MutableClock) clock).set(OffsetDateTime.parse(SEED_NOW).toInstant());
        seed.reset();
    }

    // ------------------------------------------------------------------ helpers

    private String login(String who) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + who + "@leave.demo\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private ResultActions getAs(String token, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", token));
    }

    private ResultActions postAs(String token, String url, String body) throws Exception {
        return mvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "" : body));
    }

    private static String apply(String type, String from, String to) {
        return "{\"leaveTypeCode\":\"" + type + "\",\"fromDate\":\"" + from + "\",\"toDate\":\"" + to
                + "\",\"reason\":\"test\"}";
    }

    private static String preview(String type, String from, String to) {
        return "{\"leaveTypeCode\":\"" + type + "\",\"fromDate\":\"" + from + "\",\"toDate\":\"" + to + "\"}";
    }

    /** Id of the request a person made starting on the given date (the seed's R-numbers are not stable ids). */
    private long requestId(String who, String fromDate) {
        return jdbc.queryForObject("select r.id from leave_requests r join users u on u.id = r.employee_id "
                + "where u.email = ? and r.from_date = ?", Long.class, who + "@leave.demo", java.sql.Date.valueOf(fromDate));
    }

    private String statusOf(long id) {
        return jdbc.queryForObject("select status from leave_requests where id = ?", String.class, id);
    }

    private JsonNode balance(String token, String typeCode) throws Exception {
        String body = getAs(token, "/api/balances/me").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        for (JsonNode n : json.readTree(body)) {
            if (typeCode.equals(n.get("leaveTypeCode").asText())) {
                return n;
            }
        }
        throw new AssertionError("no balance for " + typeCode);
    }

    private static double num(JsonNode n, String field) {
        return n.get(field).asDouble();
    }

    // ------------------------------------------------------------------ auth & access

    @Test
    @DisplayName("login returns a token and the user; a wrong password is a uniform 401")
    void login() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@leave.demo\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Asha Rao")).andExpect(jsonPath("$.user.role").value("EMPLOYEE"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"asha@leave.demo\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@leave.demo\",\"password\":\"Password@123\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("no token is 401; roles are enforced on the backend (employee cannot reach HR or manager endpoints)")
    void roleEnforcement() throws Exception {
        mvc.perform(get("/api/leaves/mine")).andExpect(status().isUnauthorized());
        String asha = login("asha");
        getAs(asha, "/api/analytics/summary").andExpect(status().isForbidden());
        getAs(asha, "/api/hr/requests").andExpect(status().isForbidden());
        getAs(asha, "/api/manager/requests").andExpect(status().isForbidden());
        postAs(asha, "/api/demo/reset-seed", null).andExpect(status().isForbidden());
        String priya = login("priya");
        getAs(priya, "/api/analytics/summary").andExpect(status().isForbidden());
        getAs(login("meena"), "/api/analytics/summary").andExpect(status().isOk());
    }

    @Test
    @DisplayName("an employee cannot read someone else's request; a manager only sees their own scope")
    void rowLevelAccess() throws Exception {
        long ashaRequest = requestId("asha", "2026-10-26");
        getAs(login("kiran"), "/api/leaves/" + ashaRequest).andExpect(status().isForbidden());
        getAs(login("asha"), "/api/leaves/" + ashaRequest).andExpect(status().isOk());
        getAs(login("meena"), "/api/leaves/" + ashaRequest).andExpect(status().isOk());
        getAs(login("arjun"), "/api/leaves/" + ashaRequest).andExpect(status().isForbidden()); // Product manager, Engineering request
        getAs(login("priya"), "/api/leaves/" + ashaRequest).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ seed sanity

    @Test
    @DisplayName("seed: 18 requests, 15 users, Ravi's pro-rata is 24 x 6/12 = 12.0, Karan has 0 annual left")
    void seedBalances() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from leave_requests", Integer.class)).isEqualTo(18);
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(15);

        JsonNode ravi = balance(login("ravi"), "ANNUAL");
        assertThat(num(ravi, "entitled")).isEqualTo(12.0);
        assertThat(num(ravi, "pending")).isEqualTo(5.0);
        assertThat(num(ravi, "available")).isEqualTo(7.0);
        assertThat(ravi.get("explanation").asText()).contains("24").contains("6/12").contains("12.0");

        JsonNode karan = balance(login("karan"), "ANNUAL");
        assertThat(num(karan, "used")).isEqualTo(20.0);
        assertThat(num(karan, "pending")).isEqualTo(4.0);
        assertThat(num(karan, "available")).isEqualTo(0.0);

        JsonNode asha = balance(login("asha"), "ANNUAL");
        assertThat(num(asha, "used")).isEqualTo(8.0);
        assertThat(num(asha, "pending")).isEqualTo(2.0);
        assertThat(num(asha, "available")).isEqualTo(14.0);

        JsonNode tarun = balance(login("tarun"), "ANNUAL"); // R12 cancelled: balance released
        assertThat(num(tarun, "used")).isEqualTo(8.0);
        assertThat(num(tarun, "pending")).isEqualTo(0.0);
    }

    // ------------------------------------------------------------------ validation rules

    @Test
    @DisplayName("preview flags a team conflict but still says the request is valid (flag, never reject)")
    void conflictFlagIsNeverABlock() throws Exception {
        postAs(login("tarun"), "/api/leaves/preview", preview("ANNUAL", "2026-11-04", "2026-11-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictFlagged").value(true))
                .andExpect(jsonPath("$.conflictReason").value(org.hamcrest.Matchers.containsString("Kiran Patel")))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.workingDays").value(2.0));
    }

    @Test
    @DisplayName("one overlapping teammate is 2/7 = 28.6%, which is under 30%, so no flag")
    void oneOverlapIsNotFlagged() throws Exception {
        // Only Divya (R03, until 6 Nov) is away on Fri 6 Nov.
        postAs(login("tarun"), "/api/leaves/preview", preview("ANNUAL", "2026-11-06", "2026-11-06"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conflictFlagged").value(false));
    }

    @Test
    @DisplayName("Karan (0 annual left) is refused with 422 INSUFFICIENT_BALANCE")
    void insufficientBalance() throws Exception {
        postAs(login("karan"), "/api/leaves", apply("ANNUAL", "2026-12-21", "2026-12-22"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    @DisplayName("a holiday-only range is rejected as having no working days")
    void noWorkingDays() throws Exception {
        postAs(login("asha"), "/api/leaves", apply("CASUAL", "2026-11-09", "2026-11-09"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NO_WORKING_DAYS"));
        postAs(login("asha"), "/api/leaves", apply("CASUAL", "2026-11-14", "2026-11-15"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NO_WORKING_DAYS"));
    }

    @Test
    @DisplayName("overlapping your own leave is 409; a reversed range is 400")
    void overlapAndRange() throws Exception {
        postAs(login("ravi"), "/api/leaves", apply("ANNUAL", "2026-11-16", "2026-11-17"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OVERLAPS_OWN_LEAVE"));
        postAs(login("ravi"), "/api/leaves", apply("ANNUAL", "2026-12-04", "2026-12-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RANGE"));
    }

    @Test
    @DisplayName("UNPAID leave never touches a balance and has no balance limit")
    void unpaid() throws Exception {
        String neha = login("neha");
        double before = num(balance(neha, "ANNUAL"), "available");
        postAs(neha, "/api/leaves", apply("UNPAID", "2027-01-05", "2027-01-30")).andExpect(status().isOk());
        assertThat(num(balance(neha, "ANNUAL"), "available")).isEqualTo(before);
    }

    // ------------------------------------------------------------------ the approval chain

    @Test
    @DisplayName("nobody decides their own request, HR included; managers' own leave goes straight to HR")
    void selfApprovalBlocked() throws Exception {
        long r14 = requestId("priya", "2026-11-30"); // Priya's own leave, waiting for HR
        postAs(login("priya"), "/api/leaves/" + r14 + "/approve", null).andExpect(status().isForbidden());

        String meena = login("meena");
        postAs(meena, "/api/leaves", apply("ANNUAL", "2026-12-14", "2026-12-15"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_HR"));
        long own = requestId("meena", "2026-12-14");
        postAs(meena, "/api/leaves/" + own + "/approve", null).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("happy path Vikas: delegate Priya -> HR Meena, with balances at each step, then cancel")
    void happyPathWithDelegationAndBalances() throws Exception {
        String vikas = login("vikas");
        assertThat(num(balance(vikas, "ANNUAL"), "available")).isEqualTo(15.0);

        postAs(vikas, "/api/leaves", apply("ANNUAL", "2026-12-14", "2026-12-16"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_MANAGER"))
                .andExpect(jsonPath("$.days").value(3.0))
                .andExpect(jsonPath("$.steps[0].assigneeName").value("Priya Sharma"))          // Arjun delegated to Priya
                .andExpect(jsonPath("$.steps[0].delegatedFromName").value("Arjun Mehta"));
        long id = requestId("vikas", "2026-12-14");
        JsonNode afterApply = balance(vikas, "ANNUAL");
        assertThat(num(afterApply, "pending")).isEqualTo(3.0);
        assertThat(num(afterApply, "available")).isEqualTo(12.0);

        // Only the assignee decides: the original manager (delegated away) and HR cannot yet.
        postAs(login("arjun"), "/api/leaves/" + id + "/approve", null).andExpect(status().isForbidden());
        postAs(login("meena"), "/api/leaves/" + id + "/approve", null).andExpect(status().isForbidden());

        postAs(login("priya"), "/api/leaves/" + id + "/approve", "{\"comment\":\"ok\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_HR"));
        assertThat(num(balance(vikas, "ANNUAL"), "pending")).isEqualTo(3.0);   // still reserved, not used yet

        postAs(login("meena"), "/api/leaves/" + id + "/approve", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        JsonNode approved = balance(vikas, "ANNUAL");
        assertThat(num(approved, "pending")).isEqualTo(0.0);
        assertThat(num(approved, "used")).isEqualTo(12.0);                       // 9 baseline + 3

        postAs(vikas, "/api/leaves/" + id + "/cancel", "{\"comment\":\"plans changed\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        JsonNode cancelled = balance(vikas, "ANNUAL");
        assertThat(num(cancelled, "used")).isEqualTo(9.0);
        assertThat(num(cancelled, "available")).isEqualTo(15.0);

        // Timeline records every step, oldest first, with the delegation.
        String timeline = getAs(vikas, "/api/leaves/" + id + "/timeline").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        List<String> actions = json.readTree(timeline).findValuesAsText("action");
        assertThat(actions).containsExactly("SUBMITTED", "DELEGATED", "MANAGER_APPROVED", "HR_APPROVED", "CANCELLED");
    }

    @Test
    @DisplayName("a rejection needs a comment, and releases the reserved balance")
    void rejectNeedsCommentAndReleases() throws Exception {
        long r04 = requestId("sam", "2026-11-04");
        String priya = login("priya");
        postAs(priya, "/api/leaves/" + r04 + "/reject", "{\"comment\":\"  \"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMENT_REQUIRED"));
        assertThat(statusOf(r04)).isEqualTo("PENDING_MANAGER");

        postAs(priya, "/api/leaves/" + r04 + "/reject", "{\"comment\":\"Release freeze\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        JsonNode sam = balance(login("sam"), "ANNUAL");
        assertThat(num(sam, "pending")).isEqualTo(0.0);
        assertThat(num(sam, "available")).isEqualTo(18.0);
    }

    @Test
    @DisplayName("a sick request ends at the manager (type does not require HR) and uses the sick balance")
    void sickSkipsHr() throws Exception {
        String isha = login("isha");
        postAs(isha, "/api/leaves", apply("SICK", "2026-10-13", "2026-10-13")).andExpect(status().isOk());
        long id = requestId("isha", "2026-10-13");
        postAs(login("priya"), "/api/leaves/" + id + "/approve", null)   // Arjun delegated to Priya
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(num(balance(isha, "SICK"), "used")).isEqualTo(1.0);
    }

    // ------------------------------------------------------------------ escalation

    @Test
    @DisplayName("simulate-timeout moves a waiting request to ESCALATED and only HR can then decide")
    void simulateTimeout() throws Exception {
        long r05 = requestId("ravi", "2026-11-16");
        String priya = login("priya");
        postAs(priya, "/api/demo/simulate-timeout/" + r05, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ESCALATED"));

        postAs(priya, "/api/leaves/" + r05 + "/approve", null).andExpect(status().isForbidden());
        postAs(priya, "/api/demo/simulate-timeout/" + r05, null).andExpect(status().isConflict());

        postAs(login("meena"), "/api/leaves/" + r05 + "/approve", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        JsonNode ravi = balance(login("ravi"), "ANNUAL");
        assertThat(num(ravi, "used")).isEqualTo(5.0);
        assertThat(num(ravi, "pending")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("the scheduled job escalates only after the deadline, and running it again changes nothing")
    void schedulerEscalatesOnceAndIsIdempotent() throws Exception {
        postAs(login("divya"), "/api/leaves", apply("ANNUAL", "2026-12-14", "2026-12-15")).andExpect(status().isOk());
        long id = requestId("divya", "2026-12-14");

        assertThat(escalationJob.run()).isZero();                       // not due yet
        assertThat(statusOf(id)).isEqualTo("PENDING_MANAGER");

        ((MutableClock) clock).advance(Duration.ofMinutes(2));          // past the 1-minute timeout
        assertThat(escalationJob.run()).isEqualTo(1);
        assertThat(statusOf(id)).isEqualTo("ESCALATED");

        assertThat(escalationJob.run()).isZero();                       // idempotent
        assertThat(jdbc.queryForObject("select count(*) from audit_events where request_id = ? and action = 'ESCALATED'",
                Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from approval_steps where request_id = ? and stage = 'HR'",
                Integer.class, id)).isEqualTo(1);
        // The system never decides on anyone's behalf.
        assertThat(jdbc.queryForObject("select count(*) from audit_events where request_id = ? and action like '%APPROVED%'",
                Integer.class, id)).isZero();
    }

    // ------------------------------------------------------------------ concurrency

    @Test
    @DisplayName("two simultaneous approvals: exactly one wins, the other gets 409/403, and it is applied once")
    void concurrentApproval() throws Exception {
        long r05 = requestId("ravi", "2026-11-16");
        String priya = login("priya");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = List.of(
                pool.submit(() -> { go.await(); return postAs(priya, "/api/leaves/" + r05 + "/approve", null).andReturn().getResponse().getStatus(); }),
                pool.submit(() -> { go.await(); return postAs(priya, "/api/leaves/" + r05 + "/approve", null).andReturn().getResponse().getStatus(); }));
        go.countDown();
        int a = results.get(0).get();
        int b = results.get(1).get();
        pool.shutdown();

        assertThat(List.of(a, b)).containsOnlyOnce(200);
        assertThat(a == 200 ? b : a).isIn(403, 409);
        assertThat(statusOf(r05)).isEqualTo("PENDING_HR");
        assertThat(jdbc.queryForObject("select count(*) from audit_events where request_id = ? and action = 'MANAGER_APPROVED'",
                Integer.class, r05)).isEqualTo(1);
        assertThat(num(balance(login("ravi"), "ANNUAL"), "pending")).isEqualTo(5.0);
    }

    // ------------------------------------------------------------------ reads

    @Test
    @DisplayName("manager list is scoped; HR sees everything; filters and paging work")
    void listsAndFilters() throws Exception {
        getAs(login("meena"), "/api/hr/requests?size=100").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(18));
        getAs(login("meena"), "/api/hr/requests?status=ESCALATED").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeName").value("Rohan Das"));
        getAs(login("meena"), "/api/hr/requests?q=asha&size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content.length()").value(2));

        String priya = getAs(login("priya"), "/api/manager/requests?size=100").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        List<String> names = json.readTree(priya).get("content").findValuesAsText("employeeName");
        assertThat(names).contains("Sam Thomas", "Isha Gupta").doesNotContain("Meera Pillai"); // Isha via delegation
    }

    @Test
    @DisplayName("team calendar shows the month's leave and holidays; analytics and notifications respond")
    void calendarAnalyticsNotifications() throws Exception {
        getAs(login("priya"), "/api/calendar/team?month=2026-11").andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName").value("Engineering")).andExpect(jsonPath("$.teamSize").value(7))
                .andExpect(jsonPath("$.holidays.length()").value(2))
                .andExpect(jsonPath("$.entries.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
        getAs(login("priya"), "/api/calendar/team?month=nope").andExpect(status().isBadRequest());

        getAs(login("meena"), "/api/analytics/summary").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(18)).andExpect(jsonPath("$.flaggedCount").value(3))
                .andExpect(jsonPath("$.escalatedCount").value(1));

        String notes = getAs(login("meena"), "/api/notifications").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        JsonNode first = json.readTree(notes).get(0);
        postAs(login("asha"), "/api/notifications/" + first.get("id").asLong() + "/read", null)
                .andExpect(status().isForbidden());                                  // not yours
        postAs(login("meena"), "/api/notifications/" + first.get("id").asLong() + "/read", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @DisplayName("delegation: a manager can delegate; the delegate must be a manager; HR adds holidays")
    void delegationAndHolidays() throws Exception {
        String priya = login("priya");
        long arjunId = jdbc.queryForObject("select id from users where email = 'arjun@leave.demo'", Long.class);
        long samId = jdbc.queryForObject("select id from users where email = 'sam@leave.demo'", Long.class);
        postAs(priya, "/api/delegations", "{\"delegateId\":" + arjunId + ",\"fromDate\":\"2026-12-01\",\"toDate\":\"2026-12-05\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.delegateName").value("Arjun Mehta"));
        postAs(priya, "/api/delegations", "{\"delegateId\":" + samId + ",\"fromDate\":\"2026-12-01\",\"toDate\":\"2026-12-05\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DELEGATE"));
        postAs(login("asha"), "/api/delegations", "{\"delegateId\":" + arjunId + ",\"fromDate\":\"2026-12-01\",\"toDate\":\"2026-12-05\"}")
                .andExpect(status().isForbidden());

        postAs(login("meena"), "/api/holidays", "{\"date\":\"2026-11-11\",\"name\":\"Test Day\"}").andExpect(status().isOk());
        postAs(login("meena"), "/api/holidays", "{\"date\":\"2026-11-11\",\"name\":\"Again\"}").andExpect(status().isConflict());
        postAs(login("asha"), "/api/holidays", "{\"date\":\"2026-11-12\",\"name\":\"X\"}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("directory lists teams and managers for pickers, to managers and HR only")
    void directory() throws Exception {
        getAs(login("priya"), "/api/directory").andExpect(status().isOk())
                .andExpect(jsonPath("$.teams.length()").value(3))
                .andExpect(jsonPath("$.managers.length()").value(2));
        getAs(login("meena"), "/api/directory").andExpect(status().isOk());
        getAs(login("asha"), "/api/directory").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("forecast: 30 days from 'today', approved and pending counted separately, HR only")
    void forecast() throws Exception {
        long eng = jdbc.queryForObject("select id from teams where name = 'Engineering'", Long.class);
        // Seed today is Mon 12 Oct. Sam's R04 (4-5 Nov) is pending; Asha's R01 (26-28 Oct) is approved.
        String body = getAs(login("meena"), "/api/analytics/forecast?teamId=" + eng + "&days=30").andExpect(status().isOk())
                .andExpect(jsonPath("$.teamSize").value(7)).andExpect(jsonPath("$.days.length()").value(30))
                .andExpect(jsonPath("$.thresholdPercent").value(30.0)).andReturn().getResponse().getContentAsString();
        JsonNode days = json.readTree(body).get("days");
        JsonNode oct27 = null;
        JsonNode nov4 = null;
        for (JsonNode d : days) {
            if (d.get("date").asText().equals("2026-10-27")) oct27 = d;
            if (d.get("date").asText().equals("2026-11-04")) nov4 = d;
        }
        assertThat(oct27.get("approvedAway").asInt()).isEqualTo(1);
        assertThat(nov4.get("approvedAway").asInt()).isEqualTo(2);   // Kiran, Divya
        assertThat(nov4.get("pendingAway").asInt()).isEqualTo(1);   // Sam (R04); Asha's R18 ends 3 Nov
        getAs(login("priya"), "/api/analytics/forecast").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("errors never leak stack traces and always use {code, message, details}")
    void errorEnvelope() throws Exception {
        postAs(login("asha"), "/api/leaves", "{\"leaveTypeCode\":\"ANNUAL\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
        getAs(login("asha"), "/api/leaves/999999").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND")).andExpect(jsonPath("$.trace").doesNotExist());
    }
}
