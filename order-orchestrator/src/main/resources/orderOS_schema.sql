SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM order_item;
DELETE FROM outbox_message;
DELETE FROM order_saga;

SET FOREIGN_KEY_CHECKS = 1;

/*
ALTER TABLE outbox_message
    MODIFY COLUMN coupon_status VARCHAR(255) NOT NULL AFTER order_status;

ALTER TABLE outbox_message
    MODIFY COLUMN point_status VARCHAR(255) NOT NULL AFTER coupon_status;

ALTER TABLE outbox_message
    MODIFY COLUMN order_status VARCHAR(255) NOT NULL;
*/