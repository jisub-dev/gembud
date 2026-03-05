-- Fix temperature column precision: DECIMAL(4,2) can only hold up to 99.99,
-- but the valid range allows 100.0. Change to DECIMAL(5,2) to allow 100.00.
ALTER TABLE users ALTER COLUMN temperature TYPE DECIMAL(5, 2);
