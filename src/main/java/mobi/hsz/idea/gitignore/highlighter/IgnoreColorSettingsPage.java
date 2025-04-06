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

package mobi.hsz.idea.gitignore.highlighter;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.util.io.FileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

/**
 * {@link ColorSettingsPage} that allows to modify color scheme.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.1
 */
@ExtensionImpl
public class IgnoreColorSettingsPage implements ColorSettingsPage {
    /**
     * The path to the sample .gitignore file.
     */
    private static final String SAMPLE_GITIGNORE_PATH = "/sample.gitignore";

    /**
     * The sample .gitignore document shown in the colors settings dialog.
     *
     * @see #loadSampleGitignore()
     */
    @Nonnull
    private static final String SAMPLE_GITIGNORE = loadSampleGitignore();

    /**
     * Attributes descriptor list.
     */
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
        new AttributesDescriptor(IgnoreLocalize.highlighterHeader(), IgnoreHighlighterColors.HEADER),
        new AttributesDescriptor(IgnoreLocalize.highlighterSection(), IgnoreHighlighterColors.SECTION),
        new AttributesDescriptor(IgnoreLocalize.highlighterComment(), IgnoreHighlighterColors.COMMENT),
        new AttributesDescriptor(IgnoreLocalize.highlighterNegation(), IgnoreHighlighterColors.NEGATION),
        new AttributesDescriptor(IgnoreLocalize.highlighterBrackets(), IgnoreHighlighterColors.BRACKET),
        new AttributesDescriptor(IgnoreLocalize.highlighterSlash(), IgnoreHighlighterColors.SLASH),
        new AttributesDescriptor(IgnoreLocalize.highlighterSyntax(), IgnoreHighlighterColors.SYNTAX),
        new AttributesDescriptor(IgnoreLocalize.highlighterValue(), IgnoreHighlighterColors.VALUE),
        new AttributesDescriptor(IgnoreLocalize.highlighterUnused(), IgnoreHighlighterColors.UNUSED),
    };

    /**
     * Returns the syntax highlighter which is used to highlight the text shown in the preview
     * pane of the page.
     *
     * @return the syntax highlighter instance.
     */
    @Nonnull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new IgnoreHighlighter(null, null);
    }

    /**
     * Returns the text shown in the preview pane.
     *
     * @return demo text
     */
    @Nonnull
    @Override
    public String getDemoText() {
        return SAMPLE_GITIGNORE;
    }

    /**
     * Returns the mapping from special tag names surrounding the regions to be highlighted
     * in the preview text.
     *
     * @return <code>null</code>
     */
    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    /**
     * Returns the list of descriptors specifying the {@link TextAttributesKey} instances
     * for which colors are specified in the page. For such attribute keys, the user can choose
     * all highlighting attributes (font type, background color, foreground color, error stripe color and
     * effects).
     *
     * @return the list of attribute descriptors.
     */
    @Nonnull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    /**
     * Returns the list of descriptors specifying the {@link consulo.colorScheme.EditorColorKey}
     * instances for which colors are specified in the page. For such color keys, the user can
     * choose only the background or foreground color.
     *
     * @return the list of color descriptors.
     */
    @Nonnull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    /**
     * Returns the title of the page, shown as text in the dialog tab.
     *
     * @return the title of the custom page.
     */
    @Nonnull
    @Override
    public String getDisplayName() {
        return IgnoreLocalize.ignoreColorsettingsDisplayname().get();
    }

    /**
     * Loads sample .gitignore file
     *
     * @return the text loaded from {@link #SAMPLE_GITIGNORE_PATH}
     * @see #getDemoText()
     * @see #SAMPLE_GITIGNORE_PATH
     * @see #SAMPLE_GITIGNORE
     */
    @Nonnull
    private static String loadSampleGitignore() {
        try {
            return FileUtil.loadTextAndClose(IgnoreColorSettingsPage.class.getResourceAsStream(SAMPLE_GITIGNORE_PATH), true);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
