package consulo.dotignore.lang;

import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.language.Language;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 28/03/2023
 */
public abstract class IgnoreLanguageExtensionExtender<E> implements ExtensionExtender<E> {
    private final Class<E> extensionClass;
    private final Function<Language, ? extends E> byLanguageFactory;

    public IgnoreLanguageExtensionExtender(Class<E> extensionClass, Function<Language, ? extends E> byLanguageFactory) {
        this.extensionClass = extensionClass;
        this.byLanguageFactory = byLanguageFactory;
    }

    @Override
    public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<E> consumer) {
        consumer.accept(byLanguageFactory.apply(IgnoreLanguage.INSTANCE));

        for (IgnoreLanguage language : IgnoreBundle.LANGUAGES) {
            consumer.accept(byLanguageFactory.apply(language));
        }
    }

    @Nonnull
    @Override
    public Class<E> getExtensionClass() {
        return extensionClass;
    }

    @Override
    public boolean hasAnyExtensions(ComponentManager componentManager) {
        return !IgnoreBundle.LANGUAGES.isEmpty();
    }
}
