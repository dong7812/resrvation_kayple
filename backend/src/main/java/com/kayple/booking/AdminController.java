package com.kayple.booking;

import com.kayple.booking.Dtos.LoginRequest;
import com.kayple.booking.Dtos.AdminReservationOut;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public static final String SESSION_KEY = "ADMIN";

    private final ReservationService service;
    private final String username;
    private final String password;

    public AdminController(ReservationService service,
                           @Value("${app.admin.username}") String username,
                           @Value("${app.admin.password}") String password) {
        this.service = service;
        this.username = username;
        this.password = password;
    }

    private static boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        // 비밀번호가 설정되지 않은 서버에서는 어떤 값으로도 로그인되지 않는다.
        if (password.isBlank() || req.username() == null || req.password() == null
                || !same(req.username(), username) || !same(req.password(), password)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        request.getSession(true).setAttribute(SESSION_KEY, username);
        return Map.of("username", username);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) s.invalidate();
    }

    @GetMapping("/me")
    public Map<String, String> me(HttpSession session) {
        return Map.of("username", String.valueOf(session.getAttribute(SESSION_KEY)));
    }

    @GetMapping("/reservations")
    public List<AdminReservationOut> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findAllByDateForAdmin(date);
    }
}
