package dk.digitalidentity.medcommailbox.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inbox")
public class Inbox {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String eanIdentifier;

	@Column
	private boolean negativeReceiptNotification;

	@Column
	private boolean autoReplyEnabled;

	@Column
	private String autoReplySubject;

	@Column(columnDefinition = "TEXT")
	private String autoReplyMessage;

	@Column
	private LocalDate autoReplyStartDate;

	@Column
	private LocalDate autoReplyEndDate;

	@Builder.Default
	@OneToMany(mappedBy = "inbox", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<InboxSubscriber> subscribers = new HashSet<>();

}
