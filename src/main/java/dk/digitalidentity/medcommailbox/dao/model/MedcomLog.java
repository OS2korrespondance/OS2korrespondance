package dk.digitalidentity.medcommailbox.dao.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dk.digitalidentity.medcommailbox.dao.model.enums.ReceiptType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "receipt_log")
public class MedcomLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private String receiptXml;
	
	@Column
	private String mailXml;
	
	@Column
	private LocalDateTime mailTts;
	
	@Column
	private LocalDateTime receiptTts;
	
	@Column
	@Enumerated(EnumType.STRING)
	private ReceiptType receiptType;
	
	@Column
	private String negativeReceiptReason;
	
	@Column
	private String envelopeIdentifier;
	
	@Column
	private String letterIdentifier;
	
	@Column(name = "receipt_s3_file_key")
	private String receiptS3FileKey;
	
	@Column
	private boolean incomming;

}
