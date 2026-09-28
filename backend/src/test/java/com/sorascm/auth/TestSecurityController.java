package com.sorascm.auth;

import com.sorascm.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/test")
public class TestSecurityController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<String>> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Authenticated", principal.getName()));
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> getAdminData() {
        return ResponseEntity.ok(ApiResponse.success("Admin authorized", "CONFIDENTIAL_METRICS"));
    }

    @GetMapping("/manager-only")
    @PreAuthorize("hasAuthority('ROLE_WAREHOUSE_MANAGER')")
    public ResponseEntity<ApiResponse<String>> getManagerData() {
        return ResponseEntity.ok(ApiResponse.success("Manager authorized", "WAREHOUSE_DATA"));
    }
}