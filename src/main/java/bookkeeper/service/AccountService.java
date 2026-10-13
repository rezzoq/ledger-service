package bookkeeper.service;

import bookkeeper.model.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import bookkeeper.repository.AccountRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional (readOnly = true)
    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional (readOnly = true)
    public Optional<Account> findAccountById(long id) {
        return accountRepository.findById(id);
    }

    @Transactional
    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }

    @Transactional
    public Account createAccount(Account account) {
        if (accountRepository.existsByName(account.getName())) {
            throw new IllegalArgumentException("Account name already exists");
        }
        else {
            return accountRepository.save(account);
        }
    }

    @Transactional
    public void deleteAccount(Account account) {
        if (accountRepository.existsById(account.getAccountId())) {
            accountRepository.delete(account);
        }
        else {
            throw new IllegalArgumentException("Account does not exist");
        }
    }
}
