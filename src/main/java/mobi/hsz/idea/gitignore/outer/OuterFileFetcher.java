package mobi.hsz.idea.gitignore.outer;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/** Outer file fetcher event interface. */
public interface OuterFileFetcher {
    @Nonnull
    Collection<VirtualFile> fetch(@Nonnull Project project);
}
