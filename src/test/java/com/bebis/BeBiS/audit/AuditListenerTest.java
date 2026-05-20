package com.bebis.BeBiS.audit;

import com.bebis.BeBiS.base.BaseFullStackTest;
import com.bebis.BeBiS.equipment.EquipmentTestData;
import com.bebis.BeBiS.integration.blizzard.dto.EquipmentResponse;
import com.bebis.BeBiS.item.ItemService;
import com.bebis.BeBiS.item.ItemTestData;
import com.bebis.BeBiS.item.event.ItemPersistedEvent;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class AuditListenerTest extends BaseFullStackTest {

    @MockitoSpyBean
    private AuditListener listener;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setup() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void shouldReceiveItemPersistedEventAsynchronously() {
        // given
        EquipmentResponse.ItemDTO tf = EquipmentTestData.fromItemResponseNoSuffix(ItemTestData.thunderfuryResponse(),
                "main_hand", List.of());

        // when
        Map<EquipmentResponse.ItemDTO, ItemEntity> resolved = transactionTemplate.execute((s) -> itemService.resolveItems(List.of(tf)));

        ItemEntity persisted = entityManager.find(ItemEntity.class, resolved.get(tf).getPk());
        assertNotNull(persisted); // the item was in fact persisted

        // then
        await().untilAsserted(() -> verify(listener).onItemPersistedEvent(any(ItemPersistedEvent.class)));
    }

}
