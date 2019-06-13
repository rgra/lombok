/*
 * Copyright (C) 2009-2014 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.javac.CapturingDiagnosticListener.CompilerMessage;

public class RunTestsViaTycho extends AbstractRunTests {

	@Override public boolean transformCode(Collection<CompilerMessage> messages, StringWriter result, final File file, String encoding, Map<String, String> formatPreferences) throws Throwable {
		// \test\transform\resource\before\Accessors.java
		File f = new File(new File(file.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "tycho/logs"), file.getName() + ".xml");
		if (f.exists()) {
			parseXMLLog(f, messages);
			return true;
		}
		return false;
	}

	public void parseXMLLog(File f, Collection<CompilerMessage> messages) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		DefaultHandler handler = new DefaultHandlerExtension(messages);
		saxParser.parse(f, handler);
	}

	public static void main(String[] args) throws IOException {
		// Load Keys
		ConfigurationKeys.LOMBOK_DISABLE.getKeyName();
		// Read File
		File inputFile = new File(args[0]);
		String configuration = LombokTestSource.readConfiguration(inputFile);

		if (configuration.length() > 0) {
			// Write Config
			File outputFile = new File(args[1]);
			FileWriter w = new FileWriter(outputFile);
			try {
				w.write(configuration);
			} finally {
				w.close();
			}
		}
	}

	private static final class DefaultHandlerExtension extends DefaultHandler {

		private final Collection<CompilerMessage> messages;
		private boolean inElementProblem;
		private long line;
		private long position;
		private boolean isError;
		private String message;

		public DefaultHandlerExtension(Collection<CompilerMessage> messages) {
			this.messages = messages;
		}

		@Override public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("problem")) {
				inElementProblem = true;
				String line = attributes.getValue("line");
				this.line = Long.parseLong(line);
				String charStart = attributes.getValue("charStart");
				this.position = Long.parseLong(charStart);
				String severity = attributes.getValue("severity");
				this.isError = severity.equalsIgnoreCase("ERROR");
			} else if (inElementProblem && qName.equalsIgnoreCase("message")) {
				String messageValue = attributes.getValue("value");
				this.message = messageValue;
			}

		}

		@Override public void endElement(String uri, String localName, String qName) throws SAXException {
			if (inElementProblem && qName.equalsIgnoreCase("problem")) {
				inElementProblem = false;
				this.messages.add(new CompilerMessage(line, position, isError, message));
				this.line = -1;
				this.position = -1;
				this.isError = true;
				this.message = null;
			}

		}
	}
}
