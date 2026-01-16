package com.verdissia.security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
    @PreAuthorize("hasRole('BO')")
    @GetMapping("/api/admin/bo-only")
    public String boOnly() {
        return "[BO] OK";
    }
}
