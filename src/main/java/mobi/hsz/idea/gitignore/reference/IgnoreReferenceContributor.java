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

package mobi.hsz.idea.gitignore.reference;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.psi.IgnoreEntry;
import mobi.hsz.idea.gitignore.psi.IgnoreFile;

import static consulo.language.pattern.PlatformPatterns.psiElement;
import static consulo.language.pattern.PlatformPatterns.psiFile;

/**
 * PSI elements references contributor.
 *
 * @author Alexander Zolotov <alexander.zolotov@jetbrains.com>
 * @since 0.5
 */
@ExtensionImpl
public class IgnoreReferenceContributor extends PsiReferenceContributor {
    /**
     * Registers new references provider for PSI element.
     *
     * @param psiReferenceRegistrar reference provider
     */
    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
            psiElement().inFile(psiFile(IgnoreFile.class)),
            new IgnoreReferenceProvider()
        );
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return IgnoreLanguage.INSTANCE;
    }

    /**
     * Reference provider definition.
     */
    private static class IgnoreReferenceProvider extends PsiReferenceProvider {
        /**
         * Returns references for given @{link PsiElement}.
         *
         * @param psiElement        current element
         * @param processingContext context
         * @return {@link PsiReference} list
         */
        @Nonnull
        @Override
        public PsiReference[] getReferencesByElement(
            @Nonnull PsiElement psiElement,
            @Nonnull ProcessingContext processingContext
        ) {
            return psiElement instanceof IgnoreEntry ignoreEntry
                ? new IgnoreReferenceSet(ignoreEntry).getAllReferences()
                : PsiReference.EMPTY_ARRAY;
        }
    }
}
