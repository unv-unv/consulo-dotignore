package mobi.hsz.idea.gitignore.highlighter;

import consulo.annotation.component.ExtensionImpl;
import consulo.dotignore.lang.IgnoreLanguageExtensionExtender;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;

/**
 * @author VISTALL
 * @since 2023-03-28
 */
@ExtensionImpl
public class IgnoreHighlighterFactoryExtender extends IgnoreLanguageExtensionExtender<SyntaxHighlighterFactory> {
    public IgnoreHighlighterFactoryExtender() {
        super(SyntaxHighlighterFactory.class, IgnoreHighlighterFactory::new);
    }
}
