CREATE TABLE office_upw_team_mappings
(
    id                      UUID         NOT NULL,
    community_campus_pdu_id UUID         NOT NULL,
    office                  VARCHAR(255) NOT NULL,
    team_code               VARCHAR(255),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT office_upw_team_mappings_pk PRIMARY KEY (id),
    CONSTRAINT office_upw_team_mappings_community_campus_pdu_fk
        FOREIGN KEY (community_campus_pdu_id)
            REFERENCES community_campus_pdus (id),
    CONSTRAINT office_upw_team_mappings_pdu_office_unique
        UNIQUE (community_campus_pdu_id, office)
);
