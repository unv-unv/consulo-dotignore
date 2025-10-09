package consulo.dotignore.codeInspection;

import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-03-29
 */
public abstract class IgnoreInspection extends LocalInspectionTool {
    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return IgnoreLocalize.codeinspectionGroup();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
