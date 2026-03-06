-- Ensure only one friend relation per unordered user pair
-- (A->B and B->A cannot coexist)

WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY LEAST(user_id, friend_id), GREATEST(user_id, friend_id)
            ORDER BY
                CASE status
                    WHEN 'ACCEPTED' THEN 1
                    WHEN 'PENDING' THEN 2
                    ELSE 3
                END,
                id
        ) AS rn
    FROM friends
)
DELETE FROM friends f
USING ranked r
WHERE f.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_friends_unordered_pair
    ON friends (LEAST(user_id, friend_id), GREATEST(user_id, friend_id));
