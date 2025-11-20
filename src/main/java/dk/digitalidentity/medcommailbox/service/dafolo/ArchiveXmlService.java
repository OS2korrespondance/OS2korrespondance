package dk.digitalidentity.medcommailbox.service.dafolo;


import dk.digitalidentity.medcommailbox.model.entity.Binary;
import dk.digitalidentity.medcommailbox.model.entity.Mail;
import dk.digitalidentity.medcommailbox.service.dafolo.model.Bilag;
import dk.digitalidentity.medcommailbox.service.dafolo.model.BilagListe;
import dk.digitalidentity.medcommailbox.service.dafolo.model.Filarkiv;
import dk.digitalidentity.medcommailbox.service.dafolo.model.Hoveddokument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Service for building archive index.xml structure from Mail objects
 */
@Slf4j
@Service
public class ArchiveXmlService {

	@Autowired
	private ArchiveBinaryService archiveBinaryService;

	/**
	 * Build Filarkiv object for a mail message
	 *
	 * @param mail the mail object containing the message and attachments
	 * @param pdfFileName the name of the generated PDF file (without path)
	 * @return Filarkiv object ready for XML serialization
	 */
	public Filarkiv buildFilarkiv(Mail mail, String pdfFileName) {
		Filarkiv filarkiv = new Filarkiv();

		// Build main document
		Hoveddokument hoveddokument = buildHoveddokument(mail, pdfFileName);
		filarkiv.setHoveddokument(hoveddokument);

		// Build attachment list if there are any binaries
		if (mail.getBinaryMessage() != null && mail.getBinaryMessage().getBinaries() != null
				&& !mail.getBinaryMessage().getBinaries().isEmpty()) {
			BilagListe bilagListe = buildBilagListe(mail);
			filarkiv.setBilagListe(bilagListe);
		}

		return filarkiv;
	}

	/**
	 * Build the Hoveddokument (main document) section
	 *
	 * @param mail the mail object
	 * @param pdfFileName the name of the PDF file
	 * @return Hoveddokument object
	 */
	private Hoveddokument buildHoveddokument(Mail mail, String pdfFileName) {
		Hoveddokument hoveddokument = new Hoveddokument();
		hoveddokument.setFilNavn("./" + pdfFileName);
		hoveddokument.setFilAlternativtNavn(mail.getSubject());
		hoveddokument.setFilMediaType("application/pdf");
		hoveddokument.setFilBeskrivelse(mail.getSubject());

		return hoveddokument;
	}

	/**
	 * Build the BilagListe (attachment list) section
	 *
	 * @param mail the mail object with binaries
	 * @return BilagListe object
	 */
	private BilagListe buildBilagListe(Mail mail) {
		BilagListe bilagListe = new BilagListe();
		List<Binary> binaries = mail.getBinaryMessage().getBinaries();

		for (Binary binary : binaries) {
			Bilag bilag = buildBilag(binary, mail);
			bilagListe.getBilag().add(bilag);
		}

		return bilagListe;
	}

	/**
	 * Build a single Bilag (attachment) entry
	 *
	 * @param binary the binary attachment
	 * @param mail the mail object (for context in description)
	 * @return Bilag object
	 */
	private Bilag buildBilag(Binary binary, Mail mail) {
		Bilag bilag = new Bilag();

		// Build file path: ./vedhaeftninger/{binaryId}/{fileName}
		String fileName = archiveBinaryService.extractFileNameFromS3Key(binary.getS3FileKey());
		String filePath = String.format("./vedhaeftninger/%d/%s", binary.getId(), fileName);
		bilag.setFilNavn(filePath);

		// Alternative name - identifier if available
		if (StringUtils.hasText(binary.getIdentifier())) {
			bilag.setFilAlternativtNavn(binary.getIdentifier());
		}

		bilag.setFilMediaType(determineMediaType(fileName, binary));
		bilag.setFilBeskrivelse(buildAttachmentDescription(mail));

		return bilag;
	}

	/**
	 * Determine media type from file extension and Binary properties
	 *
	 * @param fileName the file name
	 * @param binary the binary object (may contain additional type info)
	 * @return media type string
	 */
	private String determineMediaType(String fileName, Binary binary) {
		String extension = null;

		if (StringUtils.hasText(binary.getExtensionCode())) {
			extension = binary.getExtensionCode().toLowerCase();
		} else if (fileName != null) {
			// Extract extension from filename
			int lastDotIndex = fileName.lastIndexOf('.');
			if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
				extension = fileName.substring(lastDotIndex + 1).toLowerCase();
			}
		}

		if (extension == null) {
			return "application/octet-stream";
		}

		return mapExtensionToMediaType(extension);
	}

	/**
	 * Map file extension to media type
	 *
	 * @param extension the file extension (without dot, lowercase)
	 * @return media type string
	 */
	private String mapExtensionToMediaType(String extension) {
		return switch (extension) {
			// Images
			case "jpg", "jpeg" -> "image/jpg";
			case "png" -> "image/png";
			case "gif" -> "image/gif";
			case "bmp" -> "image/bmp";
			case "tiff", "tif" -> "image/tiff";

			// Documents
			case "pdf" -> "application/pdf";
			case "doc" -> "application/msword";
			case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			case "xls" -> "application/vnd.ms-excel";
			case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

			// Text
			case "txt" -> "text/plain";
			case "xml" -> "application/xml";
			case "html", "htm" -> "text/html";

			// Default
			default -> "application/octet-stream";
		};
	}

	/**
	 * Build description for attachment
	 *
	 * @param mail the mail object
	 * @return description string
	 */
	private String buildAttachmentDescription(Mail mail) {
		String mainDoc = StringUtils.hasText(mail.getSubject()) ? mail.getSubject() : "mail message";
		return String.format("Attachment of %s", mainDoc);
	}

}