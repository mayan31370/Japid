package japidviews.Application;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import static play.templates.JavaExtensions.*;
import static cn.bran.play.JapidPlayAdapter.*;
import static play.data.validation.Validation.*;
import japidviews._layouts.*;
import play.i18n.Messages;
import play.data.validation.Validation;
import play.mvc.Scope.*;
import models.*;
import play.data.validation.Error;
import play.i18n.Lang;
import japidviews._tags.*;
import play.mvc.Http.*;
import controllers.*;
//
// NOTE: This file was generated from: japidviews/Application/validate.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class validate extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/validate.html";
	{
		putHeader("Content-Type", "text/html; charset=utf-8");
		setContentType("text/html; charset=utf-8");
	}

// - add implicit fields with Play

	final play.mvc.Http.Request request = play.mvc.Http.Request.current(); 
	final play.mvc.Http.Response response = play.mvc.Http.Response.current(); 
	final play.mvc.Scope.Session session = play.mvc.Scope.Session.current();
	final play.mvc.Scope.RenderArgs renderArgs = play.mvc.Scope.RenderArgs.current();
	final play.mvc.Scope.Params params = play.mvc.Scope.Params.current();
	final play.data.validation.Validation validation = play.data.validation.Validation.current();
	final cn.bran.play.FieldErrors errors = new cn.bran.play.FieldErrors(validation);
	final play.Play _play = new play.Play(); 

// - end of implicit fields with Play 


	public validate() {
		super(null);
	}
	public validate(StringBuilder out) {
		super(out);
	}
/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"name", "age",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "Integer",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.validate.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String name; // line 1
	private Integer age; // line 1
	public cn.bran.japid.template.RenderResult render(String name,Integer age) {
		this.name = name;
		this.age = age;
		long t = -1;
		try {super.layout();} catch (RuntimeException e) { super.handleException(e);} // line 1
		return new cn.bran.japid.template.RenderResultPartial(getHeaders(), getOut(), t, actionRunners, sourceTemplate);
	}
	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
//------
;// line 1
		p("\n" + 
"\n");// line 1
		if(asBoolean(hasErrors())) {// line 3
		p("    <p>Got some errors:</p>\n" + 
"    ");// line 3
		for (Error e: errors()){// line 5
		p("        <p>");// line 5
		p(e.getKey());// line 6
		p(" : ");// line 6
		p(e);// line 6
		p("</p>\n" + 
"    ");// line 6
		}// line 7
} else {// line 8
		p("	name is: ");// line 8
		p(name);// line 9
		p(", age is: ");// line 9
		p(age);// line 9
		;// line 9
		}// line 10
		;// line 10
		
		endDoLayout(sourceTemplate);
	}

}