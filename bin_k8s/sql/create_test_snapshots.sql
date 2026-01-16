CREATE DATABASE IF NOT EXISTS coupon_db;
USE coupon_db;

DROP PROCEDURE IF EXISTS sp_reset_coupon_test_data;
DELIMITER $$
CREATE PROCEDURE sp_reset_coupon_test_data()
BEGIN
  TRUNCATE TABLE coupon;
  TRUNCATE TABLE coupon_reservation;
  INSERT INTO coupon SELECT * FROM coupon_snapshot;
END$$
DELIMITER ;

CREATE DATABASE IF NOT EXISTS order_orchestrator_db;
USE order_orchestrator_db;

DROP PROCEDURE IF EXISTS sp_truncate_order_orchestrator_test_data;
DELIMITER $$
CREATE PROCEDURE sp_truncate_order_orchestrator_test_data()
BEGIN
  SET FOREIGN_KEY_CHECKS = 0;
  TRUNCATE TABLE order_item;
  TRUNCATE TABLE outbox_message;
  TRUNCATE TABLE order_saga;
  SET FOREIGN_KEY_CHECKS = 1;
END$$
DELIMITER ;

CREATE DATABASE IF NOT EXISTS point_db;
USE point_db;

DROP PROCEDURE IF EXISTS sp_reset_point_test_data;
DELIMITER $$
CREATE PROCEDURE sp_reset_point_test_data()
BEGIN
  TRUNCATE TABLE point;
  TRUNCATE TABLE point_reservation;
  INSERT INTO point SELECT * FROM point_snapshot;
END$$
DELIMITER ;
