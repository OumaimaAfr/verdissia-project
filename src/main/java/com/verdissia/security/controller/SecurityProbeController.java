package com.verdissia.security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secure")
public class SecurityProbeController {

    @GetMapping("/authenticated")
    public String authenticated() {
        return "AUTH OK";
    }

    @PreAuthorize("hasRole('BO')")
    @GetMapping("/bo")
    public String boOnly() {
        return "[BO] OK";
    }

    @PreAuthorize("hasAnyRole('BO', 'RC')")
    @GetMapping("/internal")
    public String internal() {
        return "OK INTERNAL BO/RC";
    }
}
