package com.jpb.reconciliation.reconciliation.entity.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "C_ROLE_MENU_MAP")
public class CRoleMenuMap {

    @EmbeddedId
    private RoleMenuMapId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "ROLE_ID")
    private ReconRoleMaster role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("menuId")
    @JoinColumn(name = "MENU_ID")
    private ReconMenuMaster menu;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleMenuMapId implements Serializable {
        @Column(name = "ROLE_ID")
        private Long roleId;

        @Column(name = "MENU_ID")
        private Long menuId;
    }
}
