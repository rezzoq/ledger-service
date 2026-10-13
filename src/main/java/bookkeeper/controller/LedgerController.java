package bookkeeper.controller;

import bookkeeper.dto.TransferRequest;
import bookkeeper.dto.TransferResponse;
import bookkeeper.model.Account;
import bookkeeper.service.AccountService;
import bookkeeper.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LedgerController {
    private final TransferService transferService;
    private final AccountService accountService;

    @PostMapping("/accounts")
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @PostMapping("/transfers")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest req) {
        return transferService.transfer(req);
    }
}
