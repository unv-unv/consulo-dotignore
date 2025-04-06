package mobi.hsz.idea.gitignore.resolving;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.ResolveResult;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import mobi.hsz.idea.gitignore.file.type.kind.GitFileType;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class ResolvingTest extends LightPlatformCodeInsightFixtureTestCase {
    @Override
    protected boolean isWriteActionRequired() {
        return true;
    }

    public void testSimple() {
        myFixture.getTempDirFixture().createFile("fileName.txt");
        doTest("fileNa<caret>me.txt", "fileName.txt");
    }

    public void testNestedDirectory() throws IOException {
        myFixture.getTempDirFixture().findOrCreateDir("dir").createChildData(this, "fileName.txt");
        doTest("dir/file<caret>Name.txt", "dir/fileName.txt");
    }

    public void testInHiddenDirectory() throws IOException {
        myFixture.getTempDirFixture().findOrCreateDir(".hidden").createChildData(this, "fileName.txt");
        doTest(".hidden/file<caret>Name.txt", ".hidden/fileName.txt");
    }

    public void testGlob() throws IOException {
        VirtualFile dir = myFixture.getTempDirFixture().findOrCreateDir("glob");
        dir.createChildData(this, "fileName1.txt");
        dir.createChildData(this, "fileName2.txt");
        doTest("glob/*<caret>.txt", "glob/fileName1.txt", "glob/fileName2.txt");
    }

    public void testGlobInParent() throws IOException {
        myFixture.getTempDirFixture().findOrCreateDir("glob1").createChildData(this, "fileName.txt");
        myFixture.getTempDirFixture().findOrCreateDir("glob2").createChildData(this, "fileName.txt");
        doTest("*/file<caret>Name.txt", "glob1/fileName.txt", "glob2/fileName.txt");
    }

    public void testInvalidRegex() throws IOException {
        myFixture.getTempDirFixture().findOrCreateDir("glob").createChildData(this, "fileName1.txt");
        doTest("glob/fileN(<caret>.txt");
    }

    public void testNegation() throws IOException {
        myFixture.getTempDirFixture().createFile("fileName.txt");
        doTest("!fileNa<caret>me.txt", "fileName.txt");
    }

    public void testNested() throws IOException {
        myFixture.getTempDirFixture().findOrCreateDir("dir1").createChildData(this, "fileName.txt");
        myFixture.getTempDirFixture().findOrCreateDir("dir2").createChildData(this, "fileName.txt");
        doTest("file<caret>Name.txt", "dir1/fileName.txt", "dir2/fileName.txt");
    }

    private void doTest(@Nonnull String beforeText, String... expectedResolve) {
        myFixture.configureByText(GitFileType.INSTANCE, beforeText);
        PsiPolyVariantReference reference = ((PsiPolyVariantReference)myFixture.getReferenceAtCaretPosition());
        assertNotNull(reference);

        VirtualFile rootFile = myFixture.getFile().getContainingDirectory().getVirtualFile();
        ResolveResult[] resolveResults = reference.multiResolve(true);
        List<String> actualResolve = ContainerUtil.map(
            resolveResults,
            (Function<ResolveResult, String>)resolveResult -> {
                PsiElement resolveResultElement = resolveResult.getElement();
                assertNotNull(resolveResultElement);
                assertInstanceOf(resolveResultElement, PsiFileSystemItem.class);
                PsiFileSystemItem fileSystemItem = (PsiFileSystemItem)resolveResultElement;
                return VfsUtilCore.getRelativePath(fileSystemItem.getVirtualFile(), rootFile, '/');
            }
        );

        assertContainsElements(actualResolve, expectedResolve);
    }
}
