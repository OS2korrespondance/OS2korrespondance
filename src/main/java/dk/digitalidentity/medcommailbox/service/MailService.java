package dk.digitalidentity.medcommailbox.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.lowagie.text.DocumentException;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import jakarta.transaction.Transactional;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dk.digitalidentity.medcommailbox.config.FolderConstants;
import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.dao.MailDao;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import dk.digitalidentity.medcommailbox.datatables.MailDatatableDao;
import dk.digitalidentity.medcommailbox.datatables.model.MailboxDatatableDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import static org.springframework.data.jpa.domain.Specification.where;

@Service
@Slf4j
public class MailService {
	@Autowired
	private MedcomMailboxConfiguration configuration;

	@Autowired
	private MailDao mailDao;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private MedcomMailboxConfiguration config;

	@Autowired
	private TemplateEngine templateEngine;

	@Autowired
	private MailDatatableDao mailDatatableDao;

	private static final Pattern INVALID_XML_CHARS =
			Pattern.compile("[^\t\n\r\u0020-\uD7FF\uE000-\uFFFD]+");


	/**
	 * Checks if the current user is constrained from seeing the mail.
	 *
	 * @throws ResponseStatusException if access is not allowed
	 */
	public void hasAccess(Mail mail) {
		Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName());
		if (constraints == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No location number constraints found");
		}
		if (!constraints.isEmpty() && constraints.stream().noneMatch(c -> mail.getAssociatedIdentifier().contains(c))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}


	public List<Mail> getByFolder(Folder folder) {
		return mailDao.findByFolderAndDraftFalseOrderByReceivedDesc(folder);
	}

	public DataTablesOutput<MailboxDatatableDTO> getByFolder(DataTablesInput datatableInput, Folder folder, Long inboxFolderId) {
		return mailDatatableDao.findAll(datatableInput,
				where(MailDatatableDao.inFolder(folder)
						.and(where(MailDatatableDao.withInboxFolderId(inboxFolderId)))));
	}

	public DataTablesOutput<MailboxDatatableDTO> getByFolder(DataTablesInput datatableInput, Folder folder) {
		return mailDatatableDao.findAll(datatableInput,
				where(MailDatatableDao.inFolder(folder))
						.and(MailDatatableDao.inboxFolderNull()));
	}

	public long getCountByReadAndFolder(boolean read, Folder folder) {
		return mailDao.countByReadAndFolderAndDraftFalse(read, folder);
	}

	public long getCountByFolderAndNotDraft(Folder folder) {
		return mailDao.countByFolderAndDraftFalse(folder);
	}

	public long getCountByFolderAndReceivedNegativeReceipt(Folder folder) {
		return mailDao.countByFolderAndDraftFalseAndReceivedNegativeReceiptTrue(folder);
	}

	public Mail getById(long id) {
		return mailDao.findById(id);
	}

	public Mail save(Mail mail) {
		return mailDao.save(mail);
	}

	public List<Mail> findByOriginalFolderAndDraftFalseAndS3FileKeyNotNull(final Folder folder) {
		return mailDao.findByOriginalFolderAndDraftFalseAndS3FileKeyNotNull(folder);
	}

	public Mail findIncomingByEnvelopeIdentifierAndLetterIdentifier(String envelopeIdentifier, String letterIdentifier, boolean incoming) {
		return mailDao.findByEnvelopeIdentifierAndLetterIdentifierAndIncoming(envelopeIdentifier, letterIdentifier, incoming);
	}

	@Transactional
	public void cleanUp() {
		long count = mailDao.deleteByDraftTrueAndCreatedBefore(LocalDateTime.now().minusDays(1));
		log.info("Deleted {} old mail drafts", count);
	}

	@Transactional
	public void deleteMails() {
		List<Mail> mailsToDelete = mailDao.findByDeletedDateBeforeAndFolder(LocalDate.now().minusDays(config.getDaysBeforeDeletion()), Folder.DELETED);
		for (Mail mail : mailsToDelete) {
			if (mail.getS3FileKey() != null) {
				s3Service.delete(mail.getS3FileKey());
			}
		}
		int count = mailsToDelete.size();
		mailDao.deleteAll(mailsToDelete);
		log.info("Deleted {} mails from deleted folder", count);
	}

	public void toArchive(Mail mail, String name, byte[] xml) {
		if (!configuration.isCreateArchives()) {
			log.info("Not creating archive, feature disabled");
		}
		try {
			String html = getPdfHtml(mail);
			byte[] pdf = convertHtmlToPdf(html);
			s3Service.upload(FolderConstants.FOLDER_ARCHIVE, name + ".pdf", pdf);
			s3Service.upload(FolderConstants.FOLDER_ARCHIVE, name + ".xml", xml);
		} catch (Exception e) {
			log.error("Could not upload copy to archive for mail with id {}", mail.getId(), e);
		}
	}

	private String sanitizeContent(String content) {
		if (content == null) return "";
		return INVALID_XML_CHARS.matcher(content).replaceAll("");
	}

	private String getPdfHtml(Mail mail) {
		var context = new Context();
		String safeContent = sanitizeContent(mail.getContent());
		String escapedContent = StringEscapeUtils.escapeHtml4(safeContent).replace("\n", "<br/>");
		context.setVariable("mail", mail);
		context.setVariable("content", escapedContent);
		return templateEngine.process("pdf/mail_pdf", context);
	}

	private byte[] convertHtmlToPdf(String html) throws DocumentException, IOException {
		var outputStream = new ByteArrayOutputStream();
		var renderer = new ITextRenderer();
		renderer.setDocumentFromString(html);
		renderer.layout();
		renderer.createPDF(outputStream);
		var result = outputStream.toByteArray();
		outputStream.close();
		return result;
	}
}