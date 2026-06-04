package com.bank.customer.code;

import com.bank.common.web.ApiResponse;
import com.bank.customer.code.dto.CodeResponse;
import com.bank.customer.code.repository.CustCodeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/codes")
@RequiredArgsConstructor
public class CodeController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CustCodeMasterRepository codeRepository;

    /** 그룹 내 유효 코드 조회 (기본: 오늘 기준 활성 코드) */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<List<CodeResponse>>> getActiveByGroup(
            @PathVariable String groupId,
            @RequestParam(required = false) String date) {
        String today = date != null ? date : LocalDate.now().format(DATE_FMT);
        List<CodeResponse> codes = codeRepository.findActiveByGroup(groupId, today)
                .stream().map(CodeResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(codes));
    }

    /** 그룹 내 전체 코드 조회 (만료 포함) */
    @GetMapping("/{groupId}/all")
    public ResponseEntity<ApiResponse<List<CodeResponse>>> getAllByGroup(
            @PathVariable String groupId) {
        List<CodeResponse> codes = codeRepository.findByCodeGroupIdOrderBySortOrderAsc(groupId)
                .stream().map(CodeResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(codes));
    }
}
