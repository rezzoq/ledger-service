package bookkeeper.service;

import bookkeeper.dto.TransferRequest;
import bookkeeper.dto.TransferResponse;
import bookkeeper.model.Account;
import bookkeeper.model.LedgerEntry;
import bookkeeper.model.Transfer;
import bookkeeper.repository.AccountRepository;
import bookkeeper.repository.LedgerEntryRepository;
import bookkeeper.repository.TransferRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    private final MeterRegistry meterRegistry;

    @Transactional
    public TransferResponse transfer(TransferRequest req) {
        if (req.fromAccId().equals(req.toAccId())) {
            throw new IllegalArgumentException("Same account");
        }

        Optional<Transfer> existing = transferRepository.findByIdempotencyKey(req.idempotencyKey());
        if (existing.isPresent()) {
            meterRegistry.counter("ledger.transfers", "status", "DUPLICATE").increment();
            return new TransferResponse(existing.get().getTransferId(), "DUPLICATE");
        }

        Transfer transfer = new Transfer();
        transfer.setIdempotencyKey(req.idempotencyKey());
        try {
            transfer = transferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException race) {
            transfer = transferRepository.findByIdempotencyKey(req.idempotencyKey())
                    .orElseThrow(() -> race);

            meterRegistry.counter("ledger.transfers", "status", "DUPLICATE").increment();
            return new TransferResponse(transfer.getTransferId(), "DUPLICATE");
        }

        long firstId = Math.min(req.fromAccId(), req.toAccId());
        long secondId = Math.max(req.fromAccId(), req.toAccId());

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + secondId));

        Account from = first.getAccountId().equals(req.fromAccId()) ? first : second;
        Account to = (from == first) ? second : first;

        if (from.getBalance().compareTo(req.amount()) < 0) {
            meterRegistry.counter("ledger.transfers", "status", "INSUFFICIENT").increment();
            throw new IllegalStateException("Insufficient funds");
        }
        from.setBalance(from.getBalance().subtract(req.amount()));
        to.setBalance(to.getBalance().add(req.amount()));

        saveEntry(transfer, from, req.amount().negate());
        saveEntry(transfer, to,   req.amount());

        meterRegistry.counter("ledger.transfers", "status", "OK").increment();
        return new TransferResponse(transfer.getTransferId(), "OK");
    }

    private void saveEntry(Transfer t, Account a, BigDecimal amount) {
        LedgerEntry e = new LedgerEntry();
        e.setTransfer(t);
        e.setAccount(a);
        e.setAmount(amount);
        ledgerEntryRepository.save(e);
    }
}