package ru.sbt.jschool.session3.problem1;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class AccountServiceImpl implements AccountService {
    protected FraudMonitoring fraudMonitoring;
    private Hashtable<Long,Account> accountTable = new Hashtable<>();
    private HashSet<Long> operations = new HashSet<>();

    public AccountServiceImpl(FraudMonitoring fraudMonitoring) {
        this.fraudMonitoring = fraudMonitoring;
    }

    @Override public Result create(long clientID, long accountID, float initialBalance, Currency currency) {
        if (accountTable.containsKey(accountID))
                return Result.ALREADY_EXISTS;
        else if (fraudMonitoring.check(clientID))
            return Result.FRAUD;
        else {
            accountTable.put(accountID, new Account(clientID, accountID, currency, initialBalance));
            return Result.OK;
        }
    }

    @Override public List<Account> findForClient(long clientID) {
        List<Account> result = new ArrayList<>();
        if (accountTable.isEmpty() == true)
            return Collections.EMPTY_LIST;
        else {
            for (Account acc : accountTable.values()) {
                if (acc.getClientID() == clientID)
                    result.add(acc);
            }
            return result;
        }
    }

    @Override public Account find(long accountID) {
        if (accountTable.containsKey(accountID))
            return accountTable.get(accountID);
        return null;
    }

    @Override public Result doPayment(Payment payment) {
        if (operations.contains(payment.getOperationID()))
            return Result.ALREADY_EXISTS;
        else if (findForClient(payment.getPayerID()).size() == 0 || find(payment.getPayerAccountID()) == null)
            return Result.PAYER_NOT_FOUND;
        else if (fraudMonitoring.check(payment.getPayerID()))
            return Result.FRAUD;
        else if (findForClient(payment.getRecipientAccountID()) .size() == 0 || find(payment.getRecipientID()) == null)
            return Result.RECIPIENT_NOT_FOUND;
        else {
            operations.add(payment.getOperationID());

            Account payer = find(payment.getPayerAccountID());
            Account recipient = find(payment.getRecipientAccountID());

            if (payer.getCurrency() == recipient.getCurrency())
                makePayment(payment, payer, recipient, 1);
            else if (payer.getCurrency() == Currency.RUR) {
                if (recipient.getCurrency() == Currency.USD)
                    makePayment(payment, payer, recipient, 1f / Currency.RUR_TO_USD);
                else if (recipient.getCurrency() == Currency.EUR)
                    makePayment(payment, payer, recipient, 1f / Currency.RUR_TO_EUR);
            }
            else if (payer.getCurrency() == Currency.USD) {
                if (recipient.getCurrency() == Currency.EUR)
                    return Result.INSUFFICIENT_FUNDS;
                else if (recipient.getCurrency() == Currency.RUR)
                    makePayment(payment, payer, recipient, Currency.RUR_TO_USD);
            }
            else if (payer.getCurrency() == Currency.EUR)
                if (recipient.getCurrency() == Currency.USD)
                    return Result.INSUFFICIENT_FUNDS;
                else if (recipient.getCurrency() == Currency.RUR)
                    makePayment(payment, payer, recipient, Currency.RUR_TO_EUR);
        }
        return Result.OK;
    }

    private void makePayment(Payment payment, Account payer, Account recipient, float curr) {
        payer.setBalance(payer.getBalance() - payment.getAmount());
        recipient.setBalance(recipient.getBalance() + payment.getAmount() * curr);
    }
}
