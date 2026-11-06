import { message, useServerContext } from '@mustache-react/client';

/** Shared header: the signed-in user comes from Spring Security through the server context. */
export default function Header() {
  const context = useServerContext();
  const { user, csrf } = context;
  return (
    <header className="header">
      <a href="/orders" className="brand">
        {message('app.name', context)}
      </a>
      {user ? (
        <form method="post" action="/logout" className="session">
          <span data-testid="user">{user.name}님</span>
          {csrf && <input type="hidden" name={csrf.parameterName} value={csrf.token} />}
          <button type="submit">로그아웃</button>
        </form>
      ) : (
        <a href="/login" className="session">
          로그인
        </a>
      )}
    </header>
  );
}
