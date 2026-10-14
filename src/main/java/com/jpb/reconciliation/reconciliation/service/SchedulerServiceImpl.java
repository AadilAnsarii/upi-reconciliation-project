package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling; // Add this
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.controller.ExtractionController;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconProcessDefMaster;
import com.jpb.reconciliation.reconciliation.entity.SchedulerJob;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.SchedulerRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;

import net.sf.jasperreports.engine.JRException;

@Service
@EnableScheduling
public class SchedulerServiceImpl implements SchedulerService {

	@Autowired
	TaskScheduler taskScheduler;

	@Autowired
	ReconFileDetailsMasterRepository reconFileDetailsMasterRepository;

	@Autowired
	ExtractionService extractionService;

	private ConcurrentHashMap<Long, ScheduledFuture<?>> runningScheduledFutures = new ConcurrentHashMap<>();

	@Autowired
	SchedulerRepository schedulerRepository;

	@Autowired
	ReconUserRepository reconUserRepository;

	@Autowired
	ExtractionController extractionController;

	@Autowired
	ReconciliationService reconciliationService;

	@Autowired
	ReconProcessDefMasterRepository reconProcessDefMasterRepository;

	@Autowired
	AuditLogManagerService auditLogManagerService;

	Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@PostConstruct
	public void init() {
		scheduleAllJobsFromDatabase();
	}

	@Override
	public void scheduleTasks() {
		logger.info(
				"SchedulerServiceImpl.scheduleTasks() - This method should only be called once via @PostConstruct or on demand.");
	}

	public void scheduleAllJobsFromDatabase() {
		List<SchedulerJob> jobs = schedulerRepository.findAll();
		logger.info("Found {} jobs from database for initial scheduling.", jobs.size());

		jobs.forEach(job -> {
			if (!runningScheduledFutures.containsKey(job.getScheduleId())) {
				try {
					scheduleSingleJob(job);
					logger.info("Successfully scheduled job with ID: {}", job.getScheduleId());
				} catch (Exception e) {
					logger.error("Error scheduling job with ID: {}. Error: {}", job.getScheduleId(), e.getMessage(), e);
				}
			} else {
				logger.info("Job with ID: {} is already scheduled. Skipping.", job.getScheduleId());
			}
		});
	}

	private void scheduleSingleJob(SchedulerJob job) {
		LocalDateTime executionTime = job.getSchedulerTime();
		String dayOfWeek = job.getDays();

		String cronExpression;
		if ("ALL".equalsIgnoreCase(dayOfWeek)) {

			cronExpression = String.format("0 %d %d * * ?", executionTime.getMinute(), executionTime.getHour());
		} else {

			DayOfWeek day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
			int cronDayOfWeek = day.getValue() + 1;
			if (day == DayOfWeek.SUNDAY) {
				cronDayOfWeek = 1;
			} else {
				cronDayOfWeek = day.getValue() + 1;
			}
			cronExpression = String.format("0 %d %d ? * %d", executionTime.getMinute(), executionTime.getHour(),
					cronDayOfWeek);
		}

		logger.info("Scheduling job ID: {} with cron expression: {}", job.getScheduleId(), cronExpression);

		ScheduledFuture<?> future = taskScheduler.schedule(() -> executeJob(job),
				new org.springframework.scheduling.support.CronTrigger(cronExpression));
		runningScheduledFutures.put(job.getScheduleId(), future);
	}

	private void executeJob(SchedulerJob job) {
		logger.info("Executing job :::::::::::::::::: [ScheduleId: {}, FileId: {}]", job.getScheduleId(),
				job.getFileId());
		Long userId = new Long(job.getInsUser());
		Long processId = new Long(job.getFileId());
		ReconUser userData = reconUserRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Scheduled job user not found: " + userId));
		if (job.getScheduleType().equalsIgnoreCase("Extraction")) {
			ReconFileDetailsMaster reconFileDetails = reconFileDetailsMasterRepository.findByReconFileId(processId);
			logger.info("FILE DETAILS WITH TEMPLATE DETAILS :::::::::::::::::" + reconFileDetails);

			List<File> fileList = extractionController
					.getFileListFromDirectory(reconFileDetails);

			List<ReconBatchProcessEntity> checkProcessIsRunning = reconBatchProcessEntityRepository
					.findByProcessIdAndStatus(processId, "Running");
			if (!checkProcessIsRunning.isEmpty()) {
				logger.info("Extraction process file is running for job ID: {}", job.getScheduleId());
				return;
			}

			List<ReconBatchProcessEntity> checkProcessCompleted = reconBatchProcessEntityRepository
					.findByProcessIdAndStatus(processId, "Completed");

			Set<String> completedFileNames = checkProcessCompleted.stream().map(ReconBatchProcessEntity::getFileName)
					.collect(Collectors.toSet());
			logger.info("COMPLETED FILE NAMES :::::::::::" + completedFileNames);
			boolean checkFileIsCompleted = fileList.stream()
					.anyMatch(file -> completedFileNames.contains(file.getName()));
			logger.info("STATUS:::::::::::::" + checkFileIsCompleted);
			if (checkFileIsCompleted) {
				logger.info(
						"Extraction process file from the source directory has already been processed for job ID: {}.",
						job.getScheduleId());
				return;
			}

			if (reconFileDetails != null) {
				if (!fileList.isEmpty()) {
					List<ReconBatchProcessEntity> runningExtraction = extractionService
							.extractionRunningStatus(fileList, reconFileDetails, userData);
					logger.info("RUNNING EXTRACTION ::::::::::::::" + runningExtraction);
					if (!runningExtraction.isEmpty()) {
						CompletableFuture<String> extractionStatus;
						try {
							extractionStatus = extractionService.startExtraction(reconFileDetails, runningExtraction,
									fileList, userData);
							logger.info("EXTRACTION STATUS ::::::::::::::" + extractionStatus);
						} catch (IOException | InterruptedException | JRException e) {
							logger.error("Error during extraction for job ID: {}. Error: {}", job.getScheduleId(),
									e.getMessage(), e);
							e.printStackTrace();
						}
					}
				} else {
					logger.info("File not found for given file path location for job ID: {}", job.getScheduleId());
				}
			}
		} else {
			ReconProcessDefMaster reconProcessDefMaster = reconProcessDefMasterRepository
					.findByReconProcessId(processId);
			if (reconProcessDefMaster != null) {
				List<ReconBatchProcessEntity> reconciliationStatus = reconciliationService.runReconciliation(processId,
						reconProcessDefMaster, userData);
				if (!reconciliationStatus.isEmpty()) {
					CompletableFuture<String> reconStatus;
					try {
						reconStatus = reconciliationService.startReconciliation(processId, reconciliationStatus,
								reconProcessDefMaster, userData);
						logger.info("RECONCILIATION STATUS ::::::::::::::" + reconStatus);

					} catch (IOException e) {
						logger.error("Error during reconciliation for job ID: {}. Error: {}", job.getScheduleId(),
								e.getMessage(), e);
						e.printStackTrace();
					}
				}

			}
		}
	}
}