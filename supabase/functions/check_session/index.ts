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
        JSON.stringify({ valid: false, error: "Server configuration missing." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body = await req.json();
    const token = body?.token?.trim();
    const deviceId = body?.device_id?.trim();

    if (!token || !deviceId) {
      return new Response(
        JSON.stringify({ valid: false, error: "Missing token or device ID." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Verify JWT Signature and Expiration
    const keyData = new TextEncoder().encode(jwtSecret);
    const cryptoKey = await crypto.subtle.importKey(
      "raw",
      keyData,
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["verify"]
    );

    let payload: any;
    try {
      payload = await djwt.verify(token, cryptoKey);
    } catch (_jwtErr) {
      return new Response(
        JSON.stringify({ valid: false, error: "Session token invalid or expired." }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. Cross-verify device_id in token against the requesting device
    if (payload.device_id !== deviceId) {
      return new Response(
        JSON.stringify({ valid: false, error: "Device identity mismatch." }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Verify license status in Supabase database using service_role
    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const { data: license, error: fetchErr } = await supabase
      .from("licenses")
      .select("status, device_id")
      .eq("key", payload.license_key)
      .maybeSingle();

    if (fetchErr || !license) {
      return new Response(
        JSON.stringify({ valid: false, error: "License not found." }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (license.status !== "active") {
      return new Response(
        JSON.stringify({ valid: false, error: "License is no longer active." }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (license.device_id !== deviceId) {
      return new Response(
        JSON.stringify({ valid: false, error: "Device binding mismatch." }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 4. Update last_validated_at
    await supabase
      .from("licenses")
      .update({ last_validated_at: new Date().toISOString() })
      .eq("key", payload.license_key);

    return new Response(
      JSON.stringify({ valid: true }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ valid: false, error: "Session verification error." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
