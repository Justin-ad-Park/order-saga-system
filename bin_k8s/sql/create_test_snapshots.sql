CREATE DATABASE IF NOT EXISTS coupon_db;
USE coupon_db;

CREATE TABLE IF NOT EXISTS coupon_snapshot LIKE coupon;
TRUNCATE TABLE coupon_snapshot;
INSERT INTO coupon_snapshot
SELECT *
FROM coupon;

DROP PROCEDURE IF EXISTS sp_reset_coupon_test_data;
DELIMITER $$
CREATE PROCEDURE sp_reset_coupon_test_data()
BEGIN
  TRUNCATE TABLE coupon;
  INSERT INTO coupon SELECT * FROM coupon_snapshot;
END$$
DELIMITER ;

CREATE DATABASE IF NOT EXISTS point_db;
USE point_db;

CREATE TABLE IF NOT EXISTS point_snapshot LIKE point;
TRUNCATE TABLE point_snapshot;
INSERT INTO point_snapshot
SELECT *
FROM point;

DROP PROCEDURE IF EXISTS sp_reset_point_test_data;
DELIMITER $$
CREATE PROCEDURE sp_reset_point_test_data()
BEGIN
  TRUNCATE TABLE point;
  INSERT INTO point SELECT * FROM point_snapshot;
END$$
DELIMITER ;
