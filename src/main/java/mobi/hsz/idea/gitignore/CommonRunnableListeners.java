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

import consulo.module.Module;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.event.ModuleListener;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Wrapper for common listeners.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 2.2.0
 */
public class CommonRunnableListeners implements RefreshStatusesListener, ModuleRootListener, ModuleListener {
    /**
     * Task to run.
     */
    @Nonnull
    private final Runnable task;

    /**
     * Constructor.
     *
     * @param task to run by all listeners
     */
    public CommonRunnableListeners(@Nonnull Runnable task) {
        this.task = task;
    }

    /**
     * {@link RefreshStatusesListener} event.
     */
    @Override
    public void refresh() {
        task.run();
    }

    /**
     * {@link ModuleRootListener} event (ignored).
     */
    @Override
    public void beforeRootsChange(@Nonnull ModuleRootEvent event) {
    }

    /**
     * {@link ModuleRootListener} event.
     */
    @Override
    public void rootsChanged(@Nonnull ModuleRootEvent event) {
        task.run();
    }

    /**
     * {@link ModuleListener} event.
     */
    @Override
    public void moduleAdded(@Nonnull Project project, @Nonnull Module module) {
        task.run();
    }

    /**
     * {@link ModuleListener} event (ignored).
     */
    @Override
    public void beforeModuleRemoved(@Nonnull Project project, @Nonnull Module module) {
    }

    /**
     * {@link ModuleListener} event.
     */
    @Override
    public void moduleRemoved(@Nonnull Project project, @Nonnull Module module) {
        task.run();
    }

    @Override
    public void modulesRenamed(Project project, List<Module> list) {
        task.run();
    }
}
