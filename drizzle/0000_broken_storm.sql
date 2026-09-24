CREATE TABLE `shares` (
	`id` text PRIMARY KEY NOT NULL,
	`editor_hash` text NOT NULL,
	`viewer_hash` text NOT NULL,
	`latitude` real,
	`longitude` real,
	`accuracy` real,
	`updated_at` integer,
	`active` integer DEFAULT 1 NOT NULL,
	`created_at` integer NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `shares_editor_hash_unique` ON `shares` (`editor_hash`);--> statement-breakpoint
CREATE UNIQUE INDEX `shares_viewer_hash_unique` ON `shares` (`viewer_hash`);