package app.revanced.patches.music.layout.blacknavbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch.Companion.colorGreyId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object TabLayoutFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = { it.isWideLiteralExists(colorGreyId) }
)

