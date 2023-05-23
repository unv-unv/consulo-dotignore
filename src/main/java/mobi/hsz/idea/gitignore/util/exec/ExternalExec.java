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

package mobi.hsz.idea.gitignore.util.exec;

import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import git4idea.config.GitExecutableManager;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.lang.kind.GitLanguage;
import mobi.hsz.idea.gitignore.util.Utils;
import mobi.hsz.idea.gitignore.util.exec.parser.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that holds util methods for calling external executables (i.e. git/hg)
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.4
 */
public class ExternalExec {
    private static final Logger LOG = Logger.getInstance(ExternalExec.class);
    /** Default external exec timeout. */
    private static final int DEFAULT_TIMEOUT = 5000;

    /** Private constructor to prevent creating Icons instance. */
    private ExternalExec() {
    }

    /** Checks if Git plugin is enabled. */
    private static final boolean GIT_ENABLED = Utils.isGitPluginEnabled();

    /** Git command to get user's excludesfile path. */
    @NonNls
    private static final String GIT_CONFIG_EXCLUDES_FILE = "config --global core.excludesfile";

    /** Git command to list unversioned files. */
    @NonNls
    private static final String GIT_UNIGNORED_FILES = "clean -dn";

    /** Git command to list ignored but tracked files. */
    @NonNls
    private static final String GIT_IGNORED_FILES = "ls-files -i --exclude-standard";

    /** Git command to remove file from tracking. */
    @NonNls
    private static final String GIT_REMOVE_FILE_FROM_TRACKING = "rm --cached --force";

    /**
     * Returns {@link VirtualFile} instance of the Git excludes file if available.
     *
     * @return Git excludes file
     */
    @Nullable
    public static VirtualFile getGitExcludesFile() {
        return runForSingle(GitLanguage.INSTANCE, GIT_CONFIG_EXCLUDES_FILE, null, new GitExcludesOutputParser());
    }

    /**
     * Returns list of unignored files for the given directory.
     *
     * @param language to check
     * @param project  current project
     * @param file     current file
     * @return unignored files list
     */
    @NotNull
    public static List<String> getUnignoredFiles(
            @NotNull IgnoreLanguage language, @NotNull Project project,
            @NotNull VirtualFile file)
    {
        if (!Utils.isInProject(file, project)) {
            return ContainerUtil.newArrayList();
        }

        ArrayList<String> result = run(
                language,
                GIT_UNIGNORED_FILES,
                file.getParent(),
                new GitUnignoredFilesOutputParser()
        );
        return Lists.notNullize(result);
    }

    /**
     * Returns list of ignored files for the given repository.
     *
     * @param vcsRoot repository to check
     * @return unignored files list
     */
    @NotNull
    public static List<String> getIgnoredFiles(@NotNull VcsRoot vcsRoot) {
        ArrayList<String> result = run(
                GitLanguage.INSTANCE,
                GIT_IGNORED_FILES,
                vcsRoot.getPath(),
                new SimpleOutputParser()
        );
        return Lists.notNullize(result);
    }

    /**
     * Removes given files from the git tracking.
     *
     * @param file    to untrack
     * @param vcsRoot file's repository
     */
    public static void removeFileFromTracking(@NotNull VirtualFile file, @NotNull VcsRoot vcsRoot) {
        final VirtualFile root = vcsRoot.getPath();
        if (root != null) {
            final String command = GIT_REMOVE_FILE_FROM_TRACKING + " " + Utils.getRelativePath(root, file);
            run(GitLanguage.INSTANCE, command, root);
        }
    }

    /**
     * Returns path to the {@link IgnoreLanguage} binary or null if not available.
     * Currently only  {@link GitLanguage} is supported.
     *
     * @param language current language
     * @return path to binary
     */
    @Nullable
    private static String bin(@NotNull IgnoreLanguage language) {
        if (GitLanguage.INSTANCE.equals(language) && GIT_ENABLED) {
            final String bin = GitExecutableManager.getInstance().getPathToGit();
            return StringUtil.nullize(bin);
        }
        return null;
    }

    /**
     * Runs {@link IgnoreLanguage} executable with the given command and current working directory.
     *
     * @param language  current language
     * @param command   to call
     * @param directory current working directory
     * @param parser    {@link ExecutionOutputParser} implementation
     * @param <T>       return type
     * @return result of the call
     */
    @Nullable
    private static <T> T runForSingle(
            @NotNull IgnoreLanguage language, @NotNull String command,
            @Nullable VirtualFile directory, @NotNull final ExecutionOutputParser<T> parser)
    {
        return ContainerUtil.getFirstItem(run(language, command, directory, parser));
    }

    /**
     * Runs {@link IgnoreLanguage} executable with the given command and current working directory.
     *
     * @param language  current language
     * @param command   to call
     * @param directory current working directory
     */
    private static void run(
            @NotNull IgnoreLanguage language,
            @NotNull String command,
            @Nullable VirtualFile directory)
    {
        run(language, command, directory, null);
    }

    /**
     * Runs {@link IgnoreLanguage} executable with the given command and current working directory.
     *
     * @param language  current language
     * @param command   to call
     * @param directory current working directory
     * @param parser    {@link ExecutionOutputParser} implementation
     * @param <T>       return type
     * @return result of the call
     */
    @Nullable
    private static <T> ArrayList<T> run(
            @NotNull IgnoreLanguage language,
            @NotNull String command,
            @Nullable VirtualFile directory,
            @Nullable final ExecutionOutputParser<T> parser)
    {
        final String bin = bin(language);
        if (bin == null) {
            return null;
        }

        try {
            final File workingDirectory = directory != null ? new File(directory.getPath()) : null;

            GeneralCommandLine commandLine = new GeneralCommandLine(bin, command);
            if (workingDirectory != null) {
                commandLine.withWorkDirectory(workingDirectory);
            }

            ProcessHandler handler = ProcessHandlerBuilder.create(commandLine).build();
            handler.addProcessListener(new ProcessListener() {
                @Override
                public void onTextAvailable(ProcessEvent event, Key outputType) {
                    if (parser != null) {
                        parser.onTextAvailable(event.getText(), outputType);
                    }
                }
            });
            handler.startNotify();
            if (!handler.waitFor(DEFAULT_TIMEOUT)) {
                return null;
            }
            if (parser != null) {
                parser.notifyFinished(ObjectUtil.notNull(handler.getExitCode(), 0));
                if (parser.isErrorsReported()) {
                    return null;
                }
                return parser.getOutput();
            }
        } catch (ExecutionException e) {
            LOG.warn(e);
        }

        return null;
    }
}
