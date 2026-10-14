package com.jpb.reconciliation.reconciliation.service.xmlreader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconTmpltFieldDtls;

/**
 * Converts a template-configured XML file into a flat CSV file, the same way
 * ExcelToCsvConvertorService turns an .xlsx into a CSV. Once converted, the
 * existing delimited/CSV extraction pipeline (control-file generation,
 * SQL*Loader, segregation) handles it with no further changes — this class
 * only has to solve "how do I read this XML shape", not re-implement loading.
 */
@Service
public class XmlToCsvConvertorService {

    private static final Logger logger = LoggerFactory.getLogger(XmlToCsvConvertorService.class);

    public void convertXmlToCsv(File xmlFile, String rootTag, String rowTag,
            List<ReconTmpltFieldDtls> fieldDetails) throws IOException {

        String csvFilePath = xmlFile.getPath().replaceFirst("\\..*", ".csv");

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
            Document document = parseSecurely(xmlFile);

            if (rowTag == null || rowTag.trim().isEmpty()) {
                throw new IOException("Template's xmlRowTag is not configured — cannot locate transaction rows.");
            }

            NodeList rowNodes = document.getElementsByTagName(rowTag.trim());
            logger.info("XML rows found for tag '{}': {}", rowTag, rowNodes.getLength());

            writeHeaderRow(fieldDetails, writer);

            for (int i = 0; i < rowNodes.getLength(); i++) {
                Node node = rowNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                writeDataRow((Element) node, fieldDetails, writer);
            }

        } catch (ParserConfigurationException | SAXException e) {
            logger.error("Error converting XML to CSV", e);
            throw new IOException("Failed to convert XML file.", e);
        }
    }

    /**
     * DocumentBuilderFactory is vulnerable to XXE (external entity injection) by
     * default — disable external DTDs/entities since this parses user-supplied files.
     */
    private Document parseSecurely(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();
        return document;
    }

    private void writeHeaderRow(List<ReconTmpltFieldDtls> fieldDetails, PrintWriter writer) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fieldDetails.size(); i++) {
            line.append(fieldDetails.get(i).getShortName());
            if (i < fieldDetails.size() - 1) {
                line.append(",");
            }
        }
        writer.println(line.toString());
    }

    private void writeDataRow(Element rowElement, List<ReconTmpltFieldDtls> fieldDetails, PrintWriter writer) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fieldDetails.size(); i++) {
            String value = resolveXmlFieldValue(rowElement, fieldDetails.get(i).getXmlXpath());
            line.append(value);
            if (i < fieldDetails.size() - 1) {
                line.append(",");
            }
        }
        writer.println(line.toString());
    }

    /**
     * Resolves a field's value from a row element using a simple tag-name or
     * slash-separated relative path (e.g. "TransactionDate" or "Header/TxnDate").
     * Only walks direct child elements at each path segment — good enough for
     * the flat/lightly-nested transaction records these files actually contain,
     * without needing a full XPath engine.
     */
    private String resolveXmlFieldValue(Element rowElement, String xmlXpath) {
        if (xmlXpath == null || xmlXpath.trim().isEmpty()) {
            return "";
        }

        String[] pathSegments = xmlXpath.trim().split("/");
        Element current = rowElement;

        for (int i = 0; i < pathSegments.length; i++) {
            Element next = findFirstChildElement(current, pathSegments[i].trim());
            if (next == null) {
                return "";
            }
            current = next;
        }

        return current.getTextContent() == null ? "" : current.getTextContent().trim();
    }

    private Element findFirstChildElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equalsIgnoreCase(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }
}
