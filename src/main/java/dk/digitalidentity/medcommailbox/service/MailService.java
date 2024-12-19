package dk.digitalidentity.medcommailbox.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.lowagie.text.DocumentException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.medcommailbox.config.FolderConstants;
import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import dk.digitalidentity.medcommailbox.dao.MailDao;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.enums.Folder;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@Slf4j
public class MailService {

	@Autowired
	private MailDao mailDao;
	
	@Autowired
	private S3Service s3Service;

	@Autowired
	private MedcomMailboxConfiguration config;

	@Autowired
	private TemplateEngine templateEngine;


	public List<Mail> getByFolder(Folder folder) {
		return mailDao.findByFolderAndDraftFalseOrderByReceivedDesc(folder);
	}
	
	public long getCountByReadAndFolder(boolean read, Folder folder) {
		return mailDao.countByReadAndFolderAndDraftFalse(read, folder);
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

	public Mail findByEnvelopeIdentifierAndLetterIdentifier(String envelopeIdentifier, String letterIdentifier) {
		return mailDao.findByEnvelopeIdentifierAndLetterIdentifier(envelopeIdentifier, letterIdentifier);
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
		try {
			String html = getPdfHtml(mail);
			byte[] pdf = convertHtmlToPdf(html);
			s3Service.upload(FolderConstants.FOLDER_ARCHIVE, name + ".pdf", pdf);
			s3Service.upload(FolderConstants.FOLDER_ARCHIVE, name + ".xml", xml);
		} catch (Exception e) {
            log.error("Could not upload copy to archive for mail with id {}", mail.getId(), e);
		}
	}

	private String getPdfHtml(Mail mail) {
		var context = new Context();
		context.setVariable("mail", mail);
		context.setVariable("content", mail.getContent().replace("\n", "<br/>"));
		var html = templateEngine.process("pdf/mail_pdf", context);
		return html;
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