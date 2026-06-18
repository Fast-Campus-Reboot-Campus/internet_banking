import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react-swc'
import tsconfigPaths from 'vite-tsconfig-paths'

// Vite/esbuild가 TS·JSX 변환을 담당하므로 별도 babel/transform 설정이 필요 없다.
// tsconfigPaths() 가 tsconfig.json 의 "@/*" 별칭을 테스트에서도 그대로 해석한다.
export default defineConfig({
  plugins: [react(), tsconfigPaths()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    include: ['**/*.{test,spec}.{ts,tsx}'],
    exclude: ['node_modules/**', '.next/**', 'e2e/**'],
  },
})
