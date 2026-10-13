package bookkeeper.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table (name = "account")
@Data
@NoArgsConstructor

public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "accountId", nullable = false)
    private Long accountId;

    @NotNull
    @Column (length = 100, nullable = false)
    private String name;

    @NotNull
    @Column(nullable = false)
    private BigDecimal balance;

    @Version
    private Long version;
}
