package app.revanced.patches.music.layout.branding.icon.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.resources.IconHelper.customIconMusic
import app.revanced.util.resources.IconHelper.customIconMusicAdditional

@Patch(false)
@Name("custom-branding-music-afn-blue")
@Description("Changes the YouTube Music launcher icon to Afn Blue.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class CustomBrandingMusicIconAfnBluePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        context.customIconMusic("afn-blue")
        context.customIconMusicAdditional("afn-blue")

        return PatchResultSuccess()
    }

}
