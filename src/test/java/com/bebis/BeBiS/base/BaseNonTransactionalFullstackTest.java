package com.bebis.BeBiS.base;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test base class that starts the whole application context. Similar to {@link com.bebis.BeBiS.base.BaseFullStackTest},
 * the only differences being that transactions here are manually committed and have to be manually dealt with and redis is left as is.
 * <p>
 * NOTE: manual db teardown with @AfterEach is advised.
 */
@SpringBootTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // replace H2 from DataJpaTest with postgre
public abstract non-sealed class BaseNonTransactionalFullstackTest extends BaseContainerTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    protected BlizzardUserClient blizzardUserClient; // temporary solution - without it this base class was using the real beans

    @MockitoBean
    protected BlizzardServiceClient blizzardServiceClient;

    protected TransactionTemplate transactionTemplate;

    @PostConstruct
    void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

}
