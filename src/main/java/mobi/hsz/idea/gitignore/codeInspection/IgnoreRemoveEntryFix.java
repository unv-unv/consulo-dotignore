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
import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Editor;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.psi.IgnoreEntry;
import mobi.hsz.idea.gitignore.psi.IgnoreTypes;

/**
 * QuickFix action that removes specified entry handled by code inspections like
 * {@link IgnoreCoverEntryInspection},
 * {@link IgnoreDuplicateEntryInspection},
 * {@link IgnoreUnusedEntryInspection}.
 *
 * @author Alexander Zolotov <alexander.zolotov@jetbrains.com>
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.4
 */
public class IgnoreRemoveEntryFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    /**
     * Builds a new instance of {@link IgnoreRemoveEntryFix}.
     *
     * @param entry an element that will be handled with QuickFix
     */
    public IgnoreRemoveEntryFix(@Nonnull IgnoreEntry entry) {
        super(entry);
    }

    /**
     * Gets QuickFix name.
     *
     * @return QuickFix action name
     */
    @Nonnull
    @Override
    @RequiredReadAction
    public LocalizeValue getText() {
        return IgnoreLocalize.quickFixRemoveEntry();
    }

    /**
     * Handles QuickFix action invoked on {@link IgnoreEntry}.
     *
     * @param project      the {@link Project} containing the working file
     * @param file         the {@link PsiFile} containing handled entry
     * @param editor       is null when called from inspection
     * @param startElement the {@link IgnoreEntry} that will be removed
     * @param endElement   the {@link PsiElement} which is ignored in invoked action
     */
    @Override
    @RequiredWriteAction
    public void invoke(
        @Nonnull Project project,
        @Nonnull PsiFile file,
        @Nullable Editor editor,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement
    ) {
        if (startElement instanceof IgnoreEntry) {
            removeCrlf(startElement);
            startElement.delete();
        }
    }

    /**
     * Shorthand method for removing CRLF element.
     *
     * @param startElement working PSI element
     */
    @RequiredWriteAction
    private void removeCrlf(PsiElement startElement) {
        ASTNode node = TreeUtil.findSibling(startElement.getNode(), IgnoreTypes.CRLF);
        if (node == null) {
            node = TreeUtil.findSiblingBackward(startElement.getNode(), IgnoreTypes.CRLF);
        }
        if (node != null) {
            node.getPsi().delete();
        }
    }
}
