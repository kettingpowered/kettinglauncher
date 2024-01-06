package org.kettingpowered.task

import java.security.MessageDigest

final class Util {
    public static void init() {
        File.metaClass.sha512 = { ->
            MessageDigest md = MessageDigest.getInstance('SHA3-512')
            delegate.eachByte 4096, {bytes, size ->
                md.update(bytes, 0, size)
            }
            return md.digest().collect {String.format "%02x", it}.join()
        }
        File.metaClass.getSha512 = { !delegate.exists() ? null : delegate.sha512() }
    }
    public static Map getMavenInfoFromDep(dep) {
        return getMavenInfoFromMap([
                group: dep.moduleVersion.id.group,
                name: dep.moduleVersion.id.name,
                version: dep.moduleVersion.id.version,
                classifier: dep.classifier,
                extension: dep.extension
        ])
    }

    private static Map getMavenInfoFromMap(art) {
        def key = "$art.group:$art.name"
        def name = "$art.group:$art.name:$art.version"
        def path = "${art.group.replace('.', '/')}/$art.name/$art.version/$art.name-$art.version"
        if (art.classifier != null) {
            name += ":$art.classifier"
            path += "-$art.classifier"
        }
        if (!'jar'.equals(art.extension)) {
            name += "@$art.extension"
            path += ".$art.extension"
        } else {
            path += ".jar"
        }
        return [
                key: key,
                name: name,
                path: path,
                art: art
        ]
    }
}
