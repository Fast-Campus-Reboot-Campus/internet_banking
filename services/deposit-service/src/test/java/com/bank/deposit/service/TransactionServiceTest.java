package com.bank.deposit.service;

import com.bank.deposit.domain.entity.Account;
import com.bank.deposit.domain.entity.Transaction;
import com.bank.deposit.domain.enums.*;
import com.bank.deposit.exception.BusinessException;
import com.bank.deposit.repository.AccountRepository;
import com.bank.deposit.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private Clock clock;

    @BeforeEach
    void setUpClock() {
        org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        org.mockito.Mockito.lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    @Nested
    @DisplayName("입금")
    class Deposit {

        @Test
        @DisplayName("입금하면 잔액이 증가하고 거래 내역이 저장된다")
        void deposit() {
            Account acc = activeAccount(BigDecimal.valueOf(100_000));
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(acc));
            given(transactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Transaction result = transactionService.deposit(1L, BigDecimal.valueOf(50_000),
                    TransactionChannel.INTERNET, "테스트 입금", null, null);

            assertThat(result.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(result.getDirectionType()).isEqualTo(DirectionType.IN);
            assertThat(result.getAmount()).isEqualByComparingTo("50000");
            assertThat(result.getBalanceBefore()).isEqualByComparingTo("100000");
            assertThat(result.getBalanceAfter()).isEqualByComparingTo("150000");
            assertThat(result.getTransactionNumber()).startsWith("DEP-");
        }

        @Test
        @DisplayName("비활성 계좌에 입금하면 예외가 발생한다")
        void depositToInactiveAccount() {
            Account closed = closedAccount();
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(closed));

            assertThatThrownBy(() -> transactionService.deposit(1L, BigDecimal.valueOf(50_000),
                    null, null, null, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("출금")
    class Withdraw {

        @Test
        @DisplayName("잔액이 충분하면 출금이 정상 처리된다")
        void withdraw() {
            Account acc = activeAccount(BigDecimal.valueOf(500_000));
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(acc));
            given(transactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Transaction result = transactionService.withdraw(1L, BigDecimal.valueOf(200_000),
                    TransactionChannel.ATM, "ATM 출금");

            assertThat(result.getDirectionType()).isEqualTo(DirectionType.OUT);
            assertThat(result.getBalanceAfter()).isEqualByComparingTo("300000");
        }

        @Test
        @DisplayName("잔액보다 많은 금액을 출금하면 예외가 발생한다")
        void withdrawInsufficientBalance() {
            Account acc = activeAccount(BigDecimal.valueOf(10_000));
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(acc));

            assertThatThrownBy(() -> transactionService.withdraw(1L, BigDecimal.valueOf(50_000),
                    null, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("이체")
    class Transfer {

        @Test
        @DisplayName("내부 이체 시 출금 거래가 생성된다")
        void transfer() {
            Account source = activeAccount(BigDecimal.valueOf(1_000_000));
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(source));
            given(transactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Transaction result = transactionService.transfer(1L, null, "001-1234-5678",
                    BigDecimal.valueOf(300_000), TransferType.EXTERNAL,
                    "001", "국민은행", "홍길동", TransactionChannel.INTERNET, "이체");

            assertThat(result.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
            assertThat(result.getDirectionType()).isEqualTo(DirectionType.OUT);
            assertThat(result.getTransactionNumber()).startsWith("TRF-");
            assertThat(result.getTransferType()).isEqualTo(TransferType.EXTERNAL);
            assertThat(result.getCounterpartyAccountNo()).isEqualTo("001-1234-5678");
            assertThat(result.getCounterpartyBankCode()).isEqualTo("001");
            assertThat(result.getCounterpartyBankName()).isEqualTo("국민은행");
            assertThat(result.getCounterpartyName()).isEqualTo("홍길동");
            assertThat(result.getChannelType()).isEqualTo(TransactionChannel.INTERNET);
            assertThat(result.getTransferRequestedAt()).isNotNull();
            assertThat(result.getTransferCompletedAt()).isNotNull();
            assertThat(source.getBalance()).isEqualByComparingTo("700000");
        }

        @Test
        @DisplayName("내부 계좌 이체 시 상대 계좌 입금 거래도 생성한다")
        void transferToInternalAccountCreatesInboundTransaction() {
            Account source = activeAccount(BigDecimal.valueOf(1_000_000));
            Account target = Account.builder()
                    .accountNumber("ACC-002")
                    .customerId("CUST-002")
                    .contractId(2L)
                    .accountType(ProductType.DEPOSIT)
                    .accountPassword("5678")
                    .openedAt(LocalDate.of(2026, 1, 1))
                    .balance(BigDecimal.valueOf(200_000))
                    .build();
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(source));
            given(accountRepository.findByIdForUpdate(2L)).willReturn(Optional.of(target));
            given(transactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Transaction result = transactionService.transfer(1L, 2L, "ACC-002",
                    BigDecimal.valueOf(300_000), TransferType.INTERNAL,
                    "001", "우리은행", "김수신", TransactionChannel.MOBILE, "내부 이체");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            then(transactionRepository).should(org.mockito.Mockito.times(2)).save(captor.capture());
            List<Transaction> saved = captor.getAllValues();

            assertThat(result.getDirectionType()).isEqualTo(DirectionType.OUT);
            assertThat(source.getBalance()).isEqualByComparingTo("700000");
            assertThat(target.getBalance()).isEqualByComparingTo("500000");
            assertThat(saved)
                    .extracting(Transaction::getDirectionType)
                    .containsExactly(DirectionType.OUT, DirectionType.IN);
            assertThat(saved.get(1).getTransferType()).isEqualTo(TransferType.INTERNAL);
            assertThat(saved.get(1).getChannelType()).isEqualTo(TransactionChannel.SYSTEM);
            assertThat(saved.get(1).getTransactionSummary()).isEqualTo("이체 수신");
        }

        @Test
        @DisplayName("잔액이 부족하면 이체 시 예외가 발생한다")
        void transferInsufficientBalance() {
            Account source = activeAccount(BigDecimal.valueOf(100));
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(source));

            assertThatThrownBy(() -> transactionService.transfer(1L, 2L, null,
                    BigDecimal.valueOf(1_000_000), null, null, null, null, null, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("적금 납입")
    class SavingsPayment {

        @Test
        @DisplayName("적금 납입 시 잔액과 누적 납입액을 증가시킨다")
        void savingsPayment() {
            Account acc = activeAccount(BigDecimal.valueOf(100_000));
            given(accountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(acc));
            given(transactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Transaction result = transactionService.savingsPayment(1L, 10L,
                    BigDecimal.valueOf(50_000), 3, TransactionChannel.MOBILE);

            assertThat(result.getTransactionType()).isEqualTo(TransactionType.SAVINGS_PAYMENT);
            assertThat(result.getDirectionType()).isEqualTo(DirectionType.IN);
            assertThat(result.getContractId()).isEqualTo(10L);
            assertThat(result.getPaymentRound()).isEqualTo(3);
            assertThat(result.getBalanceBefore()).isEqualByComparingTo("100000");
            assertThat(result.getBalanceAfter()).isEqualByComparingTo("150000");
            assertThat(acc.getBalance()).isEqualByComparingTo("150000");
            assertThat(acc.getTotalPaidAmount()).isEqualByComparingTo("50000");
        }
    }

    @Nested
    @DisplayName("거래 취소")
    class Reversal {

        @Test
        @DisplayName("입금 거래를 취소하면 잔액이 감소하고 취소 거래가 생성된다")
        void reverseDepositTx() {
            Account acc = activeAccount(BigDecimal.valueOf(500_000));
            Transaction original = buildTx(1L, DirectionType.OUT, BigDecimal.valueOf(100_000), TransactionStatus.SUCCESS);
            given(transactionRepository.findById(1L)).willReturn(Optional.of(original));
            given(accountRepository.findByIdForUpdate(original.getAccountId())).willReturn(Optional.of(acc));
            given(transactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Transaction result = transactionService.reversal(1L, TransactionChannel.SYSTEM);

            assertThat(result.getTransactionType()).isEqualTo(TransactionType.REVERSAL);
            assertThat(result.getDirectionType()).isEqualTo(DirectionType.IN);
            assertThat(result.getTransactionNumber()).startsWith("REV-");
        }

        @Test
        @DisplayName("이미 취소된 거래를 재취소하면 예외가 발생한다")
        void reversalAlreadyCanceled() {
            Transaction canceled = buildTx(1L, DirectionType.IN, BigDecimal.valueOf(100_000), TransactionStatus.CANCELED);
            given(transactionRepository.findById(1L)).willReturn(Optional.of(canceled));

            assertThatThrownBy(() -> transactionService.reversal(1L, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── 픽스처 ──────────────────────────────────────────────────────────────

    private Account activeAccount(BigDecimal balance) {
        return Account.builder()
                .accountNumber("ACC-001")
                .customerId("CUST-001")
                .contractId(1L)
                .accountType(ProductType.DEPOSIT)
                .accountPassword("1234")
                .openedAt(LocalDate.of(2026, 1, 1))
                .balance(balance)
                .build();
    }

    private Account closedAccount() {
        return Account.builder()
                .accountNumber("ACC-CLOSED")
                .customerId("CUST-001")
                .contractId(2L)
                .accountType(ProductType.DEPOSIT)
                .accountPassword("1234")
                .openedAt(LocalDate.of(2026, 1, 1))
                .accountStatus(AccountStatus.CLOSED)
                .build();
    }

    private Transaction buildTx(Long accountId, DirectionType direction,
                                 BigDecimal amount, TransactionStatus status) {
        Transaction tx = Transaction.builder()
                .transactionNumber("DEP-20260101-ABCDEFGH")
                .accountId(accountId)
                .transactionType(direction == DirectionType.IN ? TransactionType.DEPOSIT : TransactionType.WITHDRAW)
                .directionType(direction)
                .amount(amount)
                .balanceBefore(BigDecimal.valueOf(400_000))
                .balanceAfter(BigDecimal.valueOf(500_000))
                .channelType(TransactionChannel.INTERNET)
                .transactionAt(java.time.OffsetDateTime.now())
                .build();
        if (status == TransactionStatus.CANCELED) {
            tx.cancel();
        }
        return tx;
    }
}
