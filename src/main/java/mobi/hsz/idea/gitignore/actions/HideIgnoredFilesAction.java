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
package mobi.hsz.idea.gitignore.actions;

import consulo.annotation.component.ActionImpl;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.util.Icons;

/**
 * Action that hides or show ignored files in the project tree view.
 *
 * @author Maximiliano Najle <maximilianonajle@gmail.com>
 * @since 1.7
 */
@ActionImpl(id = "HideIgnoredFiles")
public class HideIgnoredFilesAction extends AnAction {
    /** {@link IgnoreSettings} instance. */
    public static final IgnoreSettings SETTINGS = IgnoreSettings.getInstance();

    /** Builds a new instance of {@link HideIgnoredFilesAction}. */
    public HideIgnoredFilesAction() {
        super(getText(), LocalizeValue.empty(), Icons.IGNORE);
    }

    /**
     * Returns proper action's presentation text depending on the {@link IgnoreSettings#hideIgnoredFiles} value.
     *
     * @return presentation text
     */
    @Nonnull
    private static LocalizeValue getText() {
        boolean hideIgnoredFiles = SETTINGS.isHideIgnoredFiles();
        return hideIgnoredFiles ? IgnoreLocalize.actionShowignoredvisibility() : IgnoreLocalize.actionHideignoredvisibility();
    }

    /**
     * Toggles {@link IgnoreSettings#hideIgnoredFiles} value.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        SETTINGS.setHideIgnoredFiles(!SETTINGS.isHideIgnoredFiles());

        getTemplatePresentation().setTextValue(getText());
    }
}
