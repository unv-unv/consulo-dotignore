package consulo.dotignore.lang;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.PairedBraceMatcher;
import mobi.hsz.idea.gitignore.lang.IgnoreBraceMatcher;

/**
 * @author VISTALL
 * @since 28/03/2023
 */
@ExtensionImpl
public class IgnoreBraceMatcherExtender extends IgnoreLanguageExtensionExtender<PairedBraceMatcher> {
    public IgnoreBraceMatcherExtender() {
        super(PairedBraceMatcher.class, IgnoreBraceMatcher::new);
    }
}
