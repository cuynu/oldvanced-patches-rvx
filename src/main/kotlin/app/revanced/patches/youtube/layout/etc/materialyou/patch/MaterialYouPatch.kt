package app.revanced.patches.youtube.layout.etc.materialyou.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.etc.theme.patch.GeneralThemePatch
import app.revanced.patches.youtube.layout.etc.theme.patch.GeneralThemePatch.Companion.isMonetPatchIncluded
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper.updatePatchStatusTheme
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch(false)
@Name("materialyou")
@Description("Enables MaterialYou theme for Android 12+")
@DependsOn(
    [
        GeneralThemePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class MaterialYouPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        arrayOf(
            ResourceUtils.ResourceGroup(
                "drawable-night-v31",
                "new_content_dot_background.xml"
            ),
            ResourceUtils.ResourceGroup(
                "drawable-v31",
                "new_content_count_background.xml",
                "new_content_dot_background.xml"
            ),
            ResourceUtils.ResourceGroup(
                "layout-v31",
                "new_content_count.xml"
            )
        ).forEach {
            context["res/${it.resourceDirectoryName}"].mkdirs()
            context.copyResources("youtube/materialyou", it)
        }

        context.copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        /**
         * Add settings
         */
        context.updatePatchStatusTheme("materialyou")

        isMonetPatchIncluded = true

        return PatchResultSuccess()
    }
}
