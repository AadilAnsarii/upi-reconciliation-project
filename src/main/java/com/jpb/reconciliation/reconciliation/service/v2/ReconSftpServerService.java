package com.jpb.reconciliation.reconciliation.service.v2;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.SftpTestConnectionRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpDownloadRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.v2.SftpServerRequestDTO;

public interface ReconSftpServerService {

    ResponseEntity<RestWithMapStatusList> createServer(SftpServerRequestDTO request, String createdBy);

    ResponseEntity<RestWithMapStatusList> updateServer(Long serverId, SftpServerRequestDTO request, String updatedBy);

    ResponseEntity<RestWithMapStatusList> getServerById(Long serverId);

    ResponseEntity<RestWithStatusList> getAllServers(boolean activeOnly);

    ResponseEntity<RestWithMapStatusList> deleteServer(Long serverId, String updatedBy);

    ResponseEntity<RestWithStatusList> testConnection(SftpTestConnectionRequestDTO request);
    
    ResponseEntity<RestWithMapStatusList> downloadFiles(SftpDownloadRequestDTO request, String actor);
}