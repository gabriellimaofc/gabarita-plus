import Cookies from "js-cookie";

import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
} from "@/lib/constants";

export function setSessionCookies(accessToken: string, refreshToken: string, expiresIn: number) {
  const accessTokenDays = Math.max(expiresIn / 86_400, 1 / 24);
  const refreshTokenDays = 15;

  Cookies.set(ACCESS_TOKEN_COOKIE, accessToken, {
    sameSite: "lax",
    expires: accessTokenDays,
  });
  Cookies.set(REFRESH_TOKEN_COOKIE, refreshToken, {
    sameSite: "lax",
    expires: refreshTokenDays,
  });
}

export function clearSessionCookies() {
  Cookies.remove(ACCESS_TOKEN_COOKIE);
  Cookies.remove(REFRESH_TOKEN_COOKIE);
}
