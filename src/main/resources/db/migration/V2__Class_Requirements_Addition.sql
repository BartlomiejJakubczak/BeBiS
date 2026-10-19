CREATE TABLE item_playable_classes
(
    base_id        BIGINT      NOT NULL,
    suffix_id      BIGINT      NOT NULL,
    playable_class VARCHAR(30) NOT NULL,
    PRIMARY KEY (base_id, suffix_id, playable_class),
    CONSTRAINT fk_item_playable_classes_to_items
        FOREIGN KEY (base_id, suffix_id)
            REFERENCES items (base_id, suffix_id)
);

CREATE INDEX item_effects_index ON item_effects (base_id, suffix_id);
