-- liquibase formatted sql

-- changeset aurum:fix-status-log-seed-dates
-- The original seed (2026-03-22-add-asset-status-log.sql) set changed_at = CURRENT_DATE
-- for all archived assets, regardless of when they were actually archived.
-- This caused the chart to treat those assets as active until 2026-03-22,
-- inflating net worth values for months between the real archive date and 2026-03-22.
--
-- Fix: set the deactivation date to the asset's last snapshot reference date
-- (or the asset's updated_at if no snapshots exist).
UPDATE asset_status_log asl
SET changed_at = COALESCE(
    (SELECT MAX(s.reference_date)
     FROM snapshots s
     WHERE s.asset_id = asl.asset_id),
    (SELECT DATE(a.created_at)
     FROM assets a
     WHERE a.id = asl.asset_id)
)
WHERE asl.is_active = false
  AND asl.changed_at = '2026-03-22'
  AND EXISTS (
      SELECT 1 FROM assets a
      WHERE a.id = asl.asset_id AND a.is_active = false
  );
