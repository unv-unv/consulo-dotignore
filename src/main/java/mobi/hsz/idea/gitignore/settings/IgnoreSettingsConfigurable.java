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

package mobi.hsz.idea.gitignore.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.VcsConfigurableProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import mobi.hsz.idea.gitignore.ui.IgnoreSettingsPanel;
import mobi.hsz.idea.gitignore.util.Utils;

import javax.swing.*;

/**
 * Configuration interface for {@link IgnoreSettings}.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.6.1
 */
@ExtensionImpl
public class IgnoreSettingsConfigurable implements SearchableConfigurable, VcsConfigurableProvider {
    /** The settings storage object. */
    private final IgnoreSettings settings;

    /** The settings UI form. */
    private IgnoreSettingsPanel settingsPanel;

    @Inject
    public IgnoreSettingsConfigurable(IgnoreSettings settings) {
        this.settings = settings;
    }

    /**
     * Returns the user-visible name of the settings component.
     *
     * @return the visible name of the component {@link IgnoreSettingsConfigurable}
     */
    @Override
    public LocalizeValue getDisplayName() {
        return IgnoreLocalize.settingsDisplayname();
    }

    /**
     * Returns the topic in the help file which is shown when help for the configurable is requested.
     *
     * @return the help topic, or null if no help is available {@link #getDisplayName()}
     */
    @Nonnull
    @Override
    public String getHelpTopic() {
        return getId();
    }

    /**
     * Returns the user interface component for editing the configuration.
     *
     * @return the {@link IgnoreSettingsPanel} component instance
     */
    @Nullable
    @Override
    @RequiredUIAccess
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new IgnoreSettingsPanel();
        }
        reset();
        return settingsPanel.panel;
    }

    /**
     * Checks if the settings in the user interface component were modified by the user and need to be saved.
     *
     * @return true if the settings were modified, false otherwise.
     */
    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return settingsPanel == null
            || !Comparing.equal(settings.isMissingGitignore(), settingsPanel.isMissingGitignore())
            || !Utils.equalLists(settings.getUserTemplates(), settingsPanel.getUserTemplates())
            || !Comparing.equal(settings.isIgnoredFileStatus(), settingsPanel.isIgnoredFileStatus())
            || !Comparing.equal(settings.isOuterIgnoreRules(), settingsPanel.isOuterIgnoreRules())
            || !Comparing.equal(settings.isInsertAtCursor(), settingsPanel.isInsertAtCursor())
            || !Comparing.equal(settings.isAddUnversionedFiles(), settingsPanel.isAddUnversionedFiles())
            || !Comparing.equal(settings.isUnignoreActions(), settingsPanel.isUnignoreActions())
            || !Comparing.equal(settings.isInformTrackedIgnored(), settingsPanel.isInformTrackedIgnored())
            || !Comparing.equal(settings.isNotifyIgnoredEditing(), settingsPanel.isNotifyIgnoredEditing())
            || !settingsPanel.getLanguagesSettings().equalSettings(settings.getLanguagesSettings());
    }

    /** Store the settings from configurable to other components. */
    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }
        settings.setMissingGitignore(settingsPanel.isMissingGitignore());
        settings.setUserTemplates(settingsPanel.getUserTemplates());
        settings.setIgnoredFileStatus(settingsPanel.isIgnoredFileStatus());
        settings.setOuterIgnoreRules(settingsPanel.isOuterIgnoreRules());
        settings.setInsertAtCursor(settingsPanel.isInsertAtCursor());
        settings.setAddUnversionedFiles(settingsPanel.isAddUnversionedFiles());
        settings.setLanguagesSettings(settingsPanel.getLanguagesSettings().getSettings());
        settings.setUnignoreActions(settingsPanel.isUnignoreActions());
        settings.setInformTrackedIgnored(settingsPanel.isInformTrackedIgnored());
        settings.setNotifyIgnoredEditing(settingsPanel.isNotifyIgnoredEditing());
    }

    /** Load settings from other components to configurable. */
    @Override
    @RequiredUIAccess
    public void reset() {
        if (settingsPanel == null) {
            return;
        }
        settingsPanel.setMissingGitignore(settings.isMissingGitignore());
        settingsPanel.setUserTemplates(settings.getUserTemplates());
        settingsPanel.setIgnoredFileStatus(settings.isIgnoredFileStatus());
        settingsPanel.setOuterIgnoreRules(settings.isOuterIgnoreRules());
        settingsPanel.setInsertAtCursor(settings.isInsertAtCursor());
        settingsPanel.setAddUnversionedFiles(settings.isAddUnversionedFiles());
        settingsPanel.setUnignoreActions(settings.isUnignoreActions());
        settingsPanel.setInformTrackedIgnored(settings.isInformTrackedIgnored());
        settingsPanel.setNotifyIgnoredEditing(settings.isNotifyIgnoredEditing());

        IgnoreSettingsPanel.LanguagesTableModel model = settingsPanel.getLanguagesSettings();
        model.update(settings.getLanguagesSettings().clone());
    }

    /** Disposes the Swing components used for displaying the configuration. */
    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        if (settingsPanel != null) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }

    /**
     * Returns current {@link Configurable} instance.
     *
     * @param project ignored
     * @return current instance
     */
    @Nullable
    @Override
    public Configurable getConfigurable(Project project) {
        return this;
    }

    /**
     * Returns help topic as an ID.
     *
     * @return id
     */
    @Nonnull
    @Override
    public String getId() {
        return getHelpTopic();
    }

    /**
     * An action to perform when this configurable is opened.
     *
     * @param option setting search query
     * @return null
     */
    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }
}
