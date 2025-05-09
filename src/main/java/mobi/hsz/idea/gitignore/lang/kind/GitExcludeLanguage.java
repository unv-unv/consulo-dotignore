/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mobi.hsz.idea.gitignore.lang.kind;

import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.file.type.kind.GitExcludeFileType;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.outer.OuterFileFetcher;
import mobi.hsz.idea.gitignore.util.Icons;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Gitignore Exclude {@link IgnoreLanguage} definition.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.4
 */
public class GitExcludeLanguage extends IgnoreLanguage {
    /** The {@link GitExcludeLanguage} instance. */
    public static final GitExcludeLanguage INSTANCE = new GitExcludeLanguage();

    /** {@link IgnoreLanguage} is a non-instantiable static class. */
    private GitExcludeLanguage() {
        super("Git exclude", "exclude", ".git", Icons.GIT, new OuterFileFetcher[]{

            // `exclude` files located in .git directory
            new OuterFileFetcher() {
                /** Relative path to the exclude file. */
                private static final String EXCLUDE = "info/exclude";

                @Nonnull
                @Override
                public Collection<VirtualFile> fetch(@Nonnull Project project) {
                    Collection<VirtualFile> files = new ArrayList<>();
                    VirtualFile baseDir = project.getBaseDir();
                    if (baseDir == null) {
                        return files;
                    }

                    VirtualFile root = baseDir.findChild(".git");
                    return processExcludes(root, files);
                }

                /**
                 * Recursively finds exclude files in given root directory.
                 *
                 * @param root  current root
                 * @param files collection of {@link VirtualFile}
                 * @return exclude files collection
                 */
                @Nonnull
                private Collection<VirtualFile> processExcludes(
                    @Nullable VirtualFile root,
                    @Nonnull Collection<VirtualFile> files
                ) {
                    if (root != null) {
                        ContainerUtil.addIfNotNull(files, root.findFileByRelativePath(EXCLUDE));

                        VirtualFile modules = root.findChild("modules");
                        if (modules != null) {
                            VirtualFileUtil.visitChildrenRecursively(modules, new VirtualFileVisitor() {
                                @Override
                                public boolean visitFile(@Nonnull VirtualFile dir) {
                                    if (dir.findChild("index") != null) {
                                        processExcludes(dir, files);
                                        return false;
                                    }
                                    return dir.isDirectory();
                                }
                            });
                        }
                    }

                    return files;
                }
            }

        });
    }

    /**
     * Language file type.
     *
     * @return {@link GitExcludeFileType} instance
     */
    @Nonnull
    @Override
    public IgnoreFileType getFileType() {
        return GitExcludeFileType.INSTANCE;
    }

    /**
     * The Gitignore exclude filename.
     *
     * @return filename
     */
    @Nonnull
    @Override
    public String getFilename() {
        return super.getExtension();
    }

    /**
     * Defines if {@link GitExcludeLanguage} supports outer ignore files.
     *
     * @return supports outer ignore files
     */
    @Override
    public boolean isOuterFileSupported() {
        return true;
    }

    /**
     * Returns fixed directory for the given {@link IgnoreLanguage}.
     *
     * @param project current project
     * @return fixed directory
     */
    @Nullable
    @Override
    public VirtualFile getFixedDirectory(@Nonnull Project project) {
        return project.getBaseDir().findFileByRelativePath(getVcsDirectory() + "/info");
    }
}
