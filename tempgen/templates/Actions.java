package templates;
import java.util.*;
import java.io.*;
import tag.*;
import bran.japid.tags.Each;
import layout.*;
import static bran.WebUtils.*;
// This file was generated from: templates/Actions.html
// Change to this file will be lost next time the template file is compiled.
public class Actions extends bran.japid.BranTemplateBase{
	public static final String sourceTemplate = "templates/Actions.html";
static byte[] static_0 = getBytes(""
);
static byte[] static_1 = getBytes("\n" + 
"\n" + 
"<form url=\""
);
static byte[] static_2 = getBytes("\"></form>\n" + 
"<form url=\""
);
static byte[] static_3 = getBytes("\"></form>\n" + 
"");
	public Actions(OutputStream out) {
		super(out);
	}
	bran.Post post;
	public void render(
bran.Post post
) {
		this.post = post;
		super.layout();
	}
	@Override protected void doLayout() {
p(static_0);// line 1
p(static_1);// line 3
p(lookup("Clients.showAccounts", post.title, post.title));// line 5
p(static_2);// line 5
p(lookupAbs("Clients.showAccounts", post.title.substring(1, 2)));// line 6
p(static_3);// line 6

	}
}