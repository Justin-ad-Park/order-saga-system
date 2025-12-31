CREATE TABLE IF NOT EXISTS point (
                                     point_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE point;

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-BOTH-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-BOTH-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-ONLY-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-SVC-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
