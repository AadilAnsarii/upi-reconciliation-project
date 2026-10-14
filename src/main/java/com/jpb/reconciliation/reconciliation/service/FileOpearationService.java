package com.jpb.reconciliation.reconciliation.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.repository.ReconBatchProcessEntityRepository;

@Service
public class FileOpearationService {

	Logger logger = LoggerFactory.getLogger(FileOpearationService.class);

	@Autowired
	ReconBatchProcessEntityRepository reconBatchProcessEntityRepository;

	@Value("${app.targetFilePath}")
	private String destinationDirectoryPath;

	public void moveExtractedFiles(ReconFileDetailsMaster reconFileDetails) {

		Path sourceDir = Paths.get(reconFileDetails.getReconFileDestinationPath());
		Path destinationDir = Paths.get(destinationDirectoryPath);

		String sourceDirectoryName = sourceDir.getFileName().toString();

		Path finalDestinationDir = destinationDir.resolve(sourceDirectoryName);

		List<ReconBatchProcessEntity> completedProcesses = reconBatchProcessEntityRepository
				.findByProcessIdAndStatus(reconFileDetails.getReconFileId(), "Completed");
		Set<String> completedFileNames = completedProcesses.stream().map(ReconBatchProcessEntity::getFileName)
				.collect(Collectors.toSet());

		try {
			Files.createDirectories(finalDestinationDir);
			logger.info("Ensured destination directory exists: " + finalDestinationDir);

			if (!Files.isDirectory(sourceDir)) {
				logger.info("Error: Source path is not a directory. Expected a directory to move files from: "
						+ sourceDirectoryName);
				return;
			}

			try (Stream<Path> files = Files.list(sourceDir)) {
				files.forEach(file -> {
					if (Files.isRegularFile(file)) {
						String fileName = file.getFileName().toString();
						if (completedFileNames.contains(fileName)) {
							Path targetFile = finalDestinationDir.resolve(fileName);

							try {
								// Attempt 1: Fast Atomic Move
								Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING,
										StandardCopyOption.ATOMIC_MOVE);
								logger.info("File moved successfully via ATOMIC_MOVE: " + file + " to " + targetFile);

							} catch (IOException e) {
								if (e.getMessage() != null && e.getMessage().contains("Invalid cross-device link")) {
									logger.warn("Cross-device move detected for " + file
											+ ". Falling back to Copy-and-Delete.");
									try {
										// Fallback: Step 1 - Copy the file
										Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
										// Fallback: Step 2 - Delete the original file
										Files.delete(file);
										logger.info("File successfully moved via COPY-DELETE fallback: " + file + " to "
												+ targetFile);
									} catch (IOException copyDeleteException) {
										logger.error("FATAL: Failed to move file using COPY-DELETE fallback: " + file,
												copyDeleteException);
									}
								} else {
									logger.error(
											"Error moving file " + file + " (General IOException): " + e.getMessage(),
											e);
								}

							} catch (SecurityException e) {
								logger.error("Permission denied for file " + file + ": " + e.getMessage());
								logger.info(
										"Ensure the Java process has write permissions to the destination directory ("
												+ finalDestinationDir + ")");
							}

						} else {
							System.out.println("Skipping file (not completed): " + fileName);
						}
					} else {
						System.out.println("Skipping non-regular file/directory in source: " + file);
					}
				});
			}

		} catch (IOException e) {
			logger.info("Error creating destination directory or listing source directory: " + e.getMessage());
			e.printStackTrace();
		} catch (SecurityException e) {
			logger.info("Permission denied during directory creation or listing: " + e.getMessage());
			logger.info("Ensure the Java process has necessary permissions for " + destinationDirectoryPath + " and "
					+ sourceDirectoryName);
			e.printStackTrace();
		}
	}

}