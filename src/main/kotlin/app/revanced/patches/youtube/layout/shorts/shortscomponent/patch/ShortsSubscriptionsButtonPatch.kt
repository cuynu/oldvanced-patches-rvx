package app.revanced.patches.youtube.layout.shorts.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelPlayerFooterId
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelPlayerPausedId
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Name("hide-shorts-subscriptions")
@YouTubeCompatibility
@Version("0.0.1")
class ShortsSubscriptionsButtonPatch : BytecodePatch(
    listOf(
        ShortsSubscriptionsFingerprint,
        ShortsSubscriptionsTabletParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsSubscriptionsFingerprint.result?.mutableMethod?.let {
            val insertIndex = it.getWideLiteralIndex(reelPlayerPausedId) + 2
            val insertRegister = it.instruction<OneRegisterInstruction>(insertIndex).registerA

            it.addInstruction(
                insertIndex + 1,
                "invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerSubscriptionsButton(Landroid/view/View;)V"
            )
        } ?: return ShortsSubscriptionsFingerprint.toErrorResult()

        ShortsSubscriptionsTabletParentFingerprint.result?.let { parentResult ->
            parentResult.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(reelPlayerFooterId) - 1
                if (instruction(targetIndex).opcode != Opcode.IPUT) return ShortsSubscriptionsTabletFingerprint.toErrorResult()
                subscriptionFieldReference = (instruction<ReferenceInstruction>(targetIndex)).reference as FieldReference
            }

            ShortsSubscriptionsTabletFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let {
                with (it.implementation!!.instructions) {
                    filter { instruction ->
                        val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        instruction.opcode == Opcode.IGET && fieldReference == subscriptionFieldReference
                    }.forEach { instruction ->
                        val insertIndex = indexOf(instruction) + 1
                        val register = (instruction as TwoRegisterInstruction).registerA

                        it.addInstructions(
                            insertIndex,"""
                                invoke-static {v$register}, $SHORTS->hideShortsPlayerSubscriptionsButton(I)I
                                move-result v$register
                                """
                        )
                    }
                }
            } ?: return ShortsSubscriptionsTabletFingerprint.toErrorResult()
        } ?: return ShortsSubscriptionsTabletParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object {
        private lateinit var subscriptionFieldReference: FieldReference
    }
}
