-- Development seed data: destinations / pois / qa_rules
-- db/schema.sql only creates tables. Without this data, destination search, hot destinations,
-- the customize Q&A flow and the itinerary engine have nothing to work with.
-- Insert-only (no schema changes); run it once on a database created with db/schema.sql.
-- Place names and addresses are real Chinese locations and are intentionally kept in Chinese.

USE `travel_app`;

-- Destinations: 4 cities + 2 scenic areas under Dali (type 3 = city, 4 = scenic area)
INSERT INTO `destinations` (`id`, `name`, `name_pinyin`, `type`, `parent_id`, `cover_image`, `description`, `popularity_score`) VALUES
(1, '大理', 'dali', 3, NULL, 'https://picsum.photos/seed/dali/600/400', 'Cangshan Mountain and Erhai Lake, slow-paced old-town life', 98),
(2, '丽江', 'lijiang', 3, NULL, 'https://picsum.photos/seed/lijiang/600/400', 'An old town on the Tea Horse Road at the foot of a snow mountain', 95),
(3, '成都', 'chengdu', 3, NULL, 'https://picsum.photos/seed/chengdu/600/400', 'Capital of food and pandas, with a famously relaxed pace of life', 92),
(4, '西安', 'xian', 3, NULL, 'https://picsum.photos/seed/xian/600/400', 'Ancient capital of thirteen dynasties, steeped in history', 90),
(5, '大理古城', 'daliguchen', 4, 1, 'https://picsum.photos/seed/daliguchen/600/400', 'The heart of Dali, home to the Renmin Road bar street', 80),
(6, '洱海', 'erhai', 4, 1, 'https://picsum.photos/seed/erhai', 'Cycle around the lake and take in the sunset', 85);

-- POIs: only Dali (1) and Chengdu (3) have enough data for itinerary generation demos.
-- type: 1 attraction / 2 food / 3 accommodation. destination_name is what the itinerary engine matches on.
INSERT INTO `pois` (`name`, `type`, `destination_id`, `destination_name`, `address`, `avg_cost`, `rating`, `images`, `source`) VALUES
('崇圣寺三塔', 1, 1, '大理', '大理市苍山应乐峰下', 75.00, 4.5, '["https://picsum.photos/seed/santa/400/300"]', 1),
('洱海生态廊道', 1, 1, '大理', '大理市洱海沿岸', 0.00, 4.7, '["https://picsum.photos/seed/erhailane/400/300"]', 1),
('喜洲古镇', 1, 1, '大理', '大理市喜洲镇', 0.00, 4.4, '["https://picsum.photos/seed/xizhou/400/300"]', 1),
('才村码头', 1, 1, '大理', '大理市才村', 0.00, 4.3, '["https://picsum.photos/seed/caicun/400/300"]', 1),
('大理古城南门', 1, 1, '大理', '大理市大理古城', 0.00, 4.2, '["https://picsum.photos/seed/nanmen/400/300"]', 1),
('白族三道茶馆', 2, 1, '大理', '大理市人民路', 60.00, 4.3, '["https://picsum.photos/seed/sandaocha/400/300"]', 1),
('砂锅鱼风味餐厅', 2, 1, '大理', '大理市洱海月', 90.00, 4.4, '["https://picsum.photos/seed/shaguoyu/400/300"]', 1),
('人民路小吃一条街', 2, 1, '大理', '大理市人民路', 40.00, 4.1, '["https://picsum.photos/seed/renminlu/400/300"]', 1),
('洱海边客栈', 3, 1, '大理', '大理市才村', 320.00, 4.6, '["https://picsum.photos/seed/inn1/400/300"]', 1),
('大理古城精品民宿', 3, 1, '大理', '大理市大理古城', 280.00, 4.5, '["https://picsum.photos/seed/inn2/400/300"]', 1),

('宽窄巷子', 1, 3, '成都', '成都市青羊区', 0.00, 4.3, '["https://picsum.photos/seed/kuanzhai/400/300"]', 1),
('成都大熊猫繁育研究基地', 1, 3, '成都', '成都市成华区', 58.00, 4.8, '["https://picsum.photos/seed/panda/400/300"]', 1),
('武侯祠', 1, 3, '成都', '成都市武侯区', 50.00, 4.4, '["https://picsum.photos/seed/wuhou/400/300"]', 1),
('锦里古街', 1, 3, '成都', '成都市武侯区', 0.00, 4.2, '["https://picsum.photos/seed/jinli/400/300"]', 1),
('成都博物馆', 1, 3, '成都', '成都市青羊区', 0.00, 4.5, '["https://picsum.photos/seed/cdmuseum/400/300"]', 1),
('陈麻婆豆腐', 2, 3, '成都', '成都市青羊区', 70.00, 4.3, '["https://picsum.photos/seed/mapo/400/300"]', 1),
('龙抄手', 2, 3, '成都', '成都市锦江区', 35.00, 4.1, '["https://picsum.photos/seed/longchaoshou/400/300"]', 1),
('蜀大侠火锅', 2, 3, '成都', '成都市高新区', 110.00, 4.6, '["https://picsum.photos/seed/shudaxia/400/300"]', 1),
('成都市中心快捷酒店', 3, 3, '成都', '成都市锦江区', 260.00, 4.3, '["https://picsum.photos/seed/inn3/400/300"]', 1),
('宽窄巷子精品酒店', 3, 3, '成都', '成都市青羊区', 350.00, 4.6, '["https://picsum.photos/seed/inn4/400/300"]', 1);

-- Customize Q&A questions. question_key must map to a customize_answers column, except
-- destinationIds: it is stored in customize_destinations instead, and the frontend renders it
-- specially via inputType = destination_search.
INSERT INTO `qa_rules` (`question_key`, `question_text`, `input_type`, `options`, `is_required`, `sort_order`, `is_active`) VALUES
('destinationIds', 'Where do you want to travel?', 'destination_search', NULL, 1, 1, 1),
('days', 'How many days are you planning for?', 'stepper', '{"min":1,"max":30}', 1, 2, 1),
('departDate', 'What are your departure and return dates?', 'date_range', NULL, 1, 3, 1),
('departCity', 'Which city are you departing from?', 'text', NULL, 1, 4, 1),
('hasBookedTransport', 'Have you already booked flights/train tickets?', 'chip_single', NULL, 1, 5, 1),
('budgetMin', 'What is your approximate budget for this trip?', 'slider', '{"min":1000,"max":15000,"step":500}', 1, 6, 1),
('travelerCount', 'How many travelers?', 'stepper', '{"min":1,"max":20}', 1, 7, 1),
('travelStyle', 'What travel style do you prefer?', 'chip_single', '{"options":["checklist","deep_experience","leisure","adventure"]}', 0, 8, 1);
