package consulo.dotignore;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 29/03/2023
 */
@ExtensionImpl
public class IgnoreNotificationGroup implements NotificationGroupContributor {
    public static final NotificationGroup GROUP = NotificationGroup.balloonGroup("dotIgnore", LocalizeValue.localizeTODO(".ignore"));

    @Override
    public void contribute(@Nonnull Consumer<NotificationGroup> consumer) {
        consumer.accept(GROUP);
    }
}
