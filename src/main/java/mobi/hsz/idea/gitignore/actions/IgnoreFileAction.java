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

import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.command.AppendFileCommandAction;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.util.Notify;
import mobi.hsz.idea.gitignore.util.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Action that adds currently selected {@link VirtualFile} to the specified Ignore {@link VirtualFile}.
 * Action is added to the IDE context menus not directly but with {@link IgnoreFileGroupAction} action.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.4
 */
@SuppressWarnings("ComponentNotRegistered")
public class IgnoreFileAction extends DumbAwareAction {
    /**
     * Ignore {@link VirtualFile} that will be used for current action.
     */
    private final VirtualFile ignoreFile;

    /**
     * Current ignore file type.
     */
    private final IgnoreFileType fileType;

    /**
     * Builds a new instance of {@link IgnoreFileAction}.
     * Default project's Ignore file will be used.
     */
    public IgnoreFileAction() {
        this(null);
    }

    /**
     * Builds a new instance of {@link IgnoreFileAction}.
     * Describes action's presentation.
     *
     * @param virtualFile Gitignore file
     */
    public IgnoreFileAction(@Nullable VirtualFile virtualFile) {
        this(Utils.getFileType(virtualFile), virtualFile);
    }

    /**
     * Builds a new instance of {@link IgnoreFileAction}.
     * Describes action's presentation.
     *
     * @param fileType    Current file type
     * @param virtualFile Gitignore file
     */
    public IgnoreFileAction(@Nullable IgnoreFileType fileType, @Nullable VirtualFile virtualFile) {
        this(fileType, virtualFile, IgnoreLocalize::actionAddtoignore, IgnoreLocalize::actionAddtoignoreDescription);
    }

    /**
     * Builds a new instance of {@link IgnoreFileAction}.
     * Describes action's presentation.
     *
     * @param fileType       Current file type
     * @param virtualFile    Gitignore file
     * @param textKey        Action presentation's text key
     * @param descriptionKey Action presentation's description key
     */
    public IgnoreFileAction(
        @Nullable IgnoreFileType fileType,
        @Nullable VirtualFile virtualFile,
        @Nonnull Function<String, LocalizeValue> textKey,
        @Nonnull Function<String, LocalizeValue> descriptionKey
    ) {
        super(
            textKey.apply(fileType != null ? fileType.getIgnoreLanguage().getFilename() : null),
            descriptionKey.apply(fileType != null ? fileType.getIgnoreLanguage().getFilename() : null),
            fileType != null ? fileType.getIcon() : null
        );
        this.ignoreFile = virtualFile;
        this.fileType = fileType;
    }

    /**
     * Adds currently selected {@link VirtualFile} to the {@link #ignoreFile}.
     * If {@link #ignoreFile} is null, default project's Gitignore file will be used.
     * Files that cannot be covered with Gitignore file produces error notification.
     * When action is performed, Gitignore file is opened with additional content added
     * using {@link AppendFileCommandAction}.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VirtualFile[] files = e.getRequiredData(VirtualFile.KEY_OF_ARRAY);
        Project project = e.getRequiredData(Project.KEY);

        PsiFile ignore = null;
        if (ignoreFile != null) {
            ignore = Utils.getPsiFile(project, ignoreFile);
        }
        if (ignore == null && fileType != null) {
            ignore = Utils.getIgnoreFile(project, fileType, null, true);
        }

        if (ignore != null) {
            Set<String> paths = new HashSet<>();
            for (VirtualFile file : files) {
                String path = getPath(ignore.getVirtualFile().getParent(), file);
                if (path.isEmpty()) {
                    VirtualFile baseDir = project.getBaseDir();
                    if (baseDir != null) {
                        Notify.show(
                            project,
                            IgnoreLocalize.actionIgnorefileAdderror(Utils.getRelativePath(baseDir, file)).get(),
                            IgnoreLocalize.actionIgnorefileAdderrorTo(Utils.getRelativePath(baseDir, ignore.getVirtualFile())).get(),
                            NotificationType.ERROR
                        );
                    }
                }
                else {
                    paths.add(path);
                }
            }
            Utils.openFile(project, ignore);
            try {
                new AppendFileCommandAction(project, ignore, paths, false, false).execute();
            }
            catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    /**
     * Shows action in the context menu if current file is covered by the specified {@link #ignoreFile}.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        Project project = e.getData(Project.KEY);

        if (project == null || files == null || (files.length == 1 && files[0].equals(project.getBaseDir()))) {
            e.getPresentation().setVisible(false);
        }
    }

    /**
     * Gets the file's path relative to the specified root directory.
     *
     * @param root root directory
     * @param file file used for generating output path
     * @return relative path
     */
    @Nonnull
    protected String getPath(@Nonnull VirtualFile root, @Nonnull VirtualFile file) {
        String path = StringUtil.notNullize(Utils.getRelativePath(root, file));
        path = StringUtil.escapeChar(path, '[');
        path = StringUtil.escapeChar(path, ']');
        path = StringUtil.trimLeading(path, '/');
        return path.isEmpty() ? path : '/' + path;
    }
}
