INSERT INTO users (id, name, balance) VALUES
    (1, 'Alex Johnson', 20.00),
    (2, 'Maria Smith', 10.00);

INSERT INTO vehicles (id, plate_number, user_id) VALUES
    (1, 'CA1234AB', 1),
    (2, 'CB5678CD', 1),
    (3, 'PB1234CD', 2);

INSERT INTO cities (id, name) VALUES
    (1, 'Sofia'),
    (2, 'Plovdiv');

INSERT INTO parking_zones (id, city_id, name, price_per_hour, active) VALUES
    (1, 1, 'Blue Zone', 2.00, true),
    (2, 1, 'Green Zone', 1.00, true),
    (3, 2, 'Blue Zone', 1.50, true),
    (4, 2, 'Green Zone', 1.00, true),
    (5, 2, 'Red Zone', 3.00, false);

-- Fixtures use explicit IDs; move identity sequences past them so generated IDs do not collide.
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('vehicles', 'id'), (SELECT max(id) FROM vehicles));
SELECT setval(pg_get_serial_sequence('cities', 'id'), (SELECT max(id) FROM cities));
SELECT setval(pg_get_serial_sequence('parking_zones', 'id'), (SELECT max(id) FROM parking_zones));
