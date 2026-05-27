import Cookies from "js-cookie";

import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
} from "@/lib/constants";

export function setSessionCookies(accessToken: string, refreshToken: string, expiresIn: number) {
  const accessTokenDays = Math.max(expiresIn / 86_400, 1 / 24);
  const refreshTokenDays = 15;
  const secure = typeof window !== "undefined" && window.location.protocol === "https:";

  Cookies.set(ACCESS_TOKEN_COOKIE, accessToken, {
    sameSite: "lax",
    expires: accessTokenDays,
    path: "/",
    secure,
  });
  Cookies.set(REFRESH_TOKEN_COOKIE, refreshToken, {
    sameSite: "lax",
    expires: refreshTokenDays,
    path: "/",
    secure,
  });
}

export function clearSessionCookies() {
  Cookies.remove(ACCESS_TOKEN_COOKIE, { path: "/" });
  Cookies.remove(REFRESH_TOKEN_COOKIE, { path: "/" });
}
