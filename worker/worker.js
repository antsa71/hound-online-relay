/**
 * Hound Internet Relay — Cloudflare Worker + Durable Object.
 * All Server and Client requests are routed to a Durable Object selected by
 * the 16-character pairing token. The token is a bearer secret: only share it
 * with the intended Client. No file is uploaded until the Client requests it.
 */
const json = (value, status = 200) => new Response(JSON.stringify(value), {
  status,
  headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
});

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { status: 204 });
    const url = new URL(request.url);
    if (!url.pathname.startsWith("/api/")) return json({ error: "not_found" }, 404);
    const token = (request.headers.get("X-Pairing-Token") || "").trim().toUpperCase();
    if (!/^[A-Z0-9]{16}$/.test(token)) return json({ error: "invalid_pairing_token" }, 401);
    const id = env.HOUND_RELAY.idFromName(token);
    return env.HOUND_RELAY.get(id).fetch(request);
  }
};

export class HoundRelay {
  constructor(state, env) { this.state = state; this.env = env; }

  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    try {
      if (path === "/api/device/register" && request.method === "POST") {
        const body = await request.json();
        await this.state.storage.put("device", {
          device_id: String(body.device_id || "hound-server-1"),
          device_label: String(body.device_label || "Hound Server"),
          online: true, last_seen: Date.now()
        });
        return json({ ok: true, online: true });
      }
      if (path === "/api/status" && request.method === "GET") {
        const device = await this.state.storage.get("device");
        const lastSeen = (await this.state.storage.get("device:last_seen")) || device?.last_seen || null;
        const online = !!device && !!lastSeen && Date.now() - lastSeen < 30000;
        return json({ online, last_seen: lastSeen, device_label: device?.device_label || null });
      }
      if (path === "/api/manifest" && request.method === "POST") {
        const body = await request.json();
        const items = Array.isArray(body.items) ? body.items : [];
        // Split manifest text into small DO values to avoid the per-value limit.
        const serialized = JSON.stringify({ items, updated_at: Date.now(), total: items.length });
        const oldCount = (await this.state.storage.get("manifest:parts")) || 0;
        const partSize = 400000;
        const count = Math.ceil(serialized.length / partSize);
        const batch = this.state.storage;
        for (let i = 0; i < count; i++) await batch.put(`manifest:${i}`, serialized.slice(i * partSize, (i + 1) * partSize));
        for (let i = count; i < oldCount; i++) await batch.delete(`manifest:${i}`);
        await batch.put("manifest:parts", count);
        await batch.put("device:last_seen", Date.now());
        return json({ ok: true, items: items.length });
      }
      if (path === "/api/manifest" && request.method === "GET") {
        const count = (await this.state.storage.get("manifest:parts")) || 0;
        if (!count) return json({ error: "manifest_not_ready" }, 503);
        let serialized = "";
        for (let i = 0; i < count; i++) serialized += (await this.state.storage.get(`manifest:${i}`)) || "";
        return new Response(serialized, { headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });
      }
      if (path === "/api/download/start" && request.method === "POST") {
        const body = await request.json();
        const filePath = String(body.path || "");
        if (!filePath || filePath.startsWith("/") || filePath.split("/").includes("..")) return json({ error: "invalid_path" }, 400);
        const id = crypto.randomUUID();
        const commands = (await this.state.storage.get("commands")) || [];
        commands.push({ id, path: filePath, created_at: Date.now() });
        await this.state.storage.put("commands", commands);
        await this.state.storage.put(`transfer:${id}`, { path: filePath, created_at: Date.now(), finished: false, chunks: 0 });
        return json({ id }, 201);
      }
      if (path === "/api/commands" && request.method === "GET") {
        const commands = (await this.state.storage.get("commands")) || [];
        if (!commands.length) return new Response(null, { status: 204 });
        const command = commands.shift();
        await this.state.storage.put("commands", commands);
        await this.state.storage.put("device:last_seen", Date.now());
        return json(command);
      }
      if (path === "/api/download/chunk") {
        const transfer = url.searchParams.get("transfer") || "";
        const seq = Number(url.searchParams.get("seq"));
        if (!/^[0-9a-f-]{36}$/i.test(transfer) || !Number.isInteger(seq) || seq < 0) return json({ error: "invalid_transfer_or_sequence" }, 400);
        const key = `chunk:${transfer}:${seq}`;
        if (request.method === "POST") {
          const bytes = await request.arrayBuffer();
          if (bytes.byteLength > 1100000) return json({ error: "chunk_too_large" }, 413);
          await this.state.storage.put(key, bytes);
          const info = await this.state.storage.get(`transfer:${transfer}`);
          if (info) { info.chunks = Math.max(info.chunks || 0, seq + 1); await this.state.storage.put(`transfer:${transfer}`, info); }
          return new Response(null, { status: 204 });
        }
        if (request.method === "GET") {
          const bytes = await this.state.storage.get(key);
          if (!bytes) return new Response(null, { status: 204, headers: { "cache-control": "no-store" } });
          return new Response(bytes, { headers: { "content-type": "application/octet-stream", "cache-control": "no-store" } });
        }
      }
      if (path === "/api/download/finish" && request.method === "POST") {
        const transfer = url.searchParams.get("transfer") || "";
        const info = await this.state.storage.get(`transfer:${transfer}`);
        if (!info) return json({ error: "transfer_not_found" }, 404);
        info.finished = true; info.finished_at = Date.now();
        await this.state.storage.put(`transfer:${transfer}`, info);
        return json({ ok: true, chunks: info.chunks || 0 });
      }
      if (path === "/api/download/complete" && request.method === "POST") {
        const transfer = url.searchParams.get("transfer") || "";
        const info = await this.state.storage.get(`transfer:${transfer}`);
        if (info) {
          for (let i = 0; i < (info.chunks || 0); i++) await this.state.storage.delete(`chunk:${transfer}:${i}`);
          await this.state.storage.delete(`transfer:${transfer}`);
        }
        return new Response(null, { status: 204 });
      }
      return json({ error: "not_found", path, method: request.method }, 404);
    } catch (err) {
      return json({ error: "relay_error", message: String(err?.message || err) }, 500);
    }
  }
}
