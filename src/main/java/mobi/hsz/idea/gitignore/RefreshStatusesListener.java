package mobi.hsz.idea.gitignore;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.component.messagebus.MessageBusConnection;

@TopicAPI(ComponentScope.PROJECT)
public interface RefreshStatusesListener {
    /** Topic to refresh files statuses using {@link MessageBusConnection}. */
    Class<RefreshStatusesListener> REFRESH_STATUSES = RefreshStatusesListener.class;

    void refresh();
}
