package com.kayple.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class ReservationApiTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    static final LocalDate TOMORROW = TODAY.plusDays(1);
    static final String DUP = "이미 예약된 시간입니다. 다른 시간을 선택해 주세요.";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired ReservationRepository repository;
    @Autowired TestClockConfig.MutableClock clock;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        clock.reset();
    }

    private String body(Object date, Object hour, Object name, Object phone) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", date == null ? null : date.toString());
        m.put("hour", hour);
        m.put("name", name);
        m.put("phone", phone);
        return om.writeValueAsString(m);
    }

    private ResultActions post(Object date, Object hour, Object name, Object phone) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON).content(body(date, hour, name, phone)));
    }

    private long createOk(LocalDate date, int hour, String phone) throws Exception {
        String res = post(date, hour, "홍길동", phone).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(res).get("id").asLong();
    }

    // ---------- GET /api/slots ----------

    @Test
    void slots_validDate_returns7Slots() throws Exception {
        mvc.perform(get("/api/slots").param("date", TOMORROW.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].hour", contains(10, 11, 13, 14, 15, 16, 17)))
                .andExpect(jsonPath("$[*].available", everyItem(is(true))))
                .andExpect(jsonPath("$[*].past", everyItem(is(false))));
    }

    @Test
    void slots_bookedHour_returnsUnavailable() throws Exception {
        createOk(TOMORROW, 13, "010-1234-5678");
        mvc.perform(get("/api/slots").param("date", TOMORROW.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.hour == 13)].available", contains(false)))
                .andExpect(jsonPath("$[?(@.hour == 14)].available", contains(true)));
    }

    @Test
    void slots_todayAfterSomeHours_marksPast() throws Exception {
        clock.set(LocalDateTime.of(2026, 11, 25, 13, 0));
        mvc.perform(get("/api/slots").param("date", TODAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.hour == 10)].past", contains(true)))
                .andExpect(jsonPath("$[?(@.hour == 10)].available", contains(false)))
                .andExpect(jsonPath("$[?(@.hour == 13)].past", contains(true)))
                .andExpect(jsonPath("$[?(@.hour == 14)].past", contains(false)))
                .andExpect(jsonPath("$[?(@.hour == 14)].available", contains(true)));
    }

    @Test
    void slots_lastDayInRange_returns200() throws Exception {
        mvc.perform(get("/api/slots").param("date", TODAY.plusDays(6).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(7)));
    }

    @Test
    void slots_dateAfterRange_returns400() throws Exception {
        mvc.perform(get("/api/slots").param("date", TODAY.plusDays(7).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void slots_dateBeforeToday_returns400() throws Exception {
        mvc.perform(get("/api/slots").param("date", TODAY.minusDays(1).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---------- POST /api/reservations ----------

    @Test
    void create_valid_returns201WithDigitsOnlyPhone() throws Exception {
        post(TOMORROW, 10, "홍길동", "010-1234-5678")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.date").value(TOMORROW.toString()))
                .andExpect(jsonPath("$.hour").value(10))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.phone").value("01012345678"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void create_phone3DigitMiddleWithoutHyphens_returns201() throws Exception {
        post(TOMORROW, 11, "홍길동", "0101234567")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.phone").value("0101234567"));
    }

    @Test
    void create_phone3DigitMiddleWithHyphens_returns201() throws Exception {
        post(TOMORROW, 11, "홍길동", "010-123-4567")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.phone").value("0101234567"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        post(TOMORROW, 10, "   ", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_emptyName_returns400() throws Exception {
        post(TOMORROW, 10, "", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_name21Chars_returns400() throws Exception {
        post(TOMORROW, 10, "가".repeat(21), "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_name20Chars_returns201() throws Exception {
        post(TOMORROW, 10, "가".repeat(20), "010-1234-5678").andExpect(status().isCreated());
    }

    @Test
    void create_englishNameWithSpace_returns201() throws Exception {
        post(TOMORROW, 10, "John Smith", "010-1234-5678")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("John Smith"));
    }

    @Test
    void create_nameSqlInjection_returns400() throws Exception {
        post(TOMORROW, 10, "Robert'); DROP TABLE reservation;--", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @Test
    void create_nameScriptTag_returns400() throws Exception {
        post(TOMORROW, 10, "<script>", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @Test
    void create_nameWithDigits_returns400() throws Exception {
        post(TOMORROW, 10, "홍길동1", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_hourSqlInjectionString_returns400() throws Exception {
        post(TOMORROW, "10 OR 1=1", "홍길동", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @Test
    void create_invalidPhonePrefix_returns400() throws Exception {
        post(TOMORROW, 10, "홍길동", "011-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_phoneTooShort_returns400() throws Exception {
        post(TOMORROW, 10, "홍길동", "010-12-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_phoneNonDigits_returns400() throws Exception {
        post(TOMORROW, 10, "홍길동", "010-abcd-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_hour12_returns400() throws Exception {
        post(TOMORROW, 12, "홍길동", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_hour9_returns400() throws Exception {
        post(TOMORROW, 9, "홍길동", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_dateAfterRange_returns400() throws Exception {
        post(TODAY.plusDays(7), 10, "홍길동", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_dateBeforeToday_returns400() throws Exception {
        post(TODAY.minusDays(1), 10, "홍길동", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_pastHourToday_returns400() throws Exception {
        clock.set(LocalDateTime.of(2026, 11, 25, 13, 30));
        post(TODAY, 13, "홍길동", "010-1234-5678")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @Test
    void create_hourEqualsNowToday_returns400() throws Exception {
        clock.set(LocalDateTime.of(2026, 11, 25, 14, 0));
        post(TODAY, 14, "홍길동", "010-1234-5678").andExpect(status().isBadRequest());
    }

    @Test
    void create_futureHourToday_returns201() throws Exception {
        post(TODAY, 10, "홍길동", "010-1234-5678").andExpect(status().isCreated());
    }

    @Test
    void create_duplicateSlot_returns409() throws Exception {
        createOk(TOMORROW, 15, "010-1234-5678");
        post(TOMORROW, 15, "김철수", "010-9999-8888")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(DUP));
        assertThat(repository.count()).isEqualTo(1);
    }

    // ---------- GET /api/reservations?phone= ----------

    @Test
    void findByPhone_hyphenAndDigitsForms_returnSameReservations() throws Exception {
        createOk(TOMORROW, 10, "010-1234-5678");
        createOk(TOMORROW, 11, "01012345678");
        createOk(TOMORROW, 13, "010-0000-1111");

        String a = mvc.perform(get("/api/reservations").param("phone", "010-1234-5678"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].date", everyItem(is(TOMORROW.toString()))))
                .andExpect(jsonPath("$[*].hour", contains(10, 11)))
                .andReturn().getResponse().getContentAsString();
        String b = mvc.perform(get("/api/reservations").param("phone", "01012345678"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        JsonNode ja = om.readTree(a), jb = om.readTree(b);
        assertThat(ja).isEqualTo(jb);
    }

    @Test
    void findByPhone_noReservations_returnsEmptyArray() throws Exception {
        mvc.perform(get("/api/reservations").param("phone", "010-5555-6666"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void findByPhone_sqlInjection_returns400() throws Exception {
        createOk(TOMORROW, 10, "010-1234-5678");
        mvc.perform(get("/api/reservations").param("phone", "010-1234-5678' OR '1'='1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void findByPhone_badPhone_returns400() throws Exception {
        mvc.perform(get("/api/reservations").param("phone", "12345"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---------- DELETE /api/reservations/{id}?phone= ----------

    @Test
    void cancel_correctPhone_returns204AndDeletes() throws Exception {
        long id = createOk(TOMORROW, 10, "010-1234-5678");
        mvc.perform(delete("/api/reservations/" + id).param("phone", "010-1234-5678"))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void cancel_digitsOnlyPhone_returns204AndDeletes() throws Exception {
        long id = createOk(TOMORROW, 10, "010-1234-5678");
        mvc.perform(delete("/api/reservations/" + id).param("phone", "01012345678"))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void cancel_wrongPhone_returns204AndNotDeleted() throws Exception {
        long id = createOk(TOMORROW, 10, "010-1234-5678");
        mvc.perform(delete("/api/reservations/" + id).param("phone", "010-9999-9999"))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isTrue();
    }

    @Test
    void cancel_twice_returns204BothTimes() throws Exception {
        long id = createOk(TOMORROW, 10, "010-1234-5678");
        mvc.perform(delete("/api/reservations/" + id).param("phone", "010-1234-5678"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/reservations/" + id).param("phone", "010-1234-5678"))
                .andExpect(status().isNoContent());
        assertThat(repository.count()).isZero();
    }

    @Test
    void cancel_thenRebookSameSlot_returns201() throws Exception {
        long id = createOk(TOMORROW, 16, "010-1234-5678");
        mvc.perform(delete("/api/reservations/" + id).param("phone", "010-1234-5678"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/slots").param("date", TOMORROW.toString()))
                .andExpect(jsonPath("$[?(@.hour == 16)].available", contains(true)));
        post(TOMORROW, 16, "김철수", "010-2222-3333").andExpect(status().isCreated());
    }
}
