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
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import mobi.hsz.idea.gitignore.IgnoreManager;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.util.Icons;
import mobi.hsz.idea.gitignore.util.Properties;

import java.util.function.Supplier;

/**
 * Editor notification provider that informs about the attempt of the ignored file modification.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.8
 */
@ExtensionImpl
public class IgnoredEditingNotificationProvider implements EditorNotificationProvider {
    /** Current project. */
    @Nonnull
    private final Project project;

    /** Notifications component. */
    @Nonnull
    private final EditorNotifications notifications;

    /** Plugin settings holder. */
    @Nonnull
    private final IgnoreSettings settings;

    /** {@link IgnoreManager} instance. */
    @Nonnull
    private final IgnoreManager manager;

    /**
     * Builds a new instance of {@link IgnoredEditingNotificationProvider}.
     *
     * @param project       current project
     * @param notifications notifications component
     */
    @Inject
    public IgnoredEditingNotificationProvider(@Nonnull Project project, @Nonnull EditorNotifications notifications) {
        this.project = project;
        this.notifications = notifications;
        this.settings = IgnoreSettings.getInstance();
        this.manager = IgnoreManager.getInstance(project);
    }

    @Nonnull
    @Override
    public String getId() {
        return ".ignore-ignored-editing";
    }

    @RequiredReadAction
    @Nullable
    @Override
    public EditorNotificationBuilder buildNotification(
        @Nonnull VirtualFile file,
        @Nonnull FileEditor fileEditor,
        @Nonnull Supplier<EditorNotificationBuilder> supplier
    ) {
        if (!settings.isNotifyIgnoredEditing() || !manager.isFileIgnored(file) ||
            Properties.isDismissedIgnoredEditingNotification(project, file)) {
            return null;
        }

        EditorNotificationBuilder builder = supplier.get();

        builder.withText(IgnoreLocalize.daemonIgnoredediting());
        builder.withAction(
            IgnoreLocalize.daemonOk(),
            (e) -> {
                Properties.setDismissedIgnoredEditingNotification(project, file);
                notifications.updateAllNotifications();
            }
        );

        builder.withIcon(Icons.IGNORE);
        return builder;
    }
}
