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

package mobi.hsz.idea.gitignore.codeInsight;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.ui.color.ColorValue;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ContainerUtil;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.highlighter.IgnoreHighlighterColors;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Severities provider that checks if entry points to any file or directory.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.5.4
 */
@ExtensionImpl
public class UnusedEntrySeveritiesProvider extends SeveritiesProvider {
    /** Unused entry {@link HighlightSeverity} instance. */
    @NotNull
    private static final HighlightSeverity UNUSED_ENTRY = new HighlightSeverity("UNUSED ENTRY", 10);

    /**
     * Defines the style of matched entry.
     *
     * @return style definition
     */
    @NotNull
    @Override
    public List<HighlightInfoType> getSeveritiesHighlightInfoTypes() {
        final List<HighlightInfoType> result = ContainerUtil.newArrayList();

        result.add(new HighlightInfoType.HighlightInfoTypeImpl(
                UNUSED_ENTRY,
                TextAttributesKey.createTextAttributesKey(
                        IgnoreBundle.message("codeInspection.unusedEntry"),
                        IgnoreHighlighterColors.UNUSED
                )
        ));
        return result;
    }

    /**
     * Defines color of the matched entry.
     *
     * @param textAttributes current attribute
     * @return entry color
     */
    @Override
    public ColorValue getTrafficRendererColor(@NotNull TextAttributes textAttributes) {
        return StandardColors.GRAY;
    }

    /**
     * Checks if severity goto is enabled.
     *
     * @param minSeverity severity to compare
     * @return severity equals to the {@link #UNUSED_ENTRY}
     */
    @Override
    public boolean isGotoBySeverityEnabled(HighlightSeverity minSeverity) {
        return UNUSED_ENTRY != minSeverity;
    }
}
