import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: "class",
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      // ============ KB 컬러 토큰 ============
      colors: {
        // Primary — AX풀뱅크 진한 민트
        "kb-yellow":      "#5BC9A8",   // 메인 (진한 민트)
        "kb-yellow-dark": "#3FA889",   // 호버·강조
        "kb-yellow-light":"#A8E5D4",   // 옅은 강조

        // Secondary
        "kb-taupe":       "#5A7569",   // 슬레이트 그린
        "kb-taupe-dark":  "#3D4F47",

        // Background / Surface
        "kb-beige":       "#F5F0E8",   // 따뜻한 베이지 배경
        "kb-beige-light": "#FAFAF7",   // 크림 흰 배경

        // 기업홈 전용
        "kb-beige-warm":       "#F5F0E8",   // 기업 GNB 배경
        "kb-beige-warm-light": "#FAFAF7",   // 기업 GNB 호버

        // GNB 전용 (KB 사이트 벤치마킹)
        "kb-gnb-personal":        "#5BC9A8",   // 개인 GNB 민트
        "kb-gnb-personal-hover":  "#3FA889",   // 개인 GNB 호버
        "kb-gnb-personal-active": "#3FA889",   // 개인 GNB active
        "kb-gnb-biz":             "#EBEBEB",   // 기업 GNB 연회색
        "kb-gnb-biz-hover":       "#D8D8D8",   // 기업 GNB 호버
        "kb-gnb-biz-active":      "#1B3A6B",   // 기업 GNB active 네이비
        "kb-white":       "#FFFFFF",

        // Border
        "kb-border":      "#C5D5CD",   // 민트 톤 보더
        "kb-border-dark": "#9AAEA4",

        // Text
        "kb-text":        "#1A1A1A",   // (유지) 검정
        "kb-text-body":   "#333333",   // (유지) 본문
        "kb-text-muted":  "#777777",   // (유지) 회색
        "kb-text-light":  "#AAAAAA",   // (유지) 옅은 회색

        // Semantic
        "kb-red":         "#D0021B",   // 에러·경고
        "kb-blue":        "#0066CC",   // 링크·정보
        "kb-blue-dark":   "#1A56DB",   // 인터넷뱅킹 뱃지, 상담 전화번호
        "kb-green":       "#2E8B6F",   // 진한 그린

        // 고객센터
        "kb-brown":       "#5D3D2B",   // 고객센터 탭바 배경
        "kb-brown-dark":  "#3D2B1F",   // 고객센터 다크 브라운

        // 대출
        "kb-gold":        "#C09B3A",   // 대출 스타뱅킹 뱃지
        "kb-teal":        "#4A90D9",   // 대출 상세 정보 아이콘

        // 금리·강조
        "kb-orange":      "#FF6B35",   // 예금 금리 강조 (아이콘·텍스트)
        "kb-tab-line":    "#E8A020",   // 탭 active 언더라인 (예금 상세)
        "kb-error-soft":  "#C05050",   // 계산기 결과 강조

        // 다크 버튼
        "kb-btn-dark":    "#5A504A",   // 계산기 결과보기 버튼
        "kb-btn-gray":    "#5C5C5C",   // 예금 계산기 결과보기 버튼
      },

      // ============ KB 레이아웃 토큰 ============
      maxWidth: {
        "kb-container": "1280px",
      },
      width: {
        sidebar: "200px",
      },
      height: {
        header:   "60px",   // 글로벌 헤더
        "user-bar": "44px", // 사용자 영역 바
        gnb:      "130px",   // GNB 바
      },

      // ============ KB 타이포그래피 토큰 ============
      fontSize: {
        // 화면 표시용
        display: ["22px", { lineHeight: "1.45", fontWeight: "700" }],
        h1:      ["20px", { lineHeight: "1.45", fontWeight: "700" }],
        h2:      ["18px", { lineHeight: "1.45", fontWeight: "700" }],
        h3:      ["16px", { lineHeight: "1.55", fontWeight: "700" }],
        body:    ["14px", { lineHeight: "1.75", fontWeight: "400" }],
        caption: ["13px", { lineHeight: "1.65", fontWeight: "400" }],
        small:   ["12px", { lineHeight: "1.65", fontWeight: "400" }],
        gnb:     ["15px", { lineHeight: "1",    fontWeight: "700" }],
      },

      // ============ KB 기타 토큰 ============
      borderRadius: {
        kb:    "4px",
        "kb-sm": "2px",
        "kb-lg": "8px",
      },
      transitionDuration: {
        kb: "150ms",
      },
      fontFamily: {
        sans: ["Noto Sans KR", "-apple-system", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
