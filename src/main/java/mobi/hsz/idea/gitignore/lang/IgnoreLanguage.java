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
package mobi.hsz.idea.gitignore.lang;

import consulo.language.InjectableLanguage;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.outer.OuterFileFetcher;
import mobi.hsz.idea.gitignore.psi.IgnoreFile;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.util.ExpiringMap;
import mobi.hsz.idea.gitignore.util.Icons;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * Gitignore {@link Language} definition.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.8
 */
public class IgnoreLanguage extends Language implements InjectableLanguage {
    /** The {@link IgnoreLanguage} instance. */
    public static final IgnoreLanguage INSTANCE = new IgnoreLanguage();

    /** The dot. */
    private static final String DOT = ".";

    /** The Ignore file extension suffix. */
    @Nonnull
    private final String extension;

    /** The Ignore VCS directory name. */
    @Nullable
    private final String vcsDirectory;

    /** The GitignoreLanguage icon. */
    @Nonnull
    private final Image icon;

    /** Outer files for the specified {@link IgnoreLanguage}. */
    @Nonnull
    private final OuterFileFetcher[] fetchers;

    /** Outer files cache. */
    @Nonnull
    protected final ExpiringMap<Integer, Set<VirtualFile>> outerFiles = new ExpiringMap<>(5000);

    /** {@link IgnoreLanguage} is a non-instantiable static class. */
    protected IgnoreLanguage() {
        this("Ignore", "ignore", null, Icons.IGNORE);
    }

    /** {@link IgnoreLanguage} is a non-instantiable static class. */
    protected IgnoreLanguage(
        @Nonnull String name, 
        @Nonnull String extension, 
        @Nullable String vcsDirectory,
        @Nonnull Image icon
    ) {
        this(name, extension, vcsDirectory, icon, new OuterFileFetcher[0]);
    }

    /** {@link IgnoreLanguage} is a non-instantiable static class. */
    protected IgnoreLanguage(
        @Nonnull String name, 
        @Nonnull String extension, 
        @Nullable String vcsDirectory,
        @Nonnull Image icon,
        @Nonnull OuterFileFetcher[] fetchers
    ) {
        super(name);
        this.extension = extension;
        this.vcsDirectory = vcsDirectory;
        this.icon = icon;
        this.fetchers = fetchers;
    }

    /**
     * Returns Ignore file extension suffix.
     *
     * @return extension
     */
    @Nonnull
    public String getExtension() {
        return extension;
    }

    /**
     * Returns Ignore VCS directory name.
     *
     * @return VCS directory name
     */
    @Nullable
    public String getVcsDirectory() {
        return vcsDirectory;
    }

    /**
     * The Gitignore file filename.
     *
     * @return filename.
     */
    @Nonnull
    public String getFilename() {
        return DOT + getExtension();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(getFilename() + " (" + getID() + ")");
    }

    /**
     * Returns Ignore file icon.
     *
     * @return icon
     */
    @Nonnull
    public Image getIcon() {
        return icon;
    }

    /**
     * Language file type.
     *
     * @return {@link IgnoreFileType} instance.
     */
    @Nonnull
    public IgnoreFileType getFileType() {
        return IgnoreFileType.INSTANCE;
    }

    /**
     * Creates {@link IgnoreFile} instance.
     *
     * @return {@link IgnoreFile} instance.
     */
    public final IgnoreFile createFile(@Nonnull FileViewProvider viewProvider) {
        return new IgnoreFile(viewProvider, getFileType());
    }

    /**
     * Returns <code>true</code> if `syntax: value` entry is supported by the language (i.e. Mercurial).
     *
     * @return <code>true</code> if `syntax: value` entry is supported
     */
    public boolean isSyntaxSupported() {
        return false;
    }

    /**
     * Returns default language syntax.
     *
     * @return default syntax
     */
    @Nonnull
    public IgnoreBundle.Syntax getDefaultSyntax() {
        return IgnoreBundle.Syntax.GLOB;
    }

    /**
     * Defines if current {@link IgnoreLanguage} supports outer ignore files.
     *
     * @return supports outer ignore files
     */
    public boolean isOuterFileSupported() {
        return false;
    }

    /**
     * Returns {@link OuterFileFetcher} instances. method is called when
     * {@link #isOuterFileSupported()} returns <code>true</code> value.
     *
     * @return outer file fetcher array
     */
    @Nonnull
    public final OuterFileFetcher[] getOuterFileFetchers() {
        return fetchers;
    }

    /**
     * Returns outer files for the current language.
     *
     * @param project current project
     * @return outer files
     */
    @Nonnull
    public Set<VirtualFile> getOuterFiles(@Nonnull Project project) {
        return getOuterFiles(project, false);
    }

    /**
     * Returns outer files for the current language.
     *
     * @param project current project
     * @return outer files
     */
    @Nonnull
    public Set<VirtualFile> getOuterFiles(@Nonnull Project project, boolean dumb) {
        int key = new HashCodeBuilder().append(project).append(getFileType()).toHashCode();
        if (outerFiles.get(key) == null) {
            Set<VirtualFile> files = new HashSet<>();
            for (OuterFileFetcher fetcher : getOuterFileFetchers()) {
                ContainerUtil.addAllNotNull(files, fetcher.fetch(project));
            }
            outerFiles.set(key, files);
        }
        return outerFiles.getOrElse(key, new HashSet<>());
    }

    /**
     * Checks is language is enabled or with {@link IgnoreSettings}.
     *
     * @return language is enabled
     */
    public final boolean isEnabled() {
        TreeMap<IgnoreSettings.IgnoreLanguagesSettings.KEY, Object> data =
                IgnoreSettings.getInstance().getLanguagesSettings().get(this);
        boolean value = false;
        if (data != null) {
            value = Boolean.valueOf(data.get(IgnoreSettings.IgnoreLanguagesSettings.KEY.ENABLE).toString());
        }
        return value;
    }

    /**
     * Checks if creating new file for given language is allowed with the settings.
     *
     * @return new file action is allowed
     */
    public final boolean isNewAllowed() {
        TreeMap<IgnoreSettings.IgnoreLanguagesSettings.KEY, Object> data =
                IgnoreSettings.getInstance().getLanguagesSettings().get(this);
        boolean value = false;
        if (data != null) {
            value = Boolean.valueOf(data.get(IgnoreSettings.IgnoreLanguagesSettings.KEY.NEW_FILE).toString());
        }
        return value;
    }

    /**
     * Language is related to the VCS.
     *
     * @return is VCS
     */
    public boolean isVCS() {
        return true;
    }

    /**
     * Returns fixed directory for the given {@link IgnoreLanguage}.
     *
     * @param project current project
     * @return fixed directory
     */
    @Nullable
    public VirtualFile getFixedDirectory(@Nonnull Project project) {
        return null;
    }
}
