/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trigonic.gradle.plugins.rpm

import org.gradle.api.tasks.bundling.Tar

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import org.apache.commons.io.FileUtils
import static org.freecompany.redline.payload.CpioHeader.*
import static org.freecompany.redline.header.Header.HeaderTag.*

import org.freecompany.redline.header.Header.HeaderTag
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.gmock.GMockController

class RpmPluginTest {
    @Test
    public void files() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        File noParentsDir = new File(buildDir, 'noParentsDir')
        noParentsDir.mkdirs()
        FileUtils.writeStringToFile(new File(noParentsDir, 'alone'), 'alone')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386
            os = LINUX
            group = 'Development/Libraries'
            summary = 'Bleah blarg'
            description = 'Not a very interesting library.'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'
            
            requires('blarg', '1.0', GREATER | EQUAL)
            requires('blech')

            into '/opt/bleah'
            from(srcDir)

            from(srcDir.toString() + '/main/groovy') {
                createDirectoryEntry = true
                fileType = CONFIG | NOREPLACE
            }
            
            from(noParentsDir) {
                addParentDirs = false
                into '/a/path/not/to/create'
            }

            link('/opt/bleah/banana', '/opt/bleah/apple')
        })

        project.tasks.buildRpm.execute()
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        assertEquals('bleah', getHeaderEntryString(scan, NAME))
        assertEquals('1.0', getHeaderEntryString(scan, VERSION))
        assertEquals('1', getHeaderEntryString(scan, RELEASE))
        assertEquals('i386', getHeaderEntryString(scan, ARCH))
        assertEquals('linux', getHeaderEntryString(scan, OS))
        assertEquals(['./a/path/not/to/create/alone', './opt/bleah',
                      './opt/bleah/apple', './opt/bleah/banana'], scan.files*.name)
        assertEquals([FILE, DIR, FILE, SYMLINK], scan.files*.type)
    }


    @Test
    public void filesWithCopySpec() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        File noParentsDir = new File(buildDir, 'noParentsDir')
        noParentsDir.mkdirs()
        FileUtils.writeStringToFile(new File(noParentsDir, 'alone'), 'alone')

        project.apply plugin: 'rpm'

        def rpmCopySpec = project.copySpec {
            into '/opt/bleah'
            from(srcDir)

            from(srcDir.toString() + '/main/groovy') {
                fileMode = 0644
                createDirectoryEntry = true
                fileType = CONFIG | NOREPLACE
            }

            from(noParentsDir) {
                fileMode = 0755
                addParentDirs = false
                into '/a/path/not/to/create'
            }
        }

        project.task([type: Rpm], 'buildRpm', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            destinationDir.mkdirs()

            packageName = 'bleah'
            version = '1.0'
            release = '1'
            type = BINARY
            arch = I386
            os = LINUX
            group = 'Development/Libraries'
            summary = 'Bleah blarg'
            description = 'Not a very interesting library.'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'

            with rpmCopySpec
            link('/opt/bleah/banana', '/opt/bleah/apple')

        })

        project.tasks.buildRpm.execute()
        def scan = Scanner.scan(project.file('build/tmp/RpmPluginTest/bleah-1.0-1.i386.rpm'))
        assertEquals('bleah', getHeaderEntryString(scan, NAME))
        assertEquals('1.0', getHeaderEntryString(scan, VERSION))
        assertEquals('1', getHeaderEntryString(scan, RELEASE))
        assertEquals('i386', getHeaderEntryString(scan, ARCH))
        assertEquals('linux', getHeaderEntryString(scan, OS))
        assertEquals(['./a/path/not/to/create/alone', './opt/bleah',
                './opt/bleah/apple', './opt/bleah/banana'], scan.files*.name)
        assertEquals([FILE, DIR, FILE, SYMLINK], scan.files*.type)

        project.apply plugin: 'java'
        project.task([type: Tar], 'buildJar', {
            destinationDir = project.file('build/tmp/RpmPluginTest')
            with rpmCopySpec
        })
        project.tasks.buildJar.execute()
    }

    @Test
    public void projectNameDefault() {
        Project project = ProjectBuilder.builder().build()

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {})
        assertEquals 'test', project.buildRpm.packageName

        project.tasks.buildRpm.execute()
    }

    @Test
    void buildHost_shouldHaveASensibleDefault_whenHostNameResolutionFails() {
        GMockController mock = new GMockController()
        InetAddress mockInetAddress = (InetAddress) mock.mock(InetAddress)

        mockInetAddress.static.getLocalHost().raises(new UnknownHostException())

        mock.play {
            Project project = ProjectBuilder.builder().build()

            File buildDir = project.buildDir
            File srcDir = new File(buildDir, 'src')
            srcDir.mkdirs()
            FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

            project.apply plugin: 'rpm'

            project.task([type: Rpm], 'buildRpm', {})
            assertEquals 'unknown', project.buildRpm.buildHost

            project.tasks.buildRpm.execute()
        }

    }

    @Test
    public void usesArchivesBaseName() {
        Project project = ProjectBuilder.builder().build()
        project.archivesBaseName = 'foo'

        File buildDir = project.buildDir
        File srcDir = new File(buildDir, 'src')
        srcDir.mkdirs()
        FileUtils.writeStringToFile(new File(srcDir, 'apple'), 'apple')

        project.apply plugin: 'rpm'

        project.task([type: Rpm], 'buildRpm', {})
        assertEquals 'foo', project.buildRpm.packageName

        project.tasks.buildRpm.execute()
    }
    
    def getHeaderEntry = { scan, tag ->
        def header = scan.format.header
        header.getEntry(tag.code)
    }
    
    def getHeaderEntryString = { scan, tag ->
        getHeaderEntry(scan, tag).values.join('')
    }
}
