package consulo.dotignore.lang;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.PairedBraceMatcher;
import mobi.hsz.idea.gitignore.lang.IgnoreBraceMatcher;

/**
 * @author VISTALL
 * @since 2023-03-28
 */
@ExtensionImpl
public class IgnoreBraceMatcherExtender extends IgnoreLanguageExtensionExtender<PairedBraceMatcher> {
    public IgnoreBraceMatcherExtender() {
        super(PairedBraceMatcher.class, IgnoreBraceMatcher::new);
    }
}
