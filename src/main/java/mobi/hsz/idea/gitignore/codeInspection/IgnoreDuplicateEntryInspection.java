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
import consulo.dotignore.codeInspection.IgnoreInspection;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.psi.IgnoreEntry;
import mobi.hsz.idea.gitignore.psi.IgnoreFile;
import mobi.hsz.idea.gitignore.psi.IgnoreVisitor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Inspection tool that checks if entries are duplicated by others.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.5
 */
@ExtensionImpl
public class IgnoreDuplicateEntryInspection extends IgnoreInspection {
    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return IgnoreLocalize.codeinspectionDuplicateentry();
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly, @Nonnull LocalInspectionToolSession session, @Nonnull Object state) {
        PsiFile file = holder.getFile();
        if (!(file instanceof IgnoreFile)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitFile(PsiFile file) {
                if (file instanceof IgnoreFile ignoreFile) {
                    checkFile(holder, ignoreFile);
                }
            }
        };
    }

    @RequiredReadAction
    private void checkFile(@Nonnull ProblemsHolder problemsHolder, @Nonnull IgnoreFile file) {
        MultiMap<String, IgnoreEntry> entries = MultiMap.create();

        file.acceptChildren(new IgnoreVisitor() {
            @Override
            @RequiredReadAction
            public void visitEntry(@Nonnull IgnoreEntry entry) {
                entries.putValue(entry.getText(), entry);
                super.visitEntry(entry);
            }
        });

        for (Map.Entry<String, Collection<IgnoreEntry>> stringCollectionEntry : entries.entrySet()) {
            Iterator<IgnoreEntry> iterator = stringCollectionEntry.getValue().iterator();
            iterator.next();
            while (iterator.hasNext()) {
                IgnoreEntry entry = iterator.next();
                problemsHolder.newProblem(IgnoreLocalize.codeinspectionDuplicateentryMessage())
                    .range(entry)
                    .withFixes(new IgnoreRemoveEntryFix(entry))
                    .create();
            }
        }
    }

    /**
     * Forces checking every entry in checked file.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean runForWholeFile() {
        return true;
    }
}
