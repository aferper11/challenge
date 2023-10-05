package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transferMoney(String accountFromId, String accountToId, BigDecimal amount) {
    Account accountFrom = accountsRepository.getAccount(accountFromId);
    Account accountTo = accountsRepository.getAccount(accountToId);

    if (accountFrom == null || accountTo == null) {
      throw new AccountNotFoundException("One or both of the accounts do not exist!");
    }

    // Verify that the accountFrom balance is sufficient for the transfer
    BigDecimal newBalanceFrom = accountFrom.getBalance().subtract(amount);
    if (newBalanceFrom.compareTo(BigDecimal.ZERO) < 0) {
      throw new InsufficientBalanceException("Insufficient balance in account " + accountFromId);
    }

    // Perform the transfer
    accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
    accountTo.setBalance(accountTo.getBalance().add(amount));

    // Save updated account information
    accountsRepository.updateAccount(accountFrom);
    accountsRepository.updateAccount(accountTo);

    // Notify account holders
    notificationService.notifyAboutTransfer(accountTo, "Account to: " + accountToId + ", amount transferred: " + amount);

  }
}
