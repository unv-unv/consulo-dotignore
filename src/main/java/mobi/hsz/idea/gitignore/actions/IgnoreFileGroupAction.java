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
package mobi.hsz.idea.gitignore.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.util.ExternalFileException;
import mobi.hsz.idea.gitignore.util.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Group action that ignores specified file or directory.
 * {@link ActionGroup} expands single action into a more child options to allow user specify
 * the IgnoreFile that will be used for file's path storage.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.5
 */
@ActionImpl(
    id = "Ignore.IgnoreGroup",
    parents = @ActionParentRef(
        value = @ActionRef(id = "ChangesViewPopupMenu"),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "ChangesView.Ignore")
    )
)
public class IgnoreFileGroupAction extends ActionGroup {
    /** Maximum filename length for the action name. */
    private static final int FILENAME_MAX_LENGTH = 30;

    /** List of suitable Gitignore {@link VirtualFile}s that can be presented in an IgnoreFile action. */
    @Nonnull
    private final Map<IgnoreFileType, List<VirtualFile>> files = new HashMap<>();

    /** Action presentation's text for single element. */
    @Nonnull
    private final Function<String, LocalizeValue> presentationTextSingle;

    /** {@link Project}'s base directory. */
    @Nullable
    private VirtualFile baseDir;

    /**
     * Builds a new instance of {@link IgnoreFileGroupAction}.
     * Describes action's presentation.
     */
    @Inject
    public IgnoreFileGroupAction() {
        this(
            IgnoreLocalize.actionAddtoignoreGroup(),
            IgnoreLocalize.actionAddtoignoreGroupDescription(),
            IgnoreLocalize::actionAddtoignoreGroupNopopup
        );
    }

    /**
     * Builds a new instance of {@link IgnoreFileGroupAction}.
     * Describes action's presentation.
     *
     * @param text        Action presentation's text key
     * @param description Action presentation's description key
     */
    protected IgnoreFileGroupAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nonnull Function<String, LocalizeValue> textSingleKey
    ) {
        Presentation p = getTemplatePresentation();
        p.setTextValue(text);
        p.setDescriptionValue(description);
        this.presentationTextSingle = textSingleKey;
    }

    /**
     * Presents a list of suitable Gitignore files that can cover currently selected {@link VirtualFile}.
     * Shows a subgroup with available files or one option if only one Gitignore file is available.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        VirtualFile file = e.getData(VirtualFile.KEY);
        Project project = e.getData(Project.KEY);
        Presentation presentation = e.getPresentation();
        files.clear();

        if (project != null && file != null) {
            try {
                presentation.setVisible(true);
                baseDir = project.getBaseDir();

                for (IgnoreLanguage language : IgnoreBundle.LANGUAGES) {
                    IgnoreFileType fileType = language.getFileType();
                    List<VirtualFile> list = Utils.getSuitableIgnoreFiles(project, fileType, file);
                    Collections.reverse(list);
                    files.put(fileType, list);
                }
            }
            catch (ExternalFileException e1) {
                presentation.setVisible(false);
            }
        }

        setPopup(countFiles() > 1);
    }

    /**
     * Creates subactions bound to the specified Gitignore {@link VirtualFile}s using {@link IgnoreFileAction}.
     *
     * @param e action event
     * @return actions list
     */
    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        AnAction[] actions;
        int count = countFiles();

        if (count == 0 || baseDir == null) {
            actions = new AnAction[0];
        }
        else {
            actions = new AnAction[count];

            int i = 0;
            for (Map.Entry<IgnoreFileType, List<VirtualFile>> entry : files.entrySet()) {
                for (VirtualFile file : entry.getValue()) {
                    IgnoreFileAction action = createAction(file);
                    actions[i++] = action;

                    String relativePath = Utils.getRelativePath(baseDir, file);
                    if (StringUtil.isNotEmpty(relativePath)) {
                        relativePath = StringUtil.shortenPathWithEllipsis(relativePath, FILENAME_MAX_LENGTH);
                    }

                    Presentation presentation = action.getTemplatePresentation();
                    presentation.setIcon(entry.getKey().getIcon());
                    presentation.setTextValue(
                        count == 1 ? presentationTextSingle.apply(relativePath) : LocalizeValue.ofNullable(relativePath)
                    );
                }
            }
        }
        return actions;
    }

    /**
     * Creates new {@link IgnoreFileAction} action instance.
     *
     * @param file current file
     * @return action instance
     */
    protected IgnoreFileAction createAction(@Nonnull VirtualFile file) {
        return new IgnoreFileAction(file);
    }

    /**
     * Counts items in {@link #files} map.
     *
     * @return files amount
     */
    private int countFiles() {
        int size = 0;
        for (List value : files.values()) {
            size += value.size();
        }
        return size;
    }
}
