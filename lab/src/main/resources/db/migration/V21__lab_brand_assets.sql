ALTER TABLE `t_lab`
    ADD COLUMN `logo_url` VARCHAR(255) NULL AFTER `current_admins`,
    ADD COLUMN `cover_image_url` VARCHAR(255) NULL AFTER `logo_url`;

ALTER TABLE `t_outstanding_graduate`
    ADD COLUMN `cover_image_url` VARCHAR(255) NULL AFTER `avatar_url`;
