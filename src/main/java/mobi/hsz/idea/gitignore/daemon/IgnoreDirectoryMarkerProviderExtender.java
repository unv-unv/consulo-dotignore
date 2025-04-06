package mobi.hsz.idea.gitignore.daemon;

import consulo.annotation.component.ExtensionImpl;
import consulo.dotignore.lang.IgnoreLanguageExtensionExtender;
import consulo.language.editor.gutter.LineMarkerProvider;

/**
 * @author VISTALL
 * @since 2023-03-28
 */
@ExtensionImpl
public class IgnoreDirectoryMarkerProviderExtender extends IgnoreLanguageExtensionExtender<LineMarkerProvider> {
    public IgnoreDirectoryMarkerProviderExtender() {
        super(LineMarkerProvider.class, IgnoreDirectoryMarkerProvider::new);
    }
}
