package com.security.monitor.controller;

import com.security.monitor.dto.LogStatsDTO;
import com.security.monitor.dto.SystemLogDTO;
import com.security.monitor.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/developer/logs")
@PreAuthorize("hasRole('DEVELOPER')")
public class SystemLogController {

    @Autowired
    private SystemLogService systemLogService;

    @GetMapping
    public ResponseEntity<Page<SystemLogDTO>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SystemLogDTO> logs = systemLogService.getLogs(
                level, type, userId, startTime, endTime, keyword, pageable
        );
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats")
    public ResponseEntity<LogStatsDTO> getStats(
            @RequestParam(defaultValue = "24") int hours) {
        LogStatsDTO stats = systemLogService.getStats(hours);
        return ResponseEntity.ok(stats);
    }
}
