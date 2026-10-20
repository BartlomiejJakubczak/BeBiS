CREATE TABLE quest_rewards
(
    base_id          BIGINT      NOT NULL,
    quest_id         BIGINT      NOT NULL,
    quest_min_level  INTEGER     NOT NULL,
    quest_level      INTEGER     NOT NULL,
    reward_kind      VARCHAR(20) NOT NULL CHECK (reward_kind IN ('CHOICE', 'GUARANTEED')),
    required_classes INTEGER     NOT NULL,
    required_races   INTEGER     NOT NULL,
    PRIMARY KEY (base_id, quest_id)
);