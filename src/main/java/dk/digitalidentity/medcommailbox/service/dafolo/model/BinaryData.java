package dk.digitalidentity.medcommailbox.service.dafolo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class BinaryData {
	private Map<Long, byte[]> attachments;
	private Map<Long, String> attachmentFileNames;
}
