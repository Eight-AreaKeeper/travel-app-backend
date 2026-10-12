-- ============================================================
-- Travel app database schema (MySQL 8.0)
--
-- Database:  travel_app
-- Charset:   utf8mb4 / utf8mb4_0900_ai_ci, InnoDB
-- Naming:    snake_case tables and columns, primary key `id`, foreign keys `xxx_id`,
--            timestamps `created_at` / `updated_at`
--
-- Run the whole file top to bottom on a fresh MySQL instance; tables are already ordered by
-- foreign-key dependency. This file already includes the changes from db/migrations/, so a
-- fresh database does not need the migrations.
--
-- WARNING: every table is created with DROP TABLE IF EXISTS first. Do NOT run this against a
-- database whose data you want to keep; use the scripts in db/migrations/ to upgrade instead.
-- ============================================================

CREATE DATABASE IF NOT EXISTS `travel_app`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `travel_app`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. Users and staff
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nickname` VARCHAR(50) NOT NULL,
  `avatar_url` VARCHAR(255) NULL,
  `phone` VARCHAR(20) NULL,
  `email` VARCHAR(100) NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `gender` TINYINT NULL COMMENT '0 unknown / 1 male / 2 female',
  `birthday` DATE NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 active / 1 banned',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_phone` (`phone`),
  UNIQUE KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='End users';

DROP TABLE IF EXISTS `admin_users`;
CREATE TABLE `admin_users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` VARCHAR(20) NOT NULL COMMENT 'editor (content ops) / advisor (travel advisor) / admin',
  `status` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Internal staff: content editors / travel advisors / admins';

-- ------------------------------------------------------------
-- 2. Reference data (shared by both modules)
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `destinations`;
CREATE TABLE `destinations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `name_pinyin` VARCHAR(100) NOT NULL,
  `type` TINYINT NOT NULL COMMENT '1 country / 2 province / 3 city / 4 scenic area',
  `parent_id` BIGINT UNSIGNED NULL,
  `cover_image` VARCHAR(255) NULL,
  `description` TEXT NULL,
  `popularity_score` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_name_pinyin` (`name_pinyin`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `fk_destinations_parent` FOREIGN KEY (`parent_id`) REFERENCES `destinations` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Destinations (country / province / city / scenic area, self-referencing hierarchy)';

DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(30) NOT NULL,
  `category` TINYINT NOT NULL COMMENT '1 travel style / 2 interest',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tags';

DROP TABLE IF EXISTS `pois`;
CREATE TABLE `pois` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `type` TINYINT NOT NULL COMMENT '1 attraction / 2 food / 3 accommodation',
  `destination_id` BIGINT UNSIGNED NULL COMMENT 'Set only when it matches a local destinations row; display enhancement only. POI lookup uses destination_name',
  `destination_name` VARCHAR(100) NOT NULL COMMENT 'Destination name (free text); the customize module looks up POIs by this column, not by destination_id',
  `address` VARCHAR(255) NULL,
  `latitude` DECIMAL(10,7) NULL,
  `longitude` DECIMAL(10,7) NULL,
  `avg_cost` DECIMAL(10,2) NULL COMMENT 'Reference cost per person',
  `rating` DECIMAL(2,1) NULL,
  `images` JSON NULL COMMENT 'Array of image URLs',
  `source` TINYINT NOT NULL COMMENT '1 entered by staff / 2 third-party API / 3 crawled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_destination_name_type` (`destination_name`, `type`),
  CONSTRAINT `fk_pois_destination` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Attractions / restaurants / accommodation; real places referenced when generating itineraries';

-- ------------------------------------------------------------
-- 3. Module 1: travel notes and guides
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `travel_notes`;
CREATE TABLE `travel_notes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `author_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `cover_image` VARCHAR(255) NULL,
  `content` MEDIUMTEXT NOT NULL COMMENT 'Rich text / Markdown body',
  `destination_id` BIGINT UNSIGNED NOT NULL COMMENT 'Primary destination, used for search / filtering',
  `days` INT NOT NULL,
  `budget_per_person` DECIMAL(10,2) NULL,
  `transport_type` VARCHAR(30) NULL,
  `best_season` VARCHAR(20) NULL,
  `source` TINYINT NOT NULL COMMENT '1 UGC / 2 official (PGC)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 draft / 1 pending review / 2 published / 3 rejected',
  `view_count` INT NOT NULL DEFAULT 0,
  `like_count` INT NOT NULL DEFAULT 0,
  `favorite_count` INT NOT NULL DEFAULT 0,
  `comment_count` INT NOT NULL DEFAULT 0,
  `published_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_destination_status` (`destination_id`, `status`, `published_at`),
  KEY `idx_author_id` (`author_id`),
  CONSTRAINT `fk_notes_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_notes_destination` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Travel notes';

DROP TABLE IF EXISTS `travel_note_images`;
CREATE TABLE `travel_note_images` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `note_id` BIGINT UNSIGNED NOT NULL,
  `image_url` VARCHAR(255) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `caption` VARCHAR(100) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`),
  CONSTRAINT `fk_note_images_note` FOREIGN KEY (`note_id`) REFERENCES `travel_notes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Images embedded in a travel note';

DROP TABLE IF EXISTS `travel_note_days`;
CREATE TABLE `travel_note_days` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `note_id` BIGINT UNSIGNED NOT NULL,
  `day_number` INT NOT NULL,
  `title` VARCHAR(100) NULL,
  `description` TEXT NULL,
  `cost` DECIMAL(10,2) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`),
  CONSTRAINT `fk_note_days_note` FOREIGN KEY (`note_id`) REFERENCES `travel_notes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Structured day-by-day route of a note, shown as "Trip at a Glance"';

DROP TABLE IF EXISTS `travel_note_tags`;
CREATE TABLE `travel_note_tags` (
  `note_id` BIGINT UNSIGNED NOT NULL,
  `tag_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`note_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`),
  CONSTRAINT `fk_note_tags_note` FOREIGN KEY (`note_id`) REFERENCES `travel_notes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Note-tag many-to-many';

DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `note_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `parent_comment_id` BIGINT UNSIGNED NULL COMMENT 'Supports one level of replies',
  `content` VARCHAR(500) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 visible / 1 deleted / 2 hidden',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_note_id` (`note_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_comment_id` (`parent_comment_id`),
  CONSTRAINT `fk_comments_note` FOREIGN KEY (`note_id`) REFERENCES `travel_notes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_comments_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Comments';

DROP TABLE IF EXISTS `likes`;
CREATE TABLE `likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `target_type` TINYINT NOT NULL COMMENT '1 note / 2 comment',
  `target_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_target` (`user_id`, `target_type`, `target_id`),
  CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Likes (polymorphic: target_id points to a different table per target_type, so no foreign key)';

DROP TABLE IF EXISTS `favorites`;
CREATE TABLE `favorites` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `note_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_note` (`user_id`, `note_id`),
  CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_favorites_note` FOREIGN KEY (`note_id`) REFERENCES `travel_notes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Favorites';

-- ------------------------------------------------------------
-- 4. Module 2: personalized itinerary customization
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `customize_sessions`;
CREATE TABLE `customize_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NULL COMMENT 'Users may answer before logging in; bound to the account after login',
  `source_note_id` BIGINT UNSIGNED NULL COMMENT 'Set when the session was started from a note ("plan a trip like this")',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 in progress / 1 completed / 2 abandoned',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_sessions_source_note` FOREIGN KEY (`source_note_id`) REFERENCES `travel_notes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='One customization session';

DROP TABLE IF EXISTS `customize_answers`;
CREATE TABLE `customize_answers` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL COMMENT '1:1 with customize_sessions',
  `days` INT NULL,
  `depart_date` DATE NULL,
  `return_date` DATE NULL,
  `depart_city` VARCHAR(50) NULL,
  `has_booked_transport` BOOLEAN NOT NULL DEFAULT FALSE,
  `transport_arrival_time` DATETIME NULL,
  `transport_departure_time` DATETIME NULL,
  `budget_min` DECIMAL(10,2) NULL,
  `budget_max` DECIMAL(10,2) NULL,
  `traveler_count` INT NULL,
  `traveler_composition` VARCHAR(20) NULL COMMENT 'enum: couple/family_with_kids/elderly/friends/solo',
  `travel_style` VARCHAR(20) NULL COMMENT 'enum: checklist/deep_experience/leisure/adventure',
  `interest_tags` JSON NULL COMMENT 'Array of tag ids, e.g. [3,7,12]',
  `pace_preference` VARCHAR(10) NULL COMMENT 'enum: relaxed/packed',
  `accommodation_type` VARCHAR(20) NULL COMMENT 'enum: hotel/homestay/hostel',
  `special_needs` VARCHAR(200) NULL,
  `extra_answers` JSON NULL COMMENT 'Catch-all for questions added later, so new questions do not need a schema change',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_session_id` (`session_id`),
  CONSTRAINT `fk_answers_session` FOREIGN KEY (`session_id`) REFERENCES `customize_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Structured Q&A answers, one row per session';

DROP TABLE IF EXISTS `customize_destinations`;
CREATE TABLE `customize_destinations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `destination_id` BIGINT UNSIGNED NULL COMMENT 'Set only when it matches a local destinations row; itinerary generation uses destination_name, so free-text destinations work',
  `destination_name` VARCHAR(100) NOT NULL COMMENT 'Destination name (free text) as the user typed or picked it',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'Visiting order',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  CONSTRAINT `fk_customize_dest_session` FOREIGN KEY (`session_id`) REFERENCES `customize_sessions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_customize_dest_destination` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Session destinations; supports multi-destination trips, free text not tied to the local table';

DROP TABLE IF EXISTS `itinerary_plans`;
CREATE TABLE `itinerary_plans` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NULL,
  `version` INT NOT NULL DEFAULT 1 COMMENT 'Each regeneration creates a new version',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 generating / 1 generated / 2 edited by user / 3 confirmed / 4 generation failed',
  `generated_by` TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending (placeholder while generating) / 1 rule engine / 2 AI / 3 human advisor',
  `total_estimated_cost` DECIMAL(10,2) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_version` (`session_id`, `version`),
  CONSTRAINT `fk_plans_session` FOREIGN KEY (`session_id`) REFERENCES `customize_sessions` (`id`),
  CONSTRAINT `fk_plans_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Generated itinerary plans, versioned';

DROP TABLE IF EXISTS `itinerary_days`;
CREATE TABLE `itinerary_days` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT UNSIGNED NOT NULL,
  `day_number` INT NOT NULL,
  `date` DATE NULL,
  `destination_id` BIGINT UNSIGNED NULL COMMENT 'Set only when it matches a local destinations row',
  `destination_name` VARCHAR(100) NULL COMMENT 'Which destination of a multi-destination trip this day belongs to (free text); for single-destination plans it is always that destination',
  `daily_estimated_cost` DECIMAL(10,2) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  CONSTRAINT `fk_itin_days_plan` FOREIGN KEY (`plan_id`) REFERENCES `itinerary_plans` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_itin_days_destination` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Plan days';

DROP TABLE IF EXISTS `itinerary_items`;
CREATE TABLE `itinerary_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `day_id` BIGINT UNSIGNED NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `start_time` TIME NULL,
  `category` TINYINT NOT NULL COMMENT '1 attraction / 2 food / 3 transport / 4 accommodation',
  `poi_id` BIGINT UNSIGNED NULL COMMENT 'Linked POI; NULL for custom items. Optional reference only',
  `title` VARCHAR(100) NOT NULL COMMENT 'Name stored redundantly so old plans still display if the POI is deleted',
  `description` VARCHAR(300) NULL,
  `estimated_cost` DECIMAL(10,2) NULL,
  `is_user_edited` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Set when the user edited the item; regeneration keeps it',
  PRIMARY KEY (`id`),
  KEY `idx_day_id` (`day_id`),
  CONSTRAINT `fk_itin_items_day` FOREIGN KEY (`day_id`) REFERENCES `itinerary_days` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_itin_items_poi` FOREIGN KEY (`poi_id`) REFERENCES `pois` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Itinerary items for each day (finest-grained data)';

DROP TABLE IF EXISTS `advisor_tickets`;
CREATE TABLE `advisor_tickets` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `advisor_id` BIGINT UNSIGNED NULL COMMENT 'NULL until assigned',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 waiting / 1 in progress / 2 resolved',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_advisor_id` (`advisor_id`),
  CONSTRAINT `fk_tickets_session` FOREIGN KEY (`session_id`) REFERENCES `customize_sessions` (`id`),
  CONSTRAINT `fk_tickets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_tickets_advisor` FOREIGN KEY (`advisor_id`) REFERENCES `admin_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Hand-off tickets to a human travel advisor';

-- ------------------------------------------------------------
-- 5. Back-office support tables
-- ------------------------------------------------------------

DROP TABLE IF EXISTS `qa_rules`;
CREATE TABLE `qa_rules` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `question_key` VARCHAR(50) NOT NULL COMMENT 'Matching customize_answers field name',
  `question_text` VARCHAR(200) NOT NULL,
  `input_type` VARCHAR(20) NOT NULL COMMENT 'destination_search/date_range/slider/stepper/chip_single/chip_multi/text',
  `options` JSON NULL,
  `is_required` BOOLEAN NOT NULL DEFAULT FALSE,
  `sort_order` INT NOT NULL DEFAULT 0,
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Q&A flow configuration (question order / options adjustable from the back office)';

DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `target_type` TINYINT NOT NULL COMMENT '1 note / 2 comment',
  `target_id` BIGINT UNSIGNED NOT NULL,
  `action` TINYINT NOT NULL COMMENT '1 approved / 2 rejected',
  `operator_id` BIGINT UNSIGNED NOT NULL,
  `reason` VARCHAR(200) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  CONSTRAINT `fk_audit_operator` FOREIGN KEY (`operator_id`) REFERENCES `admin_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='UGC moderation log';

SET FOREIGN_KEY_CHECKS = 1;
