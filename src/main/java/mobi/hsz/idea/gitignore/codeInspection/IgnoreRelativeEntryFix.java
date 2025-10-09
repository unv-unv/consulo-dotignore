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
import consulo.document.Document;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.psi.IgnoreEntry;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * QuickFix action that removes relative parts of the entry
 * {@link IgnoreRelativeEntryInspection}.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.8
 */
public class IgnoreRelativeEntryFix extends LocalQuickFixOnPsiElement {
    /**
     * Builds a new instance of {@link IgnoreRelativeEntryFix}.
     *
     * @param entry an element that will be handled with QuickFix
     */
    public IgnoreRelativeEntryFix(@Nonnull IgnoreEntry entry) {
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
        return IgnoreLocalize.quickFixRelativeEntry();
    }

    /**
     * Handles QuickFix action invoked on {@link IgnoreEntry}.
     *
     * @param project      the {@link Project} containing the working file
     * @param psiFile      the {@link PsiFile} containing handled entry
     * @param startElement the {@link IgnoreEntry} that will be removed
     * @param endElement   the {@link PsiElement} which is ignored in invoked action
     */
    @Override
    @RequiredUIAccess
    public void invoke(
        @Nonnull Project project,
        @Nonnull PsiFile psiFile,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement
    ) {
        if (!(startElement instanceof IgnoreEntry)) {
            return;
        }
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        int start = startElement.getStartOffsetInParent();
        String text = startElement.getText();
        String fixed = getFixedPath(text);
        document.replaceString(start, start + text.length(), fixed);
    }

    /**
     * Removes relative parts from the given path.
     *
     * @param path element
     * @return fixed path
     */
    private String getFixedPath(String path) {
        path = path.replaceAll("\\/", "/").replaceAll("\\\\\\.", ".");
        try {
            path = new URI(path).normalize().getPath();
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return path.replaceAll("/\\.{1,2}/", "/").replaceAll("^\\.{0,2}/", "");
    }
}
