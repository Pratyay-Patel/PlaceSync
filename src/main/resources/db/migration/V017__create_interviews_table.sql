CREATE TABLE IF NOT EXISTS interviews (
    id                      UUID                NOT NULL DEFAULT gen_random_uuid(),
    application_id          UUID                NOT NULL,
    round_number            SMALLINT            NOT NULL,
    interview_type          interview_type      NOT NULL,
    status                  interview_status    NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at            TIMESTAMPTZ         NOT NULL,
    duration_minutes        SMALLINT            NOT NULL,
    meeting_link            VARCHAR(1000)       NULL,
    venue                   TEXT                NULL,
    cancellation_reason     TEXT                NULL,
    created_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_interviews PRIMARY KEY (id),
    CONSTRAINT fk_interviews_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE RESTRICT,
    CONSTRAINT chk_interviews_round_number    CHECK (round_number >= 1),
    CONSTRAINT chk_interviews_duration        CHECK (duration_minutes > 0 AND duration_minutes <= 480),
    CONSTRAINT chk_interviews_online_meeting_link CHECK (interview_type != 'ONLINE'  OR meeting_link IS NOT NULL),
    CONSTRAINT chk_interviews_offline_venue       CHECK (interview_type != 'OFFLINE' OR venue IS NOT NULL),
    CONSTRAINT chk_interviews_cancellation_reason CHECK (status != 'CANCELLED' OR cancellation_reason IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_interviews_application_id ON interviews (application_id);
CREATE INDEX IF NOT EXISTS idx_interviews_scheduled_at   ON interviews (scheduled_at) WHERE status IN ('SCHEDULED', 'RESCHEDULED');

CREATE TRIGGER trg_interviews_updated_at
    BEFORE UPDATE ON interviews
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  interviews                    IS 'Interview schedule entries per application. Supports multiple rounds per application.';
COMMENT ON COLUMN interviews.round_number       IS 'Interview round (1 = first round). Multiple rows per application represent multiple rounds.';
COMMENT ON COLUMN interviews.cancellation_reason IS 'Required when status = CANCELLED.';
