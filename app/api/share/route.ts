import { env } from "cloudflare:workers";
import { NextResponse } from "next/server";

export const runtime = "edge";
const headers = { "Cache-Control": "no-store" };
const valid = /^[a-f0-9]{64}$/;
const reply = (data: unknown, status = 200) => NextResponse.json(data, { status, headers });
const token = () => Array.from(crypto.getRandomValues(new Uint8Array(32))).map(x => x.toString(16).padStart(2, "0")).join("");
const hash = async (s: string) => Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(s)))).map(x => x.toString(16).padStart(2, "0")).join("");

export async function POST(req: Request) {
  const body = await req.json().catch(() => null);
  if (!body || typeof body.action !== "string") return reply({ error: "Neteisinga užklausa" }, 400);
  if (body.action === "create") {
    const editor = token(), viewer = token();
    try {
      await env.DB.prepare("INSERT INTO shares(id,editor_hash,viewer_hash,created_at,active) VALUES(?,?,?,?,1)")
        .bind(crypto.randomUUID(), await hash(editor), await hash(viewer), Date.now()).run();
      return reply({ editor, viewer });
    } catch { return reply({ error: "Nepavyko pradėti bendrinimo" }, 503); }
  }
  if (typeof body.token !== "string" || !valid.test(body.token)) return reply({ error: "Nuoroda negalioja" }, 404);
  const secret = await hash(body.token);
  try {
    if (body.action === "view") {
      const row = await env.DB.prepare("SELECT latitude,longitude,accuracy,speed,heading,battery,gps,network,measured_at,updated_at,active FROM shares WHERE viewer_hash=?").bind(secret).first();
      if (!row || row.active !== 1) return reply({ error: "Bendrinimas išjungtas" }, 410);
      return reply({ latitude: row.latitude, longitude: row.longitude, accuracy: row.accuracy,
        speed: row.speed, heading: row.heading, battery: row.battery, gps: row.gps, network: row.network,
        measuredAt: row.measured_at, updatedAt: row.updated_at });
    }
    if (body.action === "update") {
      const lat = Number(body.latitude), lon = Number(body.longitude), acc = Number(body.accuracy);
      if (!Number.isFinite(lat) || lat < -90 || lat > 90 || !Number.isFinite(lon) || lon < -180 || lon > 180 || !Number.isFinite(acc) || acc < 0 || acc > 100000)
        return reply({ error: "Neteisinga vieta" }, 400);
      const optionalNumber = (v: unknown, max: number) => v === null || v === undefined ? null
        : typeof v === "number" && Number.isFinite(v) && v >= 0 && v <= max ? v : NaN;
      const speed = optionalNumber(body.speed, 1000), heading = optionalNumber(body.heading, 360);
      const measuredAt = optionalNumber(body.measuredAt, Date.now() + 60000);
      if (Number.isNaN(speed) || Number.isNaN(heading) || Number.isNaN(measuredAt))
        return reply({ error: "Neteisingi judėjimo duomenys" }, 400);
      const detail = (v: unknown) => typeof v === "string" ? v.slice(0, 160) : null;
      const result = await env.DB.prepare("UPDATE shares SET latitude=?,longitude=?,accuracy=?,speed=?,heading=?,battery=?,gps=?,network=?,measured_at=?,updated_at=? WHERE editor_hash=? AND active=1")
        .bind(lat, lon, acc, speed, heading, detail(body.battery), detail(body.gps), detail(body.network), measuredAt, Date.now(), secret).run();
      return result.meta.changes ? reply({ ok: true }) : reply({ error: "Bendrinimas išjungtas" }, 410);
    }
    if (body.action === "stop") {
      const result = await env.DB.prepare("UPDATE shares SET latitude=NULL,longitude=NULL,accuracy=NULL,speed=NULL,heading=NULL,battery=NULL,gps=NULL,network=NULL,measured_at=NULL,updated_at=NULL,active=0 WHERE editor_hash=? AND active=1")
        .bind(secret).run();
      return result.meta.changes ? reply({ ok: true }) : reply({ error: "Bendrinimas jau išjungtas" }, 410);
    }
  } catch { return reply({ error: "Paslauga laikinai nepasiekiama" }, 503); }
  return reply({ error: "Neteisinga užklausa" }, 400);
}
