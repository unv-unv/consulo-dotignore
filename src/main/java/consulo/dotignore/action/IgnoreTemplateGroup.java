/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.dotignore.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import mobi.hsz.idea.gitignore.actions.AddTemplateAction;
import mobi.hsz.idea.gitignore.actions.CreateUserTemplateAction;

/**
 * @author UNV
 * @since 2025-09-20
 */
@ActionImpl(
    id = "Ignore.TemplateGroup",
    children = {
        @ActionRef(type = AddTemplateAction.class),
        @ActionRef(type = CreateUserTemplateAction.class)
    },
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.FIRST)
)
public class IgnoreTemplateGroup extends DefaultActionGroup implements DumbAware {
    public IgnoreTemplateGroup() {
        super(LocalizeValue.empty(), false);
    }
}
