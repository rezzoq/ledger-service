ALTER TABLE account ADD COLUMN openingBalance DECIMAL(19, 4) NOT NULL DEFAULT 0;

UPDATE account a
SET a.openingBalance = a.balance - COALESCE(
        (SELECT SUM(e.amount) FROM ledger_entry e WHERE e.accountId = a.accountId), 0);