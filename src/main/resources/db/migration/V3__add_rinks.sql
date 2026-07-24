-- Добавление тестовых ЛДС
WITH moscow AS (SELECT id FROM city WHERE name = 'Москва'),
     russia AS (SELECT id FROM country WHERE code = 'RU'),
     moscow_region AS (SELECT id FROM region WHERE auto_code = '77')
INSERT INTO rinks (name, country_id, region_id, city_id, address, phone, latitude, longitude, features)
VALUES
    ('ЦСКА Арена',
     (SELECT id FROM russia),
     (SELECT id FROM moscow_region),
     (SELECT id FROM moscow),
     'Москва, Ленинградский проспект, 39',
     '+74951234567',
     55.7915,
     37.5598,
     ARRAY['парковка', 'раздевалки', 'кафе']),

    ('Мегаспорт',
     (SELECT id FROM russia),
     (SELECT id FROM moscow_region),
     (SELECT id FROM moscow),
     'Москва, Ходынский бульвар, 3',
     '+74957654321',
     55.7887,
     37.5412,
     ARRAY['парковка', 'раздевалки', 'трибуны']),

    ('ВТБ Арена',
     (SELECT id FROM russia),
     (SELECT id FROM moscow_region),
     (SELECT id FROM moscow),
     'Москва, Ленинградский проспект, 36',
     '+74959876543',
     55.7921,
     37.5589,
     ARRAY['парковка', 'раздевалки', 'кафе', 'магазин']);

WITH spb AS (SELECT id FROM city WHERE name = 'Санкт-Петербург'),
     russia AS (SELECT id FROM country WHERE code = 'RU'),
     spb_region AS (SELECT id FROM region WHERE auto_code = '78')
INSERT INTO rinks (name, country_id, region_id, city_id, address, phone, latitude, longitude, features)
VALUES
    ('СКА Арена',
     (SELECT id FROM russia),
     (SELECT id FROM spb_region),
     (SELECT id FROM spb),
     'Санкт-Петербург, пр. Юрия Гагарина, 8',
     '+78121234567',
     59.8498,
     30.3275,
     ARRAY['парковка', 'раздевалки', 'кафе']),

    ('Юбилейный',
     (SELECT id FROM russia),
     (SELECT id FROM spb_region),
     (SELECT id FROM spb),
     'Санкт-Петербург, пр. Добролюбова, 18',
     '+78127654321',
     59.9528,
     30.2876,
     ARRAY['парковка', 'раздевалки']);

WITH kazan AS (SELECT id FROM city WHERE name = 'Казань'),
     russia AS (SELECT id FROM country WHERE code = 'RU'),
     tatarstan AS (SELECT id FROM region WHERE auto_code = '16')
INSERT INTO rinks (name, country_id, region_id, city_id, address, phone, latitude, longitude, features)
VALUES
    ('Татнефть Арена',
     (SELECT id FROM russia),
     (SELECT id FROM tatarstan),
     (SELECT id FROM kazan),
     'Казань, ул. Чистопольская, 42',
     '+78431234567',
     55.8205,
     49.1234,
     ARRAY['парковка', 'раздевалки', 'кафе', 'трибуны']);