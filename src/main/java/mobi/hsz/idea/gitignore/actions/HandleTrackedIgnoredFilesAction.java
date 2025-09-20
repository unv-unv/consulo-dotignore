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
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.Maps;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.IgnoreManager;
import mobi.hsz.idea.gitignore.ui.untrackFiles.UntrackFilesDialog;
import mobi.hsz.idea.gitignore.util.Icons;

import java.util.concurrent.ConcurrentMap;

/**
 * Action that invokes {@link UntrackFilesDialog} dialog.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.7.2
 */
@ActionImpl(id = "HandleTrackedIgnoredFiles")
public class HandleTrackedIgnoredFilesAction extends AnAction {
    /**
     * Builds a new instance of {@link HandleTrackedIgnoredFilesAction}.
     */
    public HandleTrackedIgnoredFilesAction() {
        super(
            IgnoreLocalize.actionHandletrackedignoredfiles(),
            IgnoreLocalize.actionHandletrackedignoredfilesDescription(),
            Icons.IGNORE
        );
    }

    /**
     * Toggles {@link mobi.hsz.idea.gitignore.settings.IgnoreSettings#hideIgnoredFiles} value.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        if (project == null) {
            return;
        }

        new UntrackFilesDialog(project, getTrackedIgnoredFiles(e)).show();
    }

    /**
     * Shows action in the context menu.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setVisible(!getTrackedIgnoredFiles(e).isEmpty());
    }

    /**
     * Helper method to return tracked and ignored files map.
     *
     * @param event current event
     * @return map of files
     */
    private ConcurrentMap<VirtualFile, VcsRoot> getTrackedIgnoredFiles(@Nonnull AnActionEvent event) {
        Project project = event.getData(Project.KEY);

        if (project != null) {
            return IgnoreManager.getInstance(project).getConfirmedIgnoredFiles();
        }

        return Maps.newConcurrentWeakHashMap();
    }
}
