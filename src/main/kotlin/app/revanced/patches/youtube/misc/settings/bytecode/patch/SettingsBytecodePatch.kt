package app.revanced.patches.youtube.misc.settings.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.misc.integrations.patch.IntegrationsPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.bytecode.fingerprints.ThemeSetterSystemFingerprint
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH

@Name("settings-bytecode-patch")
@DependsOn(
    [
        IntegrationsPatch::class,
        ResourceMappingPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SettingsBytecodePatch : BytecodePatch(
    listOf(ThemeSetterSystemFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // apply the current theme of the settings page
        ThemeSetterSystemFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                replaceInstruction(
                    targetIndex,
                    SET_THEME
                )
                addInstruction(
                    targetIndex + 1,
                    "return-object v0"
                )
                addInstruction(
                    this.implementation!!.instructions.size - 1,
                    SET_THEME
                )
            }
        } ?: return ThemeSetterSystemFingerprint.toErrorResult()

        context.injectInit("FirstRun", "initializationRVX")

        return PatchResultSuccess()
    }
    companion object {
        const val SET_THEME =
            "invoke-static {v0}, $INTEGRATIONS_PATH/utils/ThemeHelper;->setTheme(Ljava/lang/Object;)V"
    }
}
