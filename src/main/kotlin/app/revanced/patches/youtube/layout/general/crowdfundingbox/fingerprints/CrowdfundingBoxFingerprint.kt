package app.revanced.patches.youtube.layout.general.crowdfundingbox.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.donationCompanionResourceId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object CrowdfundingBoxFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT
    ),
    customFingerprint = { it.isWideLiteralExists(donationCompanionResourceId) }
)