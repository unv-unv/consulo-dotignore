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

package mobi.hsz.idea.gitignore.projectView;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.ui.view.tree.*;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import mobi.hsz.idea.gitignore.IgnoreManager;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Extension for the {@link TreeStructureProvider} that provides the ability to hide ignored files
 * or directories in the project tree view.
 *
 * @author Maximiliano Najle <maximilianonajle@gmail.com>
 * @since 1.7
 */
@ExtensionImpl
public class HideIgnoredFilesTreeStructureProvider implements TreeStructureProvider {
    /** {@link IgnoreSettings} instance. */
    @NotNull
    private final Provider<IgnoreSettings> ignoreSettings;

    /** {@link IgnoreManager} instance. */
    @NotNull
    private final Provider<IgnoreManager> ignoreManager;

    @Inject
    public HideIgnoredFilesTreeStructureProvider(Provider<IgnoreSettings> ignoreSettings, Provider<IgnoreManager> ignoreManager) {
        this.ignoreSettings = ignoreSettings;
        this.ignoreManager = ignoreManager;
    }


    /**
     * If {@link IgnoreSettings#hideIgnoredFiles} is set to <code>true</code>, checks if specific
     * nodes are ignored and filters them out.
     *
     * @param parent   the parent node
     * @param children the list of child nodes according to the default project structure
     * @param settings the current project view settings
     * @return the modified collection of child nodes
     */
    @NotNull
    @Override
    public Collection<AbstractTreeNode> modify(
            @NotNull AbstractTreeNode parent,
            @NotNull Collection<AbstractTreeNode> children,
            @Nullable ViewSettings settings)
    {
        IgnoreSettings ignoreSettings = this.ignoreSettings.get();
        if (!ignoreSettings.isHideIgnoredFiles() || children.isEmpty()) {
            return children;
        }

        IgnoreManager ignoreManager = this.ignoreManager.get();
        return ContainerUtil.filter(children, node -> {
            if (node instanceof BasePsiNode) {
                final VirtualFile file = ((BasePsiNode) node).getVirtualFile();
                return file != null && (!ignoreManager.isFileIgnored(file) || ignoreManager.isFileTracked(file));
            }
            return true;
        });
    }
}
