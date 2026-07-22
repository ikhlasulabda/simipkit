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
    
    // Simple rate limiting store: IP -> failed attempt count
    private static final Map<String, Integer> FAILED_ATTEMPTS = new ConcurrentHashMap<>();

    public LoginController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session, Model model) {
        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute("csrfToken", csrfToken);
        model.addAttribute("csrfToken", csrfToken);
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

        // Check CSRF Token
        String sessionCsrf = (String) session.getAttribute("csrfToken");
        if (sessionCsrf != null && !sessionCsrf.equals(csrfToken)) {
            model.addAttribute("error", "Invalid CSRF Token.");
            return "login";
        }

        // Rate limiting check
        int attempts = FAILED_ATTEMPTS.getOrDefault(ipAddress, 0);
        if (attempts >= 5) {
            model.addAttribute("error", "Terlalu banyak percobaan login yang gagal. Silakan coba lagi nanti.");
            auditLogService.logAction(null, "LOGIN_RATE_LIMITED", ipAddress, "IP rate limited for username: " + username);
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
            FAILED_ATTEMPTS.put(ipAddress, attempts + 1);
            Integer userId = (user != null) ? user.getId() : null;
            auditLogService.logAction(userId, "LOGIN_FAILED", ipAddress, "Percobaan login gagal untuk username: " + username);

            model.addAttribute("error", "Username atau password salah / akun tidak aktif.");
            String newCsrfToken = UUID.randomUUID().toString();
            session.setAttribute("csrfToken", newCsrfToken);
            model.addAttribute("csrfToken", newCsrfToken);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpSession session) {
        if (session != null) {
            Integer userId = (Integer) session.getAttribute("userId");
            String username = (String) session.getAttribute("username");
            auditLogService.logAction(userId, "LOGOUT", request.getRemoteAddr(), "User logout: " + username);
            session.invalidate();
        }
        return "redirect:/login";
    }
}
