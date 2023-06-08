package app.revanced.patches.music.misc.upgradebutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isNarrowLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object PivotBarConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { it.name == "<init>" && it.isNarrowLiteralExists(117501096) }
)