package com.bebis.BeBiS.base;

import com.bebis.BeBiS.integration.blizzard.BlizzardServiceClient;
import com.bebis.BeBiS.integration.blizzard.BlizzardUserClient;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SpringBootTest
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // replace H2 from DataJpaTest with postgre
@Transactional // this makes sure that jdbctemplate updates are rolled back after each test
public abstract non-sealed class BaseFullStackTest extends BaseContainerTest {

    @Autowired
    protected CacheManager cacheManager;

    @MockitoBean
    protected BlizzardUserClient blizzardUserClient;

    @MockitoBean
    protected BlizzardServiceClient blizzardServiceClient;

    @AfterEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name ->
                Optional.ofNullable(cacheManager.getCache(name)).ifPresent(Cache::clear));
    }

}
