package cn.bran.japid.rendererloader;

import java.io.File;
import java.util.Date;

import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;
import cn.bran.japid.util.DirUtil;

public class RendererClass {
	String className;
	String sourceCode; // the java file
	String oriSourceCode; // japid source
	
	long lastUpdated;
	byte[] bytecode;
	Class<? extends JapidTemplateBaseWithoutPlay> clz;
	ClassLoader cl;
	private File srcFile; // the original template source file
	
	public ClassLoader getCl() {
		return cl;
	}
	public void setCl(ClassLoader cl) {
		this.cl = cl;
	}
	public Class<? extends JapidTemplateBaseWithoutPlay> getClz() {
		return clz;
	}
	public void setClz(Class<? extends JapidTemplateBaseWithoutPlay> clz) {
		this.clz = clz;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getSourceCode() {
		return sourceCode;
	}
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}
	public long getLastUpdates() {
		return lastUpdated;
	}
	public void setLastUpdated(long lastUpdates) {
		this.lastUpdated = lastUpdates;
	}
	public byte[] getBytecode() {
		return bytecode;
	}
	public void setBytecode(byte[] bytecode) {
		this.bytecode = bytecode;
	}
	
	public void clear() {
		this.bytecode = null;
	}
	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param srcFile
	 */
	public void setSrcFile(File srcFile) {
		this.srcFile = srcFile;
	}
	/**
	 * @return the srcFile
	 */
	public File getSrcFile() {
		return srcFile;
	}

	public String getOriSourceCode() {
		return oriSourceCode;
	}

	public void setOriSourceCode(String oriSourceCode) {
		this.oriSourceCode = oriSourceCode;
	}
	/**
	 * @return the lastUpdated
	 */
	public long getLastUpdated() {
		return lastUpdated;
	}
	
	/**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param lineNumber
	 * @return
	 */
	public int mapJavaLineToJapidScriptLine(int lineNumber) {
		String jsrc = getSourceCode();
//		String[] splitSrc = jsrc.split("\n");
//		String line = splitSrc[lineNumber - 1];
//		// can we have a line marker?
//		int lineMarker = line.lastIndexOf("// line ");
//		if (lineMarker > 0) 
//			return Integer.parseInt(line.substring(lineMarker + 8).trim());
//		else
//			return -1;	
		return DirUtil.mapJavaLineToSrcLine(jsrc, lineNumber);
	}
}
