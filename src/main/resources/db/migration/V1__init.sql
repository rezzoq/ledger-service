CREATE TABLE IF NOT EXISTS account (
    accountId BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_account_balance CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transfer (
    transferId BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    idempotencyKey VARCHAR(64) NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_transfer_idem UNIQUE (idempotencyKey)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ledger_entry (
    ledgerEntryId BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transferId BIGINT UNSIGNED NOT NULL,
    accountId BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_entry_amount CHECK (amount <> 0),
    CONSTRAINT fk_entry_transfer FOREIGN KEY (transferId) REFERENCES transfer(transferId),
    CONSTRAINT fk_entry_account  FOREIGN KEY (accountId)  REFERENCES account(accountId),
    INDEX idx_entry_account (accountId, createdAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;