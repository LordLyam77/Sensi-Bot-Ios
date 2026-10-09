import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import * as djwt from "https://deno.land/x/djwt@v2.8/mod.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const jwtSecret = Deno.env.get("JWT_SECRET");

    if (!supabaseUrl || !serviceRoleKey || !jwtSecret) {
      return new Response(
        JSON.stringify({ error: "Server configuration missing." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. IP-based Rate Limiting (max 10 requests per minute per IP)
    const clientIp = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
                     req.headers.get("cf-connecting-ip") || "unknown";

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const oneMinuteAgo = new Date(Date.now() - 60 * 1000).toISOString();
    const { count: attemptCount, error: countErr } = await supabase
      .from("license_rate_limits")
      .select("*", { count: "exact", head: true })
      .eq("ip_address", clientIp)
      .gte("attempt_time", oneMinuteAgo);

    if (!countErr && attemptCount !== null && attemptCount >= 10) {
      return new Response(
        JSON.stringify({ error: "Too many attempts, try again later" }),
        { status: 429, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Record rate limit attempt
    await supabase.from("license_rate_limits").insert({ ip_address: clientIp });

    // 2. Parse request payload
    const body = await req.json();
    const rawKey = body?.key?.trim().toUpperCase();
    const deviceId = body?.device_id?.trim();
    const deviceInfo = body?.device_info?.trim() || "Unknown Device";

    if (!rawKey || !deviceId) {
      return new Response(
        JSON.stringify({ error: "Invalid or inactive key" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Query license record (using service_role)
    const { data: license, error: fetchErr } = await supabase
      .from("licenses")
      .select("*")
      .eq("key", rawKey)
      .maybeSingle();

    if (fetchErr || !license || license.status === "revoked") {
      // Do not reveal whether key exists vs revoked
      return new Response(
        JSON.stringify({ error: "Invalid or inactive key" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const nowIso = new Date().toISOString();

    // 4. Device Binding Verification
    if (!license.device_id) {
      // Case A: Unused key -> Bind to this device
      const { error: updateErr } = await supabase
        .from("licenses")
        .update({
          device_id: deviceId,
          device_info: deviceInfo,
          status: "active",
          bound_at: nowIso,
          last_validated_at: nowIso,
        })
        .eq("key", rawKey);

      if (updateErr) {
        return new Response(
          JSON.stringify({ error: "Failed to activate license. Try again." }),
          { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    } else if (license.device_id === deviceId) {
      // Case B: Same device -> Re-activate / update last validated
      await supabase
        .from("licenses")
        .update({
          last_validated_at: nowIso,
          device_info: deviceInfo,
        })
        .eq("key", rawKey);
    } else {
      // Case C: Mismatch -> Device already bound to another device
      return new Response(
        JSON.stringify({ error: "This key is already active on another device." }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 5. Generate Signed JWT Session Token
    const keyData = new TextEncoder().encode(jwtSecret);
    const cryptoKey = await crypto.subtle.importKey(
      "raw",
      keyData,
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );

    // 30 days session validity
    const exp = Math.floor(Date.now() / 1000) + (30 * 24 * 60 * 60);
    const jwt = await djwt.create(
      { alg: "HS256", typ: "JWT" },
      {
        license_key: rawKey,
        device_id: deviceId,
        exp: exp,
      },
      cryptoKey
    );

    return new Response(
      JSON.stringify({
        success: true,
        token: jwt,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ error: "Server processing error." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
