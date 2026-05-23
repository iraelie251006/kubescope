ALTER TABLE refresh_token
ADD COLUMN family VARCHAR(255);

UPDATE refresh_token SET family = 'legacy';

ALTER TABLE refresh_token
ALTER COLUMN family SET NOT NULL;