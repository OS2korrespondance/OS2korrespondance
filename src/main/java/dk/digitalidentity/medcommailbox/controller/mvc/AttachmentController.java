package dk.digitalidentity.medcommailbox.controller.mvc;

import dk.digitalidentity.medcommailbox.dao.model.Binary;
import dk.digitalidentity.medcommailbox.dao.model.Mail;
import dk.digitalidentity.medcommailbox.dao.model.Reference;
import dk.digitalidentity.medcommailbox.dao.model.enums.AuditLogOperation;
import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import dk.digitalidentity.medcommailbox.service.AuditLogService;
import dk.digitalidentity.medcommailbox.service.BinaryService;
import dk.digitalidentity.medcommailbox.service.MailService;
import dk.digitalidentity.medcommailbox.service.S3Service;
import dk.digitalidentity.medcommailbox.util.MedcomContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RequireUserAccess
@Controller
@RequiredArgsConstructor
public class AttachmentController {
    private final AuditLogService auditLogService;
    private final MailService mailService;
    private final BinaryService binaryService;
    private final S3Service s3Service;

    @GetMapping("/attachments/{mailId}/attachments/{id}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable("mailId") Long mailId, @PathVariable("id") Long id) throws IOException {
        Mail mail = mailService.getById(mailId);
        if (mail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        mailService.hasAccess(mail);
        final Reference reference = mail.getReferences().stream().filter(a -> a.getId() == id).findAny().orElse(null);
        if (reference == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        final Binary binary = binaryService.findByIdentifier(reference.getObjectIdentifier()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final byte[] bytes = s3Service.downloadFromS3(binary);
        final MediaType contentType = MedcomContentType.contentTypeFor(reference.getObjectExtensionCode());
        final String referenceFilename = binaryService.getReferenceFilename(reference);
        auditLogService.log(AuditLogOperation.ATTACHMENT_DOWNLOAD, auditLogService.getDetails(reference, referenceFilename));

        final HttpHeaders headers = new HttpHeaders();
        if (contentType == MediaType.APPLICATION_OCTET_STREAM) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(referenceFilename)
                    .build().toString());
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(bytes.length)
                .headers(headers)
                .body(bytes);
    }

}
