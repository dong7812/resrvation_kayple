package com.kayple.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.kayple.booking.Dtos.ReservationCreate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/** 동시성 테스트: @Transactional 을 붙이지 않는다 (각 스레드가 실제로 커밋해야 함). */
@SpringBootTest
@Import(TestClockConfig.class)
class ConcurrencyTest {
    static final LocalDate TOMORROW = LocalDate.of(2026, 11, 26);

    @Autowired ReservationService service;
    @Autowired ReservationRepository repository;
    @Autowired TestClockConfig.MutableClock clock;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        clock.reset();
    }

    @Test
    void create_10SimultaneousSameSlot_exactlyOneSucceeds9Conflict() throws Exception {
        int n = 10;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    service.create(new ReservationCreate(TOMORROW, 14, "User " + (char) ('A' + idx),
                            String.format("010-1000-%04d", idx)));
                    success.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getStatus() == HttpStatus.CONFLICT) conflict.incrementAndGet();
                    else unexpected.add(e);
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(unexpected).as("unexpected exceptions").isEmpty();
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(9);
        assertThat(repository.findByDate(TOMORROW).stream().filter(r -> r.getHour() == 14).count()).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void create_10SimultaneousDifferentSlots_allSucceed() throws Exception {
        int[] hours = {10, 11, 13, 14, 15, 16, 17};
        int n = hours.length;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int h = hours[i];
            futures.add(pool.submit(() -> {
                start.await();
                service.create(new ReservationCreate(TOMORROW, h, "사용자", "010-1234-5678"));
                success.incrementAndGet();
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        assertThat(success.get()).isEqualTo(n);
        assertThat(repository.count()).isEqualTo(n);
    }
}
