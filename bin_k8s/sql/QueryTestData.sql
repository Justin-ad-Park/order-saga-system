SELECT * FROM order_orchestrator_db.OUTBOX_MESSAGE ;
SELECT * FROM order_orchestrator_db.ORDER_SAGA ;
SELECT * FROM order_orchestrator_db.ORDER_ITEM ;

SELECT * FROM point_db.point;
SELECT * FROM point_db.point_reservation;
SELECT * FROM coupon_db.coupon;
SELECT * FROM coupon_db.coupon_reservation;

SELECT * FROM point_db.point_snapshot;
SELECT * FROM coupon_db.coupon_snapshot;


/*
truncate table order_orchestrator_db.OUTBOX_MESSAGE;
delete from order_orchestrator_db.ORDER_SAGA where id < 99999;
truncate table order_orchestrator_db.ORDER_ITEM ;
*/
