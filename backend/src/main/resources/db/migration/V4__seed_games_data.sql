-- Insert initial games (롤, 배그, 발로란트)
INSERT INTO games (name, genre, description) VALUES
('League of Legends', 'MOBA', '5v5 팀 전략 게임'),
('PUBG', 'Battle Royale', '배틀로얄 서바이벌 슈팅 게임'),
('Valorant', 'FPS', '5v5 택티컬 슈팅 게임');

-- Insert common options (공통 매칭 옵션)
-- 나이대 옵션 (모든 게임 공통)
INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'age_range', 'SELECT', '["10대", "20대", "30대", "40대", "50대+"]'::jsonb, true
FROM games;

-- 마이크 사용 여부 (모든 게임 공통)
INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'mic_required', 'BOOLEAN', 'null'::jsonb, true
FROM games;

-- 플레이 스타일 (모든 게임 공통)
INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'play_style', 'SELECT', '["캐주얼", "경쟁", "랭크"]'::jsonb, true
FROM games;

-- League of Legends specific options
INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'position', 'MULTI_SELECT', '["탑", "정글", "미드", "원딜", "서폿"]'::jsonb, false
FROM games WHERE name = 'League of Legends';

INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'tier', 'SELECT', '["아이언", "브론즈", "실버", "골드", "플래티넘", "다이아", "마스터", "그랜드마스터", "챌린저"]'::jsonb, false
FROM games WHERE name = 'League of Legends';

-- PUBG specific options
INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'squad_size', 'SELECT', '["솔로", "듀오", "스쿼드"]'::jsonb, false
FROM games WHERE name = 'PUBG';

INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'map_preference', 'MULTI_SELECT', '["에란겔", "미라마", "사녹", "비켄디"]'::jsonb, false
FROM games WHERE name = 'PUBG';

INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'tier', 'SELECT', '["브론즈", "실버", "골드", "플래티넘", "다이아", "마스터"]'::jsonb, false
FROM games WHERE name = 'PUBG';

-- Valorant specific options
INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'agent_preference', 'MULTI_SELECT', '["듀얼리스트", "컨트롤러", "센티넬", "이니시에이터"]'::jsonb, false
FROM games WHERE name = 'Valorant';

INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'role', 'SELECT', '["엔트리", "서포트", "IGL", "플렉스"]'::jsonb, false
FROM games WHERE name = 'Valorant';

INSERT INTO game_options (game_id, option_key, option_type, option_values, is_common)
SELECT id, 'tier', 'SELECT', '["아이언", "브론즈", "실버", "골드", "플래티넘", "다이아", "불멸", "레디언트"]'::jsonb, false
FROM games WHERE name = 'Valorant';
