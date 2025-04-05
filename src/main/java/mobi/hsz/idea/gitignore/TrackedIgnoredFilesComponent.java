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

package mobi.hsz.idea.gitignore;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.ui.untrackFiles.UntrackFilesDialog;
import mobi.hsz.idea.gitignore.util.Notify;

import java.util.concurrent.ConcurrentMap;

/**
 * ProjectComponent instance to handle {@link TrackedIgnoredListener} event
 * and display {@link Notification} about tracked and ignored files which invokes {@link UntrackFilesDialog}.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.7
 */
@TopicImpl(ComponentScope.PROJECT)
public class TrackedIgnoredFilesComponent implements TrackedIgnoredListener {
    /** Disable action event. */
    private static final String DISABLE_ACTION = "#disable";

    /** {@link IgnoreSettings} instance. */
    private final IgnoreSettings settings;

    /** Notification about tracked files was shown for current project. */
    private boolean notificationShown;

    private final Project myProject;

    /**
     * Constructor.
     *
     * @param project current project
     */
    @Inject
    public TrackedIgnoredFilesComponent(@Nonnull Project project, IgnoreSettings settings) {
        myProject = project;
        this.settings = settings;
    }

    /**
     * {@link TrackedIgnoredListener} method implementation to handle incoming files.
     *
     * @param files tracked and ignored files list
     */
    @Override
    public void handleFiles(@Nonnull ConcurrentMap<VirtualFile, VcsRoot> files) {
        if (!settings.isInformTrackedIgnored() || notificationShown || myProject.getBaseDir() == null) {
            return;
        }

        notificationShown = true;
        Notify.show(
            myProject,
            IgnoreLocalize.notificationUntrackTitle().get(),
            IgnoreLocalize.notificationUntrackContent().get(),
            NotificationType.INFORMATION,
            (notification, event) -> {
                if (DISABLE_ACTION.equals(event.getDescription())) {
                    settings.setInformTrackedIgnored(false);
                } else if (!myProject.isDisposed()) {
                    new UntrackFilesDialog(myProject, files).show();
                }
                notification.expire();
            }
        );
    }
}
