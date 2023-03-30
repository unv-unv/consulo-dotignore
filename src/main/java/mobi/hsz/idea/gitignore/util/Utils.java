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

package mobi.hsz.idea.gitignore.util;

import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsScheme;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.document.Document;
import consulo.fileEditor.FileEditorManager;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.command.CreateFileCommandAction;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static consulo.ui.ex.SimpleTextAttributes.REGULAR_ATTRIBUTES;

/**
 * {@link Utils} class that contains various methods.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.3.3
 */
public class Utils {
    /**
     * Private constructor to prevent creating {@link Utils} instance.
     */
    private Utils() {
    }

    /**
     * Gets relative path of given @{link VirtualFile} and root directory.
     *
     * @param directory root directory
     * @param file      file to get it's path
     * @return relative path
     */
    @Nullable
    public static String getRelativePath(@NotNull VirtualFile directory, @NotNull VirtualFile file) {
        final String path = VirtualFileUtil.getRelativePath(file, directory, '/');
        return path == null ? null : path + (file.isDirectory() ? '/' : "");
    }

    /**
     * Gets Ignore file for given {@link Project} and root {@link PsiDirectory}.
     * If file is missing - creates new one.
     *
     * @param project         current project
     * @param fileType        current ignore file type
     * @param directory       root directory
     * @param createIfMissing create new file if missing
     * @return Ignore file
     */
    @Nullable
    public static PsiFile getIgnoreFile(@NotNull Project project, @NotNull IgnoreFileType fileType,
                                        @Nullable PsiDirectory directory, boolean createIfMissing) {
        if (directory == null) {
            directory = PsiManager.getInstance(project).findDirectory(project.getBaseDir());
        }

        assert directory != null;
        String filename = fileType.getIgnoreLanguage().getFilename();
        PsiFile file = directory.findFile(filename);
        VirtualFile virtualFile = file == null ? directory.getVirtualFile().findChild(filename) : file.getVirtualFile();

        if (file == null && virtualFile == null && createIfMissing) {
            try {
                file = new CreateFileCommandAction(project, directory, fileType).execute();
            }
            catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return file;
    }

    /**
     * Finds {@link PsiFile} for the given {@link VirtualFile} instance. If file is outside current project,
     * it's required to create new {@link PsiFile} manually.
     *
     * @param project     current project
     * @param virtualFile to handle
     * @return {@link PsiFile} instance
     */
    @Nullable
    public static PsiFile getPsiFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

        if (psiFile == null) {
            FileViewProvider viewProvider = PsiManager.getInstance(project).findViewProvider(virtualFile);
            if (viewProvider != null) {
                IgnoreLanguage language = IgnoreBundle.obtainLanguage(virtualFile);
                if (language != null) {
                    psiFile = language.createFile(viewProvider);
                }
            }
        }

        return psiFile;
    }

    /**
     * Opens given file in editor.
     *
     * @param project current project
     * @param file    file to open
     */
    public static void openFile(@NotNull Project project, @NotNull PsiFile file) {
        openFile(project, file.getVirtualFile());
    }

    /**
     * Opens given file in editor.
     *
     * @param project current project
     * @param file    file to open
     */
    public static void openFile(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManager.getInstance(project).openFile(file, true);
    }

    /**
     * Returns all Ignore files in given {@link Project} that can match current passed file.
     *
     * @param project current project
     * @param file    current file
     * @return collection of suitable Ignore files
     */
    public static List<VirtualFile> getSuitableIgnoreFiles(@NotNull Project project, @NotNull IgnoreFileType fileType,
                                                           @NotNull VirtualFile file)
            throws ExternalFileException {
        List<VirtualFile> files = ContainerUtil.newArrayList();
        if (file.getCanonicalPath() == null || project.getBaseDir() == null ||
                !VirtualFileUtil.isAncestor(project.getBaseDir(), file, true)) {
            throw new ExternalFileException();
        }
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null && !baseDir.equals(file)) {
            do {
                file = file.getParent();
                VirtualFile ignoreFile = file.findChild(fileType.getIgnoreLanguage().getFilename());
                ContainerUtil.addIfNotNull(files, ignoreFile);
            }
            while (!file.equals(project.getBaseDir()));
        }
        return files;
    }

    /**
     * Checks if given directory is a {@link IgnoreLanguage#getVcsDirectory()}.
     *
     * @param directory to check
     * @return given file is VCS directory
     */
    public static boolean isVcsDirectory(@NotNull VirtualFile directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        for (IgnoreLanguage language : IgnoreBundle.VCS_LANGUAGES) {
            final String vcsName = language.getVcsDirectory();
            if (directory.getName().equals(vcsName) && IgnoreBundle.ENABLED_LANGUAGES.get(language.getFileType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches for excluded roots in given {@link Project}.
     *
     * @param project current project
     * @return list of excluded roots
     */
    public static List<VirtualFile> getExcludedRoots(@NotNull Project project) {
        List<VirtualFile> roots = ContainerUtil.newArrayList();
        ModuleManager manager = ModuleManager.getInstance(project);
        for (Module module : manager.getModules()) {
            ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
            if (model.isDisposed()) {
                continue;
            }
            Collections.addAll(roots, model.getExcludeRoots());
            model.dispose();
        }
        return roots;
    }

    /**
     * Gets list of words for given {@link String} excluding special characters.
     *
     * @param filter input string
     * @return list of words without special characters
     */
    public static List<String> getWords(@NotNull String filter) {
        List<String> words = ContainerUtil.newArrayList(filter.toLowerCase().split("\\W+"));
        words.removeAll(Arrays.asList(null, ""));
        return words;
    }

    /**
     * Checks if lists are equal.
     *
     * @param l1 first list
     * @param l2 second list
     * @return lists are equal
     */
    public static boolean equalLists(@NotNull List<?> l1, @NotNull List<?> l2) {
        return l1.size() == l2.size() && l1.containsAll(l2) && l2.containsAll(l1);
    }

    /**
     * Returns {@link IgnoreFileType} basing on the {@link VirtualFile} file.
     *
     * @param virtualFile current file
     * @return file type
     */
    public static IgnoreFileType getFileType(@Nullable VirtualFile virtualFile) {
        if (virtualFile != null) {
            FileType fileType = virtualFile.getFileType();
            if (fileType instanceof IgnoreFileType) {
                return (IgnoreFileType) fileType;
            }
        }
        return null;
    }

    /**
     * Checks if file is under given directory.
     *
     * @param file      file
     * @param directory directory
     * @return file is under directory
     */
    public static boolean isUnder(@NotNull VirtualFile file, @NotNull VirtualFile directory) {
        if (directory.equals(file)) {
            return true;
        }
        VirtualFile parent = file.getParent();
        while (parent != null) {
            if (directory.equals(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Checks if file is in project directory.
     *
     * @param file    file
     * @param project project
     * @return file is under directory
     */
    public static boolean isInProject(@NotNull final VirtualFile file, @NotNull final Project project) {
        return project.getBaseDir() != null && (isUnder(file, project.getBaseDir()) ||
                StringUtil.startsWith(file.getUrl(), "temp://"));
    }

    /**
     * Creates and configures template preview editor.
     *
     * @param document virtual editor document
     * @param project  current project
     * @return editor
     */
    @NotNull
    public static Editor createPreviewEditor(@NotNull Document document, @Nullable Project project, boolean isViewer) {
        EditorEx editor = (EditorEx) EditorFactory.getInstance().createEditor(document, project,
                IgnoreFileType.INSTANCE, isViewer);
        editor.setCaretEnabled(!isViewer);

        final EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(false);
        settings.setAdditionalColumnsCount(1);
        settings.setAdditionalLinesCount(0);
        settings.setRightMarginShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setIndentGuidesShown(false);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);

        EditorColorsScheme colorsScheme = editor.getColorsScheme();
        colorsScheme.setColor(EditorColors.CARET_ROW_COLOR, null);

        return editor;
    }

    /**
     * Checks if specified plugin is enabled.
     *
     * @param id plugin id
     * @return plugin is enabled
     */
    private static boolean isPluginEnabled(@NotNull final String id) {
        PluginDescriptor p = PluginManager.findPlugin(PluginId.getId(id));
        return p != null && p.isEnabled();
    }

    /**
     * Checks if Git plugin is enabled.
     *
     * @return Git plugin is enabled
     */
    public static boolean isGitPluginEnabled() {
        return isPluginEnabled("com.intellij.git");
    }

    /**
     * Resolves user directory with the <code>user.home</code> property.
     *
     * @param path path with leading ~
     * @return resolved path
     */
    public static String resolveUserDir(@Nullable String path) {
        if (StringUtil.startsWithChar(path, '~')) {
            assert path != null;
            path = System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    /**
     * Adds ColoredFragment to the node's presentation.
     *
     * @param data       node's presentation data
     * @param text       text to add
     * @param attributes custom {@link SimpleTextAttributes}
     */
    public static void addColoredText(@NotNull PresentationData data, @NotNull String text,
                                      @NotNull SimpleTextAttributes attributes) {
        if (data.getColoredText().isEmpty()) {
            data.addText(data.getPresentableText(), REGULAR_ATTRIBUTES);
        }
        data.addText(" " + text, attributes);
    }
}
