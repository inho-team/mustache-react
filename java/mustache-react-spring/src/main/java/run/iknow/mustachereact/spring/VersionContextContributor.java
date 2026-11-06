package run.iknow.mustachereact.spring;

import java.util.Collections;
import java.util.Map;

import run.iknow.mustachereact.core.MustacheReactVersion;

/**
 * {@code mustacheReact.version}: the version of the Java library, so that {@code @mustache-react/client} can warn
 * when it was built with another version.
 */
public class VersionContextContributor implements ServerContextContributor {

    @Override
    public void contribute(Map<String, Object> context) {
        context.put("mustacheReact", Collections.singletonMap("version", MustacheReactVersion.get()));
    }
}
