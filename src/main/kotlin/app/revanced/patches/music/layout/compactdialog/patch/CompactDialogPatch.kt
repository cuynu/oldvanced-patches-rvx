package app.revanced.patches.music.layout.compactdialog.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.layout.compactdialog.fingerprints.DialogSolidFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT


@Patch
@Name("enable-compact-dialog")
@Description("Enable compact dialog on phone.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class CompactDialogPatch : BytecodePatch(
    listOf(DialogSolidFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        DialogSolidFingerprint.result?.let {
            with(context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
            ) {
                addInstructions(
                    2, """
                        invoke-static {p0}, $MUSIC_LAYOUT->enableCompactDialog(I)I
                        move-result p0
                        """
                )
            }
        } ?: return DialogSolidFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.LAYOUT, "revanced_enable_compact_dialog", "true")

        return PatchResultSuccess()
    }
}
