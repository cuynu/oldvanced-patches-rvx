package app.revanced.patches.youtube.misc.externalbrowser.patch

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
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.externalbrowser.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-external-browser")
@Description("Open url outside the app in an external browser.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ExternalBrowserPatch : BytecodePatch(
    listOf(
        ExternalBrowserPrimaryFingerprint,
        ExternalBrowserSecondaryFingerprint,
        ExternalBrowserTertiaryFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            ExternalBrowserPrimaryFingerprint,
            ExternalBrowserSecondaryFingerprint,
            ExternalBrowserTertiaryFingerprint
        ).forEach {
            val result = it.result?: return it.toErrorResult()
            it.result?.mutableMethod?.apply {
                val endIndex = result.scanResult.patternScanResult!!.endIndex
                val register = instruction<OneRegisterInstruction>(endIndex).registerA
                addInstructions(
                    endIndex + 1, """
                        invoke-static {v$register}, $MISC_PATH/ExternalBrowserPatch;->enableExternalBrowser(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_EXTERNAL_BROWSER"
            )
        )

        SettingsPatch.updatePatchStatus("enable-external-browser")

        return PatchResultSuccess()
    }
}