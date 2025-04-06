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

package mobi.hsz.idea.gitignore.ui;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.command.AppendFileCommandAction;
import mobi.hsz.idea.gitignore.command.CreateFileCommandAction;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.ui.template.TemplateTreeComparator;
import mobi.hsz.idea.gitignore.ui.template.TemplateTreeNode;
import mobi.hsz.idea.gitignore.ui.template.TemplateTreeRenderer;
import mobi.hsz.idea.gitignore.util.Constants;
import mobi.hsz.idea.gitignore.util.Resources;
import mobi.hsz.idea.gitignore.util.Utils;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

import static mobi.hsz.idea.gitignore.util.Resources.Template.Container.STARRED;
import static mobi.hsz.idea.gitignore.util.Resources.Template.Container.USER;

/**
 * {@link GeneratorDialog} responsible for displaying list of all available templates and adding selected ones
 * to the specified file.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.2
 */
public class GeneratorDialog extends DialogWrapper {
    /** {@link FilterComponent} search history key. */
    private static final String TEMPLATES_FILTER_HISTORY = "TEMPLATES_FILTER_HISTORY";

    /**
     * Cache set to store checked templates for the current action.
     */
    private final Set<Resources.Template> checked = new HashSet<>();

    /** Set of the starred templates. */
    private final Set<String> starred = new HashSet<>();

    /** Current working project. */
    @Nonnull
    private final Project project;

    /** Settings instance. */
    @Nonnull
    private final IgnoreSettings settings;

    /** Current working file. */
    @Nullable
    private PsiFile file;

    /** Templates tree root node. */
    @Nonnull
    private final TemplateTreeNode root;

    /** {@link CreateFileCommandAction} action instance to generate new file in the proper time. */
    @Nullable
    private CreateFileCommandAction action;

    /** Templates tree with checkbox feature. */
    private CheckboxTree tree;

    /** Tree expander responsible for expanding and collapsing tree structure. */
    private TreeExpander treeExpander;

    /** Dynamic templates filter. */
    private FilterComponent profileFilter;

    /** Preview editor with syntax highlight. */
    private Editor preview;

    /** {@link Document} related to the {@link Editor} feature. */
    private Document previewDocument;

    /** CheckboxTree selection listener. */
    private final TreeSelectionListener treeSelectionListener = e -> {
        TreePath path = getCurrentPath();
        if (path != null) {
            updateDescriptionPanel(path);
        }
    };

    /**
     * Builds a new instance of {@link GeneratorDialog}.
     *
     * @param project current working project
     * @param file    current working file
     */
    public GeneratorDialog(@Nonnull Project project, @Nullable PsiFile file) {
        super(project, false);
        this.project = project;
        this.file = file;
        this.root = new TemplateTreeNode();
        this.action = null;
        this.settings = IgnoreSettings.getInstance();

        setTitle(IgnoreLocalize.dialogGeneratorTitle());
        setOKButtonText(IgnoreLocalize.globalGenerate());
        setCancelButtonText(IgnoreLocalize.globalCancel());
        init();
    }

    /**
     * Builds a new instance of {@link GeneratorDialog}.
     *
     * @param project current working project
     * @param action  {@link CreateFileCommandAction} action instance to generate new file in the proper time
     */
    public GeneratorDialog(@Nonnull Project project, @Nullable CreateFileCommandAction action) {
        this(project, (PsiFile)null);
        this.action = action;
    }

    /**
     * Returns component which should be focused when the dialog appears on the screen.
     *
     * @return component to focus
     */
    @Nullable
    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return profileFilter;
    }

    /**
     * Dispose the wrapped and releases all resources allocated be the wrapper to help
     * more efficient garbage collection. You should never invoke this method twice or
     * invoke any method of the wrapper after invocation of <code>dispose</code>.
     *
     * @throws IllegalStateException if the dialog is disposed not on the event dispatch thread
     */
    @Override
    protected void dispose() {
        tree.removeTreeSelectionListener(treeSelectionListener);
        EditorFactory.getInstance().releaseEditor(preview);
        super.dispose();
    }

    /**
     * Show the dialog.
     *
     * @throws IllegalStateException if the method is invoked not on the event dispatch thread
     * @see #showAndGet()
     * @see #showAndGetOk()
     */
    @Override
    @RequiredUIAccess
    public void show() {
        if (Application.get().isUnitTestMode()) {
            dispose();
            return;
        }
        super.show();
    }

    /**
     * This method is invoked by default implementation of "OK" action. It just closes dialog
     * with <code>OK_EXIT_CODE</code>. This is convenient place to override functionality of "OK" action.
     * Note that the method does nothing if "OK" action isn't enabled.
     */
    @Override
    protected void doOKAction() {
        if (isOKActionEnabled()) {
            performAppendAction(false, false);
        }
    }

    /**
     * Performs {@link AppendFileCommandAction} action.
     *
     * @param ignoreDuplicates ignores duplicated rules
     * @param ignoreComments   ignores comments and empty lines
     */
    private void performAppendAction(boolean ignoreDuplicates, boolean ignoreComments) {
        StringBuilder content = new StringBuilder();
        Iterator<Resources.Template> iterator = checked.iterator();
        while (iterator.hasNext()) {
            Resources.Template template = iterator.next();
            if (template != null) {
                content.append(IgnoreLocalize.fileTemplatesection(template.getName()));
                content.append(Constants.NEWLINE).append(template.getContent());

                if (iterator.hasNext()) {
                    content.append(Constants.NEWLINE);
                }
            }
        }
        try {
            if (file == null && action != null) {
                file = action.execute();
            }
            if (file != null && (content.length() > 0)) {
                new AppendFileCommandAction(project, file, content.toString(), ignoreDuplicates, ignoreComments)
                    .execute();
            }
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        super.doOKAction();
    }

    /** Creates default actions with appended {@link OptionOkAction} instance. */
    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        myOKAction = new OptionOkAction();
    }

    /**
     * Factory method. It creates panel with dialog options. Options panel is located at the
     * center of the dialog's content pane. The implementation can return <code>null</code>
     * value. In this case there will be no options panel.
     *
     * @return center panel
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        // general panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setPreferredSize(new Dimension(800, 500));

        // splitter panel - contains tree panel and preview component
        JBSplitter splitter = new JBSplitter(false, 0.4f);
        centerPanel.add(splitter, BorderLayout.CENTER);

        JPanel treePanel = new JPanel(new BorderLayout());
        previewDocument = EditorFactory.getInstance().createDocument("");
        preview = Utils.createPreviewEditor(previewDocument, project, true);

        splitter.setFirstComponent(treePanel);
        splitter.setSecondComponent(preview.getComponent());

        /* Scroll panel for the templates tree. */
        JScrollPane treeScrollPanel = createTreeScrollPanel();
        treePanel.add(treeScrollPanel, BorderLayout.CENTER);

        JPanel northPanel = new JPanel(new GridBagLayout());
        northPanel.setBorder(JBUI.Borders.empty(2, 0));
        northPanel.add(
            createTreeActionsToolbarPanel(treeScrollPanel).getComponent(),
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.BASELINE_LEADING,
                GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0
            )
        );
        northPanel.add(profileFilter, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.BASELINE_TRAILING,
            GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0
        ));
        treePanel.add(northPanel, BorderLayout.NORTH);

        return centerPanel;
    }

    /**
     * Creates scroll panel with templates tree in it.
     *
     * @return scroll panel
     */
    private JScrollPane createTreeScrollPanel() {
        fillTreeData(null, true);

        TemplateTreeRenderer renderer = new TemplateTreeRenderer() {
            @Override
            protected String getFilter() {
                return profileFilter != null ? profileFilter.getFilter() : null;
            }
        };

        tree = new CheckboxTree(renderer, root) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                Dimension size = super.getPreferredScrollableViewportSize();
                size = new Dimension(size.width + 10, size.height);
                return size;
            }

            @Override
            protected void onNodeStateChanged(CheckedTreeNode node) {
                super.onNodeStateChanged(node);
                Resources.Template template = ((TemplateTreeNode)node).getTemplate();
                if (node.isChecked()) {
                    checked.add(template);
                }
                else {
                    checked.remove(template);
                }
            }
        };

        tree.setCellRenderer(renderer);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(treeSelectionListener);
        UIUtil.setLineStyleAngled(tree);
        TreeUtil.installActions(tree);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(tree);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        TreeUtil.expandAll(tree);

        treeExpander = new DefaultTreeExpander(tree);
        profileFilter = new TemplatesFilterComponent();

        return scrollPane;
    }

    @Nullable
    private TreePath getCurrentPath() {
        if (tree.getSelectionPaths() != null && tree.getSelectionPaths().length == 1) {
            return tree.getSelectionPaths()[0];
        }
        return null;
    }

    /**
     * Creates tree toolbar panel with actions for working with templates tree.
     *
     * @param target templates tree
     * @return action toolbar
     */
    private ActionToolbar createTreeActionsToolbarPanel(@Nonnull JComponent target) {
        CommonActionsManager actionManager = CommonActionsManager.getInstance();

        DefaultActionGroup actions = new DefaultActionGroup();
        actions.add(actionManager.createExpandAllAction(treeExpander, tree));
        actions.add(actionManager.createCollapseAllAction(treeExpander, tree));
        actions.add(new AnAction(
            IgnoreLocalize.dialogGeneratorUnselectall(),
            LocalizeValue.empty(),
            PlatformIconGroup.actionsUnselectall()
        ) {
            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(!checked.isEmpty());
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                checked.clear();
                filterTree(profileFilter.getTextEditor().getText());
            }
        });
        actions.add(new AnAction(IgnoreLocalize.dialogGeneratorStar(), LocalizeValue.empty(), PlatformIconGroup.nodesStar()) {
            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent e) {
                TemplateTreeNode node = getCurrentNode();
                boolean disabled = node == null || USER.equals(node.getContainer()) || !node.isLeaf();
                boolean unstar = node != null && STARRED.equals(node.getContainer());

                Image icon = disabled ? ImageEffects.grayed(PlatformIconGroup.nodesStar()) :
                    (unstar ? PlatformIconGroup.nodesStarempty() : PlatformIconGroup.nodesStar());
                LocalizeValue text = unstar ? IgnoreLocalize.dialogGeneratorUnstar() : IgnoreLocalize.dialogGeneratorStar();

                Presentation presentation = e.getPresentation();
                presentation.setEnabled(!disabled);
                presentation.setIcon(icon);
                presentation.setTextValue(text);
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                TemplateTreeNode node = getCurrentNode();
                if (node == null) {
                    return;
                }

                Resources.Template template = node.getTemplate();
                if (template != null) {
                    boolean isStarred = !template.isStarred();
                    template.setStarred(isStarred);
                    refreshTree();

                    if (isStarred) {
                        starred.add(template.getName());
                    }
                    else {
                        starred.remove(template.getName());
                    }

                    settings.setStarredTemplates(new ArrayList<>(starred));
                }
            }

            /**
             * Returns current {@link TemplateTreeNode} node if available.
             *
             * @return current node
             */
            @Nullable
            private TemplateTreeNode getCurrentNode() {
                TreePath path = getCurrentPath();
                return path == null ? null : (TemplateTreeNode)path.getLastPathComponent();
            }
        });

        ActionToolbar actionToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, actions, true);
        actionToolbar.setTargetComponent(target);
        return actionToolbar;
    }

    /**
     * Updates editor's content depending on the selected {@link TreePath}.
     *
     * @param path selected tree path
     */
    @RequiredUIAccess
    private void updateDescriptionPanel(@Nonnull TreePath path) {
        TemplateTreeNode node = (TemplateTreeNode)path.getLastPathComponent();
        Resources.Template template = node.getTemplate();

        Application.get().runWriteAction(
            () -> CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                String content = template != null ?
                    StringUtil.notNullize(template.getContent()).replace('\r', '\0') : "";
                previewDocument.replaceString(0, previewDocument.getTextLength(), content);

                List<Couple<Integer>> pairs = getFilterRanges(profileFilter.getTextEditor().getText(), content);
                highlightWords(pairs);
            })
        );
    }

    /**
     * Fills templates tree with templates fetched with {@link Resources#getGitignoreTemplates()}.
     *
     * @param filter       templates filter
     * @param forceInclude force include
     */
    private void fillTreeData(@Nullable String filter, boolean forceInclude) {
        root.removeAllChildren();
        root.setChecked(false);

        for (Resources.Template.Container container : Resources.Template.Container.values()) {
            TemplateTreeNode node = new TemplateTreeNode(container);
            node.setChecked(false);
            root.add(node);
        }

        List<Resources.Template> templatesList = Resources.getGitignoreTemplates();
        for (Resources.Template template : templatesList) {
            if (filter != null && filter.length() > 0 && !isTemplateAccepted(template, filter)) {
                continue;
            }

            TemplateTreeNode node = new TemplateTreeNode(template);
            node.setChecked(checked.contains(template));
            getGroupNode(root, template.getContainer()).add(node);
        }

        if (filter != null && forceInclude && root.getChildCount() == 0) {
            fillTreeData(filter, false);
        }

        TreeUtil.sort(root, new TemplateTreeComparator());
    }

    /**
     * Creates or gets existing group node for specified element.
     *
     * @param root      tree root node
     * @param container container type to search
     * @return group node
     */
    private static TemplateTreeNode getGroupNode(
        @Nonnull TemplateTreeNode root,
        @Nonnull Resources.Template.Container container
    ) {
        int childCount = root.getChildCount();

        for (int i = 0; i < childCount; i++) {
            TemplateTreeNode child = (TemplateTreeNode)root.getChildAt(i);
            if (container.equals(child.getContainer())) {
                return child;
            }
        }

        TemplateTreeNode child = new TemplateTreeNode(container);
        root.add(child);
        return child;
    }

    /**
     * Finds for the filter's words in the given content and returns their positions.
     *
     * @param filter  templates filter
     * @param content templates content
     * @return text ranges
     */
    private List<Couple<Integer>> getFilterRanges(@Nonnull String filter, @Nonnull String content) {
        List<Couple<Integer>> pairs = new ArrayList<>();
        content = content.toLowerCase();

        for (String word : Utils.getWords(filter)) {
            for (int index = content.indexOf(word); index >= 0; index = content.indexOf(word, index + 1)) {
                pairs.add(Couple.of(index, index + word.length()));
            }
        }

        return pairs;
    }

    /**
     * Checks if given template is accepted by passed filter.
     *
     * @param template to check
     * @param filter   templates filter
     * @return template is accepted
     */
    private boolean isTemplateAccepted(@Nonnull Resources.Template template, @Nonnull String filter) {
        filter = filter.toLowerCase();

        if (StringUtil.containsIgnoreCase(template.getName(), filter)) {
            return true;
        }

        boolean nameAccepted = true;
        for (String word : Utils.getWords(filter)) {
            if (!StringUtil.containsIgnoreCase(template.getName(), word)) {
                nameAccepted = false;
            }
        }

        List<Couple<Integer>> ranges = getFilterRanges(filter, StringUtil.notNullize(template.getContent()));
        return nameAccepted || ranges.size() > 0;
    }

    /**
     * Filters templates tree.
     *
     * @param filter text
     */
    private void filterTree(@Nullable String filter) {
        if (tree != null) {
            fillTreeData(filter, true);
            reloadModel();
            TreeUtil.expandAll(tree);
            if (tree.getSelectionPath() == null) {
                TreeUtil.selectFirstNode(tree);
            }
        }
    }

    /** Refreshes current tree. */
    private void refreshTree() {
        filterTree(profileFilter.getTextEditor().getText());
    }

    /**
     * Highlights given text ranges in {@link #preview} content.
     *
     * @param pairs text ranges
     */
    private void highlightWords(@Nonnull List<Couple<Integer>> pairs) {
        TextAttributes attr = new TextAttributes();
        attr.setBackgroundColor(TargetAWT.from(UIUtil.getTreeSelectionBackground(true)));
        attr.setForegroundColor(TargetAWT.from(UIUtil.getTreeSelectionForeground(true)));

        for (Couple<Integer> pair : pairs) {
            preview.getMarkupModel().addRangeHighlighter(pair.first, pair.second, 0, attr, HighlighterTargetArea.EXACT_RANGE);
        }
    }

    /** Reloads tree model. */
    private void reloadModel() {
        ((DefaultTreeModel)tree.getModel()).reload();
    }

    /**
     * Returns current file.
     *
     * @return file
     */
    @Nullable
    public PsiFile getFile() {
        return file;
    }

    /** Custom templates {@link FilterComponent}. */
    private class TemplatesFilterComponent extends FilterComponent {
        /** Builds a new instance of {@link TemplatesFilterComponent}. */
        public TemplatesFilterComponent() {
            super(TEMPLATES_FILTER_HISTORY, 10);
        }

        /** Filters tree using current filter's value. */
        @Override
        public void filter() {
            filterTree(getFilter());
        }
    }

    /** {@link OkAction} instance with additional `Generate without duplicates` action. */
    private class OptionOkAction extends OkAction implements OptionAction {
        @Nonnull
        @Override
        public Action[] getOptions() {
            return new Action[]{
                new DialogWrapperAction(IgnoreLocalize.globalGenerateWithoutDuplicates()) {
                    @Override
                    protected void doAction(ActionEvent e) {
                        performAppendAction(true, false);
                    }
                },
                new DialogWrapperAction(IgnoreLocalize.globalGenerateWithoutComments()) {
                    @Override
                    protected void doAction(ActionEvent e) {
                        performAppendAction(false, true);
                    }
                }
            };
        }
    }
}
