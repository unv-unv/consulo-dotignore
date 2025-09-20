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
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.psi.IgnoreFile;
import mobi.hsz.idea.gitignore.ui.template.UserTemplateDialog;
import mobi.hsz.idea.gitignore.util.Icons;

/**
 * Action that creates new user template with predefined content - i.e. from currently opened file.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.5
 */
@ActionImpl(id = "IgnoreCreateUserTemplate")
public class CreateUserTemplateAction extends AnAction
{
    public CreateUserTemplateAction() {
        super(
            IgnoreLocalize.actionCreateusertemplate(),
            IgnoreLocalize.actionCreateusertemplateDescription(),
            Icons.IGNORE
        );
    }

    /**
     * Handles an action of adding new template.
     * Ignores action if selected file is not a {@link IgnoreFile} instance, otherwise shows GeneratorDialog.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        PsiFile file = e.getData(PsiFile.KEY);

        if (project == null || !(file instanceof IgnoreFile)) {
            return;
        }

        String content = file.getText();
        Document document = file.getViewProvider().getDocument();
        if (document != null) {
            Editor[] editors = EditorFactory.getInstance().getEditors(document);
            if (editors.length > 0) {
                String selectedText = editors[0].getSelectionModel().getSelectedText();
                if (!StringUtil.isEmpty(selectedText)) {
                    content = selectedText;
                }
            }
        }

        new UserTemplateDialog(project, content).show();
    }

    /**
     * Updates visibility of the action presentation in various actions list.
     * Visible only for {@link IgnoreFile} context.
     *
     * @param e action event
     */
    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        PsiFile file = e.getData(PsiFile.KEY);

        if (!(file instanceof IgnoreFile)) {
            e.getPresentation().setVisible(false);
            return;
        }
        getTemplatePresentation().setIcon(file.getFileType().getIcon());
    }
}
