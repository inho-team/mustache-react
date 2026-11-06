import { useServerContext } from '@mustache-react/client';
import Header from '../../components/Header';
import type { PageProps } from '../../generated/types';

/** A plain HTML form posted to Spring Security's /login, with the CSRF token from the server context. */
export default function Login({ error }: PageProps<'auth/Login'>) {
  const { csrf } = useServerContext();
  return (
    <main className="page narrow">
      <Header />
      <h1>로그인</h1>
      {error && <p className="error">아이디 또는 비밀번호가 올바르지 않습니다.</p>}
      <form method="post" action="/login" className="stack">
        <label>
          아이디
          <input name="username" autoComplete="username" defaultValue="kim" />
        </label>
        <label>
          비밀번호
          <input name="password" type="password" autoComplete="current-password" />
        </label>
        {csrf && <input type="hidden" name={csrf.parameterName} value={csrf.token} />}
        <button type="submit">로그인</button>
      </form>
      <p className="hint">데모 계정: kim / password</p>
    </main>
  );
}
