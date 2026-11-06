import { defineConfig } from 'vitepress';

export default defineConfig({
  lang: 'ko-KR',
  title: 'mustache-react',
  description: 'Spring MVC 컨트롤러와 Mustache 화면에서 React 페이지와 island를 렌더링합니다.',
  base: '/mustache-react/',
  cleanUrls: true,
  lastUpdated: true,
  themeConfig: {
    nav: [
      { text: '가이드', link: '/guide/getting-started' },
      { text: '설정', link: '/reference/configuration' },
      { text: '변경 내역', link: 'https://github.com/inho-team/mustache-react/blob/main/CHANGELOG.md' },
    ],
    sidebar: [
      {
        text: '가이드',
        items: [
          { text: '시작하기', link: '/guide/getting-started' },
          { text: '@ReactPage 페이지', link: '/guide/pages' },
          { text: 'Mustache island', link: '/guide/islands' },
          { text: '폼과 서버 컨텍스트', link: '/guide/forms' },
          { text: '다국어 (i18n)', link: '/guide/i18n' },
          { text: 'TypeScript 타입 생성', link: '/guide/types' },
          { text: '개발 모드', link: '/guide/development' },
          { text: '서버 렌더링 (SSR)', link: '/guide/ssr' },
        ],
      },
      {
        text: '레퍼런스',
        items: [
          { text: '설정', link: '/reference/configuration' },
          { text: '빌드 플러그인', link: '/reference/build-plugins' },
          { text: '호환성', link: '/reference/compatibility' },
        ],
      },
    ],
    socialLinks: [{ icon: 'github', link: 'https://github.com/inho-team/mustache-react' }],
    editLink: {
      pattern: 'https://github.com/inho-team/mustache-react/edit/main/website/:path',
      text: '이 페이지 수정하기',
    },
    footer: { message: 'Apache License 2.0' },
    search: { provider: 'local' },
    outline: { label: '이 페이지에서' },
    docFooter: { prev: '이전', next: '다음' },
  },
});
