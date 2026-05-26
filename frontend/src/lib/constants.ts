export const APP_NAME = "Gabarita+";
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";
export const ACCESS_TOKEN_COOKIE = "gp_access_token";
export const REFRESH_TOKEN_COOKIE = "gp_refresh_token";
export const REFRESH_TOKEN_STORAGE_KEY = "gp_refresh_token";
export const ACCESS_TOKEN_STORAGE_KEY = "gp_access_token";
export const DEFAULT_PAGE_SIZE = 10;

export const navItems = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/questoes", label: "Questões" },
  { href: "/caderno-erros", label: "Caderno de erros" },
  { href: "/simulados", label: "Simulados" },
  { href: "/perfil", label: "Perfil" },
];
