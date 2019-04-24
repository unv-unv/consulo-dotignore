// This is a generated file. Not intended for manual editing.
package mobi.hsz.idea.gitignore.psi;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class IgnoreVisitor extends PsiElementVisitor {

  public void visitEntry(@Nonnull IgnoreEntry o) {
    visitEntryBase(o);
  }

  public void visitEntryDirectory(@Nonnull IgnoreEntryDirectory o) {
    visitEntryFile(o);
  }

  public void visitEntryFile(@Nonnull IgnoreEntryFile o) {
    visitEntry(o);
  }

  public void visitNegation(@Nonnull IgnoreNegation o) {
    visitPsiElement(o);
  }

  public void visitSyntax(@Nonnull IgnoreSyntax o) {
    visitPsiElement(o);
  }

  public void visitEntryBase(@Nonnull IgnoreEntryBase o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@Nonnull PsiElement o) {
    visitElement(o);
  }

}
