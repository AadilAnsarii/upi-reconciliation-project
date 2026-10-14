package com.jpb.reconciliation.reconciliation.service;

import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.FtpServerDTO;

public interface FtpServerService {
    FtpServerDTO createFtpServer(FtpServerDTO dto);
    FtpServerDTO getFtpServerById(Long id);
    List<FtpServerDTO> getAllFtpServers();
    FtpServerDTO updateFtpServer(Long id, FtpServerDTO dto);
    void deleteFtpServer(Long id);
    List<FtpServerDTO> getFtpServersByIp(String serverIp);
}