package com.happy.simipkit.controller;

import com.happy.simipkit.model.User;
import com.happy.simipkit.service.AuditLogService;
import com.happy.simipkit.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user-management")
public class UserManagementController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserManagementController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "user-management";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        User user = new User();
        user.setActive(true);
        user.setRole("staff");
        model.addAttribute("user", user);
        model.addAttribute("isNew", true);
        return "user-form";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") User user,
                           @RequestParam(value = "password", required = false) String password,
                           HttpServletRequest request,
                           HttpSession session,
                           Model model) {

        Integer currentUserId = (Integer) session.getAttribute("userId");

        if (user.getId() != null && user.getId() > 0) {
            userService.updateUser(user, password);
            auditLogService.logAction(currentUserId, "USER_UPDATE", request.getRemoteAddr(),
                    "Update user: " + user.getUsername() + " (ID: " + user.getId() + ")");
        } else {
            if (password == null || password.trim().isEmpty()) {
                model.addAttribute("error", "Password wajib diisi untuk user baru.");
                model.addAttribute("user", user);
                model.addAttribute("isNew", true);
                return "user-form";
            }
            userService.createUser(user, password);
            auditLogService.logAction(currentUserId, "USER_CREATE", request.getRemoteAddr(),
                    "Tambah user baru: " + user.getUsername() + " role: " + user.getRole());
        }

        return "redirect:/user-management";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/user-management";
        }
        model.addAttribute("user", user);
        model.addAttribute("isNew", false);
        return "user-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Integer id, HttpServletRequest request, HttpSession session) {
        Integer currentUserId = (Integer) session.getAttribute("userId");
        User user = userService.findById(id);
        if (user != null) {
            userService.deleteUser(id);
            auditLogService.logAction(currentUserId, "USER_DELETE", request.getRemoteAddr(),
                    "Hapus user: " + user.getUsername() + " (ID: " + id + ")");
        }
        return "redirect:/user-management";
    }
}
