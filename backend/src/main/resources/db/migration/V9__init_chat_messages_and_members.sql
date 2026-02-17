-- Create chat_room_members table
CREATE TABLE chat_room_members (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_chat_member UNIQUE (chat_room_id, user_id)
);

-- Create indexes
CREATE INDEX idx_chat_room_members_chat_room_id ON chat_room_members(chat_room_id);
CREATE INDEX idx_chat_room_members_user_id ON chat_room_members(user_id);

-- Create chat_messages table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_chat_messages_chat_room_id ON chat_messages(chat_room_id);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at DESC);

-- Add comments
COMMENT ON TABLE chat_room_members IS 'Members of chat rooms';
COMMENT ON TABLE chat_messages IS 'Chat messages (storage depends on chat room type)';
