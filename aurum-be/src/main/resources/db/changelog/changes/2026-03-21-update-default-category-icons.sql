-- liquibase formatted sql

-- changeset aurum:update-default-category-icons
UPDATE asset_categories SET icon = 'pi-wallet'      WHERE id = 'a0000000-0000-0000-0000-000000000001';
UPDATE asset_categories SET icon = 'pi-chart-line'  WHERE id = 'a0000000-0000-0000-0000-000000000002';
UPDATE asset_categories SET icon = 'pi-percentage'  WHERE id = 'a0000000-0000-0000-0000-000000000003';
UPDATE asset_categories SET icon = 'pi-calendar'    WHERE id = 'a0000000-0000-0000-0000-000000000004';
UPDATE asset_categories SET icon = 'pi-home'        WHERE id = 'a0000000-0000-0000-0000-000000000005';
UPDATE asset_categories SET icon = 'pi-car'         WHERE id = 'a0000000-0000-0000-0000-000000000006';
UPDATE asset_categories SET icon = 'pi-bitcoin'     WHERE id = 'a0000000-0000-0000-0000-000000000007';
UPDATE asset_categories SET icon = 'pi-briefcase'   WHERE id = 'a0000000-0000-0000-0000-000000000008';
UPDATE asset_categories SET icon = 'pi-box'         WHERE id = 'a0000000-0000-0000-0000-000000000009';
UPDATE asset_categories SET icon = 'pi-building'    WHERE id = 'a0000000-0000-0000-0000-000000000010';
UPDATE asset_categories SET icon = 'pi-book'        WHERE id = 'a0000000-0000-0000-0000-000000000011';
UPDATE asset_categories SET icon = 'pi-car'         WHERE id = 'a0000000-0000-0000-0000-000000000012';
UPDATE asset_categories SET icon = 'pi-credit-card' WHERE id = 'a0000000-0000-0000-0000-000000000013';
UPDATE asset_categories SET icon = 'pi-money-bill'  WHERE id = 'a0000000-0000-0000-0000-000000000014';
UPDATE asset_categories SET icon = 'pi-shield'      WHERE id = 'a0000000-0000-0000-0000-000000000015';
