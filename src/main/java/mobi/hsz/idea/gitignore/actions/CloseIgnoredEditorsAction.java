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
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.fileEditor.FileEditorComposite;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.action.CloseEditorsActionBase;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.vcs.IgnoreFileStatusProvider;

/**
 * Action that closes all opened files that are marked as {@link IgnoreFileStatusProvider#IGNORED}.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.2
 */
@ActionImpl(
    id = "CloseIgnoredEditors",
    parents = @ActionParentRef(
        value = @ActionRef(id = "CloseEditorsGroup"),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "CloseAllUnpinnedEditors")
    )
)
public class CloseIgnoredEditorsAction extends CloseEditorsActionBase {
    public CloseIgnoredEditorsAction() {
        super(IgnoreLocalize.actionCloseIgnoredEditorsText(), IgnoreLocalize.actionCloseIgnoredEditorsDescription());
    }

    /**
     * Obtains if editor is allowed to be closed.
     *
     * @return editor is allowed to be closed
     */
    @Override
    protected boolean isFileToClose(FileEditorComposite editor, FileEditorWindow window) {
        FileStatusManager fileStatusManager = FileStatusManager.getInstance(window.getManager().getProject());
        return fileStatusManager.getStatus(editor.getFile()).equals(IgnoreFileStatusProvider.IGNORED);
    }

    /**
     * Checks if action is available in UI.
     *
     * @return action is enabled
     */
    @Override
    protected boolean isActionEnabled(Project project, AnActionEvent event) {
        return super.isActionEnabled(project, event)
            && ProjectLevelVcsManager.getInstance(project).getAllActiveVcss().length > 0;
    }

    /**
     * Returns presentation text.
     *
     * @param inSplitter is in splitter
     * @return label text
     */
    @Nonnull
    @Override
    protected LocalizeValue getPresentationText(boolean inSplitter) {
        return inSplitter ?
            IgnoreLocalize.actionCloseIgnoredEditorsInTabGroupText() :
            IgnoreLocalize.actionCloseIgnoredEditorsText();
    }
}
