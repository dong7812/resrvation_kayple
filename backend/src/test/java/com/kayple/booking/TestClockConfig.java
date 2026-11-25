package com.kayple.booking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 테스트용 고정 시계: 기본 2026-11-25T09:00 Asia/Seoul, 테스트에서 시각을 바꿀 수 있다. */
@TestConfiguration
public class TestClockConfig {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalDateTime DEFAULT_NOW = LocalDateTime.of(2026, 11, 25, 9, 0);

    public static class MutableClock extends Clock {
        private volatile Instant instant = DEFAULT_NOW.atZone(ZONE).toInstant();
        public void set(LocalDateTime t) { instant = t.atZone(ZONE).toInstant(); }
        public void reset() { set(DEFAULT_NOW); }
        @Override public ZoneId getZone() { return ZONE; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant, zone); }
        @Override public Instant instant() { return instant; }
    }

    @Bean
    @Primary
    MutableClock testClock() { return new MutableClock(); }
}
