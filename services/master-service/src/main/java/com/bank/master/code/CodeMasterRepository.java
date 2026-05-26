package com.bank.master.code;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeMasterRepository extends JpaRepository<CodeMaster, Long> {

    Optional<CodeMaster> findByCodeGroupCdAndCodeCdAndDeletedAtIsNull(String codeGroupCd, String codeCd);

    List<CodeMaster> findByCodeGroupCdAndDeletedAtIsNullOrderBySortNoAsc(String codeGroupCd);
}
