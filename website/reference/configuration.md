# 설정

`application.properties` / `application.yml`의 `mustache-react.*`:

```yaml
mustache-react:
  enabled: true
  content-negotiation: true          # Accept: application/json이면 @ReactPage가 JSON으로 응답
  public-path: /mustache-react/      # 빌드된 파일의 URL 경로
  manifest-location: classpath:/static/mustache-react/.vite/manifest.json
  layout: classpath:/templates/mustache-react/layout.mustache
  frontend-dir: src/main/frontend
  ssr: off                           # off | auto | always
  ssr-server:
    bundle-location: classpath:/META-INF/mustache-react/ssr/server.mjs
    timeout: 3s
    startup-timeout: 15s
  dev-server:
    enabled: false                   # 개발 프로파일에서 true
    url: http://localhost:5173
    auto-start: true
    node: ""                         # 기본: .mustache-react/node의 최신 버전, 없으면 PATH의 node
    node-install-dir: .mustache-react/node
    startup-timeout: 30s
  context:
    csrf: true
    user: true
    messages: []                     # 노출할 메시지 키
  template:
    model-attributes: true           # react, mr를 모든 Model에 추가
    react-name: react
    helpers-name: mr
```
