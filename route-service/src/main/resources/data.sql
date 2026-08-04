-- Routes reference train IDs and station IDs from the corresponding services' databases.
-- These IDs align with the seed data in train-service/data.sql and station-service/data.sql.
-- Train IDs: 1=Rajdhani, 2=Bhopal Shatabdi, 3=Mumbai Rajdhani, 4=Delhi-Chennai SF, 5=Mumbai Local
-- Station IDs: 1=NDLS, 2=MMCT, 3=MAS, 4=HWH, 5=SBC, 6=BPL, 7=ADI
INSERT INTO routes (train_id, source_station_id, destination_station_id, departure_time, arrival_time, distance_km, fare) VALUES
(1, 1, 4, '2026-07-01T16:55:00', '2026-07-02T10:00:00', 1450, 1800.00),
(2, 1, 6, '2026-07-01T06:00:00', '2026-07-01T14:00:00',  705,  900.00),
(3, 2, 1, '2026-07-01T17:00:00', '2026-07-02T08:35:00', 1384, 1600.00),
(4, 1, 3, '2026-07-01T22:30:00', '2026-07-03T07:45:00', 2180, 2200.00),
(5, 2, 7, '2026-07-01T08:00:00', '2026-07-01T11:00:00',  492,  600.00);
