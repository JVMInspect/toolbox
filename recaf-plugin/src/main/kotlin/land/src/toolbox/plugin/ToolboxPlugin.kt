package land.src.toolbox.plugin

import jakarta.enterprise.context.Dependent
import jakarta.inject.Inject
import land.src.toolbox.plugin.ui.overrides.RemoteVirtualMachinesWindowOverride
import software.coley.recaf.plugin.Plugin
import software.coley.recaf.plugin.PluginInformation
import software.coley.recaf.services.window.WindowManager
import software.coley.recaf.ui.menubar.MainMenuProvider
import software.coley.recaf.util.FxThreadUtil
import software.coley.recaf.workspace.model.resource.WorkspaceResource

@Dependent
@PluginInformation(
    id = "land.src.toolbox",
    version = "##VERSION##",
    name = "Toolbox",
    description = "JVM Toolbox - Attach-less Instrumentation"
)
class ToolboxPlugin @Inject constructor(
    private val windowManager: WindowManager,
    private val menuProvider: MainMenuProvider
) : Plugin {
    override fun onEnable() {

        //FxThreadUtil.run {
        //    val fileMenu = menuProvider.mainMenu.fileMenu
        //    val attachItem = fileMenu.items.first {
        //        it.id == "menu.file.attach"
        //    }
        //    attachItem.setOnAction {
        //        //val remoteVmWindow = RemoteVirtualMachinesWindowOverride()
        //        //remoteVmWindow.show()
        //        //remoteVmWindow.requestFocus()
        //    }
        //}
    }

    override fun onDisable() {
    }
}