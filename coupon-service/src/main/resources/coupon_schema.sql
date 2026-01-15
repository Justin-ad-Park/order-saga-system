CREATE TABLE IF NOT EXISTS coupon (
                                      coupon_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS coupon_reservation (
    order_id VARCHAR(255) PRIMARY KEY,
    coupon_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE coupon;
TRUNCATE TABLE coupon_reservation;

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-BOTH-001',
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
           'CPN-INT-BOTH-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-ONLY-001',
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
           'CPN-INT-ONLY-002',
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
           'CPN-SVC-001',
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

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-CONFIRM-001',
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
           'CPN-INT-COMPENSATE-001',
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
           'CPN-INT-CIRCUIT-ON1',
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
           'CPN-INT-CIRCUIT-ON2',
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
           'CPN-INT-CIRCUIT-ON3',
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
           'CPN-INT-CIRCUIT-OFF1',
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
           'CPN-INT-CIRCUIT-OFF2',
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
           'CPN-INT-CIRCUIT-OFF3',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
