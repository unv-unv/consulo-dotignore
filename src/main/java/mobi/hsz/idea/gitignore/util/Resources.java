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

package mobi.hsz.idea.gitignore.util;

import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import mobi.hsz.idea.gitignore.lang.kind.GitLanguage;
import mobi.hsz.idea.gitignore.settings.IgnoreSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * {@link Resources} util class that contains methods that work on plugin resources.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 0.2
 */
public class Resources
{
	private static final Logger LOG = Logger.getInstance(Resources.class);


	/**
	 * List of fetched {@link Template} elements from resources.
	 */
	@Nullable
	private static List<Template> resourceTemplates;

	/**
	 * Private constructor to prevent creating {@link Resources} instance.
	 */
	private Resources()
	{
	}

	/**
	 * Returns list of gitignore templates.
	 *
	 * @return Gitignore templates list
	 */
	@NotNull
	public static List<Template> getGitignoreTemplates()
	{
		final IgnoreSettings settings = IgnoreSettings.getInstance();
		final List<String> starredTemplates = settings.getStarredTemplates();

		final List<Template> templates = ContainerUtil.newArrayList();
		if(resourceTemplates == null)
		{
			resourceTemplates = ContainerUtil.newArrayList();

			// fetch templates from resources
			try
			{
				File gitignoreDir = new File(PluginManager.getPluginPath(Resources.class), "gitignore");
				if(gitignoreDir.exists())
				{
					FileUtil.visitFiles(gitignoreDir, file ->
					{
						if(file.getName().endsWith(".gitignore"))
						{
							try
							{
								String content = FileUtil.loadTextAndClose(new FileReader(file));

								Template template = new Template(file, content);
								template.setStarred(starredTemplates.contains(template.getName()));
								resourceTemplates.add(template);
							}
							catch(IOException e)
							{
								throw new RuntimeException(e);
							}
						}
						return true;
					});
				}
			}
			catch(Exception e)
			{
				LOG.error(e);
			}
		}

		templates.addAll(resourceTemplates);

		// fetch user templates
		for(IgnoreSettings.UserTemplate userTemplate : settings.getUserTemplates())
		{
			templates.add(new Template(userTemplate));
		}

		return templates;
	}

	/**
	 * {@link Template} entity that defines template fetched from resources or {@link IgnoreSettings}.
	 */
	public static class Template implements Comparable<Template>
	{
		/**
		 * {@link File} pointer. <code>null</code> if template is fetched from {@link IgnoreSettings}.
		 */
		@Nullable
		private final File file;

		/**
		 * Template name.
		 */
		@NotNull
		private final String name;

		/**
		 * Template content.
		 */
		@Nullable
		private final String content;

		/**
		 * Template's {@link Container}.
		 */
		@NotNull
		private final Container container;

		/**
		 * Template is starred.
		 */
		private boolean starred;

		/**
		 * Defines if template is fetched from resources ({@link Container#ROOT} directory or {@link Container#GLOBAL}
		 * subdirectory) or is user defined and fetched from {@link IgnoreSettings}.
		 */
		public enum Container
		{
			USER,
			STARRED,
			ROOT,
			GLOBAL
		}

		/**
		 * Builds a new instance of {@link Template}. {@link Container} will be set to {@link Container#ROOT} or {@link
		 * Container#GLOBAL} depending on its location.
		 *
		 * @param file    template's file
		 * @param content template's content
		 */
		public Template(@NotNull File file, @Nullable String content)
		{
			this.file = file;
			this.name = file.getName().replace(GitLanguage.INSTANCE.getFilename(), "");
			this.content = content;
			this.container = file.getParent().endsWith("Global") ? Container.GLOBAL : Container.ROOT;
		}

		/**
		 * Builds a new instance of {@link Template}.
		 * {@link Container} will be set to {@link Container#USER}.
		 *
		 * @param userTemplate {@link IgnoreSettings} user template object
		 */
		public Template(@NotNull final IgnoreSettings.UserTemplate userTemplate)
		{
			this.file = null;
			this.name = userTemplate.getName();
			this.content = userTemplate.getContent();
			this.container = Container.USER;
		}

		/**
		 * Gets template's file.
		 *
		 * @return template's file
		 */
		@Nullable
		public File getFile()
		{
			return file;
		}

		/**
		 * Gets template's name.
		 *
		 * @return template's name
		 */
		@NotNull
		public String getName()
		{
			return name;
		}

		/**
		 * Gets template's content.
		 *
		 * @return template's content
		 */
		@Nullable
		public String getContent()
		{
			return content;
		}

		/**
		 * Gets template's container.
		 * Returns {@link Container#STARRED} if template is starred.
		 *
		 * @return template's container
		 */
		@NotNull
		public Container getContainer()
		{
			return starred ? Container.STARRED : container;
		}

		/**
		 * Template is starred.
		 *
		 * @return starred
		 */
		public boolean isStarred()
		{
			return starred;
		}

		/**
		 * Set or unset template as starred.
		 *
		 * @param starred current state
		 */
		public void setStarred(boolean starred)
		{
			this.starred = starred;
		}

		/**
		 * Returns string representation of {@link Template}.
		 *
		 * @return template's name
		 */
		@Override
		public String toString()
		{
			return name;
		}

		/**
		 * Compares given template to the current one.
		 *
		 * @param template template to compare
		 * @return templates comparison
		 */
		@Override
		public int compareTo(@NotNull final Template template)
		{
			return name.compareToIgnoreCase(template.name);
		}
	}
}
