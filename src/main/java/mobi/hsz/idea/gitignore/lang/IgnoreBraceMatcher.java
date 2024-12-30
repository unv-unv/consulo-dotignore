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

package mobi.hsz.idea.gitignore.lang;

import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import mobi.hsz.idea.gitignore.psi.IgnoreTypes;
import org.jetbrains.annotations.NotNull;

import jakarta.annotation.Nonnull;

/**
 * Definition of {@link PairedBraceMatcher} class.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.5
 */
public class IgnoreBraceMatcher implements PairedBraceMatcher {
    /** Array of definitions for brace pairs. */
    private static final BracePair[] PAIRS = new BracePair[]{
            new BracePair(IgnoreTypes.BRACKET_LEFT, IgnoreTypes.BRACKET_RIGHT, false),
    };

    private final Language language;

    public IgnoreBraceMatcher(Language language) {
        this.language = language;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return language;
    }

    /**
     * Returns the array of definitions for brace pairs that need to be matched when
     * editing code in the language.
     *
     * @return the array of brace pair definitions.
     */
    @NotNull
    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }
}
