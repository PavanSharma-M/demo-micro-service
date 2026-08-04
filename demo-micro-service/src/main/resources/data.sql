-- ─── Trains ───────────────────────────────────────────────────
INSERT INTO trains (train_number, train_name, train_type, total_seats, available_seats) VALUES
('12301', 'Rajdhani Express',    'RAJDHANI',  500, 500),
('12002', 'Bhopal Shatabdi',     'SHATABDI',  400, 400),
('12951', 'Mumbai Rajdhani',     'RAJDHANI',  450, 450),
('11001', 'Delhi-Chennai SF',    'SUPERFAST', 600, 600),
('19001', 'Mumbai Local',        'LOCAL',     800, 800);

-- ─── Stations ─────────────────────────────────────────────────
INSERT INTO stations (station_code, station_name, city, state) VALUES
('NDLS', 'New Delhi',         'New Delhi',  'Delhi'),
('MMCT', 'Mumbai Central',    'Mumbai',     'Maharashtra'),
('MAS',  'Chennai Central',   'Chennai',    'Tamil Nadu'),
('HWH',  'Howrah Junction',   'Kolkata',    'West Bengal'),
('SBC',  'KSR Bengaluru',     'Bengaluru',  'Karnataka'),
('BPL',  'Bhopal Junction',   'Bhopal',     'Madhya Pradesh'),
('ADI',  'Ahmedabad Junction','Ahmedabad',  'Gujarat');

-- ─── Routes ───────────────────────────────────────────────────
INSERT INTO routes (train_id, source_station_id, destination_station_id, departure_time, arrival_time, distance_km, fare) VALUES
(1, 1, 4, '2026-07-01T16:55:00', '2026-07-02T10:00:00', 1450, 1800.00),
(2, 1, 6, '2026-07-01T06:00:00', '2026-07-01T14:00:00',  705,  900.00),
(3, 2, 1, '2026-07-01T17:00:00', '2026-07-02T08:35:00', 1384, 1600.00),
(4, 1, 3, '2026-07-01T22:30:00', '2026-07-03T07:45:00', 2180, 2200.00),
(5, 2, 7, '2026-07-01T08:00:00', '2026-07-01T11:00:00',  492,  600.00);
