package com.kayple.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class AdminAuthTest {
    static final LocalDate TOMORROW = LocalDate.of(2026, 11, 26);

    @Autowired MockMvc mvc;
    @Autowired ReservationRepository repository;
    @Autowired TestClockConfig.MutableClock clock;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        clock.reset();
        LocalDateTime now = LocalDateTime.of(2026, 11, 25, 9, 0);
        repository.save(new Reservation(TOMORROW, 13, "홍길동", "01012345678", now));
        repository.save(new Reservation(TOMORROW, 10, "김철수", "01022223333", now));
        repository.save(new Reservation(TOMORROW.plusDays(1), 10, "이영희", "01044445555", now));
    }

    private MockHttpSession login() throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession s = (MockHttpSession) r.getRequest().getSession(false);
        assertThat(s).isNotNull();
        return s;
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mvc.perform(post("/api/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void login_wrongUsername_returns401() throws Exception {
        mvc.perform(post("/api/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"root\",\"password\":\"test-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_correct_returns200AndSession() throws Exception {
        MockHttpSession s = login();
        assertThat(s.getAttribute(AdminController.SESSION_KEY)).isNotNull();
    }

    @Test
    void adminReservations_withoutLogin_returns401() throws Exception {
        mvc.perform(get("/api/admin/reservations").param("date", TOMORROW.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminReservations_withWrongLoginSession_returns401() throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"bad\"}")).andReturn();
        MockHttpSession s = (MockHttpSession) r.getRequest().getSession(true);
        mvc.perform(get("/api/admin/reservations").param("date", TOMORROW.toString()).session(s))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminReservations_withSession_returns200ListForDate() throws Exception {
        MockHttpSession s = login();
        mvc.perform(get("/api/admin/reservations").param("date", TOMORROW.toString()).session(s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].date", everyItem(is(TOMORROW.toString()))))
                .andExpect(jsonPath("$[*].hour", containsInAnyOrder(10, 13)));
    }

    @Test
    void adminReservations_afterLogout_returns401() throws Exception {
        MockHttpSession s = login();
        mvc.perform(post("/api/admin/logout").session(s)).andExpect(status().is2xxSuccessful());
        mvc.perform(get("/api/admin/reservations").param("date", TOMORROW.toString()).session(s))
                .andExpect(status().isUnauthorized());
    }
}
