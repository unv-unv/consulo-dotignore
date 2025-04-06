package mobi.hsz.idea.gitignore.inspections;

import consulo.dotignore.localize.IgnoreLocalize;
import mobi.hsz.idea.gitignore.codeInspection.IgnoreDuplicateEntryInspection;

public class DuplicateEntryInspectionTest extends InspectionTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.enableInspections(IgnoreDuplicateEntryInspection.class);
    }

    public void testSimpleCase() throws Exception {
        doHighlightingFileTest();
    }
    
    public void testSimpleCaseWithQuickFix() throws Exception {
        doHighlightingFileTestWithQuickFix(IgnoreLocalize.quickFixRemoveEntry().get());
    }
}
