ALTER TABLE rsvps ADD COLUMN vegetarian_count INTEGER NOT NULL DEFAULT 0 CHECK (vegetarian_count BETWEEN 0 AND 20);
ALTER TABLE rsvps ADD COLUMN non_vegetarian_count INTEGER NOT NULL DEFAULT 0 CHECK (non_vegetarian_count BETWEEN 0 AND 20);
