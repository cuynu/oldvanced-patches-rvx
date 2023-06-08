package app.revanced.patches.music.misc.tastebuilder.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.tastebuilder.fingerprints.TasteBuilderConstructorFingerprint
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-taste-builder")
@Description("Removes the \"Tell us which artists you like\" card from the home screen.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class RemoveTasteBuilderPatch : BytecodePatch(
    listOf(TasteBuilderConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        TasteBuilderConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex - 8
                val register = instruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        const/16 v1, 0x8
                        invoke-virtual {v$register, v1}, Landroid/view/View;->setVisibility(I)V
                        """
                )
            }
        } ?: return TasteBuilderConstructorFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
