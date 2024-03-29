package com.transactions.api.database;

import com.transactions.api.model.dto.TransactionDTO;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import com.transactions.api.model.entity.Transaction;

import org.hibernate.query.Query;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class DatabaseManagerImpl implements DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManagerImpl.class.getName());
    private static SessionFactory sessionFactory;
    private static Session session;
    private static DatabaseManager databaseManager;

    private DatabaseManagerImpl() {
    }

    public static DatabaseManager getInstance() {
        if (databaseManager == null) {
            LOGGER.info("Initialize DatabaseManager (Singleton)");
            databaseManager = new DatabaseManagerImpl();
        }
        return databaseManager;
    }

    /**
     * This method is creates a SessionFactory only and only if the connection to database is disrupted
     * or SessionFactory is closed or null
     */
    public void initDatabase() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            LOGGER.info("initDatabase is triggered.");
            sessionFactory = new Configuration().configure().buildSessionFactory();
            session = sessionFactory.openSession();
            LOGGER.info("initDatabase has initialized the database.");
        }
    }

    /**
     * This method is used to release database resources and session when the service is stopped
     */
    public void destroyDatabase() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            LOGGER.info("destroyDatabase is triggered.");
            session.close();
            sessionFactory.close();
            LOGGER.info("destroyDatabase has destroyed in-memory database.");
        }
    }

    /**
     * creates a transaction against a specific account
     * @param accountId the account that the transaction is created for
     * @param transactionAmount the amount to be deducted
     * @param transactionUUID The assigned UUID for the transaction
     * @return TransactionStatus enum
     */
    public TransactionStatus createTransaction(int accountId, double transactionAmount, String transactionUUID) {
        LOGGER.info("createTransaction is triggered");
        try {
            Transaction tx = new Transaction();
            tx.setTransactionAmount(transactionAmount);
            tx.setAccountId(accountId);
            tx.setTransactionUUID(transactionUUID);
            session.save(tx);
            return new TransactionStatus(TransactionStatus.TransactionResult.SUCCESS, "Transaction added successfully");
        } catch (Exception ex) {
            LOGGER.error("createTransaction - Error in creating a transaction. Error is: ", ex);
            return new TransactionStatus(TransactionStatus.TransactionResult.ERROR, "Error in adding a new transaction. Error is: " + ex.getMessage());
        }
    }

    /**
     * delete a transaction by its uuid, and account id
     * @param accountId the account used for this transaction
     * @param transactionUUID the transaction uuid
     * @return TransactionStatus enum
     */
    public TransactionStatus deleteTransaction(int accountId, String transactionUUID) {
        LOGGER.info("deleteTransaction is triggered");
        org.hibernate.Transaction transaction = session.beginTransaction();
        try {
            String deleteTx = "delete from Transaction where accountId=:accountId and transactionUUID=:transactionUUID";
            Query query = session.createQuery(deleteTx);
            query.setParameter("accountId", accountId);
            query.setParameter("transactionUUID", transactionUUID);
            int res = query.executeUpdate();
            transaction.commit();
            return new TransactionStatus(TransactionStatus.TransactionResult.SUCCESS, "Transaction deleted successfully");
        } catch (Exception ex) {
            transaction.rollback();
            LOGGER.error("createTransaction - Error in deleting a transaction. Error is: ", ex);
            return new TransactionStatus(TransactionStatus.TransactionResult.ERROR, "Error in deleting a transaction. Error is: " + ex.getMessage());
        }
    }

    /**
     * retrieve transactions for the account
     * @param accountId get all transactions for the account by its id
     * @return list of transactions
     */
    public List<TransactionDTO> getTransactions(int accountId) {
        List<Transaction> transactions = session.createQuery("from Transaction where accountId=:accountId")
                .setParameter("accountId", accountId)
                .list();
        if(transactions != null && transactions.size() > 0) {
            return TransactionDTO.cloneFromEntity(transactions);
        }
        // return empty list
        return new ArrayList<>();
    }

}
