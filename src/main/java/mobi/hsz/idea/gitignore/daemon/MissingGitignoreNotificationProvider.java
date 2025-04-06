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

package mobi.hsz.idea.gitignore.daemon;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.fileEditor.*;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import mobi.hsz.idea.gitignore.command.CreateFileCommandAction;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.file.type.kind.GitFileType;
import mobi.hsz.idea.gitignore.lang.kind.GitLanguage;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.ui.GeneratorDialog;
import mobi.hsz.idea.gitignore.util.Properties;

import java.util.function.Supplier;

/**
 * Editor notification provider that checks if there is {@link GitLanguage#getFilename()}
 * in root directory and suggest to create one.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.3.3
 */
@ExtensionImpl
public class MissingGitignoreNotificationProvider implements EditorNotificationProvider {
    /** Current project. */
    @Nonnull
    private final Project project;

    /** Notifications component. */
    @Nonnull
    private final EditorNotifications notifications;

    /** Plugin settings holder. */
    @Nonnull
    private final IgnoreSettings settings;

    /**
     * Builds a new instance of {@link MissingGitignoreNotificationProvider}.
     *
     * @param project       current project
     * @param notifications notifications component
     */
    @Inject
    public MissingGitignoreNotificationProvider(@Nonnull Project project, @Nonnull EditorNotifications notifications) {
        this.project = project;
        this.notifications = notifications;
        this.settings = IgnoreSettings.getInstance();
    }

    @Nonnull
    @Override
    public String getId() {
        return ".ignore-missing-gitignore";
    }

    @RequiredReadAction
    @Nullable
    @Override
    public EditorNotificationBuilder buildNotification(
            @Nonnull VirtualFile virtualFile, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> factory)
    {
        // Break if feature is disabled in the Settings
        if (!settings.isMissingGitignore()) {
            return null;
        }
        // Break if user canceled previously this notification
        if (Properties.isIgnoreMissingGitignore(project)) {
            return null;
        }
        // Break if there is no Git directory in the project
        String vcsDirectory = GitLanguage.INSTANCE.getVcsDirectory();
        if (vcsDirectory == null) {
            return null;
        }

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        VirtualFile gitDirectory = baseDir.findChild(vcsDirectory);
        if (gitDirectory == null || !gitDirectory.isDirectory()) {
            return null;
        }
        // Break if there is Gitignore file already
        VirtualFile gitignoreFile = baseDir.findChild(GitLanguage.INSTANCE.getFilename());
        if (gitignoreFile != null) {
            return null;
        }

        return createPanel(project, factory.get());
    }

    /**
     * Creates notification panel.
     *
     * @param project current project
     * @param builder
     * @return notification panel
     */
    private EditorNotificationBuilder createPanel(@Nonnull final Project project, EditorNotificationBuilder builder) {
        final IgnoreFileType fileType = GitFileType.INSTANCE;
        builder.withText(IgnoreLocalize.daemonMissinggitignore());
        builder.withAction(IgnoreLocalize.daemonAddunversionedfilesCreate(), (e) -> {
            PsiDirectory directory = PsiManager.getInstance(project).findDirectory(project.getBaseDir());
            if (directory != null) {
                try {
                    PsiFile file = new CreateFileCommandAction(project, directory, fileType).execute();
                    FileEditorManager.getInstance(project).openFile(file.getVirtualFile(), true);
                    new GeneratorDialog(project, file).show();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        builder.withAction(IgnoreLocalize.daemonCancel(), (e) -> {
            Properties.setIgnoreMissingGitignore(project);
            notifications.updateAllNotifications();
        });

        builder.withIcon(fileType.getIcon());

        return builder;
    }
}
