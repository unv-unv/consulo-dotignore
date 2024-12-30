package consulo.dotignore.codeInspection;

import consulo.language.editor.inspection.LocalInspectionTool;
import mobi.hsz.idea.gitignore.IgnoreBundle;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/03/2023
 */
public abstract class IgnoreInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return IgnoreBundle.message("codeInspection.group");
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
