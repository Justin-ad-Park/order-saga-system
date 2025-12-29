DELIMITER $$

CREATE PROCEDURE truncate_if_exists(IN tbl_name VARCHAR(255))
BEGIN
IF EXISTS (
SELECT 1
FROM information_schema.tables
WHERE table_schema = DATABASE()
AND table_name = tbl_name
) THEN
SET @s = CONCAT('TRUNCATE TABLE ', tbl_name);
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END IF;
END$$

DELIMITER ;