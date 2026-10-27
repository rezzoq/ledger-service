package bookkeeper.controller;

import bookkeeper.dto.TransferRequest;
import bookkeeper.dto.TransferResponse;
import bookkeeper.model.Account;
import bookkeeper.service.AccountService;
import bookkeeper.service.TransferFacade;
import bookkeeper.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LedgerController {
    private final TransferFacade transferFacade;
    private final AccountService accountService;

    @PostMapping("/accounts")
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @PostMapping("/transfers")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest req) {
        return transferFacade.transfer(req);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable long id) {
        return accountService.findAccountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
