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
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.command.AppendFileCommandAction;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.file.type.kind.GitFileType;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.lang.kind.GitLanguage;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.util.Constants;
import mobi.hsz.idea.gitignore.util.Properties;
import mobi.hsz.idea.gitignore.util.exec.ExternalExec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Editor notification provider that suggests to add unversioned files to the .gitignore file.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.4
 */
@ExtensionImpl
public class AddUnversionedFilesNotificationProvider implements EditorNotificationProvider {
    /** Current project. */
    @NotNull
    private final Project project;

    /** Notifications component. */
    @NotNull
    private final EditorNotifications notifications;

    /** Plugin settings holder. */
    @NotNull
    private final IgnoreSettings settings;

    /** List of unignored files. */
    @NotNull
    private final List<String> unignoredFiles = new ArrayList<>();

    /** Map to obtain if file was handled. */
    private final Map<VirtualFile, Boolean> handledMap = ContainerUtil.createConcurrentWeakKeyWeakValueMap();

    /**
     * Builds a new instance of {@link AddUnversionedFilesNotificationProvider}.
     *
     * @param project       current project
     * @param notifications notifications component
     */
    @Inject
    public AddUnversionedFilesNotificationProvider(
            @NotNull Project project,
            @NotNull EditorNotifications notifications)
    {
        this.project = project;
        this.notifications = notifications;
        this.settings = IgnoreSettings.getInstance();
    }

    @Nonnull
    @Override
    public String getId() {
        return ".ignore-add-unversion-files";
    }

    @RequiredReadAction
    @Nullable
    @Override
    public EditorNotificationBuilder buildNotification(
            @Nonnull VirtualFile file, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> supplier)
    {
        // Break if feature is disabled in the Settings
        if (!settings.isAddUnversionedFiles()) {
            return null;
        }
        // Break if user canceled previously this notification
        if (Properties.isAddUnversionedFiles(project)) {
            return null;
        }

        if (handledMap.get(file) != null) {
            return null;
        }

        final IgnoreLanguage language = IgnoreBundle.obtainLanguage(file);
        if (language == null || !language.isVCS() || !(language instanceof GitLanguage)) {
            return null;
        }

        unignoredFiles.clear();
        unignoredFiles.addAll(ExternalExec.getUnignoredFiles(GitLanguage.INSTANCE, project, file));
        if (unignoredFiles.isEmpty()) {
            return null;
        }

        return createPanel(project, supplier.get());
    }

    /**
     * Creates notification panel.
     *
     * @param project current project
     * @return notification panel
     */
    private EditorNotificationBuilder createPanel(@NotNull final Project project, EditorNotificationBuilder builder) {
        final IgnoreFileType fileType = GitFileType.INSTANCE;
        builder.withText(IgnoreLocalize.daemonAddunversionedfiles());
        builder.withAction(IgnoreLocalize.daemonAddunversionedfilesCreate(), (e) -> {
            final VirtualFile virtualFile = project.getBaseDir().findChild(GitLanguage.INSTANCE.getFilename());
            final PsiFile file = virtualFile != null ? PsiManager.getInstance(project).findFile(virtualFile) : null;
            if (file != null) {
                final String content = StringUtil.join(unignoredFiles, Constants.NEWLINE);

                try {
                    new AppendFileCommandAction(project, file, content, true, false)
                            .execute();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                handledMap.put(virtualFile, true);
                notifications.updateAllNotifications();
            }
        });
        builder.withAction(IgnoreLocalize.daemonCancel(), (e) -> {
            Properties.setAddUnversionedFiles(project);
            notifications.updateAllNotifications();
        });

        builder.withIcon(fileType.getIcon());

        return builder;
    }
}
