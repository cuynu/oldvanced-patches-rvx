package app.revanced.patches.youtube.misc.timebar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.timebar.fingerprints.*

@Name("hook-timebar-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HookTimeBarPatch : BytecodePatch(
    listOf(EmptyColorFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        EmptyColorFingerprint.result?.let { parentResult ->
            emptyColorResult = parentResult
            emptyColorMethod = parentResult.mutableMethod
            OnDrawFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let {
                setTimeBarMethod = it
            } ?: return OnDrawFingerprint.toErrorResult()
        } ?: return EmptyColorFingerprint.toErrorResult()


        return PatchResultSuccess()
    }

    internal companion object {
        lateinit var emptyColorResult: MethodFingerprintResult
        lateinit var emptyColorMethod: MutableMethod
        lateinit var setTimeBarMethod: MutableMethod
    }
}