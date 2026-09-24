import { sqliteTable, text, integer, real } from "drizzle-orm/sqlite-core";
export const shares = sqliteTable("shares", {
  id: text("id").primaryKey(),
  editorHash: text("editor_hash").notNull().unique(),
  viewerHash: text("viewer_hash").notNull().unique(),
  latitude: real("latitude"), longitude: real("longitude"), accuracy: real("accuracy"),
  updatedAt: integer("updated_at"), active: integer("active").notNull().default(1),
  createdAt: integer("created_at").notNull(),
});
