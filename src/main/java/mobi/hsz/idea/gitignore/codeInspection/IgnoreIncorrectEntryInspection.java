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
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.psi.IgnoreEntry;
import mobi.hsz.idea.gitignore.psi.IgnoreVisitor;
import mobi.hsz.idea.gitignore.util.Glob;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Inspection tool that checks if entry has correct form in specific according to the specific {@link
 * IgnoreBundle.Syntax}.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.0
 */
@ExtensionImpl
public class IgnoreIncorrectEntryInspection extends IgnoreInspection {
    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return IgnoreLocalize.codeinspectionIncorrectentry();
    }

    /**
     * Checks if entry has correct form in specific according to the specific {@link IgnoreBundle.Syntax}.
     *
     * @param holder     where visitor will register problems found.
     * @param isOnTheFly true if inspection was run in non-batch mode
     * @return not-null visitor for this inspection
     */
    @Nonnull
    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new IgnoreVisitor() {
            @Override
            @RequiredReadAction
            public void visitEntry(@Nonnull IgnoreEntry entry) {
                String regex = entry.getText();
                if (IgnoreBundle.Syntax.GLOB.equals(entry.getSyntax())) {
                    regex = Glob.createRegex(regex, false);
                }

                try {
                    Pattern.compile(regex);
                } catch (PatternSyntaxException e) {
                    holder.newProblem(IgnoreLocalize.codeinspectionIncorrectentryMessage(e.getDescription()))
                        .range(entry)
                        .create();
                }
            }
        };
    }
}
