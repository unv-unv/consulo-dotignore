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
package mobi.hsz.idea.gitignore.codeInspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.dotignore.codeInspection.IgnoreInspection;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.path.FileReferenceOwner;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.FilesIndexCacheProjectComponent;
import mobi.hsz.idea.gitignore.IgnoreManager;
import mobi.hsz.idea.gitignore.psi.IgnoreEntry;
import mobi.hsz.idea.gitignore.psi.IgnoreVisitor;
import mobi.hsz.idea.gitignore.util.Glob;
import mobi.hsz.idea.gitignore.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Inspection tool that checks if entries are unused - does not cover any file or directory.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.5
 */
@ExtensionImpl
public class IgnoreUnusedEntryInspection extends IgnoreInspection {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return IgnoreLocalize.codeinspectionUnusedentry();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    /**
     * Checks if entries are related to any file.
     *
     * @param holder     where visitor will register problems found.
     * @param isOnTheFly true if inspection was run in non-batch mode
     * @return not-null visitor for this inspection
     */
    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        FilesIndexCacheProjectComponent cache = FilesIndexCacheProjectComponent.getInstance(project);
        IgnoreManager manager = IgnoreManager.getInstance(project);

        return new IgnoreVisitor() {
            @Override
            @RequiredReadAction
            public void visitEntry(@Nonnull IgnoreEntry entry) {
                PsiReference[] references = entry.getReferences();
                boolean resolved = true;
                int previous = Integer.MAX_VALUE;
                for (PsiReference reference : references) {
                    ProgressManager.checkCanceled();
                    if (reference instanceof FileReferenceOwner) {
                        PsiPolyVariantReference fileReference = (PsiPolyVariantReference) reference;
                        ResolveResult[] result = fileReference.multiResolve(false);
                        resolved = result.length > 0 || (previous > 0 && reference.getCanonicalText().endsWith("/*"));
                        previous = result.length;
                    }
                    if (!resolved) {
                        break;
                    }
                }

                if (!resolved) {
                    if (!isEntryExcluded(entry, holder.getProject())) {
                        holder.newProblem(IgnoreLocalize.codeinspectionUnusedentryMessage())
                            .range(entry)
                            .withFixes(new IgnoreRemoveEntryFix(entry))
                            .create();
                    }
                }

                super.visitEntry(entry);
            }

            /**
             * Checks if given {@link IgnoreEntry} is excluded in the current {@link Project}.
             *
             * @param entry   Gitignore entry
             * @param project current project
             * @return entry is excluded in current project
             */
            @RequiredReadAction
            private boolean isEntryExcluded(@Nonnull IgnoreEntry entry, @Nonnull Project project) {
                Pattern pattern = Glob.createPattern(entry);
                if (pattern == null) {
                    return false;
                }

                VirtualFile projectRoot = project.getBaseDir();
                List<VirtualFile> matched = new ArrayList<>();
                Collection<VirtualFile> files = cache.getFilesForPattern(project, pattern);

                if (projectRoot == null) {
                    return false;
                }

                for (VirtualFile root : Utils.getExcludedRoots(project)) {
                    for (VirtualFile file : files) {
                        ProgressManager.checkCanceled();
                        if (!Utils.isUnder(file, root)) {
                            continue;
                        }
                        String path = Utils.getRelativePath(projectRoot, root);
                        if (manager.getMatcher().match(pattern, path)) {
                            matched.add(file);
                            return false;
                        }
                    }

                    if (!matched.isEmpty()) {
                        return true;
                    }
                }

                return false;
            }
        };
    }
}
