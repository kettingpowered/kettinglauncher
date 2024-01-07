package org.kettingpowered.task

import java.security.MessageDigest

final class Util {
    static void init() {
        File.metaClass.sha512 = { ->
            MessageDigest md = MessageDigest.getInstance('SHA3-512')
            delegate.eachByte 4096, {bytes, size ->
                md.update(bytes as byte[], 0, size as int)
            }
            return md.digest().collect {String.format "%02x", it}.join()
        }
        File.metaClass.getSha512 = { !delegate.exists() ? null : delegate.sha512() }
    }
}
