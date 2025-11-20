package dk.digitalidentity.medcommailbox.controller.api;

import com.mysql.cj.util.StringUtils;
import dk.digitalidentity.medcommailbox.model.api.AttachmentResponseEO;
import dk.digitalidentity.medcommailbox.model.entity.ApiMessage;
import dk.digitalidentity.medcommailbox.service.ApiMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import dk.oio.rep.medcom_dk.xml.schemas._2012._03._28.ObjectExtensionCodeType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/messages")
@Tag(name = "Message resource")
@RequiredArgsConstructor
public class MessageApiController {

	private final ApiMessageService apiMessageService;

    @PostMapping(value = "attachments", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload en fil og tilknyt den en gruppe",
            description = """
        Dette endpoint bruges til at uploade en enkelt fil og knytte den til en gruppe via en `groupId`.
        Gruppen identificeres af afsenderen og kan bruges til at knytte én eller flere filer
        til en efterfølgende besked. Når en besked senere sendes, angives samme `groupId` for at referere
        til de vedhæftede filer.
        """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Filen blev uploadet og registreret",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AttachmentResponseEO.class),
                            examples = @ExampleObject(value = """
                {
                  "attachementId": "de305d54-75b4-431b-adb2-eb6b9e546014",
                  "groupId": "abc-123",
                  "keepAliveTime": 3600
                }
            """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ugyldigt input, fx hvis filen mangler")
    })
	@CrossOrigin(origins = "*", maxAge = 3600)
    public AttachmentResponseEO createAttachment(
            @Parameter(description = "Filen der skal uploades, følgende filtyper understøttes: JPG, PDF eller TIF")
            @Validated
            @RequestPart final MultipartFile[] files,
            @Parameter(        description = """
			En klientgenereret UUID identifier der grupperer vedhæftede filer.
			Skal være et gyldigt UUID format (fx: de305d54-75b4-431b-adb2-eb6b9e546014) og angives senere, når en besked sendes.
			På den måde kan beskeden og filerne kobles sammen.
			""")
            @RequestParam("groupId") final String groupId) {
		validateUuid(groupId);
		// Validate file types for all uploaded files
		for (MultipartFile file : files) {
			String originalFilename = file.getOriginalFilename();
			if (originalFilename == null || originalFilename.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filnavn mangler");
			}

			validateFileType(originalFilename);
		}

		log.debug("Received attachment upload request for groupId {}", groupId);
		String fileName = Arrays.stream(files).map(MultipartFile::getOriginalFilename).collect(Collectors.joining(","));
		Long fileSize = Arrays.stream(files).map(MultipartFile::getSize).mapToLong(Long::longValue).sum();
		ApiMessage apiMessage = apiMessageService.findByGroupId(groupId);
		// Does not exist already, upload a brand new file after zipping
		if (apiMessage == null) {
			byte[] zipBytes = createZipFromFiles(files);
			apiMessage = apiMessageService.createApiMessage(groupId, fileName, fileSize, zipBytes);
		}
		else {
			// The existing zip file from S3
			byte[] existing = apiMessageService.getExisting(groupId);

			// Unzip existing files and merge with new files
			byte[] mergedZipBytes = mergeZipFiles(existing, files);

			// Update the API message with merged zip
			apiMessage = apiMessageService.updateApiMessage(apiMessage, fileName, fileSize, mergedZipBytes);
		}
		// This is after we upload the created/edited version of the ApiMessage
		if (apiMessage == null) {
			log.warn("Unable to save file: {} with a size of {} bytes and a groupId of {}", fileName, fileSize, groupId);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Kunne ikke gemme filen. Prøv venligst igen.");
		}

		return AttachmentResponseEO.builder()
				.attachementId(UUID.fromString(groupId))
				.groupId(groupId)
				.keepAliveTime(3600L)
				.build();
    }

	private static void validateUuid(String groupId) {
		try {
			UUID.fromString(groupId);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "groupId skal være et gyldigt UUID format");
		}
	}

	private byte[] createZipFromFiles(MultipartFile[] files) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			for (MultipartFile file : files) {
				ZipEntry zipEntry = new ZipEntry(Objects.requireNonNull(file.getOriginalFilename()));
				zos.putNextEntry(zipEntry);
				zos.write(file.getBytes());
				zos.closeEntry();
			}
		} catch (IOException e) {
			log.error("Unable to zip files: {}", e.getMessage());
			return null;
		}

		return baos.toByteArray();
	}

	private byte[] mergeZipFiles(byte[] existingZipBytes, MultipartFile[] newFiles) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (ZipOutputStream zos = new ZipOutputStream(baos);
				ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(existingZipBytes))) {

			// Copy existing entries from the zip
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				zos.putNextEntry(new ZipEntry(entry.getName()));
				byte[] buffer = new byte[1024];
				int len;
				while ((len = zis.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				zos.closeEntry();
			}

			// Add new files to the zip
			for (MultipartFile file : newFiles) {
				ZipEntry zipEntry = new ZipEntry(Objects.requireNonNull(file.getOriginalFilename()));
				zos.putNextEntry(zipEntry);
				zos.write(file.getBytes());
				zos.closeEntry();
			}

		} catch (IOException e) {
			log.error("Unable to merge zip files: {}", e.getMessage());
			return null;
		}

		return baos.toByteArray();
	}

	private void validateFileType(String filename) {
		final String cleanedFilename = filename.trim().toLowerCase();

		final ObjectExtensionCodeType objectExtensionCodeType = Arrays.stream(ObjectExtensionCodeType.values())
				.filter(t -> {
					if (StringUtils.endsWithIgnoreCase(cleanedFilename, t.value())) {
						return true;
					}
					if (t.value().equals(ObjectExtensionCodeType.JPEG.value())
							&& StringUtils.endsWithIgnoreCase(cleanedFilename, "jpg")) {
						return true;
					}
					return t.value().equals(ObjectExtensionCodeType.TIFF.value())
							&& StringUtils.endsWithIgnoreCase(cleanedFilename, "tif");
				})
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Denne filtype supporteres ikke."
				));
	}
}
