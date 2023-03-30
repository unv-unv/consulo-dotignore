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
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.language.file.FileTypeManager;
import consulo.language.impl.util.NoAccessDuringPsiEvents;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.event.ModuleListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.status.FileStatusManager;
import git4idea.GitVcs;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.file.type.kind.GitExcludeFileType;
import mobi.hsz.idea.gitignore.file.type.kind.GitFileType;
import mobi.hsz.idea.gitignore.indexing.ExternalIndexableSetContributor;
import mobi.hsz.idea.gitignore.indexing.IgnoreEntryOccurrence;
import mobi.hsz.idea.gitignore.indexing.IgnoreFilesIndex;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.util.*;
import mobi.hsz.idea.gitignore.util.exec.ExternalExec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static mobi.hsz.idea.gitignore.RefreshTrackedIgnoredListener.TRACKED_IGNORED_REFRESH;
import static mobi.hsz.idea.gitignore.settings.IgnoreSettings.KEY;

/**
 * {@link IgnoreManager} handles ignore files indexing and status caching.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.0
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class IgnoreManager implements Disposable {
    /** List of all available {@link IgnoreFileType}. */
    private static final List<IgnoreFileType> FILE_TYPES =
            ContainerUtil.map(IgnoreBundle.LANGUAGES, IgnoreLanguage::getFileType);

    /** List of filenames that require to be associated with specific {@link IgnoreFileType}. */
    public static final Map<String, IgnoreFileType> FILE_TYPES_ASSOCIATION_QUEUE = ContainerUtil.newConcurrentMap();

    private final Project myProject;

    /** {@link MatcherUtil} instance. */
    @NotNull
    private final MatcherUtil matcher;

    /** {@link VirtualFileManager} instance. */
    @NotNull
    private final VirtualFileManager virtualFileManager;

    /** {@link IgnoreSettings} instance. */
    @NotNull
    private final IgnoreSettings settings;

    /** {@link FileStatusManager} instance. */
    @NotNull
    private final FileStatusManager statusManager;

    /** {@link ProjectLevelVcsManager} instance. */
    @NotNull
    private final ProjectLevelVcsManager projectLevelVcsManager;

    /** {@link RefreshTrackedIgnoredRunnable} instance. */
    @NotNull
    private final RefreshTrackedIgnoredRunnable refreshTrackedIgnoredRunnable;

    /** Common runnable listeners. */
    @NotNull
    private final CommonRunnableListeners commonRunnableListeners;

    /** {@link MessageBusConnection} instance. */
    @Nullable
    private MessageBusConnection messageBus;

    /** List of the files that are ignored and also tracked by Git. */
    @NotNull
    private final ConcurrentMap<VirtualFile, VcsRoot> confirmedIgnoredFiles = ContainerUtil.createConcurrentWeakMap();

    /** List of the new files that were not covered by {@link #confirmedIgnoredFiles} yet. */
    @NotNull
    private final HashSet<VirtualFile> notConfirmedIgnoredFiles = new HashSet<>();

    /** References to the indexed {@link IgnoreEntryOccurrence}. */
    @NotNull
    private final CachedConcurrentMap<IgnoreFileType, Collection<IgnoreEntryOccurrence>> cachedIgnoreFilesIndex =
            CachedConcurrentMap.create(key -> IgnoreFilesIndex.getEntries(getProject(), key));

    /** References to the indexed outer files. */
    @NotNull
    private final CachedConcurrentMap<IgnoreFileType, Collection<VirtualFile>> cachedOuterFiles =
            CachedConcurrentMap.create(key -> key.getIgnoreLanguage().getOuterFiles(getProject()));

    @NotNull
    private final ExpiringMap<VirtualFile, Boolean> expiringStatusCache = new ExpiringMap<>(1000);

    /** {@link FileStatusManager#fileStatusesChanged()} method wrapped with {@link Debounced}. */
    private final Debounced debouncedStatusesChanged = new Debounced(1000) {
        @Override
        protected void task(@Nullable Object argument) {
            expiringStatusCache.clear();
            statusManager.fileStatusesChanged();
        }
    };

    /** {@link FileStatusManager#fileStatusesChanged()} method wrapped with {@link Debounced}. */
    private final Debounced<Boolean> debouncedRefreshTrackedIgnores = new Debounced<Boolean>(1000) {
        @Override
        protected void task(@Nullable Boolean refresh) {
            if (Boolean.TRUE.equals(refresh)) {
                refreshTrackedIgnoredRunnable.refresh();
            } else {
                refreshTrackedIgnoredRunnable.run();
            }
        }
    };

    /** {@link DumbModeListener#exitDumbMode()} method body wrapped with {@link Debounced}. */
    private final Debounced<Boolean> debouncedExitDumbMode = new Debounced<Boolean>(3000) {
        @Override
        protected void task(@Nullable Boolean refresh) {
            cachedIgnoreFilesIndex.clear();
            for (Map.Entry<String, IgnoreFileType> entry : FILE_TYPES_ASSOCIATION_QUEUE.entrySet()) {
                associateFileType(entry.getKey(), entry.getValue());
            }
            debouncedStatusesChanged.run();
        }
    };

    /** Scheduled feature connected with {@link #debouncedRefreshTrackedIgnores}. */
    @NotNull
    private final InterruptibleScheduledFuture refreshTrackedIgnoredFeature;

    /** {@link IgnoreManager} working flag. */
    private boolean working;

    /** List of available VCS roots for the current project. */
    @NotNull
    private final List<VcsRoot> vcsRoots = ContainerUtil.newArrayList();

    /** {@link VirtualFileListener} instance to check if file's content was changed. */
    @NotNull
    private final VirtualFileListener virtualFileListener = new VirtualFileListener() {
        @Override
        public void contentsChanged(@NotNull VirtualFileEvent event) {
            handleEvent(event);
        }

        @Override
        public void fileCreated(@NotNull VirtualFileEvent event) {
            handleEvent(event);
            notConfirmedIgnoredFiles.add(event.getFile());
            debouncedRefreshTrackedIgnores.run(true);
        }

        @Override
        public void fileDeleted(@NotNull VirtualFileEvent event) {
            handleEvent(event);
            notConfirmedIgnoredFiles.add(event.getFile());
            debouncedRefreshTrackedIgnores.run(true);
        }

        @Override
        public void fileMoved(@NotNull VirtualFileMoveEvent event) {
            handleEvent(event);
            notConfirmedIgnoredFiles.add(event.getFile());
            debouncedRefreshTrackedIgnores.run(true);
        }

        @Override
        public void fileCopied(@NotNull VirtualFileCopyEvent event) {
            handleEvent(event);
            notConfirmedIgnoredFiles.add(event.getFile());
            debouncedRefreshTrackedIgnores.run(true);
        }

        private void handleEvent(@NotNull VirtualFileEvent event) {
            final FileType fileType = event.getFile().getFileType();
            if (fileType instanceof IgnoreFileType) {
                cachedIgnoreFilesIndex.remove((IgnoreFileType) fileType);
                cachedOuterFiles.remove((IgnoreFileType) fileType);

                if (fileType instanceof GitExcludeFileType) {
                    cachedOuterFiles.remove(GitFileType.INSTANCE);
                }
                expiringStatusCache.clear();
                debouncedStatusesChanged.run();
                debouncedRefreshTrackedIgnores.run();
            }
        }
    };

    /** {@link IgnoreSettings} listener to watch changes in the plugin's settings. */
    @NotNull
    private final IgnoreSettings.Listener settingsListener = new IgnoreSettings.Listener() {
        @Override
        public void onChange(@NotNull KEY key, Object value) {
            switch (key) {

                case IGNORED_FILE_STATUS:
                    toggle((Boolean) value);
                    break;

                case OUTER_IGNORE_RULES:
                case LANGUAGES:
                    IgnoreBundle.ENABLED_LANGUAGES.clear();
                    if (isEnabled()) {
                        if (working) {
                            debouncedStatusesChanged.run();
                            debouncedRefreshTrackedIgnores.run();
                        } else {
                            enable();
                        }
                    }
                    break;

                case HIDE_IGNORED_FILES:
                    ProjectView.getInstance(myProject).refresh();
                    break;

            }
        }
    };

    /**
     * Returns {@link IgnoreManager} service instance.
     *
     * @param project current project
     * @return {@link IgnoreManager instance}
     */
    @NotNull
    public static IgnoreManager getInstance(@NotNull final Project project) {
        return project.getComponent(IgnoreManager.class);
    }

    /**
     * Constructor builds {@link IgnoreManager} instance.
     *
     * @param project current project
     */
    @Inject
    public IgnoreManager(@NotNull final Project project, VirtualFileManager virtualFileManager) {
        myProject = project;
        this.matcher = new MatcherUtil();
        this.virtualFileManager = virtualFileManager;
        this.settings = IgnoreSettings.getInstance();
        this.statusManager = FileStatusManager.getInstance(project);
        this.refreshTrackedIgnoredRunnable = new RefreshTrackedIgnoredRunnable();
        this.refreshTrackedIgnoredFeature =
                new InterruptibleScheduledFuture(debouncedRefreshTrackedIgnores, 10000, 5);
        this.refreshTrackedIgnoredFeature.setTrailing(true);
        this.projectLevelVcsManager = ProjectLevelVcsManager.getInstance(project);
        this.commonRunnableListeners = new CommonRunnableListeners(debouncedStatusesChanged);
    }

    public Project getProject() {
        return myProject;
    }

    /**
     * Returns {@link MatcherUtil} instance which is required for sharing matcher cache.
     *
     * @return {@link MatcherUtil} instance
     */
    @NotNull
    public MatcherUtil getMatcher() {
        return matcher;
    }

    /**
     * Checks if file is ignored.
     *
     * @param file current file
     * @return file is ignored
     */
    public boolean isFileIgnored(@NotNull final VirtualFile file) {
        final Boolean cached = expiringStatusCache.get(file);
        final VirtualFile baseDir = myProject.getBaseDir();
        if (cached != null) {
            return cached;
        }
        if (ApplicationManager.getApplication().isDisposed() || myProject.isDisposed() ||
                DumbService.isDumb(myProject) || !isEnabled() || baseDir == null || !Utils.isUnder(file, baseDir) ||
                NoAccessDuringPsiEvents.isInsideEventProcessing()) {
            return false;
        }

        boolean ignored = false;
        boolean matched = false;
        int valuesCount = 0;

        for (IgnoreFileType fileType : FILE_TYPES) {
            ProgressManager.checkCanceled();
            if (!IgnoreBundle.ENABLED_LANGUAGES.get(fileType)) {
                continue;
            }

            final Collection<IgnoreEntryOccurrence> values = cachedIgnoreFilesIndex.get(fileType);

            valuesCount += values.size();
            for (IgnoreEntryOccurrence value : values) {
                ProgressManager.checkCanceled();
                String relativePath;
                final VirtualFile entryFile = value.getFile();
                if (entryFile == null) {
                    continue;
                } else if (fileType instanceof GitExcludeFileType) {
                    VirtualFile workingDirectory = GitExcludeFileType.getWorkingDirectory(myProject, entryFile);
                    if (workingDirectory == null || !Utils.isUnder(file, workingDirectory)) {
                        continue;
                    }
                    relativePath = StringUtil.trimStart(file.getPath(), workingDirectory.getPath());
                } else {
                    final VirtualFile vcsRoot = getVcsRootFor(file);
                    if (vcsRoot != null && !Utils.isUnder(entryFile, vcsRoot)) {
                        if (!cachedOuterFiles.get(fileType).contains(entryFile)) {
                            continue;
                        }
                    }

                    final String parentPath = !Utils.isInProject(entryFile, myProject) &&
                            myProject.getBasePath() != null ? myProject.getBasePath() : entryFile.getParent().getPath();
                    if (!StringUtil.startsWith(file.getPath(), parentPath) &&
                            !ExternalIndexableSetContributor.getAdditionalFiles(myProject).contains(entryFile)) {
                        continue;
                    }
                    relativePath = StringUtil.trimStart(file.getPath(), parentPath);
                }

                relativePath = StringUtil.trimEnd(StringUtil.trimStart(relativePath, "/"), "/");
                if (StringUtil.isEmpty(relativePath)) {
                    continue;
                }

                if (file.isDirectory()) {
                    relativePath += "/";
                }

                for (Pair<String, Boolean> item : value.getItems()) {
                    final Pattern pattern = Glob.getPattern(item.first);
                    if (matcher.match(pattern, relativePath)) {
                        ignored = !item.second;
                        matched = true;
                    }
                }
            }
        }

        if (valuesCount > 0 && !ignored && !matched) {
            final VirtualFile directory = file.getParent();
            if (directory != null && !directory.equals(baseDir)) {
                for (VcsRoot vcsRoot : vcsRoots) {
                    ProgressManager.checkCanceled();
                    if (directory.equals(vcsRoot.getPath())) {
                        return expiringStatusCache.set(file, false);
                    }
                }
                return expiringStatusCache.set(file, isFileIgnored(directory));
            }
        }

        if (ignored) {
            refreshTrackedIgnoredFeature.cancel();
        }

        return expiringStatusCache.set(file, ignored);
    }

    /**
     * Finds {@link VirtualFile} directory of {@link VcsRoot} that contains passed file.
     *
     * @param file to check
     * @return VCS Root for given file
     */
    @Nullable
    private VirtualFile getVcsRootFor(@NotNull final VirtualFile file) {
        final VcsRoot vcsRoot = ContainerUtil.find(
                ContainerUtil.reverse(vcsRoots),
                root -> root.getPath() != null && Utils.isUnder(file, root.getPath())
        );
        return vcsRoot != null ? vcsRoot.getPath() : null;
    }

    /**
     * Associates given file with proper {@link IgnoreFileType}.
     *
     * @param fileName to associate
     * @param fileType file type to bind with pattern
     */
    public static void associateFileType(@NotNull final String fileName, @NotNull final IgnoreFileType fileType) {
        final Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
            application.invokeLater(() -> application.runWriteAction(() -> {
                fileTypeManager.associate(fileType, FileNameMatcherFactory.getInstance().createExactFileNameMatcher(fileName));
                FILE_TYPES_ASSOCIATION_QUEUE.remove(fileName);
            }), application.getNoneModalityState());
        } else if (!FILE_TYPES_ASSOCIATION_QUEUE.containsKey(fileName)) {
            FILE_TYPES_ASSOCIATION_QUEUE.put(fileName, fileType);
        }
    }

    /**
     * Checks if file is ignored and tracked.
     *
     * @param file current file
     * @return file is ignored and tracked
     */
    public boolean isFileTracked(@NotNull final VirtualFile file) {
        return settings.isInformTrackedIgnored() && !notConfirmedIgnoredFiles.contains(file) &&
                !confirmedIgnoredFiles.isEmpty() && confirmedIgnoredFiles.containsKey(file);
    }

    /**
     * Invoked when the project corresponding to this component instance is opened.<p> Note that components may be
     * created for even unopened projects and this method can be never invoked for a particular component instance (for
     * example for default project).
     */
    public void projectOpened() {
        ExternalIndexableSetContributor.invalidateDisposedProjects();
        if (isEnabled() && !working) {
            enable();
        }
    }

    /**
     * Checks if ignored files watching is enabled.
     *
     * @return enabled
     */
    private boolean isEnabled() {
        return settings.isIgnoredFileStatus();
    }

    /** Enable manager. */
    private void enable() {
        if (working) {
            return;
        }

        refreshTrackedIgnoredFeature.run();
        virtualFileManager.addVirtualFileListener(virtualFileListener);
        settings.addListener(settingsListener);

        messageBus = myProject.getMessageBus().connect();

        messageBus.subscribe(TRACKED_IGNORED_REFRESH, () -> debouncedRefreshTrackedIgnores.run(true));

        messageBus.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, () -> {
            ExternalIndexableSetContributor.invalidateCache(myProject);
            vcsRoots.clear();
            vcsRoots.addAll(ContainerUtil.newArrayList(projectLevelVcsManager.getAllVcsRoots()));
        });

        messageBus.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void enteredDumbMode() {
            }

            @Override
            public void exitDumbMode() {
                debouncedExitDumbMode.run();
            }
        });

        messageBus.subscribe(ModuleRootListener.class, commonRunnableListeners);
        messageBus.subscribe(RefreshStatusesListener.class, commonRunnableListeners);
        messageBus.subscribe(ModuleListener.class, commonRunnableListeners);

        working = true;
    }

    /** Disable manager. */
    private void disable() {
        ExternalIndexableSetContributor.invalidateCache(myProject);
        virtualFileManager.removeVirtualFileListener(virtualFileListener);
        settings.removeListener(settingsListener);

        if (messageBus != null) {
            messageBus.disconnect();
            messageBus = null;
        }

        working = false;
    }

    /** Dispose and disable component. */
    @Override
    public void dispose() {
        ExternalIndexableSetContributor.invalidateDisposedProjects();
        disable();
    }

    /**
     * Runs {@link #enable()} or {@link #disable()} depending on the passed value.
     *
     * @param enable or disable
     */
    private void toggle(@NotNull Boolean enable) {
        if (enable) {
            enable();
        } else {
            disable();
        }
    }

    /**
     * Returns tracked and ignored files stored in {@link #confirmedIgnoredFiles}.
     *
     * @return tracked and ignored files map
     */
    @NotNull
    public ConcurrentMap<VirtualFile, VcsRoot> getConfirmedIgnoredFiles() {
        return confirmedIgnoredFiles;
    }

    /** {@link Runnable} implementation to rebuild {@link #confirmedIgnoredFiles}. */
    class RefreshTrackedIgnoredRunnable implements Runnable, RefreshTrackedIgnoredListener {
        /** Default {@link Runnable} run method that invokes rebuilding with bus event propagating. */
        @Override
        public void run() {
            run(false);
        }

        /** Rebuilds {@link #confirmedIgnoredFiles} map in silent mode. */
        @Override
        public void refresh() {
            this.run(true);
        }

        /**
         * Rebuilds {@link #confirmedIgnoredFiles} map.
         *
         * @param silent propagate {@link TrackedIgnoredListener#TRACKED_IGNORED} event
         */
        public void run(boolean silent) {
            if (!settings.isInformTrackedIgnored()) {
                return;
            }

            final ConcurrentMap<VirtualFile, VcsRoot> result = ContainerUtil.newConcurrentMap();
            for (VcsRoot vcsRoot : vcsRoots) {
                if (!(vcsRoot.getVcs() instanceof GitVcs) || vcsRoot.getPath() == null) {
                    continue;
                }
                final VirtualFile root = vcsRoot.getPath();
                for (String path : ExternalExec.getIgnoredFiles(vcsRoot)) {
                    final VirtualFile file = root.findFileByRelativePath(path);
                    if (file != null) {
                        result.put(file, vcsRoot);
                    }
                }
            }

            if (!silent && !result.isEmpty()) {
                myProject.getMessageBus().syncPublisher(TrackedIgnoredListener.class).handleFiles(result);
            }
            confirmedIgnoredFiles.clear();
            confirmedIgnoredFiles.putAll(result);
            notConfirmedIgnoredFiles.clear();
            debouncedStatusesChanged.run();

            for (ProjectViewPane pane : myProject.getExtensionList(ProjectViewPane.class)) {
                pane.queueUpdate();
            }
        }
    }
}
