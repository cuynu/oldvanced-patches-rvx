package app.revanced.patches.youtube.misc.overridespeed.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.*
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.overridespeed.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.overridespeed.resource.patch.OverrideSpeedHookResourcePatch
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.immutable.*

@Name("override-speed-hook")
@DependsOn([OverrideSpeedHookResourcePatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class OverrideSpeedHookPatch : BytecodePatch(
    listOf(
        SpeedClassFingerprint,
        VideoSpeedPatchFingerprint,
        VideoSpeedParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        VideoSpeedParentFingerprint.result?.let { parentResult ->
            val parentClassDef = parentResult.classDef

            VideoSpeedChangedFingerprint.also { it.resolve(context, parentClassDef) }.result?.let {
                it.mutableMethod.apply {
                    videoSpeedChangedResult = it
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val endIndex = it.scanResult.patternScanResult!!.endIndex

                    val reference1 = instruction<ReferenceInstruction>(startIndex).reference
                    val reference2 = instruction<ReferenceInstruction>(endIndex - 1).reference
                    val reference3 = instruction<ReferenceInstruction>(endIndex).reference
                    val fieldReference = reference2 as FieldReference

                    val parentMutableClass = parentResult.mutableClass

                    parentMutableClass.methods.add(
                        ImmutableMethod(
                            parentMutableClass.type,
                            "overrideSpeed",
                            listOf(ImmutableMethodParameter("F", null, null)),
                            "V",
                            AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                            null,
                            null,
                            ImmutableMethodImplementation(
                                4, """
                                    const/4 v0, 0x0
                                    cmpg-float v0, v3, v0
                                    if-lez v0, :cond_0
                                    iget-object v0, v2, $reference1
                                    check-cast v0, ${fieldReference.definingClass}
                                    iget-object v1, v0, $reference2
                                    invoke-virtual {v1, v3}, $reference3
                                    :cond_0
                                    return-void
                                    """.toInstructions(), null, null
                            )
                        ).toMutable()
                    )

                    with(context
                        .toMethodWalker(this)
                        .nextMethod(endIndex, true)
                        .getMethod() as MutableMethod
                    ) {
                        addInstruction(
                            this.implementation!!.instructions.size - 1,
                            "sput p1, $INTEGRATIONS_VIDEO_HELPER_CLASS_DESCRIPTOR->currentSpeed:F"
                        )
                    }
                }

            } ?: return VideoSpeedChangedFingerprint.toErrorResult()
        } ?: return VideoSpeedParentFingerprint.toErrorResult()


        SpeedClassFingerprint.result?.let {
            it.mutableMethod.apply {
                val index = it.scanResult.patternScanResult!!.endIndex
                val register = instruction<OneRegisterInstruction>(index).registerA
                SPEED_CLASS = this.returnType
                replaceInstruction(
                    index,
                    "sput-object v$register, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->speedClass:$SPEED_CLASS"
                )
                addInstruction(
                    index + 1,
                    "return-object v$register"
                )
            }

        } ?: return SpeedClassFingerprint.toErrorResult()

        VideoSpeedPatchFingerprint.result?.let {
            it.mutableMethod.apply {
                it.mutableClass.staticFields.add(
                    ImmutableField(
                        definingClass,
                        "speedClass",
                        SPEED_CLASS,
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        null,
                        null
                    ).toMutable()
                )

                addInstructions(
                    0, """
                        sget-object v0, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->speedClass:$SPEED_CLASS
                        invoke-virtual {v0, p0}, $SPEED_CLASS->overrideSpeed(F)V
                    """
                )
            }

        } ?: return VideoSpeedPatchFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedPatch;"

        const val INTEGRATIONS_VIDEO_HELPER_CLASS_DESCRIPTOR =
            "$INTEGRATIONS_PATH/utils/VideoHelpers;"

        lateinit var videoSpeedChangedResult: MethodFingerprintResult

        private lateinit var SPEED_CLASS: String
    }
}