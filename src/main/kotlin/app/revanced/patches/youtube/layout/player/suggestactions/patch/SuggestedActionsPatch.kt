package app.revanced.patches.youtube.layout.player.suggestactions.patch

import app.revanced.extensions.injectHideCall
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.player.suggestactions.fingerprints.SuggestedActionsFingerprint
import app.revanced.patches.youtube.misc.litho.patch.ByteBufferFilterPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("hide-suggested-actions")
@Description("Hide the suggested actions bar inside the player.")
@DependsOn(
    [
        ByteBufferFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SuggestedActionsPatch : BytecodePatch(
    listOf(SuggestedActionsFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        SuggestedActionsFingerprint.result?.let{
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = instruction<OneRegisterInstruction>(targetIndex).registerA

                implementation!!.injectHideCall(targetIndex + 1, targetRegister, "layout/PlayerPatch", "hideSuggestedActions")
            }
        } ?: return SuggestedActionsFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_SUGGESTED_ACTION"
            )
        )

        SettingsPatch.updatePatchStatus("hide-suggested-actions")

        return PatchResultSuccess()
    }
}
