import { localeHref, useMessages, useServerContext } from '@mustache-react/client';

const LANGUAGE_NAMES: Record<string, string> = { ko: '한국어', en: 'English' };

/**
 * Shared header: the signed-in user comes from Spring Security, the texts from messages.properties, and the
 * language links from mustache-react.i18n.* (all through the server context).
 */
export default function Header() {
  const { user, csrf, i18n } = useServerContext();
  const { t, locale } = useMessages();
  return (
    <header className="header">
      <a href="/orders" className="brand">
        {t('app.name')}
      </a>
      <nav className="session">
        {i18n?.locales.map((language) => (
          <a
            key={language}
            href={localeHref(language)}
            hrefLang={language}
            aria-current={locale?.startsWith(language) ? 'true' : undefined}
            data-testid={`lang-${language}`}
          >
            {LANGUAGE_NAMES[language] ?? language}
          </a>
        ))}
        {user ? (
          <form method="post" action="/logout">
            <span data-testid="user">{t('nav.user', user.name)}</span>
            {csrf && <input type="hidden" name={csrf.parameterName} value={csrf.token} />}
            <button type="submit">{t('nav.logout')}</button>
          </form>
        ) : (
          <a href="/login">{t('nav.login')}</a>
        )}
      </nav>
    </header>
  );
}
