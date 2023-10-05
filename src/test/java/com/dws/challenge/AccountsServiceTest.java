package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @MockBean
  private NotificationService notificationService;

  @BeforeEach
  void prepareMockMvc() {
    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  public void testTransferMoney_SuccessfulTransfer() {
    String accountFromId = "001";
    String accountToId = "002";
    BigDecimal amount = BigDecimal.valueOf(100.0);

    Account accountFrom = new Account(accountFromId, BigDecimal.valueOf(500.0));
    Account accountTo = new Account(accountToId, BigDecimal.valueOf(200.0));

    accountsService.createAccount(new Account(accountFromId, BigDecimal.valueOf(500.0)));
    accountsService.createAccount(new Account(accountToId, BigDecimal.valueOf(200.0)));

    accountsService.transferMoney(accountFromId, accountToId, amount);

    assertEquals(BigDecimal.valueOf(400.0), accountsService.getAccount(accountFromId).getBalance());
    assertEquals(BigDecimal.valueOf(300.0), accountsService.getAccount(accountToId).getBalance());
    verify(notificationService, times(1)).notifyAboutTransfer(accountsService.getAccount(accountToId), "Account to: 002, amount transferred: 100.0");
  }

  @Test
  public void testTransferMoney_AccountNotFound() {
    String accountFromId = "001";
    String accountToId = "002";
    BigDecimal amount = BigDecimal.valueOf(100.0);

    assertThrows(AccountNotFoundException.class, () -> {
      accountsService.transferMoney(accountFromId, accountToId, amount);
    });

    verifyNoInteractions(notificationService);
  }

  @Test
  public void testTransferMoney_InsufficientBalanceException() {
    String accountFromId = "001";
    String accountToId = "002";
    BigDecimal amount = BigDecimal.valueOf(1000.0);

    accountsService.createAccount(new Account(accountFromId, BigDecimal.valueOf(500.0)));
    accountsService.createAccount(new Account(accountToId, BigDecimal.valueOf(200.0)));

    assertThrows(InsufficientBalanceException.class, () -> {
      accountsService.transferMoney(accountFromId, accountToId, amount);
    });

    verifyNoInteractions(notificationService);
  }

}
