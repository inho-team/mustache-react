# 배포: 같이 또는 따로

빌드는 한 번이고, 결과물도 같습니다. 프론트 파일을 **jar에 같이 넣어 배포할지**, **CDN이나 별도 서버에 따로 올릴지**는 배포할 때 설정 하나로 고릅니다. 프론트를 다시 빌드할 필요가 없습니다.

## 같이 배포 (기본)

```bash
./gradlew bootJar        # 또는 mvn package
java -jar build/libs/app.jar
```

프론트 파일이 jar 안에 들어 있고 Spring이 `/mustache-react/` 아래로 직접 서빙합니다. 설정할 것이 없습니다. 서버 한 대, 파일 하나로 끝나서 작은 서비스나 사내 시스템에 알맞습니다.

## 따로 배포 (CDN, 별도 정적 서버)

1. **빌드:** 평소와 같습니다. `./gradlew bootJar`
2. **프론트 파일 업로드:** 아래 폴더의 내용을 CDN(S3 + CloudFront, Cloudflare 등)이나 정적 서버에 그대로 올립니다.
   - Gradle: `build/mustache-react/resources/static/mustache-react/`
   - Maven: `target/mustache-react/static/mustache-react/`
3. **Spring 설정:** 올린 위치를 알려줍니다. 운영 프로필에만 넣으면 됩니다.
   ```properties
   # application-prod.properties
   mustache-react.public-path=https://cdn.example.com/shop/
   ```
4. **CDN 설정:** 다른 도메인에서 JS 모듈을 불러오므로 CORS 헤더가 필요합니다.
   ```
   Access-Control-Allow-Origin: https://shop.example.com    (또는 *)
   ```

이제 페이지의 `<script>`, CSS, island 조각 파일이 모두 CDN에서 옵니다. HTML, 데이터(props), 로그인, 서버 렌더링은 계속 Spring이 맡습니다.

### 순서와 주의할 점

- **프론트 파일을 먼저 올리고, 그다음 서버를 배포하세요.** 서버가 새 파일 이름을 가리킬 때 CDN에 그 파일이 이미 있어야 합니다.
- **이전 파일은 지우지 마세요.** 파일 이름에 내용 해시가 붙어 있어서(`OrderList-BXMkDw9W.js`) 버전이 달라도 이름이 겹치지 않습니다. 서버를 순차 배포하는 동안 이전 버전 서버도 자기 파일을 찾을 수 있습니다.
- **캐시:** `assets/` 아래 파일은 이름이 바뀌지 않는 한 내용도 바뀌지 않으니, `Cache-Control: public, max-age=31536000, immutable`로 길게 캐시해도 됩니다.
- jar에도 같은 파일이 들어 있어서, `public-path`를 기본값으로 되돌리면 언제든 "같이 배포"로 돌아갈 수 있습니다.
- 개발 모드(`dev-server.enabled=true`)에서는 `public-path`가 URL이어도 Vite 개발 서버를 씁니다.

## 어느 쪽을 고를까

| | 같이 | 따로 |
|---|---|---|
| 배포물 | jar 하나 | jar + 정적 파일 폴더 |
| 설정 | 없음 | `public-path`, CDN의 CORS |
| 장점 | 단순함, 버전 불일치 없음 | 정적 파일을 CDN이 빠르게 전달, 서버 부하 감소 |
| 알맞은 곳 | 사내 시스템, 작은 서비스, 처음 시작할 때 | 사용자가 많거나 해외 사용자가 있는 서비스 |

처음에는 같이 배포로 시작하고, 필요해지면 설정 한 줄로 바꾸면 됩니다.
