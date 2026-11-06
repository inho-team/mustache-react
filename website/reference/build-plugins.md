# 빌드 플러그인

두 플러그인은 같은 일을 합니다: Node.js를 `.mustache-react/node`에 내려받고(SHA-256 검증), 프론트 의존성을 설치하고(npm, pnpm, yarn 자동 감지, lock 파일이 있으면 `npm ci`/`--frozen-lockfile`), `mustache-react-vite build`로 빌드한 결과를 jar의 `static/mustache-react/`(SSR 번들은 `META-INF/mustache-react/ssr/`)에 넣습니다.

## Gradle

```kotlin
mustacheReact {
    frontendDir = layout.projectDirectory.dir("src/main/frontend")
    publicPath = "/mustache-react/"
    ssr = false
    install = true
    packageManager = "auto"          // npm | pnpm | yarn
    nodeDownload = true              // false: PATH의 node
    nodeVersion = "24.22.2"
    nodeDistributionUrl = "https://nodejs.org/dist"
    types = true                     // mustache-react-processor와 typesFile 설정
    typesFile = layout.projectDirectory.file("src/main/frontend/generated/types.ts")
}
```

| 태스크 | 내용 |
|---|---|
| `mustacheReactInstall` | 의존성 설치. `package.json`과 lock 파일이 그대로면 up-to-date |
| `mustacheReactBuild` | 프론트 빌드 (빌드 캐시 지원). `processResources`가 결과를 포함 |

`-PmustacheReact.skipFrontend`로 프론트 빌드를 건너뜁니다. Gradle 7.6 이상, 설정 캐시 지원.

## Maven

```xml
<plugin>
  <groupId>run.iknow</groupId>
  <artifactId>mustache-react-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions><execution><goals><goal>build</goal></goals></execution></executions>
  <configuration>
    <ssr>false</ssr>
    <install>true</install>
    <packageManager>auto</packageManager>
    <nodeDownload>true</nodeDownload>
    <nodeVersion>24.22.2</nodeVersion>
  </configuration>
</plugin>
```

`build` goal은 `generate-resources` 단계에 실행되며, `package.json`과 lock 파일이 바뀌었을 때만 다시 설치합니다. `-DmustacheReact.skipFrontend`로 건너뜁니다.
