package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.CRoleMenuMap;

import java.util.List;

@Repository
public interface CRoleMenuMapRepository extends JpaRepository<CRoleMenuMap, CRoleMenuMap.RoleMenuMapId> {

    @Query("SELECT r FROM CRoleMenuMap r WHERE r.id.roleId = :roleId")
    List<CRoleMenuMap> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT r FROM CRoleMenuMap r WHERE r.id.menuId = :menuId")
    List<CRoleMenuMap> findByMenuId(@Param("menuId") Long menuId);

    // Scalar projections — avoid hydrating the full CRoleMenuMap entity (and its
    // @EmbeddedId/@MapsId reflection path), which throws a PropertyAccessException
    // on some Oracle setups where MENU_ID's numeric precision makes the JDBC driver
    // return an Integer instead of a Long for the ManyToOne-mapped identifier.
    @Query("SELECT r.id.menuId FROM CRoleMenuMap r WHERE r.id.roleId = :roleId")
    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("DELETE FROM CRoleMenuMap r WHERE r.id.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    // Wipe only a role's APPLICATION-menu grants, preserving any system-menu grant (e.g. the Default
    // Dashboard fallback). savePrivileges re-inserts the submitted app menus after this; keeping the
    // system grant here means the Default Dashboard is assigned once and never churned on every save.
    // Single bulk DELETE (a subquery on RECON_MENU_MASTER), NOT a fetch-all-then-delete loop. The
    // NOT EXISTS form deletes a grant whose menu is non-system OR has a NULL flag, and preserves only
    // a menu explicitly flagged IS_SYSTEM_MENU='Y'.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CRoleMenuMap r WHERE r.id.roleId = :roleId AND NOT EXISTS ("
         + "SELECT 1 FROM ReconMenuMaster m WHERE m.menuId = r.id.menuId AND m.isSystemMenu = 'Y')")
    void deleteApplicationGrantsByRoleId(@Param("roleId") Long roleId);
}
