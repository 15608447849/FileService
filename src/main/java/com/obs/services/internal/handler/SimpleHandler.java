/**
 * 
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.obs.services.internal.handler;

import java.lang.reflect.Method;


import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;



public abstract class SimpleHandler extends DefaultHandler{

	protected XMLReader xr = null;
	private StringBuffer textContent = null;
	protected SimpleHandler currentHandler = null;
	protected SimpleHandler parentHandler = null;

	public SimpleHandler(XMLReader xr) {
		this.xr = xr;
		this.textContent = new StringBuffer();
		currentHandler = this;
	}

	public void transferControlToHandler(SimpleHandler toHandler) {
		currentHandler = toHandler;
		toHandler.parentHandler = this;
		xr.setContentHandler(currentHandler);
		xr.setErrorHandler(currentHandler);
	}

	public void returnControlToParentHandler() {
		if (isChildHandler()) {
			parentHandler.currentHandler = parentHandler;
			parentHandler.controlReturned(this);
			currentHandler = parentHandler;
			xr.setContentHandler(currentHandler);
			xr.setErrorHandler(currentHandler);
		}
	}

	public boolean isChildHandler() {
		return parentHandler != null;
	}

	public void controlReturned(SimpleHandler childHandler) {
	}

	@Override
	public void startElement(String uri, String name, String qName, Attributes attrs) {
		try {
			Method method = currentHandler.getClass().getMethod("start" + name);
			method.invoke(currentHandler);
		} catch (Exception ignored) {

		}
	}

	@Override
	public void endElement(String uri, String name, String qName) {
		String elementText = this.textContent.toString().trim();
		try {
			Method method = currentHandler.getClass().getMethod("end" + name, String.class);
			method.invoke(currentHandler, elementText);
		} catch (Exception ignored) {

		}
		this.textContent = new StringBuffer();
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		this.textContent.append(ch, start, length);
	}

}
