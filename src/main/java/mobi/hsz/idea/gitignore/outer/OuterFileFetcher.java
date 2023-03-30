package mobi.hsz.idea.gitignore.outer;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/** Outer file fetcher event interface. */
public interface OuterFileFetcher {
    @NotNull
    Collection<VirtualFile> fetch(@NotNull Project project);
}
