package com.trigonic.gradle.plugins.rpm

import static org.freecompany.redline.header.Header.HeaderTag.HEADERIMMUTABLE
import static org.freecompany.redline.header.Signature.SignatureTag.SIGNATURES
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.List
import java.util.zip.GZIPInputStream

import org.freecompany.redline.ChannelWrapper.Key
import org.freecompany.redline.ReadableChannelWrapper
import org.freecompany.redline.header.Format
import org.freecompany.redline.payload.CpioHeader
import org.junit.Assert

/**
 * Based on {@link org.freecompany.redline.Scanner}, but modified to return scanned nformation for
 * programmatic verification.
 */
class Scanner {
    static Map scan(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file)
        try {
            return scan(fileInputStream)
        } finally {
            fileInputStream.close()
        }
    }
    
    static Map scan(InputStream inputStream) {
        ReadableChannelWrapper wrapper = new ReadableChannelWrapper(Channels.newChannel(inputStream))
        Format format = scanHeader(wrapper)
        
        InputStream uncompressed = new GZIPInputStream(inputStream)
        wrapper = new ReadableChannelWrapper(Channels.newChannel(uncompressed))
        CpioHeader header = null
        
        def files = []
        int total = 0
        while (header == null || !header.isLast()) {
            header = new CpioHeader()
            total = header.read(wrapper, total)
            if (!header.isLast()) {
                files += header
            }
            final int skip = header.getFileSize()
            assertEquals(skip, uncompressed.skip(skip))
            total += header.getFileSize()
        }
        
        return [format: format, files: files]
    }

    static Format scanHeader(ReadableChannelWrapper wrapper) throws Exception {
        Format format = new Format()
        format.getLead().read(wrapper)

        int count = format.signature.read(wrapper)
        int expected = ByteBuffer.wrap(format.signature.getEntry(SIGNATURES).values, 8, 4).getInt() / -16
        assertEquals(expected, count)

        count = format.getHeader().read(wrapper)
        expected = ByteBuffer.wrap(format.getHeader().getEntry(HEADERIMMUTABLE).values, 8, 4).getInt() / -16
        assertEquals(expected, count)

        return format
    }
}
