import assert from "node:assert/strict";
import test from "node:test";

import {
  buildSupabaseHeaders,
  redactSupabaseSecrets,
  resolveSupabaseApiKey,
} from "./supabase-auth.mjs";

test("new Supabase secret keys use only the apikey header", () => {
  const headers = buildSupabaseHeaders("sb_secret_test");

  assert.deepEqual(headers, { apikey: "sb_secret_test" });
});

test("legacy service_role keys retain bearer authorization", () => {
  const headers = buildSupabaseHeaders("legacy-jwt-test");

  assert.deepEqual(headers, {
    apikey: "legacy-jwt-test",
    authorization: "Bearer legacy-jwt-test",
  });
});

test("SUPABASE_SECRET_KEY has priority over the legacy fallback", () => {
  const apiKey = resolveSupabaseApiKey({
    SUPABASE_SECRET_KEY: "sb_secret_primary",
    SUPABASE_SERVICE_ROLE_KEY: "legacy-jwt-fallback",
  });

  assert.equal(apiKey, "sb_secret_primary");
});

test("Supabase keys are removed from error text", () => {
  const errorText = "keys: sb_secret_test and legacy-jwt-test";

  assert.equal(
    redactSupabaseSecrets(errorText, ["sb_secret_test", "legacy-jwt-test"]),
    "keys: [REDACTED] and [REDACTED]",
  );
});
