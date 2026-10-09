-- =============================================================================
-- SENSI BOT PRO: Supabase License Key & Device Lock Schema
-- =============================================================================

-- 1. Create Licenses Table
CREATE TABLE IF NOT EXISTS public.licenses (
    key TEXT PRIMARY KEY,
    device_id TEXT,
    device_info TEXT,
    status TEXT NOT NULL DEFAULT 'unused' CHECK (status IN ('unused', 'active', 'revoked')),
    buyer_email TEXT,
    issued_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()),
    bound_at TIMESTAMPTZ,
    last_validated_at TIMESTAMPTZ
);

-- Fast lookup indexes
CREATE INDEX IF NOT EXISTS idx_licenses_device_id ON public.licenses(device_id);
CREATE INDEX IF NOT EXISTS idx_licenses_status ON public.licenses(status);

-- Security: Lock down public table access.
-- Zero public access to raw table. Only Edge Functions with service_role can read/write.
ALTER TABLE public.licenses ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.licenses FROM anon, authenticated;

-- 2. Create Rate Limiting Table
CREATE TABLE IF NOT EXISTS public.license_rate_limits (
    id BIGSERIAL PRIMARY KEY,
    ip_address TEXT NOT NULL,
    attempt_time TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_rate_limits_ip_time ON public.license_rate_limits(ip_address, attempt_time);

ALTER TABLE public.license_rate_limits ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.license_rate_limits FROM anon, authenticated;

-- 3. Helpful Admin Helper Function for manual reset
-- You can run this in Supabase SQL editor whenever a customer changes phone:
-- SELECT reset_license_device('ABCD-1234-EFGH-5678');
CREATE OR REPLACE FUNCTION public.reset_license_device(target_key TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.licenses
    SET device_id = NULL,
        device_info = NULL,
        status = 'unused',
        bound_at = NULL
    WHERE key = target_key;
END;
$$;
