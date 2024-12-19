package dk.digitalidentity.medcommailbox.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "recipient")
public class Recipient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private String eanIdentifier;
	
	@Column
	private String identifier;
	
	@Enumerated(EnumType.STRING)
	@Column
	private IdentifierCode identifierCode;
	
	@Column(length = 35)
	private String shortOrganisationName;

	@Column
	private String fullOrganisationName;
	
}