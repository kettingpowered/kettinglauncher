package org.kettingpowered.launcher.dependency;

/**
 *
 * @param hash a hash, of the downloaded file, with the hash-type below
 * @param algorithm hash-type of the hash, conforming to <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#messagedigest-algorithms">Java Standard Hashing Algorithm Names</a>
 * @author C0D3 M4513R
 */
public record Hash(String hash, String algorithm) {
}
