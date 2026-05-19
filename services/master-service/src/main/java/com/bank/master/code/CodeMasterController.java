package com.bank.master.code;

import com.bank.common.code.CodeDto;
import com.bank.common.persistence.CurrentActorProvider;
import com.bank.common.web.ApiResponse;
import com.bank.common.web.BusinessException;
import com.bank.common.web.CommonErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CodeMasterController {

    private final CodeMasterAdminService service;
    private final CurrentActorProvider actorProvider;

    @GetMapping("/{groupCd}/{codeCd}")
    public ApiResponse<CodeDto> get(@PathVariable String groupCd, @PathVariable String codeCd) {
        return service.find(groupCd, codeCd)
                .map(ApiResponse::ok)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_404, "코드를 찾을 수 없습니다."));
    }

    @GetMapping
    public ApiResponse<List<CodeDto>> list(@RequestParam("group") String groupCd) {
        return ApiResponse.ok(service.findGroup(groupCd));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CodeDto>> create(@Valid @RequestBody CreateCodeRequest req) {
        CodeDto saved = service.create(req.groupCd(), req.codeCd(), req.codeName(),
                req.codeDesc(), req.sortNo(), req.activeYn());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    @PutMapping("/{codeId}")
    public ApiResponse<CodeDto> update(@PathVariable Long codeId, @Valid @RequestBody UpdateCodeRequest req) {
        return ApiResponse.ok(service.update(codeId, req.codeName(), req.codeDesc(), req.sortNo(), req.activeYn()));
    }

    @DeleteMapping("/{codeId}")
    public ApiResponse<Void> delete(@PathVariable Long codeId) {
        service.delete(codeId, actorProvider.currentActorId());
        return ApiResponse.ok();
    }

    public record CreateCodeRequest(
            @NotBlank String groupCd,
            @NotBlank String codeCd,
            String codeName,
            String codeDesc,
            Integer sortNo,
            String activeYn
    ) {}

    public record UpdateCodeRequest(
            String codeName,
            String codeDesc,
            Integer sortNo,
            String activeYn
    ) {}
}
