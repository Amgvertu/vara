-- Добавление России
INSERT INTO country (name, code, phone_code)
VALUES ('Россия', 'RU', '7');

-- Добавление регионов (пример)
WITH russia AS (SELECT id FROM country WHERE code = 'RU')
INSERT INTO region (name, country_id, auto_code)
VALUES
    ('Москва', (SELECT id FROM russia), '77'),
    ('Московская область', (SELECT id FROM russia), '50'),
    ('Санкт-Петербург', (SELECT id FROM russia), '78'),
    ('Ленинградская область', (SELECT id FROM russia), '47'),
    ('Республика Татарстан', (SELECT id FROM russia), '16');

-- Добавление городов
WITH moscow_region AS (SELECT id FROM region WHERE auto_code = '77')
INSERT INTO city (name, region_id)
VALUES ('Москва', (SELECT id FROM moscow_region));

WITH spb_region AS (SELECT id FROM region WHERE auto_code = '78')
INSERT INTO city (name, region_id)
VALUES ('Санкт-Петербург', (SELECT id FROM spb_region));

WITH tatarstan AS (SELECT id FROM region WHERE auto_code = '16')
INSERT INTO city (name, region_id)
VALUES ('Казань', (SELECT id FROM tatarstan));