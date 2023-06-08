package app.revanced.patches.youtube.layout.shorts.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints.ShortsInfoPanelFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelPlayerInfoPanelId
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("hide-shorts-info-panel")
@YouTubeCompatibility
@Version("0.0.1")
class ShortsInfoPanelPatch : BytecodePatch(
    listOf(ShortsInfoPanelFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsInfoPanelFingerprint.result?.mutableMethod?.let {
            val insertIndex = it.getWideLiteralIndex(reelPlayerInfoPanelId) + 3
            val insertRegister = it.instruction<OneRegisterInstruction>(insertIndex).registerA

            it.addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerInfoPanel(Landroid/view/ViewGroup;)Landroid/view/ViewGroup;
                    move-result-object v$insertRegister
                    """
            )
        } ?: return ShortsInfoPanelFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
