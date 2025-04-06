package mobi.hsz.idea.gitignore.outer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import mobi.hsz.idea.gitignore.file.type.IgnoreFileType;
import mobi.hsz.idea.gitignore.lang.IgnoreLanguage;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener for ignore editor manager.
 */
@TopicImpl(ComponentScope.PROJECT)
public class IgnoreEditorManagerListener implements FileEditorManagerListener {
    private final Project project;

    @Inject
    public IgnoreEditorManagerListener(@Nonnull Project project) {
        this.project = project;
    }

    /**
     * Handles file opening event and attaches outer ignore component.
     *
     * @param source editor manager
     * @param file   current file
     */
    @Override
    public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        FileType fileType = file.getFileType();
        if (!(fileType instanceof IgnoreFileType) || !IgnoreSettings.getInstance().isOuterIgnoreRules()) {
            return;
        }

        IgnoreLanguage language = ((IgnoreFileType)fileType).getIgnoreLanguage();
        if (!language.isEnabled()) {
            return;
        }

        DumbService.getInstance(project).runWhenSmart(() -> {
            List<VirtualFile> outerFiles = new ArrayList<>(language.getOuterFiles(project, false));
            if (outerFiles.isEmpty() || outerFiles.contains(file)) {
                return;
            }

            for (FileEditor fileEditor : source.getEditors(file)) {
                if (!(fileEditor instanceof TextEditor)) {
                    return;
                }
                OuterIgnoreWrapper wrapper = new OuterIgnoreWrapper(project, language, outerFiles);
                JComponent component = wrapper.getComponent();
                IgnoreSettings.Listener settingsListener = (key, value) -> {
                    if (IgnoreSettings.KEY.OUTER_IGNORE_RULES.equals(key)) {
                        component.setVisible((Boolean)value);
                    }
                };

                IgnoreSettings.getInstance().addListener(settingsListener);
                source.addBottomComponent(fileEditor, component);

                Disposer.register(fileEditor, wrapper);
                Disposer.register(
                    fileEditor,
                    () -> {
                        IgnoreSettings.getInstance().removeListener(settingsListener);
                        source.removeBottomComponent(fileEditor, component);
                    }
                );
            }
        });
    }
}
