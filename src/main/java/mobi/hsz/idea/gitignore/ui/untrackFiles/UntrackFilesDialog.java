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

package mobi.hsz.idea.gitignore.ui.untrackFiles;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ui.ex.awt.tree.TreeModelAdapter;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.util.Utils;
import mobi.hsz.idea.gitignore.util.exec.ExternalExec;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static mobi.hsz.idea.gitignore.RefreshTrackedIgnoredListener.TRACKED_IGNORED_REFRESH;

/**
 * Dialog that lists all untracked but indexed files in a tree view, allows select specific files
 * and perform command to untrack them.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.7
 */
public class UntrackFilesDialog extends DialogWrapper {
    /** Current project. */
    @Nonnull
    private final Project project;

    /** A list of the tracked but ignored files. */
    @Nonnull
    private final ConcurrentMap<VirtualFile, VcsRoot> files;

    /** Templates tree root node. */
    @Nonnull
    private final FileTreeNode root;

    /** Map of the tree view {@link FileTreeNode} nodes. */
    @Nonnull
    private final Map<VirtualFile, FileTreeNode> nodes = new HashMap<>();

    /** Commands editor with syntax highlight. */
    private Editor commands;

    /** {@link Document} related to the {@link Editor} feature. */
    private Document commandsDocument;

    /** Templates tree with checkbox feature. */
    private CheckboxTree tree;

    /** Tree expander responsible for expanding and collapsing tree structure. */
    private TreeExpander treeExpander;

    /** Listener that checks if files list has been changed and rewrites commands in {@link #commandsDocument}. */
    @Nonnull
    private final TreeModelListener treeModelListener = new TreeModelAdapter() {
        /**
         * Invoked after a tree has changed.
         *
         * @param event the event object specifying changed nodes
         */
        @Override
        @RequiredUIAccess
        public void treeNodesChanged(@Nonnull TreeModelEvent event) {
            String text = getCommandsText();

            Application.get().runWriteAction(() -> commandsDocument.setText(text));
        }
    };

    /**
     * Constructor.
     *
     * @param project current project
     * @param files   files map to present
     */
    public UntrackFilesDialog(@Nonnull Project project, @Nonnull ConcurrentMap<VirtualFile, VcsRoot> files) {
        super(project, false);
        this.project = project;
        this.files = files;
        this.root = createDirectoryNodes(project.getBaseDir(), null);

        setTitle(IgnoreLocalize.dialogUntrackfilesTitle());
        setOKButtonText(IgnoreLocalize.globalOk());
        setCancelButtonText(IgnoreLocalize.globalCancel());
        init();
    }

    /**
     * Builds recursively nested {@link FileTreeNode} nodes structure.
     *
     * @param file    current {@link VirtualFile} instance
     * @param vcsRoot {@link VcsRoot} of given file
     * @return leaf
     */
    @Nonnull
    private FileTreeNode createDirectoryNodes(@Nonnull VirtualFile file, @Nullable VcsRoot vcsRoot) {
        FileTreeNode node = nodes.get(file);
        if (node != null) {
            return node;
        }

        FileTreeNode newNode = new FileTreeNode(project, file, vcsRoot);
        nodes.put(file, newNode);

        if (nodes.size() != 1) {
            VirtualFile parent = file.getParent();
            if (parent != null) {
                createDirectoryNodes(parent, null).add(newNode);
            }
        }

        return newNode;
    }

    /**
     * Creates center panel of {@link DialogWrapper}.
     *
     * @return panel
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setPreferredSize(new Dimension(500, 400));

        JPanel treePanel = new JPanel(new BorderLayout());
        centerPanel.add(treePanel, BorderLayout.CENTER);

        /* Scroll panel for the templates tree. */
        JScrollPane treeScrollPanel = createTreeScrollPanel();
        treePanel.add(treeScrollPanel, BorderLayout.CENTER);

        JPanel northPanel = new JPanel(new GridBagLayout());
        northPanel.setBorder(JBUI.Borders.empty(2, 0));
        northPanel.add(createTreeActionsToolbarPanel(treeScrollPanel).getComponent(),
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.BASELINE_LEADING,
                        GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0)
        );
        treePanel.add(northPanel, BorderLayout.NORTH);

        // Create commands preview section
        commandsDocument = EditorFactory.getInstance().createDocument(getCommandsText());
        commands = Utils.createPreviewEditor(commandsDocument, project, true);

        JPanel commandsPanel = new JPanel(new BorderLayout());
        JLabel commandsLabel = new JBLabel(IgnoreLocalize.dialogUntrackfilesCommandsLabel().get());
        commandsLabel.setBorder(JBUI.Borders.empty(10, 0));
        commandsPanel.add(commandsLabel, BorderLayout.NORTH);

        JComponent commandsComponent = commands.getComponent();
        commandsComponent.setPreferredSize(new Dimension(0, 200));
        commandsPanel.add(commandsComponent, BorderLayout.CENTER);
        centerPanel.add(commandsPanel, BorderLayout.SOUTH);

        return centerPanel;
    }

    /**
     * Creates scroll panel with templates tree in it.
     *
     * @return scroll panel
     */
    private JScrollPane createTreeScrollPanel() {
        for (Map.Entry<VirtualFile, VcsRoot> entry : files.entrySet()) {
            createDirectoryNodes(entry.getKey(), entry.getValue());
        }

        FileTreeRenderer renderer = new FileTreeRenderer();

        tree = new CheckboxTree(renderer, root);
        tree.setCellRenderer(renderer);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(false);
        UIUtil.setLineStyleAngled(tree);
        TreeUtil.installActions(tree);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(tree);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        TreeUtil.expandAll(tree);

        tree.getModel().addTreeModelListener(treeModelListener);
        treeExpander = new DefaultTreeExpander(tree);

        return scrollPane;
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

        ActionToolbar actionToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, actions, true);
        actionToolbar.setTargetComponent(target);

        return actionToolbar;
    }

    /**
     * This method is invoked by default implementation of "OK" action. It just closes dialog with
     * <code>OK_EXIT_CODE</code>. This is convenient place to override functionality of "OK" action.
     * Note that the method does nothing if "OK" action isn't enabled.
     */
    @Override
    protected void doOKAction() {
        super.doOKAction();

        HashMap<VcsRoot, ArrayList<VirtualFile>> checked = getCheckedFiles();
        for (Map.Entry<VcsRoot, ArrayList<VirtualFile>> entry : checked.entrySet()) {
            for (VirtualFile file : entry.getValue()) {
                ExternalExec.removeFileFromTracking(file, entry.getKey());
            }
        }

        project.getMessageBus().syncPublisher(TRACKED_IGNORED_REFRESH).refresh();
    }

    /**
     * Returns structured map of selected {@link VirtualFile} list sorted by {@link VcsRoot}.
     *
     * @return sorted files map
     */
    @Nonnull
    private HashMap<VcsRoot, ArrayList<VirtualFile>> getCheckedFiles() {
        HashMap<VcsRoot, ArrayList<VirtualFile>> result = new HashMap<>();

        FileTreeNode leaf = (FileTreeNode) root.getFirstLeaf();
        if (leaf == null) {
            return result;
        }

        do {
            if (!leaf.isChecked()) {
                continue;
            }

            VcsRoot vcsRoot = leaf.getVcsRoot();
            VirtualFile file = leaf.getFile();
            if (vcsRoot == null) {
                continue;
            }

            ArrayList<VirtualFile> list = result.computeIfAbsent(vcsRoot, c -> new ArrayList<>());
            list.add(file);

            result.put(vcsRoot, list);
        } while ((leaf = (FileTreeNode) leaf.getNextLeaf()) != null);

        return result;
    }

    /**
     * Returns ready to present commands list.
     *
     * @return commands list
     */
    @Nonnull
    private String getCommandsText() {
        StringBuilder builder = new StringBuilder();

        HashMap<VcsRoot, ArrayList<VirtualFile>> checked = getCheckedFiles();
        for (Map.Entry<VcsRoot, ArrayList<VirtualFile>> entry : checked.entrySet()) {
            VirtualFile root = entry.getKey().getPath();
            if (root == null) {
                continue;
            }

            builder.append(IgnoreLocalize.dialogUntrackfilesCommandsRepository(root.getCanonicalPath())).append("\n");

            for (VirtualFile file : entry.getValue()) {
                builder.append(IgnoreLocalize.dialogUntrackfilesCommandsCommand(Utils.getRelativePath(root, file))).append("\n");
            }

            builder.append("\n");
        }
        return builder.toString();
    }

    /** Disposes current preview {@link #commands}. */
    @Override
    public void dispose() {
        super.dispose();
        tree.getModel().removeTreeModelListener(treeModelListener);
        if (!commands.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(commands);
        }
    }
}
