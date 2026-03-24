package com.linghualive.flamekit.feature.reader.format

import org.mozilla.universalchardet.UniversalDetector
import java.io.InputStream
import javax.inject.Inject

class EncodingDetector @Inject constructor() {

    fun detect(inputStream: InputStream): String {
        val detector = UniversalDetector(null)
        val buffer = ByteArray(8192)

        var bytesRead = inputStream.read(buffer)
        while (bytesRead > 0 && !detector.isDone) {
            detector.handleData(buffer, 0, bytesRead)
            bytesRead = inputStream.read(buffer)
        }
        detector.dataEnd()

        return detector.detectedCharset ?: "UTF-8"
    }
}
