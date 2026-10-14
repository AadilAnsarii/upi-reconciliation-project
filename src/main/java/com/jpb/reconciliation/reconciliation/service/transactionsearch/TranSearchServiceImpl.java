package com.jpb.reconciliation.reconciliation.service.transactionsearch;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.TranQueryResult;
import com.jpb.reconciliation.reconciliation.dto.TranSearchReqDto;
import com.jpb.reconciliation.reconciliation.dto.TranSearchResponse;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.entity.ReportEntity;
import com.jpb.reconciliation.reconciliation.entity.TransactionConfigEntity;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;
import com.jpb.reconciliation.reconciliation.repository.ReportRepository;
import com.jpb.reconciliation.reconciliation.repository.TransactionConfigRepository;

@Service
public class TranSearchServiceImpl implements TranSearchService {
	private final Logger logger = LoggerFactory.getLogger(TranSearchServiceImpl.class);

	@Value("${app.transactionFile}")
	private String outputDirPath;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ReconTemplateDetailsRepository reconTemplateDetailsRepository;

	@Autowired
	ReconProcessDefMasterRepository reconProcessDefMasterRepository;

	@Autowired
	private TransactionConfigRepository transactionConfigRepository;

	@Autowired
	private ReconFileDetailsMasterRepository reconFileDetailsMasterRepository;

	@Autowired
	private ReportRepository reportRepository;

	private final String TRAN_SEQ_NUM = "TRAN_SEQ_NUM";
	private final String TRAN_DATE = "TRAN_DATE";

	/**
	 * This method is the entry point and used to retrieve the transaction records
	 *
	 * @param tranSearchReqDto
	 * @return
	 */
	@Override
	public ResponseEntity<TranSearchResponse> getTransactionRecords(MultipartFile file,
			TranSearchReqDto tranSearchReqDto) {
		TranSearchResponse TranSearchResponse;
		logger.info("TranSearchServiceImpl.getTransactionRecords() execution Started");
		long tempId = tranSearchReqDto.getTempId();

		ReconTemplateDetails reconTemplateDetails = reconTemplateDetailsRepository.findByReconTemplateId(tempId);
		if (!Objects.nonNull(reconTemplateDetails)) {
			TranSearchResponse = new TranSearchResponse("FAILED", "Records Not Found", null, new ArrayList<>());
			return new ResponseEntity<>(TranSearchResponse, HttpStatus.NOT_FOUND);
		}
		String stageTblName = reconTemplateDetails.getStageTabName();
		String updatedTblName = replaceTrailingTWithAll(stageTblName);
		TransactionConfigEntity configDetailsLst = transactionConfigRepository.findByTemplateId(tempId);
		if (!Objects.nonNull(configDetailsLst)) {
			TranSearchResponse = new TranSearchResponse("FAILED", "Records Not Found", null, new ArrayList<>());
			return new ResponseEntity<>(TranSearchResponse, HttpStatus.NOT_FOUND);
		}
		logger.info("configDetailsLst {}", configDetailsLst);
		String colNames = configDetailsLst.getColumnNames();
		TranQueryResult result = createDynamicQuery(file, colNames, updatedTblName, tranSearchReqDto);
		List<Map<String, Object>> lst = new ArrayList<>();
		String location = null;
		if (Objects.nonNull(result)) {
			lst = result.getRows();
			location = result.getFileLocation();
		}

		logger.info("Dynamic columns {}", colNames);

		TranSearchResponse = new TranSearchResponse(CommonConstants.SUCCESS, "Records Found", location, new ArrayList<>(lst));
		logger.info("TranSearchServiceImpl.getTransactionRecords() execution end");
		return new ResponseEntity<>(TranSearchResponse, HttpStatus.OK);
	}

	/**
	 * This method is a helper method to create the dynamic query
	 * 
	 * @param colNames
	 * @param updatedTblName
	 * @param tranSearchReqDto
	 * @return
	 */
	private TranQueryResult createDynamicQuery(MultipartFile file, String colNames, String updatedTblName,
			TranSearchReqDto tranSearchReqDto) {
		String refNumber = tranSearchReqDto.getReferenceNumber();
		boolean searchByFile = false;
		if (refNumber == null || refNumber.trim().isEmpty()) {
			if (file != null && !file.isEmpty()) {
				List<String> references = extractReferencesFromFile(file);
				if (!references.isEmpty()) {
					refNumber = references.stream().map(r -> "'" + r.trim() + "'").collect(Collectors.joining(","));
					logger.info("Refrenece numbers extract from file : {}", refNumber);
					searchByFile = true;
				} else {
					logger.warn("Uploaded file is empty or contains no valid reference numbers.");
				}
			}
		} else {
			logger.info("Using reference number(s) from DTO: {}", refNumber);
		}

		Date toDate = tranSearchReqDto.getToDate();
		Date fromDate = tranSearchReqDto.getFromDate();
		StringBuilder dynamicQuery = new StringBuilder();
		dynamicQuery.append("SELECT ").append(colNames).append(" FROM ").append(updatedTblName).append(" WHERE 1=1");
		if (refNumber != null && !refNumber.trim().isEmpty()) {
			dynamicQuery.append(" AND ");

			if (refNumber.contains(",")) {
				// Multiple RRNs: Use IN
				dynamicQuery.append(TRAN_SEQ_NUM).append(" IN (").append(refNumber).append(")");
			} else {
				// Single RRN: Use =
				dynamicQuery.append(TRAN_SEQ_NUM).append(" = ").append(refNumber);
			}
		}
		if (fromDate != null && toDate != null) {
			dynamicQuery.append(" AND ");

			// Use BETWEEN for the date range
			dynamicQuery.append(TRAN_DATE).append(" BETWEEN (TO_DATE('").append(fromDate)
					.append("', 'YYYY-MM-DD')) AND (TO_DATE('").append(toDate).append("', 'YYYY-MM-DD'))");
		}

		String finalQuery = dynamicQuery.toString();
		logger.info("Executing Dynamic Query: {}", finalQuery);
		List<Map<String, Object>> maps = jdbcTemplate.queryForList(finalQuery);
		String filName = null;
		if (!maps.isEmpty() && maps != null) {
			filName = maps.get(0).get("FILE_NAME").toString();
		}

		String severFileName = generateCsvFromQuery(maps, filName, outputDirPath);
		String fileLocation = outputDirPath + severFileName;
		ReportEntity newReport = new ReportEntity();
		newReport.setReportDate(LocalDate.now());
		newReport.setProcessId(tranSearchReqDto.getProcessId());
		newReport.setFileName(severFileName);
		newReport.setReportFileName(severFileName);
		newReport.setReportLocation(fileLocation);
		newReport.setReportName(severFileName);
		reportRepository.save(newReport);
		logger.info("FINAL MAP VALUES  {}", maps);
		logger.info("TranSearchServiceImpl.createDynamicQuery() execution end");
		return new TranQueryResult(fileLocation, maps);
	}

	private List<String> extractReferencesFromFile(MultipartFile file) {
		List<String> referenceNumbers = new ArrayList<>();
		try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {

			Sheet sheet = workbook.getSheetAt(0);
			for (Row currentRow : sheet) {

				Cell cell = currentRow.getCell(0);
				if (cell != null) {
					if (cell.getCellType() != CellType.STRING) {
						cell.setCellType(CellType.STRING);
					}
					String refNum = cell.getStringCellValue().trim();
					if (!refNum.isEmpty()) {
						referenceNumbers.add(refNum);
					}
				}
			}
		} catch (IOException e) {
			logger.error("IO Error processing uploaded Excel file", e);
		} catch (Exception e) {
			logger.error("General error processing uploaded Excel file", e);
		}

		return referenceNumbers;
	}

	/**
	 * Generates the csv file and store on server
	 *
	 * @param dataList
	 * @param baseFileName
	 * @param outputDirPath
	 * @return
	 */
	private String generateCsvFromQuery(List<Map<String, Object>> dataList, String baseFileName, String outputDirPath) {
		logger.info("TranSearchServiceImpl.generateCsvFromQuery() execution start");
		if (dataList == null || dataList.isEmpty()) {
			throw new IllegalArgumentException("Data list cannot be null or empty.");
		}
		String fileName = null;
		try {
			Files.createDirectories(Paths.get(outputDirPath));
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String regex = "_\\d{2}-\\d{2}-\\d{4}\\.csv$";
			String updatedName = baseFileName.replaceAll(regex, "");
			fileName = updatedName + "_" + timestamp + ".csv";
			Path filePath = Paths.get(outputDirPath, fileName);
			try (FileWriter writer = new FileWriter(filePath.toFile())) {
				// Write header
				Map<String, Object> firstRow = dataList.get(0);
				writer.write(String.join(",", firstRow.keySet()));
				writer.write("\n");

				// Write data rows
				for (Map<String, Object> row : dataList) {
					StringBuilder sb = new StringBuilder();
					for (Object value : row.values()) {
						String cell = (value == null) ? "" : value.toString().replace("\n", " ").replace("\r", " ");
						sb.append(cell).append(",");
					}
					if (sb.length() > 0)
						sb.setLength(sb.length() - 1);
					writer.write(sb.toString());
					writer.write("\n");
				}
				writer.flush();
			}

		} catch (IOException e) {
			throw new RuntimeException("Error generating CSV file", e);
		}
		logger.info("TranSearchServiceImpl.generateCsvFromQuery() execution end");
		return fileName;
	}

	private String replaceTrailingTWithAll(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}
		if (input.endsWith("_T")) {
			return input.substring(0, input.length() - 2) + "_ALL";
		} else {
			return input;
		}
	}

	@Override
	public ResponseEntity<RestWithMapStatusList> searchReconTransactionRecords(TranSearchReqDto tranSearchReqDto) {
		RestWithMapStatusList restWithStatusList;

		try {
			Map<String, List<Map<String, Object>>> finalTransactionMap = new HashMap<>();

			// 1. Get Recon Process By Process Id
			ReconProcessDefMaster getReconProcess = reconProcessDefMasterRepository
					.findByReconProcessId(tranSearchReqDto.getProcessId());

			if (getReconProcess != null) {
				String processFileName1, processFileName2, processFileName3;
				// --- Process Table 1 ---
				ReconFileDetailsMaster firstTable = reconFileDetailsMasterRepository
						.findByReconTemplateDetails_ReconTemplateId(getReconProcess.getReconTemp1());

				// Null check for lookup results is crucial here
				if (firstTable == null) {
					processFileName1 = "FILE_1_NOT_FOUND";
					logger.warn(
							"ReconFileDetailsMaster 1 not found for template ID: " + getReconProcess.getReconTemp1());
					finalTransactionMap.put(processFileName1, Collections.emptyList());
				} else {
					processFileName1 = firstTable.getReconFileName();
					String tableQuery1 = "select * from " + getReconProcess.getReconDataTableName1()
							+ " where TRAN_SEQ_NUM = " + tranSearchReqDto.getReferenceNumber();
					List<Map<String, Object>> tableQuery1Result = jdbcTemplate.queryForList(tableQuery1);
					logger.info("tableQuery1Result" + tableQuery1Result);

					if (!tableQuery1Result.isEmpty()) {
						finalTransactionMap.put(processFileName1, tableQuery1Result);
					} else {
						finalTransactionMap.put(processFileName1, Collections.emptyList());
					}
				}

				// --- Process Table 2 ---
				ReconFileDetailsMaster secondTable = reconFileDetailsMasterRepository
						.findByReconTemplateDetails_ReconTemplateId(getReconProcess.getReconTemp2());

				if (secondTable == null) {
					processFileName2 = "FILE_2_NOT_FOUND";
					logger.warn(
							"ReconFileDetailsMaster 2 not found for template ID: " + getReconProcess.getReconTemp2());
					finalTransactionMap.put(processFileName2, Collections.emptyList());
				} else {
					processFileName2 = secondTable.getReconFileName();
					String tableQuery2 = "select * from " + getReconProcess.getReconDataTableName2()
							+ " where TRAN_SEQ_NUM = " + tranSearchReqDto.getReferenceNumber();
					List<Map<String, Object>> tableQuery2Result = jdbcTemplate.queryForList(tableQuery2);
					logger.info("tableQuery2Result" + tableQuery2Result);

					if (!tableQuery2Result.isEmpty()) {
						finalTransactionMap.put(processFileName2, tableQuery2Result);
					} else {
						finalTransactionMap.put(processFileName2, Collections.emptyList());
					}
				}

				// --- Process Table 3 ---
				ReconFileDetailsMaster thirdTable = reconFileDetailsMasterRepository
						.findByReconTemplateDetails_ReconTemplateId(getReconProcess.getReconTemp3());

				if (thirdTable == null) {
					processFileName3 = "FILE_3_NOT_FOUND";
					logger.warn(
							"ReconFileDetailsMaster 3 not found for template ID: " + getReconProcess.getReconTemp3());
					finalTransactionMap.put(processFileName3, Collections.emptyList());
				} else {
					processFileName3 = thirdTable.getReconFileName();
					String tableQuery3 = "select * from " + getReconProcess.getReconDataTableName3()
							+ " where TRAN_SEQ_NUM = " + tranSearchReqDto.getReferenceNumber();
					List<Map<String, Object>> tableQuery3Result = jdbcTemplate.queryForList(tableQuery3);
					logger.info("tableQuery3Result" + tableQuery3Result);

					finalTransactionMap.put(processFileName3, tableQuery3Result);

				}

				// Successful response
				restWithStatusList = new RestWithMapStatusList(CommonConstants.SUCCESS, "Transaction records fetched successfully.",
						finalTransactionMap);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);

			} else {
				restWithStatusList = new RestWithMapStatusList(CommonConstants.FAILURE,
						"Reconciliation Process Configuration Not Found.", null);
				return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
			}

		} catch (DataAccessException dae) {
			logger.error("Database error while searching for transactions: ", dae);
			restWithStatusList = new RestWithMapStatusList(CommonConstants.FAILURE,
					"A database error occurred during transaction search.", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);

		} catch (Exception e) {
			logger.error("An unexpected error occurred: ", e);
			restWithStatusList = new RestWithMapStatusList("ERROR",
					"An unexpected server error occurred: " + e.getMessage(), null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
