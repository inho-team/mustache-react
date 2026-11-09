# React 라이브러리와 CSS

::: v-pre

`src/main/frontend`는 평범한 React + Vite 프로젝트입니다. npm 패키지, CSS, 이미지를 일반 React 프로젝트와 같은 방법으로 씁니다.

## 패키지 추가

`src/main/frontend/package.json`의 `dependencies`에 추가합니다.

```json
{
  "dependencies": {
    "@mustache-react/client": "0.1.0",
    "react": "^19.3.1",
    "react-dom": "^19.3.1",
    "recharts": "^3.10.1"
  }
}
```

다음 빌드(`./gradlew build`, `mvn package`, 개발 모드 재시작)에서 빌드 플러그인이 설치합니다. `package.json`이나 lock 파일이 바뀌었을 때만 다시 설치합니다. Node.js가 설치되어 있다면 `src/main/frontend`에서 `npm install recharts`로 추가해도 됩니다.

lock 파일(`package-lock.json`, `pnpm-lock.yaml`, `yarn.lock`)은 커밋하세요. 있으면 빌드가 `npm ci` / `--frozen-lockfile`로 같은 버전을 설치합니다.

## 페이지와 island에서 쓰기

```tsx
// src/main/frontend/islands/OrderChart.tsx
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis } from 'recharts';
import type { IslandProps } from '../generated/types';

export default function OrderChart({ orders }: IslandProps<'OrderChart'>) {
  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart data={orders.map((o) => ({ name: String(o.id), amount: o.amount }))}>
        <XAxis dataKey="name" />
        <YAxis />
        <Bar dataKey="amount" />
      </BarChart>
    </ResponsiveContainer>
  );
}
```

공용 컴포넌트는 `components/` 같은 다른 폴더에 두고 import합니다. `pages/`와 `islands/` 아래 파일만 페이지와 island가 됩니다.

## 모든 화면을 감싸는 Provider: `app.tsx`

테마, 데이터 요청 라이브러리, 상태 관리처럼 화면 전체를 Provider로 감싸야 하는 라이브러리는 `src/main/frontend/app.tsx`의 default export에 둡니다. **모든 `@ReactPage` 페이지와 모든 island**를 각각 감쌉니다.

```tsx
// src/main/frontend/app.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import './app.css';

const queryClient = new QueryClient();

export default function App({ children }: { children?: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
```

- island는 표 칸 안에도 들어가므로 `<header>`, `<div class="layout">` 같은 레이아웃 요소는 넣지 마세요. 레이아웃은 Mustache 레이아웃이나 페이지 컴포넌트에서 만듭니다.
- island마다 따로 감싸지므로, 한 페이지의 island끼리 React 상태를 공유하지 않습니다. 모듈 수준 객체(위의 `queryClient`)는 공유됩니다.
- 서버 데이터는 가능하면 props(컨트롤러 반환값)로 받고, 화면에서 추가로 불러올 때만 `springFetch`나 데이터 요청 라이브러리를 씁니다.

## CSS

| 방법 | 쓰는 법 | 적용 범위 |
|---|---|---|
| 전역 CSS | `app.tsx`에서 `import './app.css'` | 모든 화면. `<react:head />`(`{{{mr.head}}}`)가 `<head>`에 넣습니다 |
| 컴포넌트 CSS | 컴포넌트에서 `import './OrderChart.css'` | 그 컴포넌트를 쓰는 화면에서만 불러옵니다 |
| CSS Modules | `import styles from './Badge.module.css'` → `className={styles.badge}` | 클래스 이름이 파일별로 분리됩니다 |
| 라이브러리 CSS | `import 'some-library/dist/style.css'` | import한 위치에 따라 위와 같습니다 |

Vite가 처리하므로 별도 설정은 필요 없습니다.

### Tailwind CSS 같은 Vite 플러그인

Vite 플러그인이 필요한 도구는 `src/main/frontend/vite.config.ts`를 만들어 씁니다. **이 파일이 있으면 기본 설정 대신 이 파일만 쓰므로** React 플러그인과 `mustacheReact()`를 직접 넣어야 합니다.

```ts
// src/main/frontend/vite.config.ts
import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { mustacheReact } from '@mustache-react/vite-plugin';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react(), mustacheReact(), tailwindcss()],
});
```

Mustache 템플릿에서도 Tailwind 클래스를 쓴다면, 템플릿 폴더가 프론트 디렉터리 밖에 있으므로 CSS에서 소스 위치를 알려 줘야 합니다(Tailwind v4 기준).

```css
/* app.css */
@import 'tailwindcss';
@source '../resources/templates';
```

## 이미지와 폰트

컴포넌트에서 import하면 빌드 결과에 해시가 붙은 파일로 들어가고, 올바른 URL을 돌려받습니다.

```tsx
import logo from './assets/logo.svg';

<img src={logo} alt="" />;
```

CSS의 `url(./fonts/Pretendard.woff2)`도 같은 방식으로 처리됩니다.

`public/` 폴더에 두고 `/logo.svg`처럼 절대 경로로 부르는 방식은 피하세요. 빌드 결과는 `/mustache-react/` 아래(또는 CDN)에서 서빙되므로 `/logo.svg`는 찾을 수 없습니다. Mustache 화면 전용 이미지는 Spring의 `src/main/resources/static/`에 두고 평소처럼 씁니다.

TypeScript가 이미지 import의 타입을 알도록 `tsconfig.json`에 `"types": ["vite/client"]`를 추가하세요(`vite`가 devDependencies에 있어야 합니다).

## 브라우저 전용 라이브러리와 SSR

서버 렌더링(`mustache-react.ssr=auto|always`)을 켜면 컴포넌트가 Node.js에서도 한 번 실행됩니다. 렌더링 중에 `window`, `document`, `localStorage`를 쓰는 라이브러리(지도, 에디터, 일부 차트)는 서버에서 실패합니다.

- `auto`면 그 페이지나 island만 브라우저 렌더링으로 대신하고 로그를 한 번 남깁니다. 화면은 정상입니다.
- 서버 렌더링도 살리려면 브라우저 전용 코드를 `useEffect` 안으로 옮기거나, 마운트된 뒤에만 그리게 합니다.

```tsx
import { useEffect, useState } from 'react';

export default function MapView(props: { lat: number; lng: number }) {
  const [Map, setMap] = useState<null | React.ComponentType<{ lat: number; lng: number }>>(null);
  useEffect(() => {
    import('./LeafletMap').then((m) => setMap(() => m.default)); // 브라우저에서만 불러옴
  }, []);
  return Map ? <Map {...props} /> : <div style={{ height: 300 }} />;
}
```

## 번들 크기

- 페이지와 island는 **각각 별도 파일**로 빌드되고, 화면에 실제로 있는 것만 내려받습니다. 차트 라이브러리를 쓰는 island가 없는 화면은 그 라이브러리를 받지 않습니다.
- 여러 화면이 함께 쓰는 라이브러리는 Vite가 공용 파일로 나눕니다.
- `app.tsx`와 거기서 import한 것은 **모든 화면에** 들어갑니다. 무거운 라이브러리는 `app.tsx`가 아니라 쓰는 컴포넌트에서 import하세요.
- `<react:scripts />`가 이 페이지의 island 파일을 미리 받도록(`modulepreload`) 알려 줘서, 순차적으로 받느라 늦어지지 않습니다.

:::
