package com.librarian.controller;

import com.librarian.model.dto.EvalDto.DashboardResponse;
import com.librarian.service.EvalService;
import com.librarian.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/eval")
public class EvalController {

    @Autowired
    private EvalService evalService;

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        LoggerUtil.setRequestId();
        log.info("Getting dashboard data");
        return evalService.getDashboard();
    }
}
