package dk.digitalidentity.medcommailbox.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dk.digitalidentity.medcommailbox.dao.model.enums.EpisodeOfCareStatusCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patient")
public class Patient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private String name;
	
	@Column
	private String cpr;
	
	@Column
	private boolean alternativeIdentifier;
	
	@Enumerated(EnumType.STRING)
	@Column
	private EpisodeOfCareStatusCode episodeOfCareStatusCode;
	
	@Column
	private String streetName;
	
	@Column
	private String suburbName;
	
	@Column
	private String districtName;
	
	@Column
	private String postCodeIdentifier;
	
}
