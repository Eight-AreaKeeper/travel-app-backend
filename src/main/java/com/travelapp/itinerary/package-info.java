/**
 * Itinerary generation engine module. The most technically demanding part of the whole project,
 * corresponding to section 3 (Core Challenges) of the "Project Technical Architecture Document".
 * Responsibilities: receive data collected from the customization questionnaire -> apply hard-constraint
 * filtering (query the pois table) -> call the LLM for personalized ranking and copywriting
 *      -> write structured results into itinerary_plans/days/items -> support partial adjustment and regeneration.
 *
 * Suggest splitting this into two sub-packages internally:
 * itinerary/engine   -- core generation engine logic (hard-constraint filter, LLM call wrapper, fallback strategy)
 * itinerary/          -- the usual controller/service/repository/entity/dto
 *
 * Generation tasks run asynchronously via RabbitMQ; the consumer listens on the generation queue using
 * @RabbitListener. See section 3.2 (Asynchronous Design) of the "Project Technical Architecture Document" for details.
 */
package com.travelapp.itinerary;
