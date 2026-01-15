CREATE TABLE IF NOT EXISTS point (
                                     point_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS point_reservation (
    order_id VARCHAR(255) PRIMARY KEY,
    point_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE point;
TRUNCATE TABLE point_reservation;

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
           'PNT-INT-ONLY-002',
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

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-CONFIRM-001',
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
           'PNT-INT-COMPENSATE-001',
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
           'PNT-INT-CIRCUIT-ON1',
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
           'PNT-INT-CIRCUIT-ON2',
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
           'PNT-INT-CIRCUIT-ON3',
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
           'PNT-INT-CIRCUIT-OFF1',
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
           'PNT-INT-CIRCUIT-OFF2',
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
           'PNT-INT-CIRCUIT-OFF3',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
