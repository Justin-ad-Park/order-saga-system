CREATE TABLE IF NOT EXISTS coupon (
                                      coupon_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE coupon;

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-BOTH-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-ONLY-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'C-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
