package com.bank.master.code;

import com.bank.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "code_master",
        uniqueConstraints = @UniqueConstraint(name = "uk_code_master_group_code",
                columnNames = {"code_group_cd", "code_cd"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CodeMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long codeId;

    @Column(name = "code_group_cd", nullable = false, length = 50)
    private String codeGroupCd;

    @Column(name = "code_cd", nullable = false, length = 50)
    private String codeCd;

    @Column(name = "code_name", length = 200)
    private String codeName;

    @Column(name = "code_desc", length = 500)
    private String codeDesc;

    @Column(name = "sort_no")
    private Integer sortNo;

    @Column(name = "active_yn", length = 1, nullable = false)
    private String activeYn;

    public void update(String codeName, String codeDesc, Integer sortNo, String activeYn) {
        this.codeName = codeName;
        this.codeDesc = codeDesc;
        this.sortNo = sortNo;
        this.activeYn = activeYn;
    }
}
