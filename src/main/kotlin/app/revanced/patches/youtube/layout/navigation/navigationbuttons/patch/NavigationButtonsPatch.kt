package app.revanced.patches.youtube.layout.navigation.navigationbuttons.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.layout.navigation.navigationbuttons.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import app.revanced.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.pivotbar.InjectionUtils.injectHook
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("hide-navigation-buttons")
@Description("Adds options to hide or change navigation buttons.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class NavigationButtonsPatch : BytecodePatch(
    listOf(
        AutoMotiveFingerprint,
        PivotBarCreateButtonViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PivotBarCreateButtonViewFingerprint.result?.let { parentResult ->

            /**
             * Home, Shorts, Subscriptions Button
             */
            with (
                arrayOf(
                    PivotBarEnumFingerprint,
                    PivotBarShortsButtonViewFingerprint
                ).onEach {
                    it.resolve(
                        context,
                        parentResult.mutableMethod,
                        parentResult.mutableClass
                    )
                }.map {
                    it.result?.scanResult?.patternScanResult ?: return it.toErrorResult()
                }
            ) {
                val enumScanResult = this[0]
                val buttonViewResult = this[1]

                val enumHookInsertIndex = enumScanResult.startIndex + 2
                val buttonHookInsertIndex = buttonViewResult.endIndex

                mapOf(
                    buttonHook to buttonHookInsertIndex,
                    enumHook to enumHookInsertIndex
                ).forEach { (hook, insertIndex) ->
                    parentResult.mutableMethod.injectHook(hook, insertIndex)
                }
            }

            /**
             * Create Button
             */
            with (parentResult.mutableMethod) {
                val insertIndex = implementation!!.instructions.let {
                    val scanStart = parentResult.scanResult.patternScanResult!!.endIndex

                    scanStart + it.subList(scanStart, it.size - 1).indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_STATIC
                    }
                }
                injectHook(createButtonHook, insertIndex)
            }

        } ?: return PivotBarCreateButtonViewFingerprint.toErrorResult()

        /**
         * Switch create button with notifications button
         */
        AutoMotiveFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = instruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $NAVIGATION->switchCreateNotification(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: return AutoMotiveFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_NAVIGATION_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-navigation-buttons")

        return PatchResultSuccess()
    }
    private companion object {
        const val enumHook =
            "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $NAVIGATION" +
            "->" +
            "lastPivotTab:Ljava/lang/Enum;"

        const val buttonHook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
            "->" +
            "hideNavigationButton(Landroid/view/View;)V"

        const val createButtonHook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
            "->" +
            "hideCreateButton(Landroid/view/View;)V"
    }
}