-- Create evaluations table
CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    evaluator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    evaluated_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    manner_score INT NOT NULL,
    skill_score INT NOT NULL,
    communication_score INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_manner_score CHECK (manner_score >= 1 AND manner_score <= 5),
    CONSTRAINT chk_skill_score CHECK (skill_score >= 1 AND skill_score <= 5),
    CONSTRAINT chk_communication_score CHECK (communication_score >= 1 AND communication_score <= 5),
    CONSTRAINT unique_evaluation UNIQUE (room_id, evaluator_id, evaluated_id)
);

-- Create indexes
CREATE INDEX idx_evaluations_room_id ON evaluations(room_id);
CREATE INDEX idx_evaluations_evaluator_id ON evaluations(evaluator_id);
CREATE INDEX idx_evaluations_evaluated_id ON evaluations(evaluated_id);

-- Add comments
COMMENT ON COLUMN evaluations.manner_score IS 'Manner score (1-5)';
COMMENT ON COLUMN evaluations.skill_score IS 'Skill score (1-5)';
COMMENT ON COLUMN evaluations.communication_score IS 'Communication score (1-5)';
COMMENT ON TABLE evaluations IS 'User evaluations after room completion';
