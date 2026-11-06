package run.iknow.mustachereact.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * {@code locale}: the request locale as a language tag, e.g. {@code ko-KR}. With language switching configured,
 * also {@code i18n}: {@code {param: "lang", locales: ["ko", "en"]}}, for a language menu
 * ({@code localeHref("en")} in the client).
 */
public class LocaleContextContributor implements ServerContextContributor {

    private final String param;
    private final List<String> locales = new ArrayList<>();

    /** Only the request locale. */
    public LocaleContextContributor() {
        this(null, Collections.<Locale>emptyList());
    }

    /**
     * @param param request parameter that switches the language, e.g. {@code lang}; {@code null} for none
     * @param locales the languages the application offers, for a language menu
     */
    public LocaleContextContributor(String param, List<Locale> locales) {
        this.param = param == null || param.isEmpty() ? null : param;
        for (Locale locale : locales) {
            this.locales.add(locale.toLanguageTag());
        }
    }

    @Override
    public void contribute(Map<String, Object> context) {
        context.put("locale", LocaleContextHolder.getLocale().toLanguageTag());
        if (param != null || !locales.isEmpty()) {
            Map<String, Object> i18n = new LinkedHashMap<>();
            if (param != null) {
                i18n.put("param", param);
            }
            i18n.put("locales", locales);
            context.put("i18n", i18n);
        }
    }
}
