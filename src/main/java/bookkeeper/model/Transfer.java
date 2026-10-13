package bookkeeper.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer",
        uniqueConstraints = @UniqueConstraint(name = "uq_transfer_idem",
                columnNames = "idempotencyKey"))
@Data
@NoArgsConstructor
public class Transfer {
    @Id
    @Column (name = "transferId", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transferId;

    @NotNull
    @Column (length = 64, nullable = false)
    private String idempotencyKey;

    @NotNull
    @Column (nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
