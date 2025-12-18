CREATE TABLE IF NOT EXISTS coupon (
    coupon_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
);

MERGE INTO coupon (coupon_number, status, issued_at, expired_at)
KEY(coupon_number)
VALUES ('CPN-001', 'AVAILABLE', CURRENT_TIMESTAMP, DATEADD('DAY', 30, CURRENT_TIMESTAMP));

-- MERGE INTO coupon (coupon_number, status, issued_at, expired_at)
--     KEY(coupon_number)
--     VALUES ('CPN-001', 'AVAILABLE', CURRENT_TIMESTAMP, DATEADD('DAY', 30, CURRENT_TIMESTAMP));
