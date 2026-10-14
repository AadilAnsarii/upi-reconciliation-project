package com.jpb.reconciliation.reconciliation.repository.v2;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconSftpServerMast;

@Repository
public interface ReconSftpServerMastRepository extends JpaRepository<ReconSftpServerMast, Long> {

    List<ReconSftpServerMast> findByIsActive(String isActive);

    Optional<ReconSftpServerMast> findByServerIdAndIsActive(Long serverId, String isActive);

    boolean existsByServerName(String serverName);

    boolean existsByServerNameAndServerIdNot(String serverName, Long serverId);
}