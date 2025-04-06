package consulo.dotignore.lang;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.parser.ParserDefinition;
import mobi.hsz.idea.gitignore.lang.IgnoreParserDefinition;

/**
 * @author VISTALL
 * @since 2023-03-28
 */
@ExtensionImpl
public class IgnoreParserDefinitionExtender extends IgnoreLanguageExtensionExtender<ParserDefinition> {
    public IgnoreParserDefinitionExtender() {
        super(ParserDefinition.class, IgnoreParserDefinition::new);
    }
}
