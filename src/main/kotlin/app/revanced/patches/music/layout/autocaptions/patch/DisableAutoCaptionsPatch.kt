package app.revanced.patches.music.layout.autocaptions.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.fingerprints.SubtitleTrackFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("disable-auto-captions")
@Description("Disable forced captions from automatically enabling in video player.")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class DisableAutoCaptionsPatch : BytecodePatch(
    listOf(SubtitleTrackFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SubtitleTrackFingerprint.result?.mutableMethod?.let {
            val index = it.implementation!!.instructions.size - 1
            val register = it.instruction<OneRegisterInstruction>(index).registerA
            it.addInstructions(
                index, """
                    invoke-static {v$register}, $MUSIC_LAYOUT->disableAutoCaptions(Z)Z
                    move-result v$register
                """
            )
        } ?: return SubtitleTrackFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.LAYOUT, "revanced_disable_auto_captions", "false")

        return PatchResultSuccess()
    }
}