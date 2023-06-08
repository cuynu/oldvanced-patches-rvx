package app.revanced.patches.youtube.layout.player.watermark.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.player.watermark.fingerprints.HideWatermarkFingerprint
import app.revanced.patches.youtube.layout.player.watermark.fingerprints.HideWatermarkParentFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-channel-watermark")
@Description("Hides creator's watermarks on videos.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideChannelWatermarkBytecodePatch : BytecodePatch(
    listOf(HideWatermarkParentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HideWatermarkParentFingerprint.result?.let { parentResult ->
            HideWatermarkFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val register = instruction<TwoRegisterInstruction>(insertIndex).registerA

                    removeInstruction(insertIndex)
                    addInstructions(
                        insertIndex, """
                            invoke-static {}, $PLAYER->hideChannelWatermark()Z
                            move-result v$register
                            """
                    )
                }
            } ?: return HideWatermarkFingerprint.toErrorResult()
        } ?: return HideWatermarkParentFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_CHANNEL_WATERMARK"
            )
        )

        SettingsPatch.updatePatchStatus("hide-channel-watermark")

        return PatchResultSuccess()
    }
}
