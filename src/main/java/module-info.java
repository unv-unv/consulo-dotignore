/**
 * @author VISTALL
 * @since 30/03/2023
 */
open module mobi.hsz.idea.gitignore {
    requires consulo.ide.api;

    requires org.apache.commons.lang3;

    requires static com.intellij.git;

    // TODO remove in future
    requires java.desktop;
    requires forms.rt;
}