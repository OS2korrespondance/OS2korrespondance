package dk.digitalidentity.medcommailbox.dao.model;

import java.util.HashSet;
import java.util.Set;

import dk.digitalidentity.medcommailbox.dao.model.enums.IdentifierCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

	@Column
	private String departmentName;

	@Column
	private String unitName;

	@Column
	private String streetName;

	@Column
	private String districtName;

	@Column
	private String postcodeIdentifier;

	@Column
	private String phoneNumber;
}