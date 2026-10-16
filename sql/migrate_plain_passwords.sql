-- EXISTING installations only. Export a backup in phpMyAdmin first.
-- Run once. Fresh installations use setup.sql instead.
-- Old hashes cannot be recovered; set a new password for each account afterward.
USE tapin_rebuild;
ALTER TABLE accounts CHANGE COLUMN password_hash password VARCHAR(255) NOT NULL;
UPDATE accounts SET password = '';
-- Browse accounts > Edit each account > enter its new password > Go.
-- Leave the Function selection empty for the password field.
