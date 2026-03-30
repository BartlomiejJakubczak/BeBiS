package com.bebis.BeBiS.base;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseE2ETest extends BaseDatabaseTest {
}
