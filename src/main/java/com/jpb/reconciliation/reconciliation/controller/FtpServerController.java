package com.jpb.reconciliation.reconciliation.controller;

import java.util.List; 

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.FtpServerDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.FtpServerService;

@RestController
@RequestMapping("/api/ftp-servers")
public class FtpServerController {

	@Autowired
	private FtpServerService ftpServerService;

	@PostMapping
	public ResponseEntity<FtpServerDTO> createFtpServer(@Valid @RequestBody FtpServerDTO dto) {
		FtpServerDTO created = ftpServerService.createFtpServer(dto);
		return new ResponseEntity<>(created, HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<List<FtpServerDTO>> getAllFtpServers() {
		List<FtpServerDTO> list = ftpServerService.getAllFtpServers();
		return ResponseEntity.ok(list);
	}

	@GetMapping("/{id}")
	public ResponseEntity<FtpServerDTO> getFtpServerById(@PathVariable Long id) {
		FtpServerDTO dto = ftpServerService.getFtpServerById(id);
		return ResponseEntity.ok(dto);
	}

	@GetMapping("/search")
	public ResponseEntity<RestWithStatusList> getFtpServersByIp(@RequestParam String serverIp) {
		RestWithStatusList restWithStatusList = null;
		List<Object> searchData = null;
		List<FtpServerDTO> list = ftpServerService.getFtpServersByIp(serverIp);
		if (!list.isEmpty()) {
			searchData.addAll(searchData);
			restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "Found FTP Server Data", searchData);
		} else {
			restWithStatusList = new RestWithStatusList(CommonConstants.FAILURE, "Found FTP Server Data Not Found", searchData);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@PutMapping("/{id}")
	public ResponseEntity<FtpServerDTO> updateFtpServer(@PathVariable Long id, @Valid @RequestBody FtpServerDTO dto) {
		FtpServerDTO updated = ftpServerService.updateFtpServer(id, dto);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<RestWithStatusList> deleteFtpServer(@PathVariable Long id) {
		RestWithStatusList restWithStatusList = null;
		ftpServerService.deleteFtpServer(id);
		restWithStatusList = new RestWithStatusList(CommonConstants.SUCCESS, "FTP server deleted successfully", null);
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}
}
