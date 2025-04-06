package consulo.dotignore;

import consulo.annotation.component.ExtensionImpl;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2023-03-29
 */
@ExtensionImpl
public class IgnoreNotificationGroup implements NotificationGroupContributor {
    public static final NotificationGroup GROUP = NotificationGroup.balloonGroup("dotIgnore", IgnoreLocalize.notificationGroup());

    @Override
    public void contribute(@Nonnull Consumer<NotificationGroup> consumer) {
        consumer.accept(GROUP);
    }
}
