package dk.digitalidentity.medcommailbox.service.dafolo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility for building ZIP archives with proper structure for journaling systems
 */
@Slf4j
@Component
public class ArchiveZipBuilder {

	/**
	 * Build a ZIP file with index.xml, PDF, and attachments
	 *
	 * @param indexXml the index.xml content
	 * @param pdfFileName the name of the PDF file (e.g., "besked-123.pdf")
	 * @param pdfContent the PDF file content
	 * @param attachments map of Binary ID to attachment data (key: binaryId, value: file content)
	 * @param attachmentFileNames map of Binary ID to filename (key: binaryId, value: filename)
	 * @return ZIP file as byte array
	 * @throws IOException if ZIP creation fails
	 */
	public byte[] buildArchiveZip(byte[] indexXml, String pdfFileName, byte[] pdfContent,
			Map<Long, byte[]> attachments, Map<Long, String> attachmentFileNames) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			// Add index.xml to root
			addFileToZip(zos, "index.xml", indexXml);

			// Add PDF to root
			addFileToZip(zos, pdfFileName, pdfContent);

			// Add attachments in vedhaeftninger/{binaryId}/
			if (attachments != null && !attachments.isEmpty()) {
				for (Map.Entry<Long, byte[]> entry : attachments.entrySet()) {
					Long binaryId = entry.getKey();
					byte[] fileContent = entry.getValue();
					String fileName = attachmentFileNames.get(binaryId);

					if (fileName != null && fileContent != null) {
						String zipPath = String.format("vedhaeftninger/%d/%s", binaryId, fileName);
						addFileToZip(zos, zipPath, fileContent);
					} else {
						log.warn("Skipping attachment with binaryId {} - missing filename or content", binaryId);
					}
				}
			}
		}

		return baos.toByteArray();
	}

	/**
	 * Add a file to the ZIP output stream
	 *
	 * @param zos the ZipOutputStream
	 * @param path the path within the ZIP (e.g., "index.xml" or "vedhaeftninger/123/file.jpg")
	 * @param content the file content
	 * @throws IOException if writing fails
	 */
	private void addFileToZip(ZipOutputStream zos, String path, byte[] content) throws IOException {
		ZipEntry zipEntry = new ZipEntry(path);
		zos.putNextEntry(zipEntry);
		zos.write(content);
		zos.closeEntry();
	}

}