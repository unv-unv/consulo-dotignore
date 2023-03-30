package mobi.hsz.idea.gitignore;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * Listener bounded with {@link RefreshTrackedIgnoredListener#TRACKED_IGNORED_REFRESH} topic to trigger tracked and
 * ignored files list.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface RefreshTrackedIgnoredListener {
    /** Topic for refresh tracked and indexed files. */
    Class<RefreshTrackedIgnoredListener> TRACKED_IGNORED_REFRESH = RefreshTrackedIgnoredListener.class;

    void refresh();
}
