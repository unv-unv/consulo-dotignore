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

package mobi.hsz.idea.gitignore.psi;

import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.ast.IElementType;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;

import java.util.Map;

/**
 * Token type definition.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.1
 */
public class IgnoreTokenType extends IElementType {
    Map<String, LocalizeValue> LOCALIZED_DEBUG_NAMES = Map.of(
        "/", IgnoreLocalize.tokenTypeSlash(),
        "BRACKET_LEFT", IgnoreLocalize.tokenTypeBracketLeft(),
        "BRACKET_RIGHT", IgnoreLocalize.tokenTypeBracketRight(),
        "COMMENT", IgnoreLocalize.tokenTypeComment(),
        "CRLF", IgnoreLocalize.tokenTypeEmptyLine(),
        "HEADER", IgnoreLocalize.tokenTypeHeader(),
        "SECTION", IgnoreLocalize.tokenTypeSection(),
        "VALUE", IgnoreLocalize.tokenTypeValue()
    );

    /** Token debug name. */
    private final LocalizeValue myLocalizedDebugName;

    /** Builds a new instance of @{link IgnoreTokenType}. */
    public IgnoreTokenType(@Nonnull String debugName) {
        super(debugName, IgnoreLanguage.INSTANCE);
        myLocalizedDebugName = LOCALIZED_DEBUG_NAMES.get(debugName);
    }

    /**
     * String interpretation of the token type.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return myLocalizedDebugName != null ? myLocalizedDebugName.get(): "IgnoreTokenType." + super.toString();
    }
}
