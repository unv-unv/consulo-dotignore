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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserFactory;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.JBTable;
import consulo.undoRedo.CommandProcessor;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWrapper;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mobi.hsz.idea.gitignore.IgnoreBundle;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import mobi.hsz.idea.gitignore.util.Constants;
import mobi.hsz.idea.gitignore.util.Utils;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;

import static mobi.hsz.idea.gitignore.settings.IgnoreSettings.IgnoreLanguagesSettings.KEY.ENABLE;
import static mobi.hsz.idea.gitignore.settings.IgnoreSettings.IgnoreLanguagesSettings.KEY.NEW_FILE;

/**
 * UI form for {@link IgnoreSettings} edition.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.6.1
 */
public class IgnoreSettingsPanel implements Disposable {
    /** The parent panel for the form. */
    public JPanel panel;

    /** Form element for {@link IgnoreSettings#missingGitignore}. */
    private JCheckBox missingGitignore;

    /** Templates list panel. */
    private TemplatesListPanel templatesListPanel;

    /** Enable ignored file status coloring. */
    private JCheckBox ignoredFileStatus;

    /** Enable outer ignore rules. */
    private JCheckBox outerIgnoreRules;

    /** Defines if new content should be inserted at the cursor's position or at the document end. */
    private JCheckBox insertAtCursor;

    /** Suggest to add unversioned files to the .gitignore file. */
    private JCheckBox addUnversionedFiles;

    /** Splitter element. */
    private Splitter templatesSplitter;

    /** File types scroll panel with table. */
    private JScrollPane languagesPanel;

    /** {@link IgnoreLanguage} settings table. */
    private JBTable languagesTable;

    /** Enable unignore files group. */
    public JCheckBox unignoreFiles;

    /** Inform about ignored files that are still tracked. */
    public JCheckBox informTrackedIgnored;

    /** Inform about editing ignored file. */
    private JCheckBox notifyIgnoredEditing;

    /** Editor panel element. */
    private EditorPanel editorPanel;

    public IgnoreSettingsPanel() {
        createUIComponents();

        $$$setupUI$$$();
    }

    /** Create UI components. */
    private void createUIComponents() {
        templatesListPanel = new TemplatesListPanel();
        editorPanel = new EditorPanel();
        editorPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));

        templatesSplitter = new Splitter(false, 0.3f);
        templatesSplitter.setFirstComponent(templatesListPanel);
        templatesSplitter.setSecondComponent(editorPanel);

        languagesTable = new JBTable();
        languagesTable.setModel(new LanguagesTableModel());
        languagesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        languagesTable.setColumnSelectionAllowed(false);
        languagesTable.setRowHeight(22);
        languagesTable.setPreferredScrollableViewportSize(new Dimension(
            -1,
            languagesTable.getRowHeight() * IgnoreBundle.LANGUAGES.size() / 2
        ));

        languagesTable.setStriped(true);
        languagesTable.setShowGrid(false);
        languagesTable.setBorder(JBUI.Borders.empty());
        languagesTable.setDragEnabled(false);

        languagesPanel = ScrollPaneFactory.createScrollPane(languagesTable);
    }

    private void $$$setupUI$$$() {
        panel = new JPanel();
        panel.setLayout(new GridLayoutManager(3, 1, JBUI.emptyInsets(), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 1, JBUI.emptyInsets(), -1, -1));
        panel1.setEnabled(true);
        panel.add(
            panel1,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_NORTH,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                true
            )
        );
        panel1.setBorder(BorderFactory.createTitledBorder(IgnoreLocalize.settingsGeneral().get()));
        missingGitignore = new JCheckBox();
        this.$$$loadButtonText$$$(
            missingGitignore,
            IgnoreLocalize.settingsGeneralMissinggitignore().get()
        );
        panel1.add(
            missingGitignore,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        outerIgnoreRules = new JCheckBox();
        this.$$$loadButtonText$$$(
            outerIgnoreRules,
            IgnoreLocalize.settingsGeneralOuterignorerules().get()
        );
        panel1.add(
            outerIgnoreRules,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        ignoredFileStatus = new JCheckBox();
        this.$$$loadButtonText$$$(
            ignoredFileStatus,
            IgnoreLocalize.settingsGeneralIgnoredfilestatus().get()
        );
        panel1.add(
            ignoredFileStatus,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        insertAtCursor = new JCheckBox();
        this.$$$loadButtonText$$$(
            insertAtCursor,
            IgnoreLocalize.settingsGeneralInsertatcursor().get()
        );
        panel1.add(
            insertAtCursor,
            new GridConstraints(
                3,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        addUnversionedFiles = new JCheckBox();
        this.$$$loadButtonText$$$(
            addUnversionedFiles,
            IgnoreLocalize.settingsGeneralAddunversionedfiles().get()
        );
        panel1.add(
            addUnversionedFiles,
            new GridConstraints(
                4,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        unignoreFiles = new JCheckBox();
        this.$$$loadButtonText$$$(
            unignoreFiles,
            IgnoreLocalize.settingsGeneralUnignorefiles().get()
        );
        panel1.add(
            unignoreFiles,
            new GridConstraints(
                5,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        informTrackedIgnored = new JCheckBox();
        this.$$$loadButtonText$$$(
            informTrackedIgnored,
            IgnoreLocalize.settingsGeneralInformtrackedignored().get()
        );
        panel1.add(
            informTrackedIgnored,
            new GridConstraints(
                6,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        notifyIgnoredEditing = new JCheckBox();
        this.$$$loadButtonText$$$(
            notifyIgnoredEditing,
            IgnoreLocalize.settingsGeneralNotifyignoredediting().get()
        );
        panel1.add(
            notifyIgnoredEditing,
            new GridConstraints(
                7,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        panel.add(
            panel2,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_NORTH,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        panel2.setBorder(BorderFactory.createTitledBorder(IgnoreLocalize.settingsUsertemplates().get()));
        panel2.add(
            templatesSplitter,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_NORTH,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        panel.add(
            panel3,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_NORTH,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        panel3.setBorder(BorderFactory.createTitledBorder(IgnoreLocalize.settingsLanguagessettings().get()));
        panel3.add(
            languagesPanel,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_NORTH,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel;
    }

    /** Disposes current preview {@link #editorPanel}. */
    @Override
    public void dispose() {
        if (!editorPanel.preview.isDisposed()) {
            EditorFactory.getInstance().releaseEditor(editorPanel.preview);
        }
    }

    /**
     * Returns value of @{link {@link #missingGitignore}} field.
     *
     * @return {@link #missingGitignore} is selected
     */
    public boolean isMissingGitignore() {
        return missingGitignore.isSelected();
    }

    /**
     * Sets value of {@link #missingGitignore} field.
     *
     * @param selected value for {@link #missingGitignore}
     */
    public void setMissingGitignore(boolean selected) {
        this.missingGitignore.setSelected(selected);
    }

    /**
     * Returns value of @{link {@link #ignoredFileStatus}} field.
     *
     * @return {@link #ignoredFileStatus} is selected
     */
    public boolean isIgnoredFileStatus() {
        return ignoredFileStatus.isSelected();
    }

    /**
     * Sets value of {@link #ignoredFileStatus} field.
     *
     * @param selected value for {@link #ignoredFileStatus}
     */
    public void setIgnoredFileStatus(boolean selected) {
        this.ignoredFileStatus.setSelected(selected);
    }

    /**
     * Returns {@link IgnoreSettings.UserTemplate} list of {@link #templatesListPanel}.
     *
     * @return {@link IgnoreSettings.UserTemplate} list
     */
    @Nonnull
    public List<IgnoreSettings.UserTemplate> getUserTemplates() {
        return this.templatesListPanel.getList();
    }

    /**
     * Sets new {@link IgnoreSettings.UserTemplate} list to {@link #templatesListPanel}.
     *
     * @param userTemplates {@link IgnoreSettings.UserTemplate} list
     */
    public void setUserTemplates(@Nonnull List<IgnoreSettings.UserTemplate> userTemplates) {
        this.templatesListPanel.resetForm(userTemplates);
    }

    /**
     * Returns value of @{link {@link #outerIgnoreRules}} field.
     *
     * @return {@link #outerIgnoreRules} is selected
     */
    public boolean isOuterIgnoreRules() {
        return outerIgnoreRules.isSelected();
    }

    /**
     * Sets value of {@link #outerIgnoreRules} field.
     *
     * @param selected value for {@link #outerIgnoreRules}
     */
    public void setOuterIgnoreRules(boolean selected) {
        this.outerIgnoreRules.setSelected(selected);
    }

    /**
     * Returns value of @{link {@link #insertAtCursor}} field.
     *
     * @return {@link #insertAtCursor} is selected
     */
    public boolean isInsertAtCursor() {
        return insertAtCursor.isSelected();
    }

    /**
     * Sets value of {@link #insertAtCursor} field.
     *
     * @param selected value for {@link #insertAtCursor}
     */
    public void setInsertAtCursor(boolean selected) {
        this.insertAtCursor.setSelected(selected);
    }

    /**
     * Returns value of @{link {@link #addUnversionedFiles}} field.
     *
     * @return {@link #addUnversionedFiles} is selected
     */
    public boolean isAddUnversionedFiles() {
        return addUnversionedFiles.isSelected();
    }

    /**
     * Sets value of {@link #addUnversionedFiles} field.
     *
     * @param selected value for {@link #addUnversionedFiles}
     */
    public void setAddUnversionedFiles(boolean selected) {
        this.addUnversionedFiles.setSelected(selected);
    }

    /**
     * Returns value of @{link {@link #unignoreFiles}} field.
     *
     * @return {@link #unignoreFiles} is selected
     */
    public boolean isUnignoreActions() {
        return unignoreFiles.isSelected();
    }

    /**
     * Sets value of {@link #unignoreFiles} field.
     *
     * @param selected value for {@link #unignoreFiles}
     */
    public void setUnignoreActions(boolean selected) {
        this.unignoreFiles.setSelected(selected);
    }

    /**
     * Returns value of @{link {@link #informTrackedIgnored}} field.
     *
     * @return {@link #informTrackedIgnored} is selected
     */
    public boolean isInformTrackedIgnored() {
        return informTrackedIgnored.isSelected();
    }

    /**
     * Sets value of {@link #informTrackedIgnored} field.
     *
     * @param selected value for {@link #informTrackedIgnored}
     */
    public void setInformTrackedIgnored(boolean selected) {
        this.informTrackedIgnored.setSelected(selected);
    }

    /**
     * Returns value of @{link {@link #notifyIgnoredEditing}} field.
     *
     * @return {@link #notifyIgnoredEditing} is selected
     */
    public boolean isNotifyIgnoredEditing() {
        return notifyIgnoredEditing.isSelected();
    }

    /**
     * Sets value of {@link #notifyIgnoredEditing} field.
     *
     * @param selected value for {@link #notifyIgnoredEditing}
     */
    public void setNotifyIgnoredEditing(boolean selected) {
        this.notifyIgnoredEditing.setSelected(selected);
    }

    /**
     * Returns model of {@link #languagesTable}.
     *
     * @return {@link #languagesTable} model
     */
    public LanguagesTableModel getLanguagesSettings() {
        return (LanguagesTableModel)this.languagesTable.getModel();
    }

    /** Extension for the CRUD list panel. */
    public class TemplatesListPanel extends AddEditDeleteListPanel<IgnoreSettings.UserTemplate> {
        /** Import/export file's extension. */
        private static final String FILE_EXTENSION = "xml";

        /** Constructs CRUD panel with list listener for editor updating. */
        public TemplatesListPanel() {
            super(null, new ArrayList<>());
            myList.addListSelectionListener(e -> {
                boolean enabled = myListModel.size() > 0;
                editorPanel.setEnabled(enabled);

                if (enabled) {
                    IgnoreSettings.UserTemplate template = getCurrentItem();
                    editorPanel.setContent(template != null ? template.getContent() : "");
                }
            });
        }

        /**
         * Customizes Import dialog.
         *
         * @param decorator toolbar
         */
        @Override
        protected void customizeDecorator(ToolbarDecorator decorator) {
            super.customizeDecorator(decorator);

            DefaultActionGroup group = new DefaultActionGroup();
            group.addSeparator();

            group.add(new AnAction(
                IgnoreLocalize.actionImporttemplates(),
                IgnoreLocalize.actionImporttemplatesDescription(),
                PlatformIconGroup.actionsInstall()
            ) {
                @Override
                @RequiredUIAccess
                @SuppressWarnings("unchecked")
                public void actionPerformed(@Nonnull AnActionEvent event) {
                    FileChooserDescriptor descriptor =
                        new FileChooserDescriptor(true, false, true, false, true, false) {
                            @Override
                            public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                                return super.isFileVisible(file, showHiddenFiles)
                                    && (file.isDirectory() || FILE_EXTENSION.equals(file.getExtension())
                                    || file.getFileType() instanceof ArchiveFileType);
                            }

                            @Override
                            @RequiredUIAccess
                            public boolean isFileSelectable(VirtualFile file) {
                                return file.getExtension().endsWith("xml");
                            }
                        };
                    descriptor.withDescriptionValue(IgnoreLocalize.actionImporttemplatesWrapperDescription());
                    descriptor.withTitleValue(IgnoreLocalize.actionImporttemplatesWrapper());
                    descriptor.putUserData(
                        LangDataKeys.MODULE_CONTEXT,
                        event.getData(LangDataKeys.MODULE)
                    );

                    VirtualFile file = IdeaFileChooser.chooseFile(descriptor, templatesListPanel, null, null);
                    if (file != null) {
                        try {
                            org.jdom.Document document = JDOMUtil.loadDocument(file.getInputStream());
                            Element element = document.getRootElement();
                            List<IgnoreSettings.UserTemplate> templates = IgnoreSettings.loadTemplates(element);
                            for (IgnoreSettings.UserTemplate template : templates) {
                                myListModel.addElement(template);
                            }
                            Messages.showInfoMessage(
                                templatesListPanel,
                                IgnoreLocalize.actionImporttemplatesSuccess(templates.size()).get(),
                                IgnoreLocalize.actionExporttemplatesSuccessTitle().get()
                            );
                            return;
                        }
                        catch (IOException | JDOMException e) {
                            e.printStackTrace();
                        }
                    }

                    Messages.showErrorDialog(templatesListPanel, IgnoreLocalize.actionImporttemplatesError().get());
                }
            });

            group.add(new AnAction(
                IgnoreLocalize.actionExporttemplates(),
                IgnoreLocalize.actionExporttemplatesDescription(),
                PlatformIconGroup.actionsExport()
            ) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent event) {
                    VirtualFileWrapper wrapper = FileChooserFactory.getInstance().createSaveFileDialog(
                        new FileSaverDescriptor(
                            IgnoreLocalize.actionExporttemplatesWrapper().get(),
                            LocalizeValue.empty().get(),
                            FILE_EXTENSION
                        ),
                        templatesListPanel
                    ).save(null, null);

                    if (wrapper != null) {
                        List<IgnoreSettings.UserTemplate> items = getCurrentItems();
                        org.jdom.Document document = new org.jdom.Document(
                            IgnoreSettings.createTemplatesElement(items)
                        );
                        try {
                            JDOMUtil.writeDocument(document, wrapper.getFile(), Constants.NEWLINE);
                            Messages.showInfoMessage(
                                templatesListPanel,
                                IgnoreLocalize.actionExporttemplatesSuccess(items.size()).get(),
                                IgnoreLocalize.actionExporttemplatesSuccessTitle().get()
                            );
                        }
                        catch (IOException e) {
                            Messages.showErrorDialog(
                                templatesListPanel,
                                IgnoreLocalize.actionExporttemplatesError().get()
                            );
                        }
                    }
                }

                @Override
                @RequiredUIAccess
                public void update(@Nonnull AnActionEvent e) {
                    e.getPresentation().setEnabled(getCurrentItems().size() > 0);
                }
            });
            decorator.setActionGroup(group);
        }

        /**
         * Opens edit dialog for new template.
         *
         * @return template
         */
        @Nullable
        @Override
        @RequiredUIAccess
        protected IgnoreSettings.UserTemplate findItemToAdd() {
            return showEditDialog(new IgnoreSettings.UserTemplate());
        }

        /**
         * Shows edit dialog and validates user's input name.
         *
         * @param initialValue template
         * @return modified template
         */
        @Nullable
        @RequiredUIAccess
        private IgnoreSettings.UserTemplate showEditDialog(@Nonnull IgnoreSettings.UserTemplate initialValue) {
            String name = Messages.showInputDialog(
                this,
                IgnoreLocalize.settingsUsertemplatesDialogdescription().get(),
                IgnoreLocalize.settingsUsertemplatesDialogtitle().get(),
                UIUtil.getQuestionIcon(),
                initialValue.getName(),
                new InputValidatorEx() {
                    /**
                     * Checks whether the <code>inputString</code> is valid. It is invoked each time
                     * input changes.
                     *
                     * @param inputString the input to check
                     * @return true if input string is valid
                     */
                    @Override
                    @RequiredUIAccess
                    public boolean checkInput(String inputString) {
                        return !StringUtil.isEmpty(inputString);
                    }

                    /**
                     * This method is invoked just before message dialog is closed with OK code.
                     * If <code>false</code> is returned then then the message dialog will not be closed.
                     *
                     * @param inputString the input to check
                     * @return true if the dialog could be closed, false otherwise.
                     */
                    @Override
                    @RequiredUIAccess
                    public boolean canClose(String inputString) {
                        return !StringUtil.isEmpty(inputString);
                    }

                    /**
                     * Returns error message depending on the input string.
                     *
                     * @param inputString the input to check
                     * @return error text
                     */
                    @Nullable
                    @Override
                    @RequiredUIAccess
                    public String getErrorText(String inputString) {
                        return !checkInput(inputString) ? IgnoreLocalize.settingsUsertemplatesDialogerror().get() : null;
                    }
                }
            );

            if (name != null) {
                initialValue.setName(name);
            }
            return initialValue.isEmpty() ? null : initialValue;
        }

        /**
         * Fills list element with given templates list.
         *
         * @param userTemplates templates list
         */
        @SuppressWarnings("unchecked")
        public void resetForm(@Nonnull List<IgnoreSettings.UserTemplate> userTemplates) {
            myListModel.clear();
            for (IgnoreSettings.UserTemplate template : userTemplates) {
                myListModel.addElement(new IgnoreSettings.UserTemplate(template.getName(), template.getContent()));
            }
        }

        /**
         * Edits given template.
         *
         * @param item template
         * @return modified template
         */
        @Override
        @RequiredUIAccess
        protected IgnoreSettings.UserTemplate editSelectedItem(@Nonnull IgnoreSettings.UserTemplate item) {
            return showEditDialog(item);
        }

        /**
         * Returns current templates list.
         *
         * @return templates list
         */
        public List<IgnoreSettings.UserTemplate> getList() {
            ArrayList<IgnoreSettings.UserTemplate> list = new ArrayList<>();
            for (int i = 0; i < myListModel.size(); i++) {
                list.add(myListModel.getElementAt(i));
            }
            return list;
        }

        /**
         * Updates editor component with given content.
         *
         * @param content new content
         */
        public void updateContent(String content) {
            IgnoreSettings.UserTemplate template = getCurrentItem();
            if (template != null) {
                template.setContent(content);
            }
        }

        /**
         * Returns currently selected template.
         *
         * @return template or null if none selected
         */
        @Nullable
        public IgnoreSettings.UserTemplate getCurrentItem() {
            int index = myList.getSelectedIndex();
            if (index == -1) {
                return null;
            }
            return myListModel.get(index);
        }

        /**
         * Returns selected {@link IgnoreSettings.UserTemplate} elements.
         *
         * @return {@link IgnoreSettings.UserTemplate} list
         */
        public List<IgnoreSettings.UserTemplate> getCurrentItems() {
            List<IgnoreSettings.UserTemplate> list = new ArrayList<>();
            int[] ids = myList.getSelectedIndices();
            for (int i = 0; i < ids.length; i++) {
                list.add(getList().get(i));
            }
            return list;
        }
    }

    /** Editor panel class that displays document editor or label if no template is selected. */
    private class EditorPanel extends JPanel {
        /** Preview editor. */
        private final Editor preview;

        /** `No templates is selected` label. */
        private final JBLabel label;

        /** Preview document. */
        private final Document previewDocument;

        /** Constructor that creates document editor, empty content label. */
        public EditorPanel() {
            super(new BorderLayout());
            this.previewDocument = EditorFactory.getInstance().createDocument("");
            this.label = new JBLabel(IgnoreLocalize.settingsUsertemplatesNotemplateselected().get(), JBLabel.CENTER);
            this.preview = Utils.createPreviewEditor(previewDocument, null, false);
            this.preview.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void beforeDocumentChange(@Nonnull DocumentEvent event) {
                }

                @Override
                public void documentChanged(@Nonnull DocumentEvent event) {
                    templatesListPanel.updateContent(event.getDocument().getText());
                }
            });

            setEnabled(false);
        }

        /**
         * Shows or hides label and editor.
         *
         * @param enabled if true shows editor, else shows label
         */
        @Override
        public void setEnabled(boolean enabled) {
            if (enabled) {
                remove(this.label);
                add(this.preview.getComponent());
            }
            else {
                add(this.label);
                remove(this.preview.getComponent());
            }
            revalidate();
            repaint();
        }

        /**
         * Sets new content to the editor component.
         *
         * @param content new content
         */
        @RequiredUIAccess
        public void setContent(@Nonnull String content) {
            Application.get().runWriteAction(
                () -> CommandProcessor.getInstance().runUndoTransparentAction(
                    () -> previewDocument.replaceString(0, previewDocument.getTextLength(), content)
                )
            );
        }
    }

    /** Languages table helper class. */
    public static class LanguagesTableModel extends AbstractTableModel {
        /** Languages settings instance. */
        private final IgnoreSettings.IgnoreLanguagesSettings settings = new IgnoreSettings.IgnoreLanguagesSettings();

        /** Table's columns names. */
        private final LocalizeValue[] columnNames = new LocalizeValue[]{
            IgnoreLocalize.settingsLanguagessettingsTableName(),
            IgnoreLocalize.settingsLanguagessettingsTableNewfile(),
            IgnoreLocalize.settingsLanguagessettingsTableEnable()
        };

        /** Table's columns classes. */
        private final Class[] columnClasses = new Class[]{
            String.class, Boolean.class, Boolean.class
        };

        /**
         * Returns the number of rows in this data table.
         *
         * @return the number of rows in the model
         */
        @Override
        public int getRowCount() {
            return settings.size();
        }

        /**
         * Returns the number of columns in this data table.
         *
         * @return the number of columns in the model
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Returns a default name for the column using spreadsheet conventions:
         * A, B, C, ... Z, AA, AB, etc.  If <code>column</code> cannot be found,
         * returns an empty string.
         *
         * @param column the column being queried
         * @return a string containing the default name of <code>column</code>
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column].get();
        }

        /**
         * Returns <code>Object.class</code> regardless of <code>columnIndex</code>.
         *
         * @param columnIndex the column being queried
         * @return the Object.class
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        /**
         * Returns true regardless of parameter values.
         *
         * @param row    the row whose value is to be queried
         * @param column the column whose value is to be queried
         * @return true
         * @see #setValueAt
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return column != 0;
        }

        /**
         * Returns an attribute value for the cell at <code>row</code>
         * and <code>column</code>.
         *
         * @param row    the row whose value is to be queried
         * @param column the column whose value is to be queried
         * @return the value Object at the specified cell
         * @throws ArrayIndexOutOfBoundsException if an invalid row or column was given
         */
        @Override
        public Object getValueAt(int row, int column) {
            IgnoreLanguage language = new ArrayList<>(settings.keySet()).get(row);
            if (language == null) {
                return null;
            }
            TreeMap<IgnoreSettings.IgnoreLanguagesSettings.KEY, Object> data = settings.get(language);

            switch (column) {
                case 0:
                    return language.getID();
                case 1:
                    return Boolean.valueOf(data.get(NEW_FILE).toString());
                case 2:
                    return Boolean.valueOf(data.get(ENABLE).toString());
            }

            throw new IllegalArgumentException();
        }

        /**
         * This empty implementation is provided so users don't have to implement
         * this method if their data model is not editable.
         *
         * @param value  value to assign to cell
         * @param row    row of cell
         * @param column column of cell
         */
        @Override
        public void setValueAt(Object value, int row, int column) {
            IgnoreLanguage language = new ArrayList<>(settings.keySet()).get(row);
            TreeMap<IgnoreSettings.IgnoreLanguagesSettings.KEY, Object> data = settings.get(language);

            switch (column) {
                case 1:
                    data.put(NEW_FILE, value);
                    return;
                case 2:
                    data.put(ENABLE, value);
                    return;
            }

            throw new IllegalArgumentException();
        }

        /**
         * Returns current settings.
         *
         * @return settings
         */
        public IgnoreSettings.IgnoreLanguagesSettings getSettings() {
            return settings;
        }

        /**
         * Update settings model.
         *
         * @param settings to update
         */
        public void update(@Nonnull IgnoreSettings.IgnoreLanguagesSettings settings) {
            this.settings.clear();
            this.settings.putAll(settings);
        }

        /**
         * Checks if current settings are equal to the given one.
         *
         * @param settings to check
         * @return equals
         */
        public boolean equalSettings(IgnoreSettings.IgnoreLanguagesSettings settings) {
            return this.settings.equals(settings);
        }
    }
}
