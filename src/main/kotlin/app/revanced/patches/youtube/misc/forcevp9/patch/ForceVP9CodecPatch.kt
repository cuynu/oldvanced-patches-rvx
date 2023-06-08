package app.revanced.patches.youtube.misc.forcevp9.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.misc.forcevp9.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("force-vp9-codec")
@Description("Forces the VP9 codec for videos.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ForceVP9CodecPatch : BytecodePatch(
    listOf(
        LayoutSwitchFingerprint,
        VideoCapabilitiesParentFingerprint,
        Vp9PropsParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        LayoutSwitchFingerprint.result?.classDef?.let { classDef ->
            arrayOf(
                Vp9PrimaryFingerprint,
                Vp9SecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.also { it.resolve(context, classDef) }.result?.injectOverride() ?: return fingerprint.toErrorResult()
            }
        } ?: return LayoutSwitchFingerprint.toErrorResult()

        Vp9PropsParentFingerprint.result?.let { parentResult ->
            Vp9PropsFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let {
                mapOf(
                    "MANUFACTURER" to "getManufacturer",
                    "BRAND" to "getBrand",
                    "MODEL" to "getModel"
                ).forEach { (fieldName, descriptor) ->
                    it.hookProps(fieldName, descriptor)
                }
            } ?: return Vp9PropsFingerprint.toErrorResult()
        } ?: return Vp9PropsParentFingerprint.toErrorResult()

        VideoCapabilitiesParentFingerprint.result?.let { parentResult ->
            VideoCapabilitiesFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex

                    addInstructions(
                        insertIndex, """
                            invoke-static {p1}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMinHeight(I)I
                            move-result p1
                            invoke-static {p2}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMaxHeight(I)I
                            move-result p2
                            invoke-static {p3}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMinWidth(I)I
                            move-result p3
                            invoke-static {p4}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMaxWidth(I)I
                            move-result p4
                            """
                    )
                }
            } ?: return VideoCapabilitiesFingerprint.toErrorResult()
        } ?: return VideoCapabilitiesParentFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_VP9_CODEC"
            )
        )

        SettingsPatch.updatePatchStatus("force-vp9-codec")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$MISC_PATH/CodecOverridePatch;"

        const val INTEGRATIONS_CLASS_METHOD_REFERENCE =
            "$INTEGRATIONS_CLASS_DESCRIPTOR->shouldForceVP9(Z)Z"

        fun MethodFingerprintResult.injectOverride() {
            mutableMethod.apply {
                val startIndex = scanResult.patternScanResult!!.startIndex
                val endIndex = scanResult.patternScanResult!!.endIndex

                val startRegister = instruction<OneRegisterInstruction>(startIndex).registerA
                val endRegister = instruction<OneRegisterInstruction>(endIndex).registerA

                hookOverride(endIndex + 1, endRegister)
                removeInstruction(endIndex)
                hookOverride(startIndex + 1, startRegister)
                removeInstruction(startIndex)
            }
        }

        fun MutableMethod.hookOverride(
            index: Int,
            register: Int
        ) {
            addInstructions(
                index, """
                    invoke-static {v$register}, $INTEGRATIONS_CLASS_METHOD_REFERENCE
                    move-result v$register
                    return v$register
                """
            )
        }

        fun MutableMethod.hookProps(
            fieldName: String,
            descriptor: String
        ) {
            val insertInstructions = implementation!!.instructions
            val targetString = "Landroid/os/Build;->" +
                    fieldName +
                    ":Ljava/lang/String;"

            for ((index, instruction) in insertInstructions.withIndex()) {
                if (instruction.opcode != Opcode.SGET_OBJECT) continue

                val indexString = ((instruction as? ReferenceInstruction)?.reference as? DexBackedFieldReference).toString()

                if (indexString != targetString) continue

                val register = instruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->$descriptor(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
                )
                break
            }
        }
    }

}
