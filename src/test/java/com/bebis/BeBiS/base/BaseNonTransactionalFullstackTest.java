package com.bebis.BeBiS.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * Each class extending this one needs to manually teardown the db in @AfterEach !
 */

@SpringBootTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // replace H2 from DataJpaTest with postgre
public abstract non-sealed class BaseNonTransactionalFullstackTest extends BaseContainerTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected TransactionTemplate transactionTemplate;

    @BeforeEach
    public void setup() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }
}
