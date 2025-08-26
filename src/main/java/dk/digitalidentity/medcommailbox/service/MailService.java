package dk.digitalidentity.medcommailbox.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.lowagie.text.DocumentException;
import dk.digitalidentity.medcommailbox.dao.model.InboxFolder;
import dk.digitalidentity.medcommailbox.security.SecurityUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dk.digitalidentity.medcommailbox.config.FolderConstants;
import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.config.Sender;
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
		Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName(), configuration);
		if (!constraints.isEmpty() && constraints.stream().noneMatch(c -> mail.getAssociatedIdentifier().contains(c))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	public record UnreadInInboxDTO(long unread, String name) {}
	
	public HashMap<String, UnreadInInboxDTO> getConstraints() {
		final Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName(), configuration);

		final HashMap<String, UnreadInInboxDTO> result = new HashMap<>();
		for (String constraint : constraints) {
			long unread = getUnreadForRootFolder(Folder.INBOX, Set.of(constraint));
			
			//find matching Sender
			Sender sender = configuration.getSenders().stream().filter(s -> s.getEanIdentifier().equals(constraint)).findAny().orElse(null);
			if (sender == null) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No sender configured for identifier: " + constraint);
			}
			
			result.put(constraint, new UnreadInInboxDTO(unread, sender.getOrganisationName()));
		}
		
		return result;
	}

	public long getTotalUnread() {
		Set<String> constraints = SecurityUtil.getLocationNumberConstraint(configuration.getLocationNumberConstraintName(), configuration);

		if (constraints == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No location number constraints found");
		}

		return getUnreadForRootFolder(Folder.INBOX, constraints);
	}

	public List<Mail> getByFolder(Folder folder) {
		return mailDao.findByFolderAndDraftFalseOrderByReceivedDesc(folder);
	}

	public DataTablesOutput<MailboxDatatableDTO> getByFolderAndConstraints(DataTablesInput datatableInput, Folder folder, String[] constraints) {
		return mailDatatableDao.findAll(datatableInput,
				where(MailDatatableDao.inFolder(folder)
						.and(where(MailDatatableDao.withConstraints(constraints)))));
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

	public long getUnreadForRootFolder(final Folder folder, final Set<String> constrainedLocationNumbers) {
		if (constrainedLocationNumbers == null || constrainedLocationNumbers.isEmpty()) {
			return mailDao.countUnreadForRootFolder(folder);
		} else {
			return mailDao.countUnreadForRootFolder(folder, constrainedLocationNumbers);
		}
	}

	public long getCountByFolderAndNotDraft(final Folder folder, final InboxFolder inboxFolder, final Set<String> constrainedLocationNumbers) {
		if (constrainedLocationNumbers == null || constrainedLocationNumbers.isEmpty()) {
			if (inboxFolder != null) {
				return mailDao.countByFolderAndInboxFolderAndDraftFalse(folder, inboxFolder);
			} else {
				return mailDao.countByFolderAndDraftFalseAndInboxFolderNull(folder);
			}
		} else {
			if (inboxFolder != null) {
				return mailDao.countByFolderAndInboxFolderAndDraftFalse(folder, inboxFolder, constrainedLocationNumbers);
			} else {
				return mailDao.countByFolderAndDraftFalseAndInboxFolderNull(folder, constrainedLocationNumbers);
			}
		}
	}

	public long getCountByFolderAndReceivedNegativeReceipt(final Folder folder, final Set<String> constrainedLocationNumbers) {
		if (constrainedLocationNumbers == null || constrainedLocationNumbers.isEmpty()) {
			return mailDao.countByFolderAndDraftFalseAndReceivedNegativeReceiptTrue(folder);
		} else {
			return mailDao.countByFolderAndDraftFalseAndReceivedNegativeReceiptTrue(folder, constrainedLocationNumbers);
		}
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
			return;
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

	@SuppressWarnings("UnnecessaryUnicodeEscape")
    private String sanitizeContent(String content) {
		if (content == null) return "";
		content = content.replaceAll("&", "&amp;")
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;")
				.replaceAll("\"", "&quot;")
				.replaceAll("'", "&#39;");
		content = content
				// Replace fancy quotations
				.replace("“", "\"")
				.replace("”", "\"")
				.replace("‘", "'")
				.replace("’", "'")
				.replace("´", "'")
				// Fancy spaces....
				.replace("\u00A0", "&#160;")
				.replace("\u202F", " ")
				.replace("\u2007", " ")
				.replace("&nbsp;", "&#160;")
				// Fancy dashes
        		.replace("\u2010", "-")
				.replace("\u2011", "-")
				.replace("\u2013", "-")
				.replace("\u2014", "-")
				.replace("\u2212", "-");

		return INVALID_XML_CHARS.matcher(content).replaceAll("");
	}

	public String getPdfHtml(Mail mail) {
		var context = new Context();
		String brNL = mail.getContent().replace("<br/>", "\n");
		String safeContent = sanitizeContent(brNL);
		context.setVariable("mail", mail);
		context.setVariable("content", safeContent);
		return templateEngine.process("pdf/mail_pdf", context);
	}

	public byte[] convertHtmlToPdf(String html) throws DocumentException, IOException {
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