# 빌드 플러그인

Gradle 플러그인과 Maven 플러그인은 같은 일을 합니다(공통 코드 `mustache-react-build-tools`).

1. Node.js를 `.mustache-react/node`에 내려받습니다(SHA-256 검증). 개발자 PC나 CI에 Node.js가 없어도 됩니다.
2. `src/main/frontend/package.json`이 없으면 프론트 프로젝트 파일을 만듭니다.
3. 프론트 의존성을 설치합니다(npm, pnpm, yarn 자동 감지).
4. `mustache-react-vite build`로 빌드해 jar의 `static/mustache-react/`(SSR 번들은 `META-INF/mustache-react/ssr/`)에 넣습니다.

`.gitignore`에 `.mustache-react/`(내려받은 Node.js)를 추가하세요.

## Gradle

```kotlin
plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("run.iknow.mustache-react") version "0.1.0"
    java
}
```

### 설정: `mustacheReact { }`

전부 기본값이 있습니다.

```kotlin
mustacheReact {
    frontendDir = layout.projectDirectory.dir("src/main/frontend")
    publicPath = "/mustache-react/"
    ssr = false
    install = true
    packageManager = "auto"            // npm | pnpm | yarn
    nodeDownload = true                // false: PATH의 node
    nodeVersion = "24.22.2"
    nodeDistributionUrl = "https://nodejs.org/dist"
    nodeInstallDir = layout.projectDirectory.dir(".mustache-react/node")
    types = true
    typesFile = layout.projectDirectory.file("src/main/frontend/generated/types.ts")
}
```

| 속성 | 기본값 | 설명 |
|---|---|---|
| `frontendDir` | `src/main/frontend` | `package.json`이 있는 프론트 디렉터리 |
| `publicPath` | `/mustache-react/` | jar 안에서 에셋이 놓이는 경로(`static/<publicPath>`). `mustache-react.public-path`의 기본값과 같습니다. CDN에 따로 배포할 때도 바꾸지 않습니다 |
| `frontendBuild` | `true` (`-PmustacheReact.skipFrontend`면 `false`) | 프론트를 빌드할지. `false`면 설치도 건너뜁니다 |
| `ssr` | `false` | SSR 번들(`server.mjs`)도 만들지. `mustache-react.ssr=auto/always`에 필요하고, 프론트 의존성에 `@mustache-react/server`가 있어야 합니다 |
| `install` | `true` | 빌드 전에 의존성을 설치할지 |
| `packageManager` | `auto` | `auto`, `npm`, `pnpm`, `yarn` |
| `nodeDownload` | `true` | Node.js를 내려받을지. `false`면 PATH의 `node` |
| `nodeVersion` | `24.22.2` | 내려받을 Node.js 버전 |
| `nodeDistributionUrl` | `https://nodejs.org/dist` | 다운로드 주소(사내 미러용) |
| `nodeInstallDir` | `.mustache-react/node` | 내려받은 Node.js를 두는 곳. Spring의 개발 서버 자동 시작도 여기서 찾습니다 |
| `types` | `true` | `mustache-react-processor`를 annotation processor로 추가하고 `types.ts` 경로를 넘길지 |
| `typesFile` | `<frontendDir>/generated/types.ts` | 생성할 TypeScript 파일 |
| `processorDependency(...)` | 플러그인과 같은 버전의 `run.iknow:mustache-react-processor` | 타입 생성기 artifact를 바꿀 때 |

### 태스크

| 태스크 | 내용 |
|---|---|
| `mustacheReactInstall` | 의존성 설치. `package.json`과 lock 파일이 그대로이고 `node_modules`가 있으면 up-to-date |
| `mustacheReactBuild` | 프론트 빌드. 결과는 `build/mustache-react/resources`. 빌드 캐시를 지원하고, `compileJava`(타입 생성) 뒤에 실행됩니다 |

플러그인이 연결해 주는 것:
- **리소스:** `processResources`가 `mustacheReactBuild` 결과를 jar 리소스에 넣습니다. 따라서 `build`, `bootJar`, `bootRun` 모두 프론트를 빌드합니다.
- **타입 생성:** `compileJava`가 `-Amustachereact.typesFile=...`로 `types.ts`를 씁니다.
- **실행 설정:** `bootRun`, `run` 같은 `JavaExec` 태스크에 `-Dmustache-react.frontend-dir`, `-Dmustache-react.dev-server.node-install-dir`를 넘겨, 개발 모드의 Vite 개발 서버가 소스와 Node.js를 찾게 합니다.

### 명령줄

```bash
./gradlew bootJar                                   # 프론트 포함 실행 jar
./gradlew test -PmustacheReact.skipFrontend          # 프론트 빌드 없이 Java만
./gradlew mustacheReactBuild                         # 프론트만
```

Gradle 7.6 이상, 설정 캐시를 지원합니다.

## Maven

```xml
<plugin>
  <groupId>run.iknow</groupId>
  <artifactId>mustache-react-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions>
    <execution><goals><goal>build</goal></goals></execution>
  </executions>
  <configuration>
    <ssr>false</ssr>
  </configuration>
</plugin>
```

### goal: `build`

`generate-resources` 단계에 실행되고, 결과 디렉터리를 프로젝트 리소스로 추가합니다.

| 파라미터 | 사용자 속성 | 기본값 | 설명 |
|---|---|---|---|
| `frontendDir` | `mustacheReact.frontendDir` | `${project.basedir}/src/main/frontend` | 프론트 디렉터리 |
| `publicPath` | `mustacheReact.publicPath` | `/mustache-react/` | jar 안의 에셋 경로 |
| `skipFrontend` | `mustacheReact.skipFrontend` | `false` | 프론트 빌드를 건너뜀 |
| `ssr` | `mustacheReact.ssr` | `false` | SSR 번들도 만듦 |
| `install` | `mustacheReact.install` | `true` | 의존성 설치 |
| `packageManager` | `mustacheReact.packageManager` | `auto` | `auto`, `npm`, `pnpm`, `yarn` |
| `nodeDownload` | `mustacheReact.nodeDownload` | `true` | Node.js를 내려받을지 |
| `nodeVersion` | `mustacheReact.nodeVersion` | `24.22.2` | Node.js 버전 |
| `nodeDistributionUrl` | `mustacheReact.nodeDistributionUrl` | `https://nodejs.org/dist` | 다운로드 주소 |
| `nodeInstallDir` | `mustacheReact.nodeInstallDir` | `${project.basedir}/.mustache-react/node` | Node.js 위치 |
| `outputDirectory` | - | `${project.build.directory}/mustache-react` | 빌드 결과(`static/<publicPath>/`, SSR 번들) |

Maven에는 Gradle처럼 상태를 보고 건너뛰는 기능이 없어서, `package.json`과 lock 파일의 해시를 `node_modules/.mustache-react-install`에 적어 두고 바뀌었을 때만 다시 설치합니다.

```bash
mvn package                                # 프론트 포함 jar
mvn test -DmustacheReact.skipFrontend      # Java만
```

TypeScript 타입 생성은 Maven 플러그인이 자동으로 설정하지 않습니다. `maven-compiler-plugin`에 annotation processor를 추가하세요. [타입 생성](../guide/types#maven)을 참고하세요.

## 세부 동작

### Node.js 다운로드

- `nodeDistributionUrl/v<버전>/`에서 운영체제와 CPU에 맞는 압축 파일(Linux·macOS `.tar.gz`, Windows `.zip`)을 받습니다.
- 받은 파일은 같은 곳의 `SHASUMS256.txt`로 SHA-256을 확인합니다. 맞지 않으면 빌드가 실패합니다.
- 압축을 풀어 `nodeInstallDir/node-v<버전>-<os>-<arch>`에 둡니다. 이미 있으면 다시 받지 않습니다.
- 사내망에서는 `nodeDistributionUrl`을 같은 구조의 미러로 바꾸거나 `nodeDownload = false`로 설치된 Node.js를 씁니다.

### 패키지 매니저

| 순서 | 판단 |
|---|---|
| 1 | `packageManager` 설정이 `auto`가 아니면 그것 |
| 2 | `package.json`의 `"packageManager": "pnpm@10.28.0"` 필드 |
| 3 | `pnpm-lock.yaml`이 있으면 pnpm, `yarn.lock`이 있으면 yarn |
| 4 | 그 밖에는 npm |

- **lock 파일이 있을 때:** lock 파일 그대로 설치합니다(`npm ci`, `pnpm install --frozen-lockfile`, `yarn install --frozen-lockfile`).
- **pnpm·yarn 버전:** `packageManager` 필드에 버전이 있으면 그 버전을 `npm exec`로 실행하고, 없으면 PATH의 pnpm·yarn을 씁니다.

### 프론트 프로젝트 자동 생성

`frontendDir`에 `package.json`이 없으면 설치·빌드 전에 다음을 만듭니다. 이미 있는 파일은 건드리지 않습니다.

| 파일 | 내용 |
|---|---|
| `package.json` | `@mustache-react/client`, `@mustache-react/server`, `@mustache-react/vite-plugin`(플러그인과 같은 버전), React, Vite, TypeScript, `@vitejs/plugin-react` |
| `tsconfig.json` | React JSX, strict, bundler 모듈 해석 |
| `.gitignore` | `node_modules/` |
| `pages/`, `islands/` | 빈 디렉터리 |

만들어진 파일은 커밋하세요. 의존성을 더 쓰려면 `package.json`에 추가하면 됩니다.

### 버전 확인

빌드 결과에는 빌드한 `@mustache-react/vite-plugin` 버전이 `mustache-react-build.json`으로 들어갑니다. 애플리케이션이 시작할 때 이 버전이 Java 라이브러리 버전과 다르면 로그에 경고를 남깁니다.

## CI 팁

- **캐시:** Gradle(`~/.gradle`)과 `.mustache-react/node`를 캐시하면 Node.js를 매번 받지 않습니다. lock 파일을 커밋해야 설치 결과가 매번 같습니다.
- **프론트 없는 작업:** 프론트와 상관없는 Java 테스트 작업은 `-PmustacheReact.skipFrontend`(Maven은 `-DmustacheReact.skipFrontend`)로 빨리 돌릴 수 있습니다. 이때 jar에는 프론트가 들어가지 않습니다.
- **생성된 타입 확인:** `types.ts`는 커밋하는 걸 권장합니다. CI에서 빌드 후 `git diff --exit-code src/main/frontend/generated`로 Java 변경과 타입이 맞는지 확인할 수 있습니다.
