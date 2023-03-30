package consulo.dotignore;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import mobi.hsz.idea.gitignore.IgnoreManager;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/03/2023
 */
@ExtensionImpl public class IgnoreManagerPostStartActivity implements PostStartupActivity, DumbAware {
    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        IgnoreManager manager = IgnoreManager.getInstance(project);

        manager.projectOpened();
    }
}
