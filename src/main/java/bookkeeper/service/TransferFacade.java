package bookkeeper.service;

import bookkeeper.dto.TransferRequest;
import bookkeeper.dto.TransferResponse;
import bookkeeper.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferFacade {

    private final TransferService transferService;
    private final TransferRepository transferRepository;

    public TransferResponse transfer(TransferRequest req) {
        try {
            return transferService.transfer(req);
        } catch (DataIntegrityViolationException e) {
            return transferRepository.findByIdempotencyKey(req.idempotencyKey())
                    .map(t -> new TransferResponse(t.getTransferId(), "DUPLICATE"))
                    .orElseThrow(() -> e);
        }
    }
}