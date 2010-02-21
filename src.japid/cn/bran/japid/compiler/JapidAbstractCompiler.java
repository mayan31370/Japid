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
package cn.bran.japid.compiler;

import java.io.File;
import java.util.Stack;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.template.ActionRunner;
import cn.bran.japid.template.JapidTemplate;
import cn.bran.japid.template.RenderResult;
import cn.bran.play.JapidResult;

/**
 * based on the original code from the Play! Frameowrk
 * 
 * the parent class for all three type compilers: regular template conpiler, the
 * LayoutCompiler and the TagCompiler.
 * 
 * @author original Play! authors
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public abstract class JapidAbstractCompiler {
	private static final String ARGS = "args";

	// private static Log log = LogFactory.getLog(JapidAbstractCompiler.class);

	protected static final String HTML = ".html";
	// private static final String DO_BODY = "doBody";
	protected static final String SPACE = " ";
	protected static final String NEW_LINE = "\n";
	protected JapidTemplate template;
	protected JapidParser parser;
	protected boolean doNextScan = true;
	protected Stack<Tag> tagsStack = new Stack<Tag>();
	protected int tagIndex;
	protected boolean skipLineBreak;

	protected static class Tag {
		String tagName;
		int startLine;
		boolean hasBody;
		// bran: put everything in the args tag in it
		String bodyArgsString = "";
		StringBuffer bodyBuffer = new StringBuffer(2000);
		public String innerClassName;
		public String args = "";
		public int tagIndex;
	}

	public void compile(JapidTemplate t) {
		template = t;
		getTemplateClassMetaData().setOriginalTemplate(t.name);
		hop();
	}

	/**
	 * 
	 */
	protected void parse() {
		// Parse
		loop: for (;;) {

			if (doNextScan) {
				state = parser.nextToken();
			} else {
				doNextScan = true;
			}

			switch (state) {
			case EOF:
				break loop;
			case PLAIN:
				plain();
				break;
			case SCRIPT:
				script();
				break;
			case SCRIPT_LINE:
				script();
				break;
			case EXPR:
				expr();
				break;
			case MESSAGE:
				message();
				break;
			case ACTION:
				action(false);
				break;
			case ABS_ACTION:
				action(true);
				break;
			case COMMENT:
				skipLineBreak = true;
				break;
			case START_TAG:
				startTag();
				break;
			case END_TAG:
				endTag();
				break;
			case TEMPLATE_ARGS:
				templateArgs();
				break;
			}
		}
	}

	protected void plain() {
		String text = parser.getToken().replace("\\", "\\\\").replaceAll("\"", "\\\\\"");
//		text = text.replace("``", "`"); // escaped `, already done by parser
		if (skipLineBreak && text.startsWith(NEW_LINE)) {
			text = text.substring(1);
		}

		// add static content to classmetadata and print the ref to the
		// generated source code
		if (this.getTemplateClassMetaData().getTrimStaticContent()) {
			String r = text.trim();
			if (r.length() == 0)
				return;
			else {
				text = text.trim();
			}
		}
		String lines = composeValidMultiLines(text);
		String ref = this.getTemplateClassMetaData().addStaticText(lines);
		if (ref != null) {
			print("p(");
			print(ref);
			print(");");
			markLine(parser.getLine());
			println();
		}
	}

	/**
	 * break a line with newlines to multiple lines valid in java source code
	 * 
	 * <pre>
	 * "line1\n" +
	 * "line2\n"  + 
	 * "line3";
	 * 
	 * </pre>
	 * 
	 * @param src
	 * @return
	 */
	public static String composeValidMultiLines(String text) {
		// multi-line
		String[] lines = text.split(NEW_LINE, 10000);
		String result = "";
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.length() > 0 && (int) line.charAt(line.length() - 1) == 13) {
				// remove the last newline
				line = line.substring(0, line.length() - 1);
			}

			result += "\"" + line;

			if (i == lines.length - 1 && !text.endsWith(NEW_LINE)) {
				// last line
				result += "\"\n";
			} else if (i == lines.length - 1 && line.equals("")) {
				result += "\"";
			} else {
				// regular line
				result += "\\n\" + \n";
			}

			// markLine(parser.getLine() + i);
		}
		return result;
	}

	protected abstract void startTag();

	protected abstract void endTag();

	protected void println() {
		print(NEW_LINE);

		currentLine++;
	}

	protected void print(String text) {
		Tag currentScope = this.tagsStack.peek();
		currentScope.bodyBuffer.append(text);
		// else if (this.currentInnerClassName != null)
		// this.currentInnerClassRenderBody.append(text);
		// else
		// mainRenderBodySource.append(text);
	}

	protected void println(String text) {
		print(text);
		println();
		int i = 0;
		while (i++ < indentLevel) {
			print("\t");
		}
	}

	protected void markLine(int line) {
		if (!this.getTemplateClassMetaData().getTrimStaticContent()) {
			print("// line " + line);
		}

		template.linesMatrix.put(currentLine, line);
	}

	protected void script() {
		String text = parser.getToken();
		String[] lines = new String[] { text };
		if (text.indexOf(NEW_LINE) > -1) {
			lines = parser.getToken().split(NEW_LINE);
		}

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.startsWith("import ") || line.startsWith("import\t")) {
				getTemplateClassMetaData().addImportLine(line);
			} else if (line.startsWith("//")) {
				// ignore
			} else if (line.startsWith("extends ") || line.startsWith("extends\t")) {
				String layoutName = line.substring("extends".length()).trim();
				layoutName = layoutName.replace("'", "");
				layoutName = layoutName.replace("\"", "");
				if (layoutName.endsWith(";")) {
					layoutName = layoutName.substring(0, layoutName.length() - 1);
				}
				if (layoutName.endsWith(HTML)) {
					layoutName = layoutName.substring(0, layoutName.indexOf(HTML));
				}
				if (layoutName.startsWith("/")) {
					layoutName = layoutName.substring(1);
				}
				getTemplateClassMetaData().superClass = layoutName.replace('/', '.');
			} else if (line.startsWith("contentType ") || line.startsWith("contentType	")) {
				// TODO: should also take standard tag name: Content-Type
				String contentType = line.substring("contentType".length()).trim().replace("'", "").replace("\"", "");
				if (contentType.endsWith(";"))
					contentType = contentType.substring(0, contentType.length());
				getTemplateClassMetaData().setContentType(contentType);
			} else if (line.startsWith(ARGS + " ") || line.startsWith(ARGS + "\t")) {
				String contentType = line.substring(ARGS.length()).trim().replace(";", "").replace("'", "").replace("\"", "");
				Tag currentTag = this.tagsStack.peek();
				currentTag.bodyArgsString = contentType;
			} else if (line.startsWith("trim ") || line.startsWith("trim\t")) {
				String sw = line.substring("trim".length()).trim().replace(";", "").replace("'", "").replace("\"", "");
				if ("on".equals(sw) || "true".equals(sw)) {
					getTemplateClassMetaData().trimStaticContent();
				}
			} else if (line.startsWith("stopwatch ") || line.startsWith("stopwatch\t")) {
				String sw = line.substring("stopwatch".length()).trim().replace(";", "").replace("'", "").replace("\"", "");
				if ("on".equals(sw))
					getTemplateClassMetaData().turnOnStopwatch();
				// Tag currentTag = this.tagsStack.peek();
				// currentTag.bodyArgsString = contentType;
			} else {
				// TODO: do more directives for response header control: Cache-Control, Expires
				print(lines[i]);
				markLine(parser.getLine() + i);
				println();
			}
		}
		skipLineBreak = true;
	}

	protected void expr() {
		// TODO: make difference of safe expression and raw expression
		// safe expressions are wrapped in try/catch
		String expr = parser.getToken().trim();
		print("p(" + expr + ");");
		markLine(parser.getLine());
		println();
	}

	protected void message() {
		String expr = parser.getToken().trim().replace('\'', '"');
		print(";p(getMessage(" + expr + "));");
		markLine(parser.getLine());
		println();
	}

	/**
	 * TODO: remove all the dependency on the Play classes
	 * 
	 * @param absolute
	 */
	protected void action(boolean absolute) {
		String action = parser.getToken().trim();
		if (action.matches("^'.*'$") || action.matches("^\".*\"$")) {
			// static content like @{'my.css'}
			action = action.replace('\'', '"');
			// remove Play dependecy
			if (absolute) {
				// print("p(request.getBase() + play.mvc.Router.reverseWithCheck("
				// + action + ", play.Play.getVirtualFile(" + action + ")));");
				print("p(lookupStaticAbs(" + action + "));");
			} else {
				// print("p(play.mvc.Router.reverseWithCheck(" + action +
				// ", play.Play.getVirtualFile(" + action + ")));");
				print("p(lookupStatic(" + action + "));");
			}
		} else {
			if (!action.endsWith(")")) {
				action = action + "()";
			}
			// extract params if any
			int indexOfParam = action.indexOf("(");
			if (indexOfParam < 1) {
				throw new TemplateSyntaxException("action needs pair of ()", template.name, action, this.currentLine);
			}

			String actionPart = action.substring(0, indexOfParam).trim();

			// extract the param list part
			String params = action.substring(indexOfParam + 1);
			params = params.substring(0, params.length() - 1).trim();
			if (params.length() == 0)
				params = "new Object[]{}";

			if (absolute) {
				print("p(lookupAbs(\"" + actionPart + "\", " + params + "));");
			} else {
				print("p(lookup(\"" + actionPart + "\", " + params + "));");
			}
		}
		markLine(parser.getLine());
		println();
	}

	protected void hop() {

		String source = template.source;
		Tag rootTag = new Tag() {
			{
				tagName = "root";
				startLine = 0;
				hasBody = true;
			}
		};

		this.tagsStack.push(rootTag);

		this.parser = new JapidParser(source);

		String tempName = template.name.replace("-", "_");// .replace('.', '_');
		if (tempName.endsWith(HTML)) {
			tempName = tempName.substring(0, tempName.indexOf(HTML));
		}
		// extract path
		int lastIndexOf = tempName.lastIndexOf(File.separator);
		if (lastIndexOf > 0) {
			String path = tempName.substring(0, lastIndexOf);
			path = path.replace('/', '.');
			path = path.replace('\\', '.');
			getTemplateClassMetaData().packageName = path;
			getTemplateClassMetaData().className = tempName.substring(lastIndexOf + 1);
		} else {
			getTemplateClassMetaData().className = tempName;
		}

		parse();
		// for (String n : BranTemplateCompiler.extensionsClassnames) {
		// println(" } ");
		// }
		// println("} }");
		// println("}");

		Tag tag = tagsStack.pop();
		assert (tagsStack.empty());

		// remove print nothing statement to save a few CPU cycles
		this.getTemplateClassMetaData().body = tag.bodyBuffer.toString().replace("p(\"\")", "").replace("pln(\"\")", "pln()");
		postParsing(tag);
		template.javaSource = this.getTemplateClassMetaData().toString();

		// try {
		// log.trace(String.format("%s is compiled to %s", template.name,
		// template.javaSource));
		// } catch (Exception e) {
		// //
		// }

	}

	/**
	 * add anything before the java source generation
	 */
	abstract protected void postParsing(Tag tag);

	abstract protected AbstractTemplateClassMetaData getTemplateClassMetaData();

	protected void templateArgs() {
		Tag currentTag = this.tagsStack.peek();
		String args = parser.getToken();
		currentTag.bodyArgsString = args;
	}

	/**
	 * @return
	 */
	protected Tag buildTag() {
			String tagText = parser.getToken().trim().replaceAll(NEW_LINE, SPACE);

			boolean hasBody = !parser.checkNext().endsWith("/");
	
			Tag tag = new TagInvocationLineParser().parse(tagText);
			if (tag.tagName== null || tag.tagName.length() == 0)
				throw new RuntimeException("tag name was empty: " + tagText);
			tag.startLine = parser.getLine();
			tag.hasBody = hasBody;
			tag.tagIndex = tagIndex++;
			return tag;
		}

	static String createActionRunner(String action) {
		String template = 
				"		%s.put(getOut().length(), new %s() {\r\n" + 
				"			@Override\r\n" + 
				"			public %s run() {\r\n" + 
				"				try {\r\n" + 
				"					%s;\r\n" + 
				"				} catch (%s jr) {\r\n" + 
				"					return jr.getRenderResult();\r\n" + 
				"				}\r\n" + 
				"				throw new RuntimeException(\"No render result from running: %s\");\r\n" + 
				"			}\r\n" + 
				"		});";
		return String.format(template, 
				AbstractTemplateClassMetaData.ACTION_RUNNERS , 
				ActionRunner.class.getName(),
				RenderResult.class.getName(),
				action,
				JapidResult.class.getName(),
				action);
	}

	protected int currentLine = 1;

	protected int indentLevel = 0;
	JapidParser.Token state;

	public JapidAbstractCompiler() {
		super();
	}

}