package app.revanced.patches.youtube.video.hdr.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.video.hdr.fingerprints.HdrCapabilitiesFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH

@Patch
@Name("disable-hdr-video")
@Description("Disable HDR video.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class DisableHdrVideoPatch : BytecodePatch(
    listOf(
        HdrCapabilitiesFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HdrCapabilitiesFingerprint.result?.let {
            with (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
            ) {
                addInstructions(
                    0,
                    """
                        invoke-static {}, $VIDEO_PATH/HDRVideoPatch;->disableHDRVideo()Z
                        move-result v0
                        if-nez v0, :default
                        return v0
                    """, listOf(ExternalLabel("default", instruction(0)))
                )
            }
        } ?: return HdrCapabilitiesFingerprint.toErrorResult()

        /*
 * Add settings
 */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: DISABLE_HDR_VIDEO"
            )
        )

        SettingsPatch.updatePatchStatus("disable-hdr-video")

        return PatchResultSuccess()
    }
}
