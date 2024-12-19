package dk.digitalidentity.medcommailbox.dao.model;

import dk.digitalidentity.medcommailbox.dao.model.enums.ReferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reference")
public class Reference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String objectIdentifier;

    @Column
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(length = 7681)
    private String filename;

    @Column
    private String referenceDescription;

    @Column
    private String referenceUrl;

    @Column
    private String objectCode;

    @Column
    private String objectExtensionCode;

    @Column
    private Long objectOriginalSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_id")
    private Mail mail;

}
