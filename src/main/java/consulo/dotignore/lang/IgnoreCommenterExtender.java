package consulo.dotignore.lang;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Commenter;
import mobi.hsz.idea.gitignore.lang.IgnoreCommenter;

/**
 * @author VISTALL
 * @since 28/03/2023
 */
@ExtensionImpl
public class IgnoreCommenterExtender extends IgnoreLanguageExtensionExtender<Commenter> {
    public IgnoreCommenterExtender() {
        super(Commenter.class, IgnoreCommenter::new);
    }
}
