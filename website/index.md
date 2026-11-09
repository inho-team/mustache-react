---
layout: home
hero:
  name: mustache-react
  text: Spring 방식 그대로, 화면은 React로
  tagline: 컨트롤러가 화면을 정하고 DTO가 그대로 React props가 됩니다. npm 설정 없이 실행 jar 하나로 배포합니다.
  actions:
    - theme: brand
      text: 시작하기
      link: /guide/getting-started
    - theme: alt
      text: GitHub
      link: https://github.com/inho-team/mustache-react
features:
  - title: '@ReactPage'
    details: 기존 API 메서드에 어노테이션 하나. 브라우저에는 React 페이지, API 클라이언트에는 원래 JSON을 응답합니다. ResponseEntity로 상태 코드와 헤더도 그대로.
  - title: Mustache + React 태그
    details: '기존 Mustache 화면에 <react:OrderChart orders="{{orders}}" />처럼 필요한 부분만 React로 넣습니다. 값은 객체 그대로 전달됩니다.'
  - title: Spring 연동
    details: CSRF, 로그인 사용자, @Valid 검증 에러, redirect:, @RequestBody와 @ModelAttribute 폼을 그대로 씁니다.
  - title: 다국어
    details: messages.properties를 React, Mustache, 페이지 제목에서 함께 씁니다. ?lang=en으로 언어를 바꾸고 쿠키에 기억합니다.
  - title: DTO → TypeScript
    details: Java DTO와 @ReactIsland에서 TS 타입을 생성하고, 템플릿이 넘긴 props도 서버에서 검사합니다.
  - title: 설정 없는 빌드
    details: Gradle/Maven 플러그인이 Node.js를 받고 package.json까지 만들어 프론트를 빌드해 jar에 넣습니다.
  - title: SSR과 HMR
    details: Node 사이드카로 서버 렌더링(실패 시 브라우저 렌더링), 개발 중에는 Vite dev server로 즉시 반영합니다.
  - title: 같이 또는 따로 배포
    details: jar 하나로 배포하거나, 같은 빌드 결과를 CDN에 올리고 public-path 한 줄로 바꿉니다. Java 8, Spring Boot 2.7~4.x.
---
