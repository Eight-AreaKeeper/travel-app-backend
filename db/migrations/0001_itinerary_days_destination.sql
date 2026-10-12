-- Migration script: manually patches local/test environments whose schema was already created
-- with an old version of the schema script (before db/schema.sql included this change).
-- For a fresh database, you don't need to run this — just use the latest schema script directly.
--
-- Background: once the itinerary engine supports multi-destination trips, itinerary_days needs to
-- know "which destination this day belongs to" — otherwise POI candidates, PDF export, and the
-- frontend display can't be split by destination.

ALTER TABLE `itinerary_days`
  ADD COLUMN `destination_id` BIGINT UNSIGNED NULL
    COMMENT 'Which destination (in a multi-destination trip) this day belongs to; for single-destination itineraries this always equals the plan''s one destination'
    AFTER `date`;

ALTER TABLE `itinerary_days`
  ADD CONSTRAINT `fk_itin_days_destination` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`);

-- itinerary_plans.generated_by used to be NOT NULL with no default. After the async generation
-- pipeline shipped, complete()/regenerate() need to insert a "pending generation" placeholder row
-- first (before it's known whether the final result will come from the rule engine or AI), so it
-- now gets a default of 0 (meaning: pending/generating placeholder).
ALTER TABLE `itinerary_plans`
  MODIFY COLUMN `generated_by` TINYINT NOT NULL DEFAULT 0
    COMMENT '0 pending (generating placeholder) / 1 rule engine / 2 AI-generated / 3 human advisor';

ALTER TABLE `itinerary_plans`
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0
    COMMENT '0 generating / 1 generated / 2 edited by user / 3 confirmed / 4 generation failed';
