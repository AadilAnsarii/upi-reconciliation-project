package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseDto {
	private String apiPath;
	private HttpStatus errorCode;
	private String errorMessage;
	private LocalDate errorTime;

}
