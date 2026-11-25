package com.kayple.booking;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookingApplication {
    public static void main(String[] args) {
        // JDBC 드라이버가 초기화되기 전에 시간대를 고정해야 LocalDate가 하루 밀리지 않는다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(BookingApplication.class, args);
    }
}
