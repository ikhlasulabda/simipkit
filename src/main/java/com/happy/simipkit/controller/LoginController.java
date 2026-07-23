package com.happy.simipkit.controller;

import com.happy.simipkit.model.User;
import com.happy.simipkit.security.PasswordHasher;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class LoginController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    
    private static class FailedAttempt {
        private final int count;
        private final long lastAttemptTime;

        public FailedAttempt(int count, long lastAttemptTime) {
            this.count = count;
            this.lastAttemptTime = lastAttemptTime;
        }

        public int getCount() { return count; }
        public long getLastAttemptTime() { return lastAttemptTime; }
    }

    private static final Map<String, FailedAttempt> FAILED_ATTEMPTS = new ConcurrentHashMap<>();
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000L; // 5 minutes (300,000 ms)

    public LoginController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "timeout", required = false) String timeout,
                                HttpServletRequest request,
                                HttpSession session,
                                Model model) {
        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute("csrfToken", csrfToken);
        model.addAttribute("csrfToken", csrfToken);

        String ipAddress = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        FailedAttempt record = FAILED_ATTEMPTS.get(ipAddress);

        if (record != null && record.getCount() >= 5) {
            long elapsed = now - record.getLastAttemptTime();
            if (elapsed < LOCKOUT_DURATION_MS) {
                model.addAttribute("rateLimited", true);
                model.addAttribute("error", "Terlalu banyak percobaan login yang gagal. Silakan coba lagi nanti.");
                return "login";
            } else {
                FAILED_ATTEMPTS.remove(ipAddress);
            }
        }

        if ("true".equals(timeout)) {
            model.addAttribute("timeoutMessage", "Sesi Anda telah habis. Mohon login ulang.");
        }

        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               @RequestParam(value = "csrfToken", required = false) String csrfToken,
                               HttpServletRequest request,
                               HttpSession session,
                               Model model) {

        String ipAddress = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        FailedAttempt record = FAILED_ATTEMPTS.get(ipAddress);

        if (record != null && record.getCount() >= 5) {
            long elapsed = now - record.getLastAttemptTime();
            if (elapsed < LOCKOUT_DURATION_MS) {
                model.addAttribute("rateLimited", true);
                model.addAttribute("error", "Terlalu banyak percobaan login yang gagal. Silakan coba lagi nanti.");
                auditLogService.logAction(null, "LOGIN_RATE_LIMITED", ipAddress, "IP rate limited for username: " + username);
                return "login";
            } else {
                FAILED_ATTEMPTS.remove(ipAddress);
                record = null;
            }
        }

        // Check CSRF Token
        String sessionCsrf = (String) session.getAttribute("csrfToken");
        if (sessionCsrf != null && !sessionCsrf.equals(csrfToken)) {
            model.addAttribute("error", "Invalid CSRF Token.");
            return "login";
        }

        User user = userService.findByUsername(username);
        if (user != null && user.isActive() && PasswordHasher.verify(password, user.getPasswordHash())) {
            // Login successful
            FAILED_ATTEMPTS.remove(ipAddress);
            session.setAttribute("authenticated", true);
            session.setAttribute("role", user.getRole());
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());

            auditLogService.logAction(user.getId(), "LOGIN_SUCCESS", ipAddress, "Login sukses untuk user: " + username);
            return "redirect:/";
        } else {
            // Login failed
            int currentCount = (record != null) ? record.getCount() + 1 : 1;
            FAILED_ATTEMPTS.put(ipAddress, new FailedAttempt(currentCount, now));
            Integer userId = (user != null) ? user.getId() : null;
            auditLogService.logAction(userId, "LOGIN_FAILED", ipAddress, "Percobaan login gagal untuk username: " + username);

            if (currentCount >= 5) {
                model.addAttribute("rateLimited", true);
                model.addAttribute("error", "Terlalu banyak percobaan login yang gagal. Silakan coba lagi nanti.");
            } else {
                model.addAttribute("error", "Username atau password salah / akun tidak aktif.");
            }

            String newCsrfToken = UUID.randomUUID().toString();
            session.setAttribute("csrfToken", newCsrfToken);
            model.addAttribute("csrfToken", newCsrfToken);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(@RequestParam(value = "reason", required = false) String reason,
                         HttpServletRequest request,
                         HttpSession session) {
        if (session != null) {
            Integer userId = (Integer) session.getAttribute("userId");
            String username = (String) session.getAttribute("username");
            if ("timeout".equals(reason)) {
                auditLogService.logAction(userId, "LOGOUT_TIMEOUT", request.getRemoteAddr(), "User session timeout: " + username);
            } else {
                auditLogService.logAction(userId, "LOGOUT", request.getRemoteAddr(), "User logout: " + username);
            }
            session.invalidate();
        }
        if ("timeout".equals(reason)) {
            return "redirect:/login?timeout=true";
        }
        return "redirect:/login";
    }
}
