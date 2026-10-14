package com.jpb.reconciliation.reconciliation.service.excelreader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExcelToCsvConvertorService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelToCsvConvertorService.class);

    /**
     * V1 compatibility overload — untouched original hardcoded-header-name
     * behaviour, unchanged, so ExtractionController.java (V1) keeps working
     * exactly as before. V1 must never call the new headerLineCount-based
     * overload below; that one is V2-only (see ExtractionAiController.java).
     */
    public void convertExcelToCsv(File excelFile) throws IOException {
        String csvFilePath = excelFile.getPath().replaceFirst("\\..*", ".csv");

        try (Workbook workbook = WorkbookFactory.create(excelFile);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            boolean startProcessing = false;
            int columnCount = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Find the header row "Description" to start processing
                if (!startProcessing) {
                    Cell firstCell = row.getCell(0);
//                    && "Description".equalsIgnoreCase(getCellValueAsString(firstCell).trim())
                    if (firstCell != null) {
                        String cellVal = getCellValueAsString(firstCell).trim();
                        // PEHLE: condition commented out thi → pehli non-empty row header ban jaati thi
                        //        (metadata row 1 header, rows 2-11 metadata data ban jaata tha → SQL*Loader fail)
                        // AB:    sirf matching column-name wali row header manate hain → metadata rows skip
                        if (cellVal.equalsIgnoreCase("S.NO")
                                || cellVal.equalsIgnoreCase("SNO")
                                || cellVal.equalsIgnoreCase("SR_NO")
                                || cellVal.equalsIgnoreCase("Description")) {
                            startProcessing = true;
                            columnCount = row.getLastCellNum();
                            writeRowToCsv(row, columnCount, writer, false);
                        }
                    }
                    continue;
                }
                if (isRowEmpty(row) || isFooterRow(row)) {
                    continue;
                }

                writeRowToCsv(row, columnCount, writer, false);
            }

        } catch (Exception e) {
            logger.error("Error converting Excel to CSV", e);
            throw new IOException("Failed to convert Excel file.", e);
        }
    }

    /**
     * V2-only overload — see FILE 4 in Code_Changes_LineByLine.txt for why
     * this exists separately from the V1 method above.
     */
    public void convertExcelToCsv(File excelFile, Integer headerLineCount) throws IOException {
        String csvFilePath = excelFile.getPath().replaceFirst("\\..*", ".csv");

        // Number of leading rows in the RAW excel file to skip before the real header
        // row is reached (report title / bank name / search-criteria rows etc. that
        // some source systems put above the actual column header — e.g. NEFT-ISO
        // reports have 11 such rows before the header). Template-configured, not
        // guessed from the header text, so it works for any file's header naming.
        int rowsToSkip = (headerLineCount != null && headerLineCount > 0) ? headerLineCount - 1 : 0;

        try (Workbook workbook = WorkbookFactory.create(excelFile);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            int rowIndex = 0;
            boolean headerWritten = false;
            int columnCount = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                if (rowIndex < rowsToSkip) {
                    rowIndex++;
                    continue;
                }
                if (!headerWritten) {
                    // This is the real header row (row after the configured skip count).
                    // Written as-is to the CSV — the downstream SQL*Loader control file
                    // always uses skip=1 to skip exactly this one header line.
                    columnCount = row.getLastCellNum();
                    writeRowToCsv(row, columnCount, writer, true);
                    headerWritten = true;
                    rowIndex++;
                    continue;
                }
                if (isRowEmpty(row) || isFooterRow(row)) {
                    rowIndex++;
                    continue;
                }

                writeRowToCsv(row, columnCount, writer, true);
                rowIndex++;
            }

        } catch (Exception e) {
            logger.error("Error converting Excel to CSV", e);
            throw new IOException("Failed to convert Excel file.", e);
        }
    }

    /**
     * Writes exactly columnCount comma-separated fields, one per column index.
     * Row.cellIterator() (the old approach) only visits cells POI actually
     * instantiated — a blank cell in the middle of a row (common for sparse
     * data written by tools like openpyxl) can be entirely absent from the
     * row's cell list, silently dropping that column and shifting every
     * later field left by one. Iterating by fixed index and reading each
     * cell with getCell() (returns null for missing/blank, rendered as "")
     * guarantees column alignment regardless of which cells exist.
     */
    /**
     * @param escape true → RFC-4180 quoting (V2 only). false → original raw join,
     *               kept byte-for-byte for the V1 overload, which must not change.
     */
    private void writeRowToCsv(Row row, int columnCount, PrintWriter writer, boolean escape) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < columnCount; i++) {
            Cell cell = row.getCell(i);
            String value = getCellValueAsString(cell);
            line.append(escape ? toCsvField(value) : value);
            if (i < columnCount - 1) {
                line.append(",");
            }
        }
        writer.println(line.toString());
    }

    /**
     * Renders one cell as a CSV field that SQL*Loader can read back.
     *
     * Cell values were previously written raw, so any cell containing a comma, a double
     * quote or a line break silently corrupted the file: an Excel sheet whose column held
     * a multi-line block (an ATM journal, an address) produced one physical line per
     * embedded newline, so a 1,411-row sheet came out as 12,959 lines with 1, 5 or 34
     * fields instead of 38. SQL*Loader then read misaligned values, rejected rows with
     * "Field in data file exceeds maximum length", and aborted at its 50-error limit.
     *
     * Line breaks are collapsed to a single space rather than quoted: the generated
     * control file loads via the direct path, which treats every physical line as one
     * record, so an embedded newline would break the row even inside quotes. Collapsing
     * keeps the record intact — the loader's field enclosure O(") handles the rest.
     */
    private String toCsvField(String value) {
        if (null == value || value.isEmpty()) {
            return "";
        }
        String flat = value.replaceAll("\\r\\n|[\\r\\n]", " ");
        if (flat.indexOf(',') < 0 && flat.indexOf('"') < 0) {
            return flat;
        }
        return "\"" + flat.replace("\"", "\"\"") + "\"";
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && cell.toString().trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isFooterRow(Row row) {
        if (row == null) {
            return false;
        }
        Cell firstCell = row.getCell(0);
        if (firstCell != null && firstCell.getCellType() == CellType.STRING) {
       	 return firstCell.getStringCellValue().trim().startsWith("Note:") ||
                    firstCell.getStringCellValue().trim().startsWith("1.") ||
                    firstCell.getStringCellValue().trim().startsWith("2.") ||
                    firstCell.getStringCellValue().trim().startsWith("3.");
       }
        return false;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    return String.valueOf((long) value);
                } else {
                    return String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
}