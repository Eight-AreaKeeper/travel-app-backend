-- Migration script: changes the customize/itinerary destination to free text, no longer requiring
-- a matching record in the destinations table.
-- Background: user testing found that typing "Kyoto" produced a "required field missing" error —
-- because destination selection used to be a closed picker that could only choose from the
-- destinations table (seed data only has a handful of entries like Dali/Lijiang/Chengdu/Xi'an).
-- This migration switches the customize_destinations/pois/itinerary_days tables to associate by
-- destination_name (free text) as the primary key, with destination_id becoming an optional
-- enhancement field (only populated when it matches a local record, purely for display, never required).
--
-- The travel notes/guides module (travel_notes) is unaffected and keeps its closed picker.

-- 1. customize_destinations: add destination_name, allow destination_id to be null
ALTER TABLE `customize_destinations`
  ADD COLUMN `destination_name` VARCHAR(100) NOT NULL DEFAULT '' AFTER `destination_id`;

-- Backfill names for existing rows
UPDATE `customize_destinations` cd
  JOIN `destinations` d ON cd.destination_id = d.id
  SET cd.destination_name = d.name
  WHERE cd.destination_name = '';

ALTER TABLE `customize_destinations`
  MODIFY COLUMN `destination_id` BIGINT UNSIGNED NULL;

-- Drop the old NOT NULL foreign key constraint and recreate it as nullable (MySQL foreign keys
-- themselves don't distinguish NULL/NOT NULL — this step is really about making the column
-- nullable; the constraint name here matches the one in the actual schema script, so if the DROP
-- below fails it means the constraint name differs locally — safe to skip, no functional impact,
-- just one less safeguard)
ALTER TABLE `customize_destinations` DROP FOREIGN KEY `fk_customize_dest_destination`;
ALTER TABLE `customize_destinations`
  ADD CONSTRAINT `fk_customize_dest_destination` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`);

-- 2. pois: same change, add destination_name, allow destination_id to be null
ALTER TABLE `pois`
  ADD COLUMN `destination_name` VARCHAR(100) NOT NULL DEFAULT '' AFTER `destination_id`;

UPDATE `pois` p
  JOIN `destinations` d ON p.destination_id = d.id
  SET p.destination_name = d.name
  WHERE p.destination_name = '';

ALTER TABLE `pois`
  MODIFY COLUMN `destination_id` BIGINT UNSIGNED NULL;

ALTER TABLE `pois` DROP FOREIGN KEY `fk_pois_destination`;
ALTER TABLE `pois`
  ADD CONSTRAINT `fk_pois_destination` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`);

-- Switch queries from filtering by destination_id to destination_name, add a matching index
ALTER TABLE `pois` ADD INDEX `idx_destination_name_type` (`destination_name`, `type`);

-- 3. itinerary_days: the previous migration (0001) just added a nullable destination_id; this one
-- adds destination_name too, so displaying "which destination day N belongs to" no longer needs a
-- lookup against the destinations table
ALTER TABLE `itinerary_days`
  ADD COLUMN `destination_name` VARCHAR(100) NULL AFTER `destination_id`;
