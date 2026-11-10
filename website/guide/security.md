# 보안

::: v-pre

## 페이지에 들어가는 것

화면 HTML에는 아래 데이터가 **그대로 들어갑니다.** 브라우저의 "페이지 소스 보기"로 누구나 볼 수 있다고 생각하세요.

| 데이터 | 어디서 | 끄는 법 |
|---|---|---|
| props | `@ReactPage` 반환값, island 속성 | 넣지 않으면 됩니다 |
| CSRF 토큰 | Spring Security의 `CsrfToken` | `mustache-react.context.csrf=false` |
| 로그인 사용자 | `{name, authorities}` (비로그인은 `null`) | `mustache-react.context.user=false` |
| 메시지 | `mustache-react.context.messages`에 나열한 키와 패턴만 | 목록에서 빼기 |
| 언어 | `locale`, 언어 전환 설정(`i18n`) | — |
| 라이브러리 버전 | `mustacheReact.version` | — |
| 직접 추가한 값 | `ServerContextContributor` 빈 | 빈 제거 |

**props에 넣지 말아야 할 것:** 비밀번호 해시, 토큰, 내부 ID처럼 화면에 필요 없는 필드. 엔티티를 그대로 반환하지 말고 화면용 DTO를 반환하세요. `@JsonIgnore`도 API JSON과 똑같이 적용됩니다.

`authorities`는 권한 이름(`ROLE_ADMIN` 등)을 노출합니다. 화면에서 메뉴를 숨기는 용도일 뿐이며, **실제 권한 검사는 항상 서버에서** 해야 합니다.

## 같은 URL이 JSON도 돌려줍니다

`@ReactPage` 메서드는 `Accept: application/json` 요청에 같은 데이터를 JSON으로 돌려줍니다. 이건 기존 API 컨트롤러와 같은 동작입니다.

- 브라우저 화면용 데이터는 곧 API 응답이기도 합니다. 화면에만 보여도 되는 데이터가 아니라면 props에 넣지 마세요.
- Spring Security의 URL·메서드 보안은 HTML과 JSON 요청에 **똑같이** 적용됩니다. 페이지 URL을 보호하면 JSON도 보호됩니다.
- JSON 응답이 필요 없다면 `mustache-react.content-negotiation=false`로 항상 HTML만 돌려줄 수 있습니다.

## XSS (스크립트 삽입)

mustache-react가 기본으로 막는 것:

- **props와 서버 컨텍스트:** JSON 안의 `<`, `>`, `&`, U+2028, U+2029를 `<` 같은 이스케이프로 바꿔 씁니다. 사용자가 입력한 `</script><script>alert(1)</script>`가 props에 들어가도 스크립트 태그를 닫지 못합니다. 예제 앱의 1004번 주문이 이 경우를 테스트합니다.
- **React 렌더링:** `{order.customer}`처럼 쓴 값은 React가 이스케이프합니다.
- **Mustache:** `{{값}}`은 HTML 이스케이프됩니다. `{{#mr.t}}`(메시지)도 이스케이프됩니다.

직접 조심해야 하는 것:

| 위험한 코드 | 대신 |
|---|---|
| `dangerouslySetInnerHTML={{ __html: userInput }}` | 텍스트로 렌더링. HTML이 꼭 필요하면 DOMPurify 같은 라이브러리로 정리한 뒤 |
| Mustache `{{{userInput}}}` (중괄호 3개, 이스케이프 없음) | `{{userInput}}`. 3개짜리는 `{{{mr.head}}}`, `{{{mr.scripts}}}`처럼 라이브러리가 만든 태그에만 |
| `<a href={userUrl}>` | `javascript:` URL을 막도록 http(s)만 허용 |

## CSRF

Spring Security의 CSRF 보호를 끄지 않아도 됩니다.

- `useSpringForm`과 `springFetch`는 서버 컨텍스트의 CSRF 토큰을 헤더(`X-CSRF-TOKEN` 등 Spring이 정한 이름)로 자동 첨부합니다.
- 일반 HTML 폼(로그인, 로그아웃)에는 hidden 필드로 넣습니다.

```tsx
const { csrf } = useServerContext();

<form method="post" action="/logout">
  {csrf && <input type="hidden" name={csrf.parameterName} value={csrf.token} />}
  <button>로그아웃</button>
</form>
```

Mustache 화면의 일반 폼은 기존 Mustache 앱에서 하던 방식(요청 속성 `_csrf`를 템플릿에 노출해 hidden 필드로 넣기) 그대로 처리합니다.

## 서버 렌더링 사이드카

- SSR 번들(`server.mjs`)은 애플리케이션이 직접 띄우는 Node.js 프로세스이며 **`127.0.0.1`에서만** 대기합니다. 외부에서 접근할 수 없고, 별도 포트를 열 필요도 없습니다.
- 렌더링 요청에는 props와 서버 컨텍스트(CSRF 토큰, 사용자 이름 포함)가 담겨 루프백으로 전달됩니다. 같은 서버의 다른 로컬 프로세스를 신뢰할 수 없는 환경이라면 SSR을 끄세요(`mustache-react.ssr=off`).
- 사이드카는 받은 데이터로 HTML을 만드는 일만 하며 DB나 세션에 접근하지 않습니다.

## CDN에서 프론트를 서빙할 때

- `mustache-react.public-path`가 다른 도메인이면 스크립트 태그에 `crossorigin`이 붙고, CDN은 `Access-Control-Allow-Origin` 헤더를 보내야 합니다. 가능하면 `*` 대신 서비스 도메인을 지정하세요.
- CDN에 올리는 것은 빌드된 JS, CSS, 이미지뿐입니다. 사용자 데이터는 항상 Spring이 만든 HTML에만 들어갑니다.
- Content-Security-Policy를 쓴다면 `script-src`, `style-src`에 CDN 도메인을 추가하세요. 개발 모드에서는 React Refresh 때문에 인라인 스크립트가 하나 들어갑니다.

## 빌드 공급망

- 빌드 플러그인은 Node.js를 `nodejs.org`(설정 가능)에서 내려받고, 같은 릴리스의 `SHASUMS256.txt`와 **SHA-256을 비교**합니다. 다르면 빌드가 실패합니다. 압축을 풀 때 대상 폴더 밖을 가리키는 항목도 거부합니다.
- npm 패키지는 lock 파일이 있으면 lock에 적힌 버전 그대로 설치합니다(`npm ci`, `--frozen-lockfile`). lock 파일을 커밋하세요.
- 사내 미러를 쓴다면 `nodeDistributionUrl`과 npm 레지스트리 설정(`.npmrc`)을 바꿉니다.

:::
