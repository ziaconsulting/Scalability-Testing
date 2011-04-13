	package com.ziaconsulting;

import java.io.IOException;

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class CreateNode extends AbstractWebScript {

	@Override
	public void execute(final WebScriptRequest req, final WebScriptResponse res)
			throws IOException {
		TestRunner tr = new TestRunner();
		tr.run();
		res.getWriter().write("done");
	}

}
