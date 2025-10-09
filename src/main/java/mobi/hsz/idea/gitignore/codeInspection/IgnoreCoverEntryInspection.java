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
package mobi.hsz.idea.gitignore.codeInspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.dotignore.codeInspection.IgnoreInspection;
import consulo.dotignore.localize.IgnoreLocalize;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Couple;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.*;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.IgnoreManager;
import mobi.hsz.idea.gitignore.psi.IgnoreEntry;
import mobi.hsz.idea.gitignore.psi.IgnoreFile;
import mobi.hsz.idea.gitignore.util.Constants;
import mobi.hsz.idea.gitignore.util.Glob;
import mobi.hsz.idea.gitignore.util.MatcherUtil;
import mobi.hsz.idea.gitignore.util.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Inspection tool that checks if entries are covered by others.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.5
 */
@ExtensionImpl
public class IgnoreCoverEntryInspection extends IgnoreInspection {
    /** Cache map to store handled entries' paths. */
    private final ConcurrentMap<String, Set<String>> cacheMap;

    /** {@link VirtualFileManager} instance. */
    private final VirtualFileManager virtualFileManager;

    /** Watches for the changes in the files tree and triggers the cache clear. */
    private final VirtualFileListener virtualFileListener = new VirtualFileListener() {
        @Override
        public void propertyChanged(@Nonnull VirtualFilePropertyEvent event) {
            if (event.getPropertyName().equals("name")) {
                cacheMap.clear();
            }
        }

        @Override
        public void fileCreated(@Nonnull VirtualFileEvent event) {
            cacheMap.clear();
        }

        @Override
        public void fileDeleted(@Nonnull VirtualFileEvent event) {
            cacheMap.clear();
        }

        @Override
        public void fileMoved(@Nonnull VirtualFileMoveEvent event) {
            cacheMap.clear();
        }

        @Override
        public void fileCopied(@Nonnull VirtualFileCopyEvent event) {
            cacheMap.clear();
        }
    };

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return IgnoreLocalize.codeinspectionCoverentry();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    /**
     * Builds a new instance of {@link IgnoreCoverEntryInspection}.
     * Initializes {@link VirtualFileManager} and listens for the changes in the files tree.
     */
    public IgnoreCoverEntryInspection() {
        cacheMap = ContainerUtil.newConcurrentMap();
        virtualFileManager = VirtualFileManager.getInstance();
        virtualFileManager.addVirtualFileListener(virtualFileListener);
    }

    /**
     * Unregisters {@link #virtualFileListener} and clears the paths cache.
     *
     * @param project current project
     */
    @Override
    public void cleanup(@Nonnull Project project) {
        virtualFileManager.removeVirtualFileListener(virtualFileListener);
        cacheMap.clear();
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(
        @Nonnull ProblemsHolder holder,
        boolean isOnTheFly,
        @Nonnull LocalInspectionToolSession session,
        @Nonnull Object state
    ) {
        PsiFile file = holder.getFile();
        VirtualFile virtualFile = file.getVirtualFile();
        if (!(file instanceof IgnoreFile) || !Utils.isInProject(virtualFile, file.getProject())) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitFile(PsiFile file) {
                if (file instanceof IgnoreFile ignoreFile) {
                    checkFile(holder, ignoreFile, isOnTheFly);
                }
            }
        };
    }

    /**
     * Reports problems at file level. Checks if entries are covered by other entries.
     */
    @RequiredReadAction
    private void checkFile(@Nonnull ProblemsHolder problemsHolder, @Nonnull IgnoreFile file, boolean isOnTheFly) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (!(file instanceof IgnoreFile) || !Utils.isInProject(virtualFile, file.getProject())) {
            return;
        }

        VirtualFile contextDirectory = virtualFile.getParent();
        if (contextDirectory == null) {
            return;
        }

        Set<String> ignored = new HashSet<>();
        Set<String> unignored = new HashSet<>();

        List<Couple<IgnoreEntry>> result = new ArrayList<>();
        Map<IgnoreEntry, Set<String>> map = new HashMap<>();

        ArrayList<IgnoreEntry> entries = new ArrayList<>(Arrays.asList(file.findChildrenByClass(IgnoreEntry.class)));
        MatcherUtil matcher = IgnoreManager.getInstance(file.getProject()).getMatcher();
        Map<IgnoreEntry, Set<String>> matchedMap = getPathsSet(contextDirectory, entries, matcher);

        for (IgnoreEntry entry : entries) {
            ProgressManager.checkCanceled();
            Set<String> matched = matchedMap.get(entry);
            Collection<String> intersection;
            boolean modified;

            if (!entry.isNegated()) {
                ignored.addAll(matched);
                intersection = ContainerUtil.intersection(unignored, matched);
                modified = unignored.removeAll(intersection);
            }
            else {
                unignored.addAll(matched);
                intersection = ContainerUtil.intersection(ignored, matched);
                modified = ignored.removeAll(intersection);
            }

            if (modified) {
                continue;
            }

            for (IgnoreEntry recent : map.keySet()) {
                ProgressManager.checkCanceled();
                Set<String> recentValues = map.get(recent);
                if (recentValues.isEmpty() || matched.isEmpty()) {
                    continue;
                }

                if (entry.isNegated() == recent.isNegated()) {
                    if (recentValues.containsAll(matched)) {
                        result.add(Couple.of(recent, entry));
                    }
                    else if (matched.containsAll(recentValues)) {
                        result.add(Couple.of(entry, recent));
                    }
                }
                else {
                    if (intersection.containsAll(recentValues)) {
                        result.add(Couple.of(entry, recent));
                    }
                }
            }

            map.put(entry, matched);
        }

        for (Couple<IgnoreEntry> pair : result) {
            problemsHolder.newProblem(message(pair.first, virtualFile, isOnTheFly))
                .range(pair.second)
                .withFixes(new IgnoreRemoveEntryFix(pair.second))
                .create();
        }
    }

    /**
     * Returns the paths list for the given {@link IgnoreEntry} array in {@link VirtualFile} context.
     * Stores fetched data in {@link #cacheMap} to limit the queries to the files tree.
     *
     * @param contextDirectory current context
     * @param entries          to check
     * @return paths list
     */
    @Nonnull
    @RequiredReadAction
    private Map<IgnoreEntry, Set<String>> getPathsSet(
        @Nonnull VirtualFile contextDirectory,
        @Nonnull ArrayList<IgnoreEntry> entries,
        @Nonnull MatcherUtil matcher
    ) {
        Map<IgnoreEntry, Set<String>> result = new HashMap<>();
        ArrayList<IgnoreEntry> notCached = new ArrayList<>();

        for (IgnoreEntry entry : entries) {
            ProgressManager.checkCanceled();
            String key = contextDirectory.getPath() + Constants.DOLLAR + entry.getText();
            if (!cacheMap.containsKey(key)) {
                notCached.add(entry);
            }
            result.put(entry, cacheMap.get(key));
        }

        Map<IgnoreEntry, Set<String>> found = Glob.findAsPaths(contextDirectory, notCached, matcher, true);
        for (Map.Entry<IgnoreEntry, Set<String>> item : found.entrySet()) {
            ProgressManager.checkCanceled();
            String key = contextDirectory.getPath() + Constants.DOLLAR + item.getKey().getText();
            cacheMap.put(key, item.getValue());
            result.put(item.getKey(), item.getValue());
        }

        return result;
    }

    /**
     * Helper for inspection message generating.
     *
     * @param coveringEntry entry that covers message related
     * @param virtualFile   current working file
     * @param onTheFly      true if called during on the fly editor highlighting. Called from Inspect Code action
     *                      otherwise
     * @return generated message {@link String}
     */
    @Nonnull
    @RequiredReadAction
    private static LocalizeValue message(
        @Nonnull IgnoreEntry coveringEntry,
        @Nonnull VirtualFile virtualFile,
        boolean onTheFly
    ) {
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (onTheFly || document == null) {
            return IgnoreLocalize.codeinspectionCoverentryMessage("\'" + coveringEntry.getText() + "\'");
        }

        int startOffset = coveringEntry.getTextRange().getStartOffset();
        return IgnoreLocalize.codeinspectionCoverentryMessage(
            "<a href=\"" + virtualFile.getUrl() + Constants.HASH + startOffset + "\">" +
                coveringEntry.getText() + "</a>"
        );
    }

    /**
     * Forces checking every entry in checked file.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean runForWholeFile() {
        return true;
    }
}
