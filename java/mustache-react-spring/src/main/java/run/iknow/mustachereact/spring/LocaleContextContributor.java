package run.iknow.mustachereact.spring;

import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;

/** {@code locale}: the request locale as a language tag, e.g. {@code ko-KR}. */
public class LocaleContextContributor implements ServerContextContributor {

    @Override
    public void contribute(Map<String, Object> context) {
        context.put("locale", LocaleContextHolder.getLocale().toLanguageTag());
    }
}
