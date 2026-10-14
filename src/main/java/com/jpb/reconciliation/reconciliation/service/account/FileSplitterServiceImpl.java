package com.jpb.reconciliation.reconciliation.service.account;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public class FileSplitterServiceImpl implements FileSplitterService {

	@Value("${app.accountSplitPath}")
	private String outputDirPath;

	private final Logger logger = LoggerFactory.getLogger(FileSplitterServiceImpl.class);

	@Override
	public ResponseEntity<RestWithStatusList> generateEachAcknoledgementFiles() {

//		ExecutorService executor = Executors.newFixedThreadPool(20);

//		List<CompletableFuture<Void>> futures = accountNumbers.stream()
//		        .map(accountNumber -> CompletableFuture.runAsync(() -> {
//		            // This is the concurrent task
//		            generateSingleFile(accountNumber);
//		        }, executor))
//		        .collect(Collectors.toList());
//
//		    // Wait for all tasks to complete
//		    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//		    executor.shutdown();
		return null;
	}

	private void generateSingleFile(String accountNumber) {
		// **Implementation of your file generation logic here**
		String fileName = "account_" + accountNumber + ".txt";
//	    try {
//	        // Simulate file generation (replace with actual logic)
//	        Files.writeString(Paths.get(fileName), "Data for account " + accountNumber, StandardOpenOption.CREATE);
//	        System.out.println("Generated file for account: " + accountNumber);
//	    } catch (IOException e) {
//	        // Handle file I/O errors
//	        System.err.println("Error generating file for account " + accountNumber + ": " + e.getMessage());
//	    }
	}

	@Override
	public void splitFileByAccountNumber(MultipartFile file) throws IOException, InterruptedException {
		File inputFile = File.createTempFile("temp-input-", ".csv");
		file.transferTo(inputFile);
		File outputDir = new File(outputDirPath);
		int accountNumberColumnIndex = 3;
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
        
		String awkCommand = String.format(
				"awk -F, 'NR==1{print > \"/dev/null\"; next} {file=\"%s/\"$%d\".csv\"; print > file; close(file)}' %s",
				outputDir.getAbsolutePath(), accountNumberColumnIndex, inputFile.getAbsolutePath());
		// awk command
//		String awkCommand = String.format("awk -F, '{print > \"%s/\"$%d\".csv\"}' %s", outputDir.getAbsolutePath(),
//				accountNumberColumnIndex, inputFile.getAbsolutePath());

		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", awkCommand);
		Process process = processBuilder.start();
		int exitCode = process.waitFor();

		inputFile.delete();
		if (exitCode == 0) {
			System.out.println("File split successfully. Output files created in: " + outputDir);
		} else {
			System.err.println("AWK command failed with exit code: " + exitCode);

		}

	}

}
