package consulo.dotignore.codeInspection;

import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.editor.inspection.LocalInspectionTool;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-03-29
 */
public abstract class IgnoreInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return IgnoreLocalize.codeinspectionGroup().get();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
