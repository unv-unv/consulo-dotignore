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

package mobi.hsz.idea.gitignore.indexing;

import consulo.content.scope.SearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;

import java.util.HashSet;

/**
 * Provides extended {@link GlobalSearchScope} with additional ignore files (i.e. outer gitignore files).
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 2.0
 */
public class IgnoreSearchScope extends GlobalSearchScope
{
    private IgnoreSearchScope(@Nonnull Project project) {
        super(project);
    }

    /**
     * Returns {@link GlobalSearchScope#projectScope(Project)} instance united with additional files.
     *
     * @param project current project
     * @return extended instance of {@link GlobalSearchScope}
     */
    @Nonnull
    public static GlobalSearchScope get(@Nonnull Project project) {
        IgnoreSearchScope scope = new IgnoreSearchScope(project);
        HashSet<VirtualFile> files = ExternalIndexableSetContributor.getAdditionalFiles(project);
        return scope.uniteWith(GlobalSearchScope.filesScope(project, files));
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
        return 0;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        return file.getFileType() instanceof IgnoreFileType;
    }

    @Override
    public boolean isSearchInLibraries() {
        return true;
    }

    @Override
    public boolean isForceSearchingInLibrarySources() {
        return true;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
        return true;
    }

    @Override
    public boolean isSearchOutsideRootModel() {
        return true;
    }

    @Nonnull
    @Override
    public GlobalSearchScope union(@Nonnull SearchScope scope) {
        return this;
    }

    @Nonnull
    @Override
    public SearchScope intersectWith(@Nonnull SearchScope scope2) {
        return scope2;
    }
}
