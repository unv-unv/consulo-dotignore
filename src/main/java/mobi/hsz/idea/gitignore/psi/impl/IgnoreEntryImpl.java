// This is a generated file. Not intended for manual editing.
package mobi.hsz.idea.gitignore.psi.impl;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static mobi.hsz.idea.gitignore.psi.IgnoreTypes.*;
import mobi.hsz.idea.gitignore.psi.*;

public class IgnoreEntryImpl extends IgnoreEntryExtImpl implements IgnoreEntry {

  public IgnoreEntryImpl(ASTNode node) {
    super(node);
  }

  public void accept(@Nonnull IgnoreVisitor visitor) {
    visitor.visitEntry(this);
  }

  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof IgnoreVisitor) accept((IgnoreVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public IgnoreNegation getNegation() {
    return findChildByClass(IgnoreNegation.class);
  }

}
