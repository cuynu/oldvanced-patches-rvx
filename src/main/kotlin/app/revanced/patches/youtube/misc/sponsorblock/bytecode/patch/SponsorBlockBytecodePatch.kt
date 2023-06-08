package app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.misc.overridespeed.bytecode.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.misc.playercontrols.patch.PlayerControlsPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.insetOverlayViewLayoutId
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.totalTimeId
import app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.timebar.patch.HookTimeBarPatch
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.bytecode.getWideLiteralIndex
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.BuilderInstruction
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.MethodReference

@Name("sponsorblock-bytecode-patch")
@DependsOn(
    [
        HookTimeBarPatch::class,
        LegacyVideoIdPatch::class,
        MainstreamVideoIdPatch::class,
        OverrideSpeedHookPatch::class,
        PlayerControlsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SponsorBlockBytecodePatch : BytecodePatch(
    listOf(
        EndScreenEngagementPanelsFingerprint,
        OverlayViewLayoutFingerprint,
        PlayerControllerFingerprint,
        TotalTimeFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /**
         * Hook the video time methods
         */
        MainstreamVideoIdPatch.apply {
            videoTimeHook(
                INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR,
                "setVideoTime"
            )
            onCreateHook(
                INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR,
                "initialize"
            )
        }


        /**
         * Seekbar drawing
         */
        insertMethod = HookTimeBarPatch.setTimeBarMethod
        insertInstructions = insertMethod.implementation!!.instructions


        /**
         * Get the instance of the seekbar rectangle
         */
        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_DIRECT_RANGE) continue
            insertMethod.addInstruction(
                index,
                "invoke-static/range {p0 .. p0}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )
            break
        }

        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_STATIC) continue

            val invokeInstruction = insertMethod.instruction<Instruction35c>(index)
            if ((invokeInstruction.reference as MethodReference).name != "round") continue

            val insertIndex = index + 2

            insertMethod.addInstruction(
                insertIndex,
                "invoke-static {v${invokeInstruction.registerC}}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )
            break
        }

        /**
         * Set rectangle absolute left and right positions
         */
        val drawRectangleInstructions = insertInstructions.filter {
            it is ReferenceInstruction && (it.reference as? MethodReference)?.name == "drawRect" && it is FiveRegisterInstruction
        }.map { // TODO: improve code
            insertInstructions.indexOf(it) to (it as FiveRegisterInstruction).registerD
        }

        /**
         * Deprecated in YouTube v18.17.43+.
         * TODO: remove code from integrations
         */
        if (drawRectangleInstructions.size > 3) {
            mapOf(
                "setSponsorBarAbsoluteLeft" to 3,
                "setSponsorBarAbsoluteRight" to 0
            ).forEach { (string, int) ->
                val (index, register) = drawRectangleInstructions[int]
                injectCallRectangle(index, register, string)
            }
        }

        /**
         * Draw segment
         */
        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE) continue

            val invokeInstruction = instruction as BuilderInstruction3rc
            if ((invokeInstruction.reference as MethodReference).name != "restore") continue

            val drawSegmentInstructionInsertIndex = index - 1

            val (canvasInstance, centerY) =
                insertMethod.instruction<FiveRegisterInstruction>(drawSegmentInstructionInsertIndex).let { it.registerC to it.registerE }

            insertMethod.addInstruction(
                drawSegmentInstructionInsertIndex,
                "invoke-static {v$canvasInstance, v$centerY}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
            break
        }

        /**
         * Voting & Shield button
         */
        arrayOf("CreateSegmentButtonController", "VotingButtonController").forEach {
           PlayerControlsPatch.initializeSB("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/$it;")
           PlayerControlsPatch.injectVisibility("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/$it;")
        }

        EndScreenEngagementPanelsFingerprint.result?.mutableMethod?.let {
            it.addInstruction(
                it.implementation!!.instructions.size - 1,
                "invoke-static {}, $INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/SponsorBlockViewController;->endOfVideoReached()V"
            )
        }

        /**
         * Append the new time to the player layout
         */
        TotalTimeFingerprint.result?.mutableMethod?.let {
            it.apply {
                val targetIndex = getWideLiteralIndex(totalTimeId) + 2
                val targetRegister = instruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: return TotalTimeFingerprint.toErrorResult()

        /**
         * Initialize the SponsorBlock view
         */
        OverlayViewLayoutFingerprint.result?.mutableMethod?.let{
            it.apply{
                val targetIndex = getWideLiteralIndex(insetOverlayViewLayoutId) + 3
                val targetRegister = instruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/ui/SponsorBlockViewController;->initialize(Landroid/view/ViewGroup;)V"
                )
            }
        } ?: return OverlayViewLayoutFingerprint.toErrorResult()

        /**
         * Replace strings
         */
        PlayerControllerFingerprint.result?.mutableMethod?.let {
            val instructions = it.implementation!!.instructions

            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.CONST_STRING) continue
                val register = it.instruction<OneRegisterInstruction>(index).registerA
                it.replaceInstruction(
                    index,
                    "const-string v$register, \"${MainstreamVideoIdPatch.reactReference}\""
                )
                break
            }
        } ?: return PlayerControllerFingerprint.toErrorResult()

        /**
         * Inject VideoIdPatch
         */
        LegacyVideoIdPatch.injectCall("$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")

        context.injectInit("FirstRun", "initializationSB")
        context.updatePatchStatus("SponsorBlock")

        return PatchResultSuccess()
    }

    internal companion object {
        const val INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR =
            "Lapp/revanced/integrations/sponsorblock"

        const val INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR =
            "$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/SegmentPlaybackController;"

        lateinit var insertMethod: MutableMethod
        lateinit var insertInstructions: List<BuilderInstruction>

        /**
         * Adds an invoke-static instruction, called with the new id when the video changes
         * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
         */
        fun injectCallRectangle(
            insertIndex: Int,
            targetRegister: Int,
            methodDescriptor: String
        ) {
            insertMethod.addInstruction(
                insertIndex,
                "invoke-static {v$targetRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->$methodDescriptor(Landroid/graphics/Rect;)V"
            )
        }
    }
}