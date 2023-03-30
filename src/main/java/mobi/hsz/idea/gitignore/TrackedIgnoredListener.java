package mobi.hsz.idea.gitignore;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentMap;

/**
 * Listener bounded with {@link TrackedIgnoredListener#TRACKED_IGNORED} topic to inform about new entries.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface TrackedIgnoredListener {
    /** Topic for detected tracked and indexed files. */
    Class<TrackedIgnoredListener> TRACKED_IGNORED = TrackedIgnoredListener.class;

    void handleFiles(@NotNull ConcurrentMap<VirtualFile, VcsRoot> files);
}
