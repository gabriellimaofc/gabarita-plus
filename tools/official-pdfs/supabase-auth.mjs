export function resolveSupabaseApiKey(environment) {
  if (environment.SUPABASE_SECRET_KEY?.trim()) {
    return environment.SUPABASE_SECRET_KEY;
  }
  if (environment.SUPABASE_SERVICE_ROLE_KEY?.trim()) {
    return environment.SUPABASE_SERVICE_ROLE_KEY;
  }
  return undefined;
}

export function buildSupabaseHeaders(apiKey) {
  const headers = { apikey: apiKey };
  if (!apiKey.startsWith("sb_secret_")) {
    headers.authorization = `Bearer ${apiKey}`;
  }
  return headers;
}

export function redactSupabaseSecrets(value, apiKeys) {
  let sanitized = String(value);
  for (const apiKey of apiKeys) {
    if (apiKey) {
      sanitized = sanitized.replaceAll(apiKey, "[REDACTED]");
    }
  }
  return sanitized.replace(/sb_secret_[A-Za-z0-9_-]+/g, "[REDACTED]");
}
