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

import consulo.application.dumb.DumbAware;
import consulo.dotignore.IgnoreNotificationGroup;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.ide.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.command.CreateFileCommandAction;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.ui.GeneratorDialog;
import mobi.hsz.idea.gitignore.util.Utils;

/**
 * Creates new file or returns existing one.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @author Alexander Zolotov <alexander.zolotov@jetbrains.com>
 * @since 0.1
 */
@SuppressWarnings("ComponentNotRegistered")
public class NewFileAction extends AnAction implements DumbAware {
    /**
     * Current file type.
     */
    private final IgnoreFileType fileType;

    /**
     * Builds a new instance of {@link NewFileAction}.
     */
    public NewFileAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon,
        IgnoreFileType fileType
    ) {
        super(text, description, icon);
        this.fileType = fileType;
    }

    /**
     * Creates new Gitignore file if it does not exist or uses an existing one and opens {@link GeneratorDialog}.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        IdeView view = e.getRequiredData(IdeView.KEY);

        VirtualFile fixedDirectory = fileType.getIgnoreLanguage().getFixedDirectory(project);
        PsiDirectory directory;

        if (fixedDirectory != null) {
            directory = PsiManager.getInstance(project).findDirectory(fixedDirectory);
        }
        else {
            directory = view.getOrChooseDirectory();
        }

        if (directory == null) {
            return;
        }

        GeneratorDialog dialog;
        String filename = fileType.getIgnoreLanguage().getFilename();
        PsiFile file = directory.findFile(filename);
        VirtualFile virtualFile = file == null ? directory.getVirtualFile().findChild(filename) : file.getVirtualFile();

        if (file == null && virtualFile == null) {
            CreateFileCommandAction action = new CreateFileCommandAction(project, directory, fileType);
            dialog = new GeneratorDialog(project, action);
        }
        else {
            project.getApplication().getInstance(NotificationService.class)
                .newInfo(IgnoreNotificationGroup.GROUP)
                .title(IgnoreLocalize.actionNewfileExists(fileType.getLanguageName()))
                .content(IgnoreLocalize.actionNewfileExistsIn(virtualFile.getPath()))
                .notify(project);

            if (file == null) {
                file = Utils.getPsiFile(project, virtualFile);
            }

            dialog = new GeneratorDialog(project, file);
        }

        dialog.show();
        file = dialog.getFile();

        if (file != null) {
            Utils.openFile(project, file);
        }
    }

    /**
     * Updates visibility of the action presentation in various actions list.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        IdeView view = e.getData(IdeView.KEY);

        PsiDirectory[] directory = view != null ? view.getDirectories() : null;
        if (directory == null || directory.length == 0 || !e.hasData(Project.KEY)
            || !this.fileType.getIgnoreLanguage().isNewAllowed()) {
            e.getPresentation().setVisible(false);
        }
    }
}
