package dk.digitalidentity.medcommailbox.config;

import dk.digitalidentity.medcommailbox.model.entity.enums.IdentifierCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sender {
	private String eanIdentifier;
	private String identifier;
	private IdentifierCode identifierCode;
	private String organisationName;
}
