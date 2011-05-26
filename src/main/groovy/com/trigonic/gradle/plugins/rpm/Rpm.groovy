/*
 * Copyright 2011 the original author or authors.
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

package com.trigonic.gradle.plugins.rpm

import java.io.File

import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyActionImpl
import org.gradle.api.internal.file.copy.CopySpecVisitor;
import org.gradle.api.tasks.bundling.AbstractArchiveTask

class Rpm extends AbstractArchiveTask {
	public static final String RPM_EXTENSION = "rpm";

	private final CopyActionImpl action;

	public Rpm() {
		setExtension(RPM_EXTENSION)
		action = new RpmCopyAction(getServices().get(FileResolver.class))
	}

	protected CopyActionImpl getCopyAction() {
		action
	}
	
	protected class RpmCopyAction extends CopyActionImpl {
		public RpmCopyAction(FileResolver resolver) {
			super(resolver, new RpmCopySpecVisitor());
		}
		
		String getArchivePath() {
			Rpm.this.getArchivePath();
		}
	}
}