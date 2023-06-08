package app.revanced.patches.youtube.layout.player.infocards.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object InfocardsIncognitoFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("vibrator")
)