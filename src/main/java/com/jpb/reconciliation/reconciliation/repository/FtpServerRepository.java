package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.FtpServer;

@Repository
public interface FtpServerRepository extends JpaRepository<FtpServer, Long> {

    // Find by Server IP
    List<FtpServer> findByServerIp(String serverIp);

    // Find by Username
    Optional<FtpServer> findByUserName(String userName);

    // Find by FTP Server Name
    Optional<FtpServer> findByFtpServerName(String ftpServerName);

    // Check if exists by Server IP and Port
    boolean existsByServerIpAndPort(String serverIp, Integer port);
}
