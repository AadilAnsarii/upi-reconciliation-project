package com.jpb.reconciliation.reconciliation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.FtpServerDTO;
import com.jpb.reconciliation.reconciliation.entity.FtpServer;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.repository.FtpServerRepository;

@Service
public class FtpServerServiceImpl implements FtpServerService {

    @Autowired
    private FtpServerRepository ftpServerRepository;

    @Override
    public FtpServerDTO createFtpServer(FtpServerDTO dto) {
        FtpServer entity = mapToEntity(dto);
        FtpServer saved = ftpServerRepository.save(entity);
        return mapToDTO(saved);
    }

    @Override
    public FtpServerDTO getFtpServerById(Long id) {
        FtpServer entity = ftpServerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FtpServer not found with id: " + id));
        return mapToDTO(entity);
    }

    @Override
    public List<FtpServerDTO> getAllFtpServers() {
        return ftpServerRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FtpServerDTO updateFtpServer(Long id, FtpServerDTO dto) {
        FtpServer existing = ftpServerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FtpServer not found with id: " + id));

        existing.setFtpServerName(dto.getFtpServerName());
        existing.setServerIp(dto.getServerIp());
        existing.setPort(dto.getPort());
        existing.setUserName(dto.getUserName());
        existing.setPassword(dto.getPassword());

        FtpServer updated = ftpServerRepository.save(existing);
        return mapToDTO(updated);
    }

    @Override
    public void deleteFtpServer(Long id) {
        FtpServer existing = ftpServerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FtpServer not found with id: " + id));
        ftpServerRepository.delete(existing);
    }

    @Override
    public List<FtpServerDTO> getFtpServersByIp(String serverIp) {
        return ftpServerRepository.findByServerIp(serverIp)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // --- Mapper Methods ---
    private FtpServer mapToEntity(FtpServerDTO dto) {
        FtpServer entity = new FtpServer();
        entity.setFtpServerName(dto.getFtpServerName());
        entity.setServerIp(dto.getServerIp());
        entity.setPort(dto.getPort());
        entity.setUserName(dto.getUserName());
        entity.setPassword(dto.getPassword());
        return entity;
    }

    private FtpServerDTO mapToDTO(FtpServer entity) {
        return new FtpServerDTO(
                entity.getId(),
                entity.getFtpServerName(),
                entity.getServerIp(),
                entity.getPort(),
                entity.getUserName(),
                entity.getPassword()
        );
    }
}
