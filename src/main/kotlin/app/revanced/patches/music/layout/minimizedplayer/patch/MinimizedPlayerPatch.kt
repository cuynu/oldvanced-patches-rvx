package app.revanced.patches.music.layout.minimizedplayer.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.layout.minimizedplayer.fingerprints.MinimizedPlayerFingerprint
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-force-minimized-player")
@Description("Permanently keep player minimized even if another track is played.")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class MinimizedPlayerPatch : BytecodePatch(
    listOf(MinimizedPlayerFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MinimizedPlayerFingerprint.result?.let {
            with (it.mutableMethod) {
                val index = it.scanResult.patternScanResult!!.endIndex
                val register = (implementation!!.instructions[index] as OneRegisterInstruction).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $MUSIC_LAYOUT->enableForceMinimizedPlayer(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: return MinimizedPlayerFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.LAYOUT, "revanced_enable_force_minimized_player", "true")

        return PatchResultSuccess()
    }
}