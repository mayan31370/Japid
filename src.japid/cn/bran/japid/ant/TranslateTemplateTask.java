/**
 * Copyright 2010 Bing Ran<bing_ran@hotmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package cn.bran.japid.ant;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.SourceFileScanner;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.compiler.JapidTemplateTransformer;

/**
 * modeled after the JamonTask in the <a url = "http://www.jamon.org/>Jamon
 * project</a>
 * 
 * ensure you have the Japid task properly defined,
 * 
 * <pre>
 * <path id="JapidCP">
 *     <pathelement file="/path/to/japid.jar"/>
 *   </path>
 *   <taskdef name="japid"
 *            classname="bran.ant.TranslateTemplateTask"
 *            classpathref="${JapidCP}"/>
 * 
 * To generate the Java sources, include the following target:
 * 
 *   <target name="trans-templates">
 *     <japid srcdir="templates" destdir="tempsrc" />
 *   </target>
 * </pre>
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class TranslateTemplateTask extends MatchingTask {

	private List<Class<?>> staticImports = new ArrayList<Class<?>>();
	private List<String> imports = new ArrayList<String>();
	private File[] changedFiles;

	/**
	 * get a list of changed files in the last task execution
	 * 
	 * @return
	 */
	public File[] getChangedFiles() {
		return changedFiles;
	}

	public void setDestdir(File p_destDir) {
		destDir = p_destDir;
	}

	public void setSrcdir(File p_srcDir) {
		srcDir = p_srcDir;
	}

	public void setClasspath(Path p_classpath) throws IOException {
		String[] paths = p_classpath.list();
		URL[] urls = new URL[paths.length];
		for (int i = 0; i < urls.length; ++i) {
			urls[i] = new URL("file", null, paths[i]);
		}
		// m_classLoader = AccessController.doPrivileged(new
		// ClassLoaderCreator(urls));
	}

	//
	// private static class ClassLoaderCreator implements
	// PrivilegedAction<ClassLoader>
	// {
	// private final URL[] m_urls;
	// ClassLoaderCreator(URL[] p_urls)
	// {
	// m_urls = p_urls;
	// }
	// public ClassLoader run()
	// {
	// return new URLClassLoader(m_urls,
	// ClassLoader.getSystemClassLoader());
	// }
	// }

	public void setListFiles(boolean p_listFiles) {
		listFiles = p_listFiles;
	}

	@Override
	public void execute() throws BuildException {
		// Copied from org.apache.tools.ant.taskdefs.Javac below

		// first off, make sure that we've got a srcdir

		if (srcDir == null) {
			throw new BuildException("srcdir attribute must be set!", getLocation());
		}
		if (destDir == null) {
			destDir = srcDir;
		}

		if (!srcDir.exists() && !srcDir.isDirectory()) {
			throw new BuildException("source directory \"" + srcDir + "\" does not exist or is not a directory", getLocation());
		}

		destDir.mkdirs();
		if (!destDir.exists() || !destDir.isDirectory()) {
			throw new BuildException("destination directory \"" + destDir + "\" does not exist or is not a directory", getLocation());
		}

		if (!srcDir.exists()) {
			throw new BuildException("srcdir \"" + srcDir + "\" does not exist!", getLocation());
		}

		SourceFileScanner sfs = new SourceFileScanner(this);
		changedFiles = sfs.restrictAsFiles(getDirectoryScanner(srcDir).getIncludedFiles(), srcDir, destDir, new JapidFileNameMapper());

		if (changedFiles.length > 0) {
			log("Processing " + changedFiles.length + " template" + (changedFiles.length == 1 ? "" : "s") + " to " + destDir);

			JapidTemplateTransformer tran = new JapidTemplateTransformer(srcDir.getPath(), null);
			for (Class<?> c : this.staticImports) {
				tran.addImportStatic(c);
			}
			for (String c : this.imports) {
				tran.addImportLine(c);
			}
			for (Class<? extends Annotation> a : this.typeAnnotations) {
				tran.addAnnotation(a);
			}

			for (int i = 0; i < changedFiles.length; i++) {
				File pFile = changedFiles[i];
				System.out.println("transforming template: " + pFile.getPath() + " to: " + pFile.getName().replace("html", "java"));
				if (listFiles) {
					log(pFile.getAbsolutePath());
				}

				try {
					String relativePath = JapidTemplateTransformer.getRelativePath(pFile, srcDir);
					tran.generate(relativePath);
				} catch (Exception e) {
					e.printStackTrace();
					throw new BuildException(e.getClass().getName() + ":" + e.getMessage(),
							new Location(pFile.getAbsoluteFile().toString()));
				}
			}
		}
	}

	/**
	 * add an import static entry to the template tranlator
	 * 
	 * @param clz
	 */
	public void importStatic(Class<?> clz) {
		this.staticImports.add(clz);
	}

	/**
	 * 
	 * @param imp
	 *            the package or class part of a full line of regular imports,
	 *            such as java.util.*, java.util.Hashmap
	 */
	public void addImport(String imp) {
		this.imports.add(imp);
	}

	public void addImport(Class<?> clz) {
		this.imports.add(clz.getName());
	}

	private static class JapidFileNameMapper implements FileNameMapper {
		public void setFrom(String p_from) {
		}

		public void setTo(String p_to) {
		}

		public String[] mapFileName(String sourceName) {
			String targetFileName = sourceName;
			int i = targetFileName.lastIndexOf('.');
			if (i > 0 && "html".equals(targetFileName.substring(i + 1))) {
				targetFileName = targetFileName.substring(0, i);
				return new String[] { targetFileName + ".java" };
			} else {
				return null;
			}
			// return new String[0];
		}
	}

	// private String relativize(File p_file)
	// {
	// if (!p_file.isAbsolute())
	// {
	// throw new IllegalArgumentException("Paths must be all absolute");
	// }
	// String filePath = p_file.getPath();
	// String basePath = srcDir.getAbsoluteFile().toString(); // FIXME !?
	//
	// if (filePath.startsWith(basePath))
	// {
	// return filePath.substring(basePath.length() + 1);
	// }
	// else
	// {
	// throw new IllegalArgumentException(p_file
	// + " is not based at "
	// + basePath);
	// }
	// }

	private File destDir = null;
	private File srcDir = null;
	private boolean listFiles = false;

	public void addAnnotation(Class<? extends Annotation> anno) {
		typeAnnotations.add(anno);
	}

	List<Class<? extends Annotation>> typeAnnotations = new ArrayList<Class<? extends Annotation>>();

	// private ClassLoader m_classLoader = JamonTask.class.getClassLoader();
	public void setUseStreaming(boolean streaming) {
		AbstractTemplateClassMetaData.streaming = streaming;
	}
}