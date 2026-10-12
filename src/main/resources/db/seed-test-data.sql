-- Idempotent local test data (sample users, notes, comments); does not clear existing data.
-- Run after db/schema.sql and db/seed_data.sql. Sample user-generated content is intentionally in Chinese.
-- The sample users have a placeholder password hash; register your own account (SMS code is printed in the backend log) to log in.
USE travel_app;

INSERT IGNORE INTO users (nickname, avatar_url, phone, password_hash, gender, status) VALUES
('测试旅行者小王', 'https://picsum.photos/seed/user1/160', '13900000001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1, 0),
('测试旅行者小李', 'https://picsum.photos/seed/user2/160', '13900000002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2, 0);

-- destinations.name has no unique key, so skip cities that already exist (e.g. from db/seed_data.sql)
INSERT INTO destinations (name, name_pinyin, type, cover_image, description, popularity_score)
SELECT * FROM (
  SELECT '成都' AS name, 'chengdu' AS name_pinyin, 3 AS type, 'https://picsum.photos/seed/chengdu/900/500' AS cover_image, 'A city of slow living and great food' AS description, 980 AS popularity_score
  UNION ALL SELECT '九寨沟', 'jiuzhaigou', 4, 'https://picsum.photos/seed/jiuzhaigou/900/500', 'Highland lakes and colorful autumn forests', 950
  UNION ALL SELECT '北京', 'beijing', 3, 'https://picsum.photos/seed/beijing/900/500', 'Where history meets the modern city', 920
) AS d
WHERE NOT EXISTS (SELECT 1 FROM destinations x WHERE x.name = d.name);

INSERT IGNORE INTO tags (name, category) VALUES ('美食', 1), ('自然风光', 1), ('亲子游', 2), ('周末游', 1);

SET @user1 = (SELECT id FROM users WHERE phone = '13900000001');
SET @user2 = (SELECT id FROM users WHERE phone = '13900000002');
SET @chengdu = (SELECT id FROM destinations WHERE name = '成都' ORDER BY id LIMIT 1);
SET @jiuzhaigou = (SELECT id FROM destinations WHERE name = '九寨沟' ORDER BY id LIMIT 1);

INSERT INTO travel_notes (author_id, title, cover_image, content, destination_id, days, budget_per_person, transport_type, best_season, source, status, view_count, like_count, favorite_count, comment_count, published_at)
SELECT @user1, '成都三天两夜：火锅、熊猫与老街', 'https://picsum.photos/seed/note-chengdu/900/500', '第一天去宽窄巷子，第二天看大熊猫，晚上吃火锅。', @chengdu, 3, 1800.00, '高铁', '春秋', 1, 2, 128, 1, 1, 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM travel_notes WHERE title = '成都三天两夜：火锅、熊猫与老街');
INSERT INTO travel_notes (author_id, title, cover_image, content, destination_id, days, budget_per_person, transport_type, best_season, source, status, view_count, like_count, favorite_count, comment_count, published_at)
SELECT @user2, '九寨沟秋日摄影攻略', 'https://picsum.photos/seed/note-jiuzhaigou/900/500', '十月的九寨沟色彩最丰富，建议提前预约门票。', @jiuzhaigou, 2, 2300.00, '飞机', '秋季', 1, 2, 86, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM travel_notes WHERE title = '九寨沟秋日摄影攻略');

SET @note1 = (SELECT id FROM travel_notes WHERE title = '成都三天两夜：火锅、熊猫与老街');
SET @note2 = (SELECT id FROM travel_notes WHERE title = '九寨沟秋日摄影攻略');
INSERT IGNORE INTO travel_note_images (note_id, image_url, sort_order) VALUES (@note1, 'https://picsum.photos/seed/note-chengdu-1/900/600', 1), (@note2, 'https://picsum.photos/seed/note-jiuzhaigou-1/900/600', 1);
INSERT IGNORE INTO comments (note_id, user_id, content) VALUES (@note1, @user2, '路线很实用，已经收藏了！'), (@note1, @user1, '谢谢，祝你旅途愉快。'), (@note2, @user1, '秋天一定要去一次。');
INSERT IGNORE INTO likes (user_id, target_type, target_id) VALUES (@user2, 1, @note1), (@user1, 1, @note2);
INSERT IGNORE INTO favorites (user_id, note_id) VALUES (@user2, @note1);
