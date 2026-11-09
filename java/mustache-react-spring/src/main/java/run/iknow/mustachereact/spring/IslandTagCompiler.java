package run.iknow.mustachereact.spring;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

/**
 * A Mustache compiler that understands island tags ({@code <react:StatusBadge status="{{status}}" />}), see
 * {@link HtmlIslandTags}. It keeps every setting of the compiler it wraps, and applies to views, layouts and
 * partials alike. The {@code with...} methods of JMustache return plain compilers, so wrap after configuring.
 */
public class IslandTagCompiler extends Mustache.Compiler {

    private final String prefix;
    private final String section;
    private final String helpers;

    /**
     * @param compiler the application's compiler, whose settings are kept
     * @param prefix tag prefix, {@code react} for {@code <react:Name>}
     * @param section name of the island section in the model ({@code react})
     * @param helpers name of the helpers in the model ({@code mr})
     */
    public IslandTagCompiler(Mustache.Compiler compiler, String prefix, String section, String helpers) {
        super(compiler.standardsMode, compiler.strictSections, compiler.nullValue, compiler.missingIsNull,
                compiler.emptyStringIsFalse, compiler.zeroIsFalse, compiler.formatter, compiler.escaper, compiler.loader,
                compiler.collector, compiler.delims);
        this.prefix = prefix;
        this.section = section;
        this.helpers = helpers;
    }

    @Override
    public Template compile(Reader source) {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[4096];
        try {
            int n;
            while ((n = source.read(buffer)) != -1) {
                text.append(buffer, 0, n);
            }
        } catch (IOException e) {
            throw new MustacheException(e);
        }
        return super.compile(new StringReader(HtmlIslandTags.expand(text.toString(), prefix, section, helpers)));
    }
}
