package dk.digitalidentity.medcommailbox.config;

import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
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
