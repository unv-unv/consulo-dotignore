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

package mobi.hsz.idea.gitignore.ui.untrackFiles;

import consulo.project.Project;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * {@link FileTreeNode} is an implementation of checkbox tree node.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.7
 */
public class FileTreeNode extends CheckedTreeNode {
    /** Current {@link VirtualFile} element. */
    @Nonnull
    private final VirtualFile file;

    /** Current {@link Project} element. */
    @Nonnull
    private final Project project;

    /** {@link VcsRoot} of the given {@link #file}. */
    @Nullable
    private final VcsRoot vcsRoot;

    /**
     * Creates a new instance of {@link FileTreeNode}.
     *
     * @param project current project
     * @param file    current file to render
     * @param vcsRoot VCS root
     */
    public FileTreeNode(@Nonnull Project project, @Nonnull VirtualFile file, @Nullable VcsRoot vcsRoot) {
        super(file);
        this.project = project;
        this.file = file;
        this.vcsRoot = vcsRoot;
    }

    /**
     * Returns current project.
     *
     * @return project
     */
    @Nonnull
    public Project getProject() {
        return project;
    }

    /**
     * Returns current file.
     *
     * @return file
     */
    @Nonnull
    public VirtualFile getFile() {
        return file;
    }

    /**
     * Returns {@link VcsRoot} for given {@link #file}.
     *
     * @return repository
     */
    @Nullable
    public VcsRoot getVcsRoot() {
        return vcsRoot;
    }
}
