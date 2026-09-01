package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.dto.admin.AdminResumenDto;
import com.fktlomas.turnos.service.AdminResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resumen")
@RequiredArgsConstructor
public class AdminResumenController {

    private final AdminResumenService adminResumenService;

    @GetMapping
    public AdminResumenDto resumen() {
        return adminResumenService.resumen();
    }
}
