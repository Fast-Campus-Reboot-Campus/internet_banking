import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
});

// 요청 인터셉터 — 토큰 자동 첨부
api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("accessToken");
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터 — 401이면 로그인 페이지로
// 단, 로그인 요청 자체나 선택적 보조 호출(customers/me 등)은 제외
const SILENT_401_PATHS = ['/auth/login', '/auth/cert-login', '/customers/me']

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url: string = err.config?.url ?? ''
    const isSilent = SILENT_401_PATHS.some((p) => url.includes(p))
    if (err.response?.status === 401 && !isSilent) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("access_token");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);