-- Plovdiv zones charge the same hourly rate as the Sofia zone with the same name.
-- Sessions keep the rate captured at start, so existing parking is unaffected.
UPDATE parking_zones AS plovdiv
SET price_per_hour = sofia.price_per_hour
FROM parking_zones AS sofia
WHERE plovdiv.city_id = (SELECT id FROM cities WHERE name = 'Plovdiv')
  AND sofia.city_id = (SELECT id FROM cities WHERE name = 'Sofia')
  AND sofia.name = plovdiv.name;
