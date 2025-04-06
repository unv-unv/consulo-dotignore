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

package mobi.hsz.idea.gitignore.outer;

import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.util.Utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper that creates bottom editor component for displaying outer ignore rules.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 1.1
 */
public class OuterIgnoreWrapper extends MouseAdapter implements ChangeListener, Disposable {
    /** Pixels offset to handle drag event. */
    private static final int DRAG_OFFSET = 10;

    /** Maximum height of the component to avoid losing it from view. */
    private static final int MAX_HEIGHT = 300;

    /** Main wrapper panel. */
    private final JPanel panel;

    /** List of outer editors in the wrapper. */
    @Nonnull
    private final List<Editor> outerEditors = new ArrayList<>();

    /** The settings storage object. */
    @Nonnull
    private final IgnoreSettings settings;

    /** North panel. */
    @Nonnull
    private final JPanel northPanel;

    /** Panel wrapper. */
    @Nonnull
    private final TabbedPaneWrapper tabbedPanel;

    /** Message bus instance. */
    @Nullable
    private MessageBusConnection messageBus;

    /** Link label instance. */
    @Nonnull
    private final LinkLabel linkLabel;

    /** List of all available outer files. */
    @Nonnull
    private final List<VirtualFile> outerFiles;

    /** Current panel's height. */
    private int dragPanelHeight;

    /** Y position of the drag event. */
    private int dragYOnScreen;

    /** Obtains if it's in drag mode. */
    private boolean drag;

    /** Constructor. */
    @SuppressWarnings("unchecked")
    public OuterIgnoreWrapper(
        @Nonnull Project project,
        @Nonnull IgnoreLanguage language,
        @Nonnull List<VirtualFile> outerFiles
    ) {
        this.outerFiles = outerFiles;
        settings = IgnoreSettings.getInstance();

        panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));

        northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));

        JBLabel label = new JBLabel(
            IgnoreLocalize.outerLabel().get(),
            UIUtil.ComponentStyle.REGULAR,
            UIUtil.FontColor.BRIGHTER
        );
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        northPanel.add(label);

        tabbedPanel = new TabbedPaneWrapper(project);
        messageBus = project.getMessageBus().connect();
        messageBus.subscribe(UISettingsListener.class, uiSettings -> updateTabbedPanelPolicy());
        updateTabbedPanelPolicy();

        linkLabel = new LinkLabel(
            outerFiles.get(0).getPath(),
            null,
            (aSource, aLinkData) -> Utils.openFile(project, outerFiles.get(tabbedPanel.getSelectedIndex()))
        );
        VirtualFile userHomeDir = VirtualFileUtil.getUserHomeDir();

        for (VirtualFile outerFile : outerFiles) {
            Document document = FileDocumentManager.getInstance().getDocument(outerFile);
            Editor outerEditor = document != null ? Utils.createPreviewEditor(document, null, true) : null;

            if (outerEditor != null) {
                JScrollPane scrollPanel = ScrollPaneFactory.createScrollPane(outerEditor.getComponent());
                scrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                scrollPanel.setPreferredSize(new Dimension(0, settings.getOuterIgnoreWrapperHeight()));

                String path = outerFile.getPath();
                if (userHomeDir != null) {
                    path = path.replace(userHomeDir.getPath(), "~");
                }

                tabbedPanel.addTab(path, language.getIcon(), scrollPanel, outerFile.getCanonicalPath());
                outerEditors.add(outerEditor);
            }
        }

        northPanel.addMouseListener(this);
        northPanel.addMouseMotionListener(this);
        tabbedPanel.addChangeListener(this);

        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(tabbedPanel.getComponent(), BorderLayout.CENTER);
        panel.add(linkLabel, BorderLayout.SOUTH);
    }

    /** Updates tabbedPanel policy depending on UISettings#getScrollTabLayoutInEditor() settings. */
    private void updateTabbedPanelPolicy() {
        if (UISettings.getInstance().getScrollTabLayoutInEditor()) {
            tabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        }
        else {
            tabbedPanel.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getPoint().getY() <= DRAG_OFFSET) {
            dragPanelHeight = tabbedPanel.getComponent().getHeight();
            dragYOnScreen = e.getYOnScreen();
            drag = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        drag = false;
        settings.setOuterIgnoreWrapperHeight(tabbedPanel.getComponent().getHeight());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Cursor cursor = (e.getPoint().getY() <= DRAG_OFFSET)
            ? Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
            : Cursor.getDefaultCursor();
        panel.setCursor(cursor);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (drag) {
            int height = dragPanelHeight - e.getYOnScreen() + dragYOnScreen;
            if (height > MAX_HEIGHT) {
                height = MAX_HEIGHT;
            }
            tabbedPanel.getComponent().setPreferredSize(new Dimension(0, height));
            panel.revalidate();
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        linkLabel.setText(outerFiles.get(tabbedPanel.getSelectedIndex()).getPath());
    }

    /**
     * Returns outer panel.
     *
     * @return outer panel
     */
    public JComponent getComponent() {
        return panel;
    }

    /** Disposes all outer editors stored in {@link #outerEditors}. */
    @Override
    public void dispose() {
        northPanel.removeMouseListener(this);
        northPanel.removeMouseMotionListener(this);
        tabbedPanel.removeChangeListener(this);

        if (messageBus != null) {
            messageBus.disconnect();
            messageBus = null;
        }

        for (Editor outerEditor : outerEditors) {
            EditorFactory.getInstance().releaseEditor(outerEditor);
        }
    }
}
