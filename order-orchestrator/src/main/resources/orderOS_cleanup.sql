truncate table order_item;
delete from order_saga where coupon_number = 'CPN-001';
truncate table outbox_message;