package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.injectHideCall
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.adAttributionId
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Name("hide-general-ads-bytecode-patch")
@DependsOn([SharedResourceIdPatch::class])
@Version("0.0.1")
@Suppress("LABEL_NAME_CLASH")
class GeneralAdsBytecodePatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (!method.isWideLiteralExists(adAttributionId))
                    return@forEach

                context.proxy(classDef)
                    .mutableClass
                    .findMutableMethodOf(method)
                    .apply {
                        val insertIndex = method.getWideLiteralIndex(adAttributionId) + 1
                        if (instruction(insertIndex).opcode != org.jf.dexlib2.Opcode.INVOKE_VIRTUAL)
                            return@forEach

                        val viewRegister = instruction<Instruction35c>(insertIndex).registerC

                        this.implementation!!.injectHideCall(insertIndex, viewRegister, "ads/AdsFilter", "hideAdAttributionView")
                    }
            }
        }

        context.updatePatchStatus("GeneralAds")

        return PatchResultSuccess()
    }
}
