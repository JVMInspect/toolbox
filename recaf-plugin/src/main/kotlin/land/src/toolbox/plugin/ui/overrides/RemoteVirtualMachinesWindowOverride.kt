package land.src.toolbox.plugin.ui.overrides

import jakarta.enterprise.context.Dependent
import jakarta.inject.Inject
import software.coley.recaf.services.attach.AttachManager
import software.coley.recaf.services.attach.AttachManagerConfig
import software.coley.recaf.ui.window.AbstractIdentifiableStage

@Dependent
class RemoteVirtualMachinesWindowOverride @Inject constructor(
    val attachManager: AttachManager,
    val attachManagerConfig: AttachManagerConfig
)